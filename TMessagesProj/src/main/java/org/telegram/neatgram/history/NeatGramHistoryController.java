package org.telegram.neatgram.history;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for lightweight Copy-on-Delete and Copy-on-Edit functionality.
 * Maintains zero extra database overhead by keeping flat tables inside cache4.db
 * and an in-memory cache for 120Hz smooth scrolling without disk I/O on UI thread.
 */
public class NeatGramHistoryController {

    private static final NeatGramHistoryController[] instances = new NeatGramHistoryController[UserConfig.MAX_ACCOUNT_COUNT];

    public static NeatGramHistoryController getInstance(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            account = UserConfig.selectedAccount;
        }
        NeatGramHistoryController instance = instances[account];
        if (instance == null) {
            synchronized (NeatGramHistoryController.class) {
                instance = instances[account];
                if (instance == null) {
                    instances[account] = instance = new NeatGramHistoryController(account);
                }
            }
        }
        return instance;
    }

    private final int currentAccount;

    // RAM caches for active dialogs: dialogId -> set of message IDs
    private final ConcurrentHashMap<Long, Set<Integer>> deletedMessagesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Set<Integer>> editedMessagesCache = new ConcurrentHashMap<>();

    // Short-lived pending deletions: dialogId -> set of message IDs marked for preservation
    private final ConcurrentHashMap<Long, Set<Integer>> pendingDeletions = new ConcurrentHashMap<>();
    private final Set<Integer> userDeletedMids = ConcurrentHashMap.newKeySet();
    private static final Set<String> protectedFileNames = ConcurrentHashMap.newKeySet();

    private NeatGramHistoryController(int account) {
        this.currentAccount = account;
    }

    /**
     * Initializes tables inside the existing cache4.db.
     */
    public void initDatabase(SQLiteDatabase database) {
        if (database == null) {
            return;
        }
        try {
            database.executeFast("CREATE TABLE IF NOT EXISTS neatgram_deleted(" +
                    "dialog_id INTEGER, " +
                    "mid INTEGER, " +
                    "deleted_date INTEGER, " +
                    "PRIMARY KEY(dialog_id, mid)" +
                    ");").stepThis().dispose();

            database.executeFast("CREATE TABLE IF NOT EXISTS neatgram_edits(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "dialog_id INTEGER, " +
                    "mid INTEGER, " +
                    "edit_date INTEGER, " +
                    "old_text TEXT, " +
                    "old_entities BLOB" +
                    ");").stepThis().dispose();

            database.executeFast("CREATE INDEX IF NOT EXISTS idx_neatgram_edits_dialog_mid " +
                    "ON neatgram_edits(dialog_id, mid);").stepThis().dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // --- RAM Cache Management ---

    public boolean isMessageDeleted(MessageObject messageObject) {
        if (messageObject == null) {
            return false;
        }
        return isMessageDeleted(messageObject.getDialogId(), messageObject.getId());
    }

    public boolean isMessageDeleted(long dialogId, int mid) {
        Set<Integer> set = deletedMessagesCache.get(dialogId);
        return set != null && set.contains(mid);
    }

    public boolean hasEdits(long dialogId, int mid) {
        Set<Integer> set = editedMessagesCache.get(dialogId);
        return set != null && set.contains(mid);
    }

    public void addDeletedToCache(long dialogId, int mid) {
        Set<Integer> set = deletedMessagesCache.computeIfAbsent(dialogId, k -> ConcurrentHashMap.newKeySet());
        set.add(mid);
    }

    public void removeDeletedFromCache(long dialogId, int mid) {
        Set<Integer> set = deletedMessagesCache.get(dialogId);
        if (set != null) {
            set.remove(mid);
        }
    }

    public void addEditToCache(long dialogId, int mid) {
        Set<Integer> set = editedMessagesCache.computeIfAbsent(dialogId, k -> ConcurrentHashMap.newKeySet());
        set.add(mid);
    }

    /**
     * Asynchronously loads history for the dialog on storageQueue (no disk I/O on UI thread).
     */
    public void loadDialogHistory(long dialogId, Runnable onLoaded) {
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            SQLiteDatabase db = MessagesStorage.getInstance(currentAccount).getDatabase();
            if (db == null) {
                return;
            }
            Set<Integer> deleted = ConcurrentHashMap.newKeySet();
            Set<Integer> edits = ConcurrentHashMap.newKeySet();
            SQLiteCursor cursor = null;
            try {
                cursor = db.queryFinalized("SELECT mid FROM neatgram_deleted WHERE dialog_id = " + dialogId);
                while (cursor.next()) {
                    deleted.add(cursor.intValue(0));
                }
                cursor.dispose();
                cursor = null;

                cursor = db.queryFinalized("SELECT DISTINCT mid FROM neatgram_edits WHERE dialog_id = " + dialogId);
                while (cursor.next()) {
                    edits.add(cursor.intValue(0));
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            deletedMessagesCache.put(dialogId, deleted);
            editedMessagesCache.put(dialogId, edits);

            if (onLoaded != null) {
                AndroidUtilities.runOnUIThread(onLoaded);
            }
        });
    }

    /**
     * Clears memory cache when chat is destroyed.
     */
    public void releaseDialogHistory(long dialogId) {
        deletedMessagesCache.remove(dialogId);
        editedMessagesCache.remove(dialogId);
        pendingDeletions.remove(dialogId);
    }

    // --- Copy-on-Delete Handling ---

    public void markPendingDeletions(long dialogId, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        Set<Integer> set = pendingDeletions.computeIfAbsent(dialogId, k -> ConcurrentHashMap.newKeySet());
        for (int i = 0, size = mids.size(); i < size; i++) {
            Integer mid = mids.get(i);
            if (mid != null && !userDeletedMids.contains(mid)) {
                set.add(mid);
            }
        }
    }

    public void onUserDeleteMessages(long dialogId, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        userDeletedMids.addAll(mids);
        if (userDeletedMids.size() > 2000) {
            java.util.Iterator<Integer> it = userDeletedMids.iterator();
            while (it.hasNext() && userDeletedMids.size() > 1000) {
                it.next();
                it.remove();
            }
        }
        removePendingDeletions(dialogId, mids);
    }

    public void removePendingDeletions(long dialogId, List<Integer> mids) {
        if (mids == null || mids.isEmpty()) {
            return;
        }
        Set<Integer> set = pendingDeletions.get(dialogId);
        if (set != null) {
            set.removeAll(mids);
        }
        Set<Integer> setZero = pendingDeletions.get(0L);
        if (setZero != null) {
            setZero.removeAll(mids);
        }
    }

    public boolean promotePendingDeletionToCache(long dialogId, int mid, boolean isOutgoing) {
        if (isOutgoing) {
            Set<Integer> set = pendingDeletions.get(dialogId);
            if (set != null) {
                set.remove(mid);
            }
            Set<Integer> setZero = pendingDeletions.get(0L);
            if (setZero != null) {
                setZero.remove(mid);
            }
            return false;
        }
        Set<Integer> deletedSet = deletedMessagesCache.get(dialogId);
        if (deletedSet != null && deletedSet.contains(mid)) {
            return true;
        }
        if (NeatGramConfig.isSaveDeletedMessages(currentAccount)) {
            if (userDeletedMids.contains(mid)) {
                return false;
            }
            Set<Integer> pendingSet = pendingDeletions.get(dialogId);
            boolean isPending = (pendingSet != null && pendingSet.contains(mid));
            if (!isPending) {
                Set<Integer> pendingSetZero = pendingDeletions.get(0L);
                isPending = (pendingSetZero != null && pendingSetZero.contains(mid));
            }
            if (isPending) {
                addDeletedToCache(dialogId, mid);
                return true;
            }
        }
        return false;
    }

    public boolean shouldPreserveDeleted(long dialogId, int mid) {
        if (!NeatGramConfig.isSaveDeletedMessages(currentAccount)) {
            return false;
        }
        if (userDeletedMids.contains(mid)) {
            return false;
        }
        if (isMessageDeleted(dialogId, mid)) {
            return true;
        }
        // Channel check
        if (dialogId < 0 && !NeatGramConfig.isSaveHistoryInChannels(currentAccount)) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null && ChatObject.isChannelAndNotMegaGroup(chat) && ChatObject.isPublic(chat)) {
                return false;
            }
        }
        Set<Integer> set = pendingDeletions.get(dialogId);
        if (set != null && set.contains(mid)) {
            return true;
        }
        // Also check if marked under dialogId 0 for non-channel messages
        Set<Integer> setZero = pendingDeletions.get(0L);
        return setZero != null && setZero.contains(mid);
    }

    public void recordDeleted(long dialogId, int mid, int date) {
        addDeletedToCache(dialogId, mid);
        SQLiteDatabase db = MessagesStorage.getInstance(currentAccount).getDatabase();
        if (db == null) {
            return;
        }
        try {
            SQLitePreparedStatement state = db.executeFast("INSERT OR REPLACE INTO neatgram_deleted VALUES(?, ?, ?)");
            state.requery();
            state.bindLong(1, dialogId);
            state.bindInteger(2, mid);
            state.bindInteger(3, date);
            state.step();
            state.dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void protectTtlMedia(MessageObject msg) {
        if (msg == null || msg.messageOwner == null) {
            return;
        }
        if (!NeatGramConfig.isSaveTtlMedia(currentAccount)) {
            return;
        }
        TLRPC.Message message = msg.messageOwner;
        boolean isTtl = msg.isSecretMedia() || msg.isVoiceOnce() || msg.isRoundOnce() || message.ttl > 0 || (message.media != null && message.media.ttl_seconds > 0);
        if (!isTtl) {
            return;
        }
        protectMedia(message);
    }

    public void protectTtlMedia(TLRPC.Message message) {
        if (message == null) {
            return;
        }
        if (!NeatGramConfig.isSaveTtlMedia(currentAccount)) {
            return;
        }
        boolean isTtl = message.ttl > 0 || (message.media != null && message.media.ttl_seconds > 0);
        if (!isTtl) {
            return;
        }
        protectMedia(message);
    }

    public void protectMedia(TLRPC.Message message) {
        if (message == null) {
            return;
        }
        try {
            if (!TextUtils.isEmpty(message.attachPath)) {
                File attachFile = new File(message.attachPath);
                if (attachFile.exists()) {
                    protectMediaFile(attachFile);
                }
            }
            File mainCacheFile = FileLoader.getInstance(currentAccount).getPathToMessage(message, true, false);
            File mainStandardFile = FileLoader.getInstance(currentAccount).getPathToMessage(message, false, false);
            protectAndSyncFiles(mainCacheFile, mainStandardFile);

            TLRPC.Photo photo = MessageObject.getPhoto(message);
            if (photo != null && photo.sizes != null) {
                for (int a = 0, N = photo.sizes.size(); a < N; a++) {
                    TLRPC.PhotoSize size = photo.sizes.get(a);
                    if (size == null) continue;
                    File cacheFile = FileLoader.getInstance(currentAccount).getPathToAttach(size, null, true, false);
                    File standardFile = FileLoader.getInstance(currentAccount).getPathToAttach(size, null, false, false);
                    protectAndSyncFiles(cacheFile, standardFile);
                }
            }
            TLRPC.Document doc = MessageObject.getDocument(message);
            if (doc != null) {
                File cacheFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, null, true, false);
                File standardFile = FileLoader.getInstance(currentAccount).getPathToAttach(doc, null, false, false);
                protectAndSyncFiles(cacheFile, standardFile);
                if (doc.thumbs != null) {
                    for (int a = 0, N = doc.thumbs.size(); a < N; a++) {
                        TLRPC.PhotoSize thumb = doc.thumbs.get(a);
                        if (thumb == null) continue;
                        File cacheThumb = FileLoader.getInstance(currentAccount).getPathToAttach(thumb, null, true, false);
                        File standardThumb = FileLoader.getInstance(currentAccount).getPathToAttach(thumb, null, false, false);
                        protectAndSyncFiles(cacheThumb, standardThumb);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void protectAndSyncFiles(File cacheFile, File standardFile) {
        File existingSource = null;
        if (cacheFile != null && cacheFile.exists()) {
            existingSource = cacheFile;
        } else if (standardFile != null && standardFile.exists()) {
            existingSource = standardFile;
        } else {
            String name = cacheFile != null ? cacheFile.getName() : (standardFile != null ? standardFile.getName() : null);
            if (!TextUtils.isEmpty(name)) {
                File fallback = getMediaFallbackFile(name);
                if (fallback != null && fallback.exists()) {
                    existingSource = fallback;
                }
            }
        }

        if (existingSource != null) {
            protectMediaFile(existingSource);
            if (standardFile != null && !standardFile.exists()) {
                duplicateFile(existingSource, standardFile);
            }
            if (cacheFile != null && !cacheFile.exists()) {
                duplicateFile(existingSource, cacheFile);
            }
        }

        File encCache = cacheFile != null ? new File(cacheFile.getAbsolutePath() + ".enc") : null;
        File encStandard = standardFile != null ? new File(standardFile.getAbsolutePath() + ".enc") : null;
        File existingEnc = null;
        if (encCache != null && encCache.exists()) {
            existingEnc = encCache;
        } else if (encStandard != null && encStandard.exists()) {
            existingEnc = encStandard;
        } else {
            String encName = (cacheFile != null ? cacheFile.getName() : (standardFile != null ? standardFile.getName() : "")) + ".enc";
            File fallbackEnc = getMediaFallbackFile(encName);
            if (fallbackEnc != null && fallbackEnc.exists()) {
                existingEnc = fallbackEnc;
            }
        }

        if (existingEnc != null) {
            protectMediaFile(existingEnc);
            if (encStandard != null && !encStandard.exists()) {
                duplicateFile(existingEnc, encStandard);
            }
            if (encCache != null && !encCache.exists()) {
                duplicateFile(existingEnc, encCache);
            }
            String baseName = cacheFile != null ? cacheFile.getName() : (standardFile != null ? standardFile.getName() : null);
            if (!TextUtils.isEmpty(baseName)) {
                File keyFile = new File(FileLoader.getInternalCacheDir(), baseName + ".enc.key");
                if (!keyFile.exists()) {
                    File fallbackKey = getMediaFallbackFile(baseName + ".enc.key");
                    if (fallbackKey != null && fallbackKey.exists()) {
                        duplicateFile(fallbackKey, keyFile);
                    }
                }
                if (keyFile.exists()) {
                    protectMediaFile(keyFile);
                    if (standardFile != null && !standardFile.exists()) {
                        decryptEncryptedFile(existingEnc, keyFile, standardFile);
                    }
                    if (cacheFile != null && !cacheFile.exists()) {
                        decryptEncryptedFile(existingEnc, keyFile, cacheFile);
                    }
                }
            }
        }
    }

    public boolean handleTtlMediaEmptying(TLRPC.Message message) {
        if (!NeatGramConfig.isSaveTtlMedia(currentAccount)) {
            return false;
        }
        if (message == null || message.media == null) {
            return false;
        }
        boolean hasTtl = message.ttl > 0 || message.media.ttl_seconds > 0;
        if (!hasTtl) {
            return false;
        }
        protectMedia(message);
        message.media.ttl_seconds = 0;
        message.ttl = 0;
        message.destroyTime = 0;
        message.destroyTimeMillis = 0;
        protectMedia(message);
        long dialogId = MessageObject.getDialogId(message);
        int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        recordDeleted(dialogId, message.id, currentTime);
        return true;
    }

    public boolean handleTtlMediaEdit(TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        if (!NeatGramConfig.isSaveTtlMedia(currentAccount)) {
            return false;
        }
        if (oldMessage == null || newMessage == null) {
            return false;
        }
        boolean oldHasTtl = (oldMessage.ttl > 0) || (oldMessage.media != null && oldMessage.media.ttl_seconds > 0);
        boolean newIsEmpty = newMessage.media == null ||
                             newMessage.media instanceof TLRPC.TL_messageMediaEmpty ||
                             (newMessage.media instanceof TLRPC.TL_messageMediaPhoto && (newMessage.media.photo == null || newMessage.media.photo instanceof TLRPC.TL_photoEmpty)) ||
                             (newMessage.media instanceof TLRPC.TL_messageMediaDocument && (newMessage.media.document == null || newMessage.media.document instanceof TLRPC.TL_documentEmpty));
        if (oldHasTtl && newIsEmpty) {
            protectMedia(oldMessage);
            newMessage.media = oldMessage.media;
            if (newMessage.media != null) {
                newMessage.media.ttl_seconds = 0;
            }
            newMessage.ttl = 0;
            newMessage.destroyTime = 0;
            newMessage.destroyTimeMillis = 0;
            protectMedia(newMessage);
            long dialogId = MessageObject.getDialogId(newMessage);
            int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
            recordDeleted(dialogId, newMessage.id, currentTime);
            return true;
        }
        return false;
    }

    public void clearMessageTtl(SQLiteDatabase db, TLRPC.Message message) {
        if (db == null || message == null) {
            return;
        }
        protectMedia(message);
        NativeByteBuffer data = null;
        try {
            message.ttl = 0;
            message.destroyTime = 0;
            message.destroyTimeMillis = 0;
            if (message.media != null) {
                message.media.ttl_seconds = 0;
            }
            protectMedia(message);
            data = new NativeByteBuffer(message.getObjectSize());
            message.serializeToStream(data);
            SQLitePreparedStatement state = db.executeFast("UPDATE messages_v2 SET data = ?, ttl = 0 WHERE mid = ? AND uid = ?");
            state.requery();
            state.bindByteBuffer(1, data);
            state.bindInteger(2, message.id);
            state.bindLong(3, MessageObject.getDialogId(message));
            state.step();
            state.dispose();
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (data != null) {
                data.reuse();
            }
        }
    }

    public static void markFileProtected(String name) {
        if (!TextUtils.isEmpty(name)) {
            protectedFileNames.add(name);
        }
    }

    public boolean isMediaFileProtected(File file) {
        if (file == null) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.contains("neatgram_media")) {
            return true;
        }
        if (!NeatGramConfig.isSaveTtlMedia(currentAccount) && !NeatGramConfig.isSaveDeletedMessages(currentAccount)) {
            return false;
        }
        String name = file.getName();
        if (protectedFileNames.contains(name)) {
            return true;
        }
        File mediaDir = new File(ApplicationLoader.getFilesDirFixed(), "neatgram_media");
        if (new File(mediaDir, name).exists()) {
            protectedFileNames.add(name);
            return true;
        }
        if (name.endsWith(".enc")) {
            String plainName = name.substring(0, name.length() - 4);
            if (protectedFileNames.contains(plainName) || new File(mediaDir, plainName).exists()) {
                protectedFileNames.add(name);
                return true;
            }
        }
        if (name.endsWith(".enc.key")) {
            String plainName = name.substring(0, name.length() - 8);
            if (protectedFileNames.contains(plainName) || new File(mediaDir, plainName).exists()) {
                protectedFileNames.add(name);
                return true;
            }
        }
        if (name.startsWith("q_")) {
            String plainName = name.substring(2);
            if (protectedFileNames.contains(plainName) || new File(mediaDir, plainName).exists()) {
                protectedFileNames.add(name);
                return true;
            }
        }
        return false;
    }

    public static void protectMediaFile(File originalFile) {
        if (originalFile == null || !originalFile.exists()) {
            return;
        }
        markFileProtected(originalFile.getName());
        File mediaDir = new File(ApplicationLoader.getFilesDirFixed(), "neatgram_media");
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        File targetFile = new File(mediaDir, originalFile.getName());
        if (!targetFile.exists()) {
            duplicateFile(originalFile, targetFile);
        }
        File encFile = new File(originalFile.getAbsolutePath() + ".enc");
        if (encFile.exists()) {
            markFileProtected(encFile.getName());
            File targetEnc = new File(mediaDir, encFile.getName());
            if (!targetEnc.exists()) {
                duplicateFile(encFile, targetEnc);
            }
            File keyFile = new File(FileLoader.getInternalCacheDir(), originalFile.getName() + ".enc.key");
            if (keyFile.exists()) {
                markFileProtected(keyFile.getName());
                File targetKey = new File(mediaDir, keyFile.getName());
                if (!targetKey.exists()) {
                    duplicateFile(keyFile, targetKey);
                }
            }
        }
    }

    public static boolean duplicateFile(File src, File dst) {
        if (src == null || !src.exists() || dst == null) {
            return false;
        }
        if (dst.exists()) {
            return true;
        }
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.system.Os.link(src.getAbsolutePath(), dst.getAbsolutePath());
                return true;
            }
        } catch (Throwable ignore) {
            // Fallback to streaming copy on link error (e.g. cross-filesystem)
        }
        return copyFile(src, dst);
    }

    private static boolean copyFile(File src, File dst) {
        try (FileChannel inChannel = new FileInputStream(src).getChannel();
             FileChannel outChannel = new FileOutputStream(dst).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static boolean decryptEncryptedFile(File encFile, File keyFile, File targetPlainFile) {
        if (encFile == null || !encFile.exists() || keyFile == null || !keyFile.exists() || targetPlainFile == null) {
            return false;
        }
        if (targetPlainFile.exists() && targetPlainFile.length() > 0) {
            return true;
        }
        File temp = new File(targetPlainFile.getAbsolutePath() + ".dec.temp");
        try (org.telegram.messenger.secretmedia.EncryptedFileInputStream in = new org.telegram.messenger.secretmedia.EncryptedFileInputStream(encFile, keyFile);
             FileOutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            if (temp.renameTo(targetPlainFile)) {
                return true;
            } else {
                return copyFile(temp, targetPlainFile);
            }
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        } finally {
            if (temp.exists()) {
                temp.delete();
            }
        }
    }

    public void onFileLoaded(File finalFile) {
        if (finalFile == null || !finalFile.exists()) {
            return;
        }
        protectMediaFile(finalFile);
        try {
            File cacheDir = FileLoader.getInstance(currentAccount).getDirectory(FileLoader.MEDIA_DIR_CACHE);
            if (finalFile.getParentFile() != null && finalFile.getParentFile().equals(cacheDir)) {
                String name = finalFile.getName();
                int targetType = -1;
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    targetType = FileLoader.MEDIA_DIR_IMAGE;
                } else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov")) {
                    targetType = FileLoader.MEDIA_DIR_VIDEO;
                } else if (name.endsWith(".ogg") || name.endsWith(".oga") || name.endsWith(".mp3")) {
                    targetType = FileLoader.MEDIA_DIR_AUDIO;
                }
                if (targetType != -1) {
                    File destDir = FileLoader.getInstance(currentAccount).getDirectory(targetType);
                    if (destDir != null) {
                        File destFile = new File(destDir, name);
                        duplicateFile(finalFile, destFile);
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static File resolveMediaFile(File targetFile, int account) {
        if (targetFile == null || TextUtils.isEmpty(targetFile.getName())) {
            return targetFile;
        }
        if (targetFile.exists()) {
            return targetFile;
        }
        String fileName = targetFile.getName();
        File mediaDir = new File(ApplicationLoader.getFilesDirFixed(), "neatgram_media");

        // 1. Check neatgram_media/
        File fallback = new File(mediaDir, fileName);
        if (fallback.exists()) {
            duplicateFile(fallback, targetFile);
            return targetFile.exists() ? targetFile : fallback;
        }

        // 2. Check cross-directory (cache vs standard)
        try {
            File cacheDir = FileLoader.getInstance(account).getDirectory(FileLoader.MEDIA_DIR_CACHE);
            boolean isTargetInCache = targetFile.getParentFile() != null && targetFile.getParentFile().equals(cacheDir);

            if (isTargetInCache) {
                int[] types = new int[]{FileLoader.MEDIA_DIR_IMAGE, FileLoader.MEDIA_DIR_VIDEO, FileLoader.MEDIA_DIR_AUDIO, FileLoader.MEDIA_DIR_DOCUMENT};
                for (int type : types) {
                    File candidateDir = FileLoader.getInstance(account).getDirectory(type);
                    if (candidateDir != null) {
                        File candidate = new File(candidateDir, fileName);
                        if (candidate.exists()) {
                            duplicateFile(candidate, targetFile);
                            protectMediaFile(candidate);
                            return targetFile.exists() ? targetFile : candidate;
                        }
                    }
                }
            } else {
                if (cacheDir != null) {
                    File candidate = new File(cacheDir, fileName);
                    if (candidate.exists()) {
                        duplicateFile(candidate, targetFile);
                        protectMediaFile(candidate);
                        return targetFile.exists() ? targetFile : candidate;
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }

        // 3. Check .enc version if plain file was searched
        if (!fileName.endsWith(".enc")) {
            String encName = fileName + ".enc";
            File encFallback = new File(mediaDir, encName);
            File encLocal = new File(targetFile.getParentFile(), encName);
            File encCandidate = encFallback.exists() ? encFallback : (encLocal.exists() ? encLocal : null);
            if (encCandidate == null) {
                File cacheDir = FileLoader.getInstance(account).getDirectory(FileLoader.MEDIA_DIR_CACHE);
                if (cacheDir != null) {
                    File candidate = new File(cacheDir, encName);
                    if (candidate.exists()) {
                        encCandidate = candidate;
                    }
                }
            }
            if (encCandidate != null) {
                File keyFile = new File(FileLoader.getInternalCacheDir(), fileName + ".enc.key");
                if (!keyFile.exists()) {
                    File fallbackKey = new File(mediaDir, fileName + ".enc.key");
                    if (fallbackKey.exists()) {
                        duplicateFile(fallbackKey, keyFile);
                    }
                }
                if (keyFile.exists() && decryptEncryptedFile(encCandidate, keyFile, targetFile)) {
                    protectMediaFile(targetFile);
                    return targetFile;
                }
            }
        }

        return targetFile;
    }

    public static File getMediaFallbackFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }
        File mediaDir = new File(ApplicationLoader.getFilesDirFixed(), "neatgram_media");
        File fallback = new File(mediaDir, fileName);
        if (fallback.exists()) {
            return fallback;
        }
        return null;
    }

    // --- Copy-on-Edit Handling ---

    public static boolean isContentChanged(TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        if (oldMessage == null || newMessage == null) {
            return false;
        }
        String oldText = oldMessage.message != null ? oldMessage.message : "";
        String newText = newMessage.message != null ? newMessage.message : "";
        if (!oldText.equals(newText)) {
            return true;
        }
        int oldEntitiesSize = oldMessage.entities != null ? oldMessage.entities.size() : 0;
        int newEntitiesSize = newMessage.entities != null ? newMessage.entities.size() : 0;
        if (oldEntitiesSize != newEntitiesSize) {
            return true;
        }
        if (oldEntitiesSize > 0) {
            for (int i = 0; i < oldEntitiesSize; i++) {
                TLRPC.MessageEntity e1 = oldMessage.entities.get(i);
                TLRPC.MessageEntity e2 = newMessage.entities.get(i);
                if (e1 == null || e2 == null) {
                    if (e1 != e2) {
                        return true;
                    }
                    continue;
                }
                if (e1.getClass() != e2.getClass() || e1.offset != e2.offset || e1.length != e2.length) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onMessageEditing(SQLiteDatabase db, TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        if (!NeatGramConfig.isSaveEditHistory(currentAccount)) {
            return;
        }
        if (oldMessage == null || newMessage == null || db == null) {
            return;
        }
        long dialogId = MessageObject.getDialogId(oldMessage);
        if (dialogId < 0 && !NeatGramConfig.isSaveHistoryInChannels(currentAccount)) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null && ChatObject.isChannelAndNotMegaGroup(chat) && ChatObject.isPublic(chat)) {
                return;
            }
        }
        if (!isContentChanged(oldMessage, newMessage)) {
            return;
        }

        NativeByteBuffer entitiesBuffer = null;
        try {
            int editDate = oldMessage.edit_date != 0 ? oldMessage.edit_date : oldMessage.date;
            String oldText = oldMessage.message != null ? oldMessage.message : "";
            entitiesBuffer = serializeEntities(oldMessage.entities);

            SQLitePreparedStatement state = db.executeFast("INSERT INTO neatgram_edits(dialog_id, mid, edit_date, old_text, old_entities) VALUES(?, ?, ?, ?, ?)");
            state.requery();
            state.bindLong(1, dialogId);
            state.bindInteger(2, oldMessage.id);
            state.bindInteger(3, editDate);
            state.bindString(4, oldText);
            if (entitiesBuffer != null) {
                state.bindByteBuffer(5, entitiesBuffer);
            } else {
                state.bindNull(5);
            }
            state.step();
            state.dispose();

            addEditToCache(dialogId, oldMessage.id);
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (entitiesBuffer != null) {
                entitiesBuffer.reuse();
            }
        }
    }

    private static NativeByteBuffer serializeEntities(ArrayList<TLRPC.MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        NativeByteBuffer data = null;
        try {
            int size = 4;
            for (int a = 0; a < entities.size(); a++) {
                size += entities.get(a).getObjectSize();
            }
            data = new NativeByteBuffer(size);
            data.writeInt32(entities.size());
            for (int a = 0; a < entities.size(); a++) {
                entities.get(a).serializeToStream(data);
            }
            NativeByteBuffer result = data;
            data = null;
            return result;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            if (data != null) {
                data.reuse();
            }
        }
    }

    private static ArrayList<TLRPC.MessageEntity> deserializeEntities(NativeByteBuffer data) {
        if (data == null) {
            return null;
        }
        try {
            int count = data.readInt32(false);
            if (count < 0) {
                return null;
            }
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(count);
            for (int a = 0; a < count; a++) {
                int constructor = data.readInt32(false);
                TLRPC.MessageEntity entity = TLRPC.MessageEntity.TLdeserialize(data, constructor, false);
                if (entity != null) {
                    entities.add(entity);
                }
            }
            return entities;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    // --- Permanent Deletion ---

    public void deletePermanently(long dialogId, int mid) {
        Set<Integer> deletedSet = deletedMessagesCache.get(dialogId);
        if (deletedSet != null) {
            deletedSet.remove(mid);
        }
        Set<Integer> editedSet = editedMessagesCache.get(dialogId);
        if (editedSet != null) {
            editedSet.remove(mid);
        }

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            SQLiteDatabase db = MessagesStorage.getInstance(currentAccount).getDatabase();
            if (db == null) {
                return;
            }
            try {
                db.executeFast("DELETE FROM messages_v2 WHERE mid = " + mid + " AND uid = " + dialogId).stepThis().dispose();
                db.executeFast("DELETE FROM neatgram_deleted WHERE dialog_id = " + dialogId + " AND mid = " + mid).stepThis().dispose();
                db.executeFast("DELETE FROM neatgram_edits WHERE dialog_id = " + dialogId + " AND mid = " + mid).stepThis().dispose();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    // --- UI Drawables & Edit History Sheet ---

    public static Drawable getDeletedIcon(int currentAccount) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        int iconType = NeatGramConfig.getDeletedMessageIcon(currentAccount);
        int resId;
        switch (iconType) {
            case 1:
                resId = R.drawable.ic_neatgram_del_cross;
                break;
            case 2:
                resId = R.drawable.ic_neatgram_del_eye_off;
                break;
            case 3:
                resId = R.drawable.ic_neatgram_del_ghost;
                break;
            case 0:
            default:
                resId = R.drawable.ic_neatgram_del_trash;
                break;
        }
        Drawable d = ContextCompat.getDrawable(context, resId);
        return d != null ? d.mutate() : null;
    }

    public static class EditVersion {
        public final int editDate;
        public final String text;
        public final ArrayList<TLRPC.MessageEntity> entities;

        public EditVersion(int editDate, String text, ArrayList<TLRPC.MessageEntity> entities) {
            this.editDate = editDate;
            this.text = text;
            this.entities = entities;
        }
    }

    public void showEditHistorySheet(BaseFragment fragment, MessageObject messageObject) {
        if (fragment == null || fragment.getParentActivity() == null || messageObject == null) {
            return;
        }
        final long dialogId = messageObject.getDialogId();
        final int mid = messageObject.getId();

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            ArrayList<EditVersion> versions = new ArrayList<>();
            SQLiteDatabase db = MessagesStorage.getInstance(currentAccount).getDatabase();
            if (db != null) {
                SQLiteCursor cursor = null;
                try {
                    cursor = db.queryFinalized("SELECT edit_date, old_text, old_entities FROM neatgram_edits WHERE dialog_id = " + dialogId + " AND mid = " + mid + " ORDER BY id ASC");
                    while (cursor.next()) {
                        int editDate = cursor.intValue(0);
                        String oldText = cursor.stringValue(1);
                        NativeByteBuffer entitiesData = cursor.byteBufferValue(2);
                        ArrayList<TLRPC.MessageEntity> entities = null;
                        if (entitiesData != null) {
                            try {
                                entities = deserializeEntities(entitiesData);
                            } finally {
                                entitiesData.reuse();
                            }
                        }
                        versions.add(new EditVersion(editDate, oldText, entities));
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.dispose();
                    }
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (fragment.getParentActivity() == null) {
                    return;
                }
                Context context = fragment.getParentActivity();
                BottomSheet.Builder builder = new BottomSheet.Builder(context, false);
                builder.setTitle("История правок", true);

                ScrollView scrollView = new ScrollView(context);
                LinearLayout container = new LinearLayout(context);
                container.setOrientation(LinearLayout.VERTICAL);
                container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

                int versionIndex = 1;
                for (EditVersion v : versions) {
                    String title = (versionIndex == 1 ? "Исходная версия" : ("Правка #" + (versionIndex - 1))) + " • " + LocaleController.formatDateTime(v.editDate, true);
                    container.addView(createVersionCard(context, fragment, title, v.text, v.entities));
                    versionIndex++;
                }

                // Current version
                String currentText = messageObject.messageText != null ? messageObject.messageText.toString() : "";
                int currentDate = messageObject.messageOwner.edit_date != 0 ? messageObject.messageOwner.edit_date : messageObject.messageOwner.date;
                String currentTitle = "Текущая версия • " + LocaleController.formatDateTime(currentDate, true);
                container.addView(createVersionCard(context, fragment, currentTitle, currentText, messageObject.messageOwner.entities));

                scrollView.addView(container);
                builder.setCustomView(scrollView);
                builder.show();
            });
        });
    }

    private static View createVersionCard(Context context, BaseFragment fragment, String title, String text, ArrayList<TLRPC.MessageEntity> entities) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        card.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), AndroidUtilities.dp(8), AndroidUtilities.dp(8)));

        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setText(title);
        card.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView contentView = new TextView(context);
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        contentView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        contentView.setTextIsSelectable(false);

        CharSequence formattedText = text;
        if (entities != null && !entities.isEmpty() && !TextUtils.isEmpty(text)) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            MessageObject.addEntitiesToText(ssb, entities, false, false, false, false);
            formattedText = ssb;
        }
        contentView.setText(formattedText);
        card.addView(contentView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView copyHintView = new TextView(context);
        copyHintView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        copyHintView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        copyHintView.setText("Нажмите, чтобы скопировать");
        card.addView(copyHintView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        final String copyString = text;
        card.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(copyString);
            if (fragment != null) {
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
            }
        });

        return card;
    }
}
