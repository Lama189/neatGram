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

    private static final Object sync = new Object();
    private static volatile boolean initialized;

    // Global toggle (defaults to true)
    private static volatile boolean premiumDownloadBoost = true;

    // Per-account overrides (defaults to true)
    private static final boolean[] premiumDownloadBoostAccounts = new boolean[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        for (int i = 0; i < premiumDownloadBoostAccounts.length; i++) {
            premiumDownloadBoostAccounts[i] = true;
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
                initialized = true;
            }
        }
    }

    /**
     * Checks if Telegram Premium download speed boost is enabled globally.
     */
    public static boolean isPremiumDownloadBoost() {
        ensureLoaded();
        return premiumDownloadBoost;
    }

    /**
     * Checks if Telegram Premium download speed boost is enabled for the specified account.
     */
    public static boolean isPremiumDownloadBoost(int currentAccount) {
        ensureLoaded();
        if (currentAccount >= 0 && currentAccount < premiumDownloadBoostAccounts.length) {
            return premiumDownloadBoostAccounts[currentAccount];
        }
        return premiumDownloadBoost;
    }

    /**
     * Updates Telegram Premium download speed boost globally and across all accounts.
     */
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

    /**
     * Updates Telegram Premium download speed boost for a specific account.
     */
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
}
