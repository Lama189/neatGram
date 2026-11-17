package org.telegram.neatgram.ghost;

import android.os.SystemClock;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for NeatGram Ghost Mode features.
 * Encapsulates read-suppression, temporary read permissions on user actions,
 * auto-offline forcing, typing status blocking, and schedule helpers.
 */
public class NeatGramGhostController {

    private static final NeatGramGhostController[] instances = new NeatGramGhostController[UserConfig.MAX_ACCOUNT_COUNT];

    private final int currentAccount;
    private final ConcurrentHashMap<Long, Long> allowedReadOnceDialogs = new ConcurrentHashMap<>();

    private NeatGramGhostController(int currentAccount) {
        this.currentAccount = currentAccount;
    }

    public static NeatGramGhostController getInstance(int currentAccount) {
        if (currentAccount < 0 || currentAccount >= UserConfig.MAX_ACCOUNT_COUNT) {
            currentAccount = UserConfig.selectedAccount;
        }
        synchronized (instances) {
            NeatGramGhostController instance = instances[currentAccount];
            if (instance == null) {
                instance = new NeatGramGhostController(currentAccount);
                instances[currentAccount] = instance;
            }
            return instance;
        }
    }

    /**
     * Grants a temporary 5-second window for read history and content requests to go through
     * for the specified dialog (used after user actions like message send, reaction, poll vote).
     */
    public void allowReadOnceForDialog(long dialogId) {
        allowedReadOnceDialogs.put(dialogId, SystemClock.elapsedRealtime() + 5000);
    }

    /**
     * Checks if a temporary read permission is active for this dialog.
     */
    public boolean isReadOnceAllowed(long dialogId) {
        Long until = allowedReadOnceDialogs.get(dialogId);
        if (until != null) {
            if (SystemClock.elapsedRealtime() <= until) {
                return true;
            }
            allowedReadOnceDialogs.remove(dialogId, until);
        }
        return false;
    }

    /**
     * Whether history reading RPCs (TL_messages_readHistory, TL_channels_readHistory, etc.)
     * should be blocked from going over the network.
     */
    public boolean shouldBlockReadHistory(long dialogId) {
        if (!NeatGramConfig.isGhostModeEnabled(currentAccount) || !NeatGramConfig.isGhostDontReadMessages(currentAccount)) {
            return false;
        }
        return !isReadOnceAllowed(dialogId);
    }

    /**
     * Whether stories reading RPCs should be blocked.
     */
    public boolean shouldBlockReadStories() {
        return NeatGramConfig.isGhostModeEnabled(currentAccount) && NeatGramConfig.isGhostDontReadStories(currentAccount);
    }

    /**
     * Whether sending online status in TL_account_updateStatus should be blocked (substituted with offline=true).
     */
    public boolean shouldBlockSendOnline() {
        return NeatGramConfig.isGhostModeEnabled(currentAccount) && NeatGramConfig.isGhostDontSendOnline(currentAccount);
    }

    /**
     * Whether typing status RPCs should be blocked.
     */
    public boolean shouldBlockTyping() {
        return NeatGramConfig.isGhostModeEnabled(currentAccount) && NeatGramConfig.isGhostDontSendTyping(currentAccount);
    }

    /**
     * Whether messages without explicit schedule should be automatically scheduled (+10 sec).
     */
    public boolean shouldUseSchedule() {
        return NeatGramConfig.isGhostModeEnabled(currentAccount) && NeatGramConfig.isGhostUseSchedule(currentAccount);
    }

    /**
     * Hook called on user actions in a chat (sending message, reaction, voting, callback query).
     * Handles "Read after action" and "Auto offline".
     */
    public static void onUserChatAction(int currentAccount, long dialogId) {
        getInstance(currentAccount).handleUserChatAction(dialogId);
    }

    public void handleUserChatAction(long dialogId) {
        if (!NeatGramConfig.isGhostModeEnabled(currentAccount)) {
            return;
        }

        if (NeatGramConfig.isGhostReadAfterAction(currentAccount)) {
            allowReadOnceForDialog(dialogId);
            sendForcedReadHistory(dialogId);
        }

        if (NeatGramConfig.isGhostAutoOffline(currentAccount)) {
            sendForcedOfflineStatus();
        }
    }

    /**
     * Forces sending a read request to the server for the current top message of the dialog.
     */
    public void sendForcedReadHistory(long dialogId) {
        if (dialogId == 0 || DialogObject.isEncryptedDialog(dialogId)) {
            return;
        }
        MessagesController mc = MessagesController.getInstance(currentAccount);
        TLRPC.Dialog dialog = mc.dialogs_dict.get(dialogId);
        int maxId = 0;
        if (dialog != null) {
            maxId = dialog.top_message;
        }
        Integer inboxMax = mc.dialogs_read_inbox_max.get(dialogId);
        if (inboxMax != null && inboxMax > maxId) {
            maxId = inboxMax;
        }
        if (maxId <= 0) {
            return;
        }
        TLRPC.InputPeer inputPeer = mc.getInputPeer(dialogId);
        if (inputPeer == null) {
            return;
        }
        TLObject req;
        if (inputPeer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.TL_channels_readHistory request = new TLRPC.TL_channels_readHistory();
            request.channel = mc.getInputChannel(-dialogId);
            request.max_id = maxId;
            req = request;
        } else {
            TLRPC.TL_messages_readHistory request = new TLRPC.TL_messages_readHistory();
            request.peer = inputPeer;
            request.max_id = maxId;
            req = request;
        }
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
    }

    /**
     * Forces immediate updateStatus with offline = true.
     */
    public void sendForcedOfflineStatus() {
        TL_account.updateStatus req = new TL_account.updateStatus();
        req.offline = true;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
    }
}
