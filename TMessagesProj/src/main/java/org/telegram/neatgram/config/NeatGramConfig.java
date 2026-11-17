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

                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    ghostModeEnabledAccounts[i] = preferences.getBoolean(KEY_GHOST_MODE_ENABLED + "_" + i, ghostModeEnabled);
                    ghostDontReadMessagesAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_READ_MESSAGES + "_" + i, ghostDontReadMessages);
                    ghostDontReadStoriesAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_READ_STORIES + "_" + i, ghostDontReadStories);
                    ghostDontSendOnlineAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_SEND_ONLINE + "_" + i, ghostDontSendOnline);
                    ghostDontSendTypingAccounts[i] = preferences.getBoolean(KEY_GHOST_DONT_SEND_TYPING + "_" + i, ghostDontSendTyping);
                    ghostAutoOfflineAccounts[i] = preferences.getBoolean(KEY_GHOST_AUTO_OFFLINE + "_" + i, ghostAutoOffline);
                    ghostReadAfterActionAccounts[i] = preferences.getBoolean(KEY_GHOST_READ_AFTER_ACTION + "_" + i, ghostReadAfterAction);
                    ghostUseScheduleAccounts[i] = preferences.getBoolean(KEY_GHOST_USE_SCHEDULE + "_" + i, ghostUseSchedule);
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
}
