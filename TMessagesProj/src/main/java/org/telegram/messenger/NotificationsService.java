/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.neatgram.push.NeatGramPushNotifier;

public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!NeatGramConfig.isBackgroundPushService()) {
            NeatGramPushNotifier.stopForeground(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        NeatGramPushNotifier.startForeground(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (NeatGramConfig.isBackgroundPushService()) {
            try {
                Intent restartServiceIntent = new Intent(getApplicationContext(), NotificationsService.class);
                restartServiceIntent.setPackage(getPackageName());
                int pendingFlags = PendingIntent.FLAG_ONE_SHOT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    1,
                    restartServiceIntent,
                    pendingFlags
                );
                AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                if (alarmService != null) {
                    alarmService.set(
                        AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + 1000,
                        restartServicePendingIntent
                    );
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (NeatGramConfig.isBackgroundPushService()) {
            try {
                Intent intent = new Intent("org.telegram.start");
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        } else {
            SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
            if (preferences.getBoolean("pushService", true)) {
                Intent intent = new Intent("org.telegram.start");
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            }
        }
    }
}
