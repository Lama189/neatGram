package org.telegram.neatgram.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

/**
 * NeatGram configuration management backed by SharedPreferences.
 * Provides thread-safe, multi-account aware settings with minimal latency on hot paths.
 */
public class NeatGramConfig {

    public static final String NEATGRAM_VERSION = "1.1.0";

    private static final String PREFS_NAME = "neatgramconfig";
    private static final String KEY_PREMIUM_DOWNLOAD_BOOST = "premiumDownloadBoost";
    private static final String KEY_RAM_PILL_ENABLED = "ramPillEnabled";
    private static final String KEY_DISABLE_ADS = "disableAds";
    private static final String KEY_BACKGROUND_PUSH_SERVICE = "backgroundPushService";

    private static final String KEY_GHOST_MODE_ENABLED = "ghostModeEnabled";
    private static final String KEY_GHOST_DONT_READ_MESSAGES = "ghostDontReadMessages";
    private static final String KEY_GHOST_DONT_READ_STORIES = "ghostDontReadStories";
    private static final String KEY_GHOST_DONT_SEND_ONLINE = "ghostDontSendOnline";
    private static final String KEY_GHOST_DONT_SEND_TYPING = "ghostDontSendTyping";
    private static final String KEY_GHOST_AUTO_OFFLINE = "ghostAutoOffline";
    private static final String KEY_GHOST_READ_AFTER_ACTION = "ghostReadAfterAction";
    private static final String KEY_GHOST_USE_SCHEDULE = "ghostUseSchedule";

    private static final String KEY_SAVE_DELETED_MESSAGES = "saveDeletedMessages";
    private static final String KEY_SAVE_TTL_MEDIA = "saveTtlMedia";
    private static final String KEY_SAVE_EDIT_HISTORY = "saveEditHistory";
    private static final String KEY_SAVE_HISTORY_IN_CHANNELS = "saveHistoryInChannels";
    private static final String KEY_DIM_DELETED_MESSAGES = "dimDeletedMessages";
    private static final String KEY_DELETED_MESSAGE_ICON = "deletedMessageIcon";
    private static final String KEY_DELETED_ICON_COLOR = "deletedIconColor";

    private static final Object sync = new Object();
    private static volatile boolean initialized;

    // Global settings
    private static volatile boolean premiumDownloadBoost = true;
    private static volatile boolean ramPillEnabled = true;
    private static volatile boolean disableAds = true;
    private static volatile boolean backgroundPushService = true;

    private static volatile boolean ghostModeEnabled = false;
    private static volatile boolean ghostDontReadMessages = true;
    private static volatile boolean ghostDontReadStories = true;
    private static volatile boolean ghostDontSendOnline = true;
    private static volatile boolean ghostDontSendTyping = true;
    private static volatile boolean ghostAutoOffline = true;
    private static volatile boolean ghostReadAfterAction = true;
    private static volatile boolean ghostUseSchedule = false;

    private static volatile boolean saveDeletedMessages = true;
    private static volatile boolean saveTtlMedia = true;
    private static volatile boolean saveEditHistory = true;
    private static volatile boolean saveHistoryInChannels = false;
    private static volatile boolean dimDeletedMessages = true;
    private static volatile int deletedMessageIcon = 0;
    private static volatile int deletedIconColor = 0;

    // Per-account settings
    private static final boolean[] premiumDownloadBoostAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostModeEnabledAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostDontReadMessagesAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostDontReadStoriesAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostDontSendOnlineAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostDontSendTypingAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostAutoOfflineAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostReadAfterActionAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] ghostUseScheduleAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    private static final boolean[] saveDeletedMessagesAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] saveTtlMediaAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] saveEditHistoryAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] saveHistoryInChannelsAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final boolean[] dimDeletedMessagesAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private static final int[] deletedMessageIconAccounts = new int[UserConfig.MAX_ACCOUNT_COUNT];
    private static final int[] deletedIconColorAccounts = new int[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            premiumDownloadBoostAccounts[i] = true;
            ghostModeEnabledAccounts[i] = false;
            ghostDontReadMessagesAccounts[i] = true;
            ghostDontReadStoriesAccounts[i] = true;
            ghostDontSendOnlineAccounts[i] = true;
            ghostDontSendTypingAccounts[i] = true;
            ghostAutoOfflineAccounts[i] = true;
            ghostReadAfterActionAccounts[i] = true;
            ghostUseScheduleAccounts[i] = false;

            saveDeletedMessagesAccounts[i] = true;
            saveTtlMediaAccounts[i] = true;
            saveEditHistoryAccounts[i] = true;
            saveHistoryInChannelsAccounts[i] = false;
            dimDeletedMessagesAccounts[i] = true;
            deletedMessageIconAccounts[i] = 0;
            deletedIconColorAccounts[i] = 0;
        }
    }

    private static void ensureLoaded() {
        if (initialized) {
            return;
        }
        synchronized (sync) {
            if (initialized) {
                return;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                premiumDownloadBoost = preferences.getBoolean(KEY_PREMIUM_DOWNLOAD_BOOST, true);
                for (int i = 0; i < premiumDownloadBoostAccounts.length; i++) {
                    premiumDownloadBoostAccounts[i] = preferences.getBoolean(KEY_PREMIUM_DOWNLOAD_BOOST + "_" + i, premiumDownloadBoost);
                }
                ramPillEnabled = preferences.getBoolean(KEY_RAM_PILL_ENABLED, true);
                disableAds = preferences.getBoolean(KEY_DISABLE_ADS, true);
                backgroundPushService = preferences.getBoolean(KEY_BACKGROUND_PUSH_SERVICE, true);

                ghostModeEnabled = preferences.getBoolean(KEY_GHOST_MODE_ENABLED, false);
                ghostDontReadMessages = preferences.getBoolean(KEY_GHOST_DONT_READ_MESSAGES, true);
                ghostDontReadStories = preferences.getBoolean(KEY_GHOST_DONT_READ_STORIES, true);
                ghostDontSendOnline = preferences.getBoolean(KEY_GHOST_DONT_SEND_ONLINE, true);
                ghostDontSendTyping = preferences.getBoolean(KEY_GHOST_DONT_SEND_TYPING, true);
                ghostAutoOffline = preferences.getBoolean(KEY_GHOST_AUTO_OFFLINE, true);
                ghostReadAfterAction = preferences.getBoolean(KEY_GHOST_READ_AFTER_ACTION, true);
                ghostUseSchedule = preferences.getBoolean(KEY_GHOST_USE_SCHEDULE, false);

                saveDeletedMessages = preferences.getBoolean(KEY_SAVE_DELETED_MESSAGES, true);
                saveTtlMedia = preferences.getBoolean(KEY_SAVE_TTL_MEDIA, true);
                saveEditHistory = preferences.getBoolean(KEY_SAVE_EDIT_HISTORY, true);
                saveHistoryInChannels = preferences.getBoolean(KEY_SAVE_HISTORY_IN_CHANNELS, false);
                dimDeletedMessages = preferences.getBoolean(KEY_DIM_DELETED_MESSAGES, true);
                deletedMessageIcon = preferences.getInt(KEY_DELETED_MESSAGE_ICON, 0);
                deletedIconColor = preferences.getInt(KEY_DELETED_ICON_COLOR, 0);

                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    ghostModeEnabledAccounts[i] = preferences.getBoolean(KEY_GHOST_MODE_ENABLED + "_" + i, ghostModeEnabled);
                    ghostDontReadMessagesAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_READ_MESSAGES + "_" + i, ghostDontReadMessages);
                    ghostDontReadStoriesAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_READ_STORIES + "_" + i, ghostDontReadStories);
                    ghostDontSendOnlineAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_SEND_ONLINE + "_" + i, ghostDontSendOnline);
                    ghostDontSendTypingAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_SEND_TYPING + "_" + i, ghostDontSendTyping);
                    ghostAutoOfflineAccounts[i] = preferences.getBoolean(KEY_GHOST_AUTO_OFFLINE + "_" + i, ghostAutoOffline);
                    ghostReadAfterActionAccounts[i] = preferences.getBoolean(KEY_GHOST_READ_AFTER_ACTION + "_" + i, ghostReadAfterAction);
                    ghostUseScheduleAccounts[i] = preferences.getBoolean(KEY_GHOST_USE_SCHEDULE + "_" + i, ghostUseSchedule);

                    saveDeletedMessagesAccounts[i] = preferences.getBoolean(KEY_SAVE_DELETED_MESSAGES + "_" + i, saveDeletedMessages);
                    saveTtlMediaAccounts[i] = preferences.getBoolean(KEY_SAVE_TTL_MEDIA + "_" + i, saveTtlMedia);
                    saveEditHistoryAccounts[i] = preferences.getBoolean(KEY_SAVE_EDIT_HISTORY + "_" + i, saveEditHistory);
                    saveHistoryInChannelsAccounts[i] = preferences.getBoolean(KEY_SAVE_HISTORY_IN_CHANNELS + "_" + i, saveHistoryInChannels);
                    dimDeletedMessagesAccounts[i] = preferences.getBoolean(KEY_DIM_DELETED_MESSAGES + "_" + i, dimDeletedMessages);
                    deletedMessageIconAccounts[i] = preferences.getInt(KEY_DELETED_MESSAGE_ICON + "_" + i, deletedMessageIcon);
                    deletedIconColorAccounts[i] = preferences.getInt(KEY_DELETED_ICON_COLOR + "_" + i, deletedIconColor);
                }

                initialized = true;
            }
        }
    }

    // --- Premium Download Boost ---

    public static boolean isPremiumDownloadBoost() {
        ensureLoaded();
        return premiumDownloadBoost;
    }

    public static boolean isPremiumDownloadBoost(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < premiumDownloadBoostAccounts.length) {
            return premiumDownloadBoostAccounts[currentAccount];
        }
        return premiumDownloadBoost;
    }

    public static void setPremiumDownloadBoost(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            premiumDownloadBoost = enabled;
            for (int i = 0; i < premiumDownloadBoostAccounts.length; i++) {
                premiumDownloadBoostAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_PREMIUM_DOWNLOAD_BOOST, enabled);
                for (int i = 0; i < premiumDownloadBoostAccounts.length; i++) {
                    editor.putBoolean(KEY_PREMIUM_DOWNLOAD_BOOST + "_" + i, enabled);
                }
                editor.apply();
            }
            for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                boolean accountPremium = enabled || UserConfig.getInstance(i).isPremium();
                ConnectionsManager.setUserPremium(i, accountPremium);
            }
        }
    }

    public static void setPremiumDownloadBoost(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < premiumDownloadBoostAccounts.length) {
                premiumDownloadBoostAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_PREMIUM_DOWNLOAD_BOOST + "_" + currentAccount, enabled);
                editor.apply();
            }
            if (currentAccount >= 0 && currentAccount < UserConfig.MAX_ACCOUNT_COUNT) {
                boolean accountPremium = enabled || UserConfig.getInstance(currentAccount).isPremium();
                ConnectionsManager.setUserPremium(currentAccount, accountPremium);
            }
        }
    }

    // --- RAM Pill ---

    public static boolean isRamPillEnabled() {
        ensureLoaded();
        return ramPillEnabled;
    }

    public static void setRamPillEnabled(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ramPillEnabled = enabled;
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_RAM_PILL_ENABLED, enabled);
                editor.apply();
            }
        }
    }

    // --- Ads ---

    public static boolean isDisableAds() {
        ensureLoaded();
        return disableAds;
    }

    public static boolean isDisableAds(int currentAccount) {
        return isDisableAds();
    }

    public static void setDisableAds(boolean disabled) {
        ensureLoaded();
        synchronized (sync) {
            disableAds = disabled;
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_DISABLE_ADS, disabled);
                editor.apply();
            }
        }
    }

    // --- Background Push Service ---

    public static boolean isBackgroundPushService() {
        ensureLoaded();
        return backgroundPushService;
    }

    public static void setBackgroundPushService(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            backgroundPushService = enabled;
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_BACKGROUND_PUSH_SERVICE, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Mode Settings ---

    public static boolean isGhostModeEnabled() {
        ensureLoaded();
        return ghostModeEnabled;
    }

    public static boolean isGhostModeEnabled(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostModeEnabledAccounts.length) {
            return ghostModeEnabledAccounts[currentAccount];
        }
        return ghostModeEnabled;
    }

    public static void setGhostModeEnabled(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostModeEnabled = enabled;
            for (int i = 0; i < ghostModeEnabledAccounts.length; i++) {
                ghostModeEnabledAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_MODE_ENABLED, enabled);
                for (int i = 0; i < ghostModeEnabledAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_MODE_ENABLED + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostModeEnabled(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostModeEnabledAccounts.length) {
                ghostModeEnabledAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_MODE_ENABLED + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Don't Read Messages ---

    public static boolean isGhostDontReadMessages() {
        ensureLoaded();
        return ghostDontReadMessages;
    }

    public static boolean isGhostDontReadMessages(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostDontReadMessagesAccounts.length) {
            return ghostDontReadMessagesAccounts[currentAccount];
        }
        return ghostDontReadMessages;
    }

    public static void setGhostDontReadMessages(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostDontReadMessages = enabled;
            for (int i = 0; i < ghostDontReadMessagesAccounts.length; i++) {
                ghostDontReadMessagesAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_READ_MESSAGES, enabled);
                for (int i = 0; i < ghostDontReadMessagesAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_DONT_READ_MESSAGES + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostDontReadMessages(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostDontReadMessagesAccounts.length) {
                ghostDontReadMessagesAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_READ_MESSAGES + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Don't Read Stories ---

    public static boolean isGhostDontReadStories() {
        ensureLoaded();
        return ghostDontReadStories;
    }

    public static boolean isGhostDontReadStories(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostDontReadStoriesAccounts.length) {
            return ghostDontReadStoriesAccounts[currentAccount];
        }
        return ghostDontReadStories;
    }

    public static void setGhostDontReadStories(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostDontReadStories = enabled;
            for (int i = 0; i < ghostDontReadStoriesAccounts.length; i++) {
                ghostDontReadStoriesAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_READ_STORIES, enabled);
                for (int i = 0; i < ghostDontReadStoriesAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_DONT_READ_STORIES + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostDontReadStories(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostDontReadStoriesAccounts.length) {
                ghostDontReadStoriesAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_READ_STORIES + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Don't Send Online ---

    public static boolean isGhostDontSendOnline() {
        ensureLoaded();
        return ghostDontSendOnline;
    }

    public static boolean isGhostDontSendOnline(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostDontSendOnlineAccounts.length) {
            return ghostDontSendOnlineAccounts[currentAccount];
        }
        return ghostDontSendOnline;
    }

    public static void setGhostDontSendOnline(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostDontSendOnline = enabled;
            for (int i = 0; i < ghostDontSendOnlineAccounts.length; i++) {
                ghostDontSendOnlineAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_SEND_ONLINE, enabled);
                for (int i = 0; i < ghostDontSendOnlineAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_DONT_SEND_ONLINE + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostDontSendOnline(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostDontSendOnlineAccounts.length) {
                ghostDontSendOnlineAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_SEND_ONLINE + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Don't Send Typing ---

    public static boolean isGhostDontSendTyping() {
        ensureLoaded();
        return ghostDontSendTyping;
    }

    public static boolean isGhostDontSendTyping(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostDontSendTypingAccounts.length) {
            return ghostDontSendTypingAccounts[currentAccount];
        }
        return ghostDontSendTyping;
    }

    public static void setGhostDontSendTyping(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostDontSendTyping = enabled;
            for (int i = 0; i < ghostDontSendTypingAccounts.length; i++) {
                ghostDontSendTypingAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_SEND_TYPING, enabled);
                for (int i = 0; i < ghostDontSendTypingAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_DONT_SEND_TYPING + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostDontSendTyping(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostDontSendTypingAccounts.length) {
                ghostDontSendTypingAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_DONT_SEND_TYPING + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Auto Offline ---

    public static boolean isGhostAutoOffline() {
        ensureLoaded();
        return ghostAutoOffline;
    }

    public static boolean isGhostAutoOffline(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostAutoOfflineAccounts.length) {
            return ghostAutoOfflineAccounts[currentAccount];
        }
        return ghostAutoOffline;
    }

    public static void setGhostAutoOffline(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostAutoOffline = enabled;
            for (int i = 0; i < ghostAutoOfflineAccounts.length; i++) {
                ghostAutoOfflineAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_AUTO_OFFLINE, enabled);
                for (int i = 0; i < ghostAutoOfflineAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_AUTO_OFFLINE + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostAutoOffline(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostAutoOfflineAccounts.length) {
                ghostAutoOfflineAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_AUTO_OFFLINE + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Read After Action ---

    public static boolean isGhostReadAfterAction() {
        ensureLoaded();
        return ghostReadAfterAction;
    }

    public static boolean isGhostReadAfterAction(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostReadAfterActionAccounts.length) {
            return ghostReadAfterActionAccounts[currentAccount];
        }
        return ghostReadAfterAction;
    }

    public static void setGhostReadAfterAction(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostReadAfterAction = enabled;
            for (int i = 0; i < ghostReadAfterActionAccounts.length; i++) {
                ghostReadAfterActionAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_READ_AFTER_ACTION, enabled);
                for (int i = 0; i < ghostReadAfterActionAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_READ_AFTER_ACTION + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostReadAfterAction(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostReadAfterActionAccounts.length) {
                ghostReadAfterActionAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_READ_AFTER_ACTION + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Ghost Use Schedule ---

    public static boolean isGhostUseSchedule() {
        ensureLoaded();
        return ghostUseSchedule;
    }

    public static boolean isGhostUseSchedule(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < ghostUseScheduleAccounts.length) {
            return ghostUseScheduleAccounts[currentAccount];
        }
        return ghostUseSchedule;
    }

    public static void setGhostUseSchedule(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            ghostUseSchedule = enabled;
            for (int i = 0; i < ghostUseScheduleAccounts.length; i++) {
                ghostUseScheduleAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_USE_SCHEDULE, enabled);
                for (int i = 0; i < ghostUseScheduleAccounts.length; i++) {
                    editor.putBoolean(KEY_GHOST_USE_SCHEDULE + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setGhostUseSchedule(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < ghostUseScheduleAccounts.length) {
                ghostUseScheduleAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_GHOST_USE_SCHEDULE + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    // --- Message History & Deleted Messages ---

    public static boolean isSaveDeletedMessages() {
        ensureLoaded();
        return saveDeletedMessages;
    }

    public static boolean isSaveDeletedMessages(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < saveDeletedMessagesAccounts.length) {
            return saveDeletedMessagesAccounts[currentAccount];
        }
        return saveDeletedMessages;
    }

    public static void setSaveDeletedMessages(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            saveDeletedMessages = enabled;
            for (int i = 0; i < saveDeletedMessagesAccounts.length; i++) {
                saveDeletedMessagesAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_DELETED_MESSAGES, enabled);
                for (int i = 0; i < saveDeletedMessagesAccounts.length; i++) {
                    editor.putBoolean(KEY_SAVE_DELETED_MESSAGES + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setSaveDeletedMessages(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < saveDeletedMessagesAccounts.length) {
                saveDeletedMessagesAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_DELETED_MESSAGES + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    public static boolean isSaveTtlMedia() {
        ensureLoaded();
        return saveTtlMedia;
    }

    public static boolean isSaveTtlMedia(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < saveTtlMediaAccounts.length) {
            return saveTtlMediaAccounts[currentAccount];
        }
        return saveTtlMedia;
    }

    public static void setSaveTtlMedia(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            saveTtlMedia = enabled;
            for (int i = 0; i < saveTtlMediaAccounts.length; i++) {
                saveTtlMediaAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_TTL_MEDIA, enabled);
                for (int i = 0; i < saveTtlMediaAccounts.length; i++) {
                    editor.putBoolean(KEY_SAVE_TTL_MEDIA + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setSaveTtlMedia(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < saveTtlMediaAccounts.length) {
                saveTtlMediaAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_TTL_MEDIA + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    public static boolean isSaveEditHistory() {
        ensureLoaded();
        return saveEditHistory;
    }

    public static boolean isSaveEditHistory(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < saveEditHistoryAccounts.length) {
            return saveEditHistoryAccounts[currentAccount];
        }
        return saveEditHistory;
    }

    public static void setSaveEditHistory(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            saveEditHistory = enabled;
            for (int i = 0; i < saveEditHistoryAccounts.length; i++) {
                saveEditHistoryAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_EDIT_HISTORY, enabled);
                for (int i = 0; i < saveEditHistoryAccounts.length; i++) {
                    editor.putBoolean(KEY_SAVE_EDIT_HISTORY + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setSaveEditHistory(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < saveEditHistoryAccounts.length) {
                saveEditHistoryAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_EDIT_HISTORY + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    public static boolean isSaveHistoryInChannels() {
        ensureLoaded();
        return saveHistoryInChannels;
    }

    public static boolean isSaveHistoryInChannels(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < saveHistoryInChannelsAccounts.length) {
            return saveHistoryInChannelsAccounts[currentAccount];
        }
        return saveHistoryInChannels;
    }

    public static void setSaveHistoryInChannels(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            saveHistoryInChannels = enabled;
            for (int i = 0; i < saveHistoryInChannelsAccounts.length; i++) {
                saveHistoryInChannelsAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_HISTORY_IN_CHANNELS, enabled);
                for (int i = 0; i < saveHistoryInChannelsAccounts.length; i++) {
                    editor.putBoolean(KEY_SAVE_HISTORY_IN_CHANNELS + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setSaveHistoryInChannels(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < saveHistoryInChannelsAccounts.length) {
                saveHistoryInChannelsAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_SAVE_HISTORY_IN_CHANNELS + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    public static boolean isDimDeletedMessages() {
        ensureLoaded();
        return dimDeletedMessages;
    }

    public static boolean isDimDeletedMessages(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < dimDeletedMessagesAccounts.length) {
            return dimDeletedMessagesAccounts[currentAccount];
        }
        return dimDeletedMessages;
    }

    public static void setDimDeletedMessages(boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            dimDeletedMessages = enabled;
            for (int i = 0; i < dimDeletedMessagesAccounts.length; i++) {
                dimDeletedMessagesAccounts[i] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_DIM_DELETED_MESSAGES, enabled);
                for (int i = 0; i < dimDeletedMessagesAccounts.length; i++) {
                    editor.putBoolean(KEY_DIM_DELETED_MESSAGES + "_" + i, enabled);
                }
                editor.apply();
            }
        }
    }

    public static void setDimDeletedMessages(int currentAccount, boolean enabled) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < dimDeletedMessagesAccounts.length) {
                dimDeletedMessagesAccounts[currentAccount] = enabled;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(KEY_DIM_DELETED_MESSAGES + "_" + currentAccount, enabled);
                editor.apply();
            }
        }
    }

    public static int getDeletedMessageIcon() {
        ensureLoaded();
        return deletedMessageIcon;
    }

    public static int getDeletedMessageIcon(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < deletedMessageIconAccounts.length) {
            return deletedMessageIconAccounts[currentAccount];
        }
        return deletedMessageIcon;
    }

    public static void setDeletedMessageIcon(int iconIndex) {
        ensureLoaded();
        synchronized (sync) {
            deletedMessageIcon = iconIndex;
            for (int i = 0; i < deletedMessageIconAccounts.length; i++) {
                deletedMessageIconAccounts[i] = iconIndex;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_DELETED_MESSAGE_ICON, iconIndex);
                for (int i = 0; i < deletedMessageIconAccounts.length; i++) {
                    editor.putInt(KEY_DELETED_MESSAGE_ICON + "_" + i, iconIndex);
                }
                editor.apply();
            }
        }
    }

    public static void setDeletedMessageIcon(int currentAccount, int iconIndex) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < deletedMessageIconAccounts.length) {
                deletedMessageIconAccounts[currentAccount] = iconIndex;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_DELETED_MESSAGE_ICON + "_" + currentAccount, iconIndex);
                editor.apply();
            }
        }
    }

    public static int getDeletedIconColor() {
        ensureLoaded();
        return deletedIconColor;
    }

    public static int getDeletedIconColor(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < deletedIconColorAccounts.length) {
            return deletedIconColorAccounts[currentAccount];
        }
        return deletedIconColor;
    }

    public static void setDeletedIconColor(int color) {
        ensureLoaded();
        synchronized (sync) {
            deletedIconColor = color;
            for (int i = 0; i < deletedIconColorAccounts.length; i++) {
                deletedIconColorAccounts[i] = color;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_DELETED_ICON_COLOR, color);
                for (int i = 0; i < deletedIconColorAccounts.length; i++) {
                    editor.putInt(KEY_DELETED_ICON_COLOR + "_" + i, color);
                }
                editor.apply();
            }
        }
    }

    public static void setDeletedIconColor(int currentAccount, int color) {
        ensureLoaded();
        synchronized (sync) {
            if (currentAccount >= 0 && currentAccount < deletedIconColorAccounts.length) {
                deletedIconColorAccounts[currentAccount] = color;
            }
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_DELETED_ICON_COLOR + "_" + currentAccount, color);
                editor.apply();
            }
        }
    }
}
