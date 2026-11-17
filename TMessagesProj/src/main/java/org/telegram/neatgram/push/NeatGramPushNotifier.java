package org.telegram.neatgram.push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

import java.util.Random;

public final class NeatGramPushNotifier {

    public static final String CHANNEL_ID = "neatgram_background_push";
    public static final int NOTIFICATION_ID = 999;

    private static final String[] KAOMOJIS = {
        "(•‿•)",
        "(¬‿¬)",
        "(￣▽￣)ノ",
        "(•̀ᴗ•́)و",
        "( ˘ω˘ )",
        "ʕ•ᴥ•ʔ",
        "(◉_◉)",
        "(づ￣ ³￣)づ",
        "(・_・;)",
        "¯\\_(ツ)_/¯",
        "(ง •̀_•́)ง",
        "(ᵔᴥᵔ)"
    };

    private static final Random RANDOM = new Random();

    private NeatGramPushNotifier() {
    }

    private static void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Фоновая служба neatGram",
                        NotificationManager.IMPORTANCE_MIN
                    );
                    channel.setDescription("Поддержание фонового подключения для мгновенной доставки сообщений");
                    channel.enableLights(false);
                    channel.enableVibration(false);
                    channel.setSound(null, null);
                    channel.setShowBadge(false);
                    nm.createNotificationChannel(channel);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    private static Notification buildNotification(Service service) {
        String kaomoji = KAOMOJIS[RANDOM.nextInt(KAOMOJIS.length)];

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, CHANNEL_ID);
        builder.setSmallIcon(R.drawable.notification);
        builder.setContentTitle("neatGram");
        builder.setContentText(kaomoji);
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setSound(null);
        builder.setVibrate(null);

        Intent launchIntent = new Intent(service, LaunchActivity.class);
        launchIntent.setAction(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, launchIntent, flags);
        builder.setContentIntent(pendingIntent);

        return builder.build();
    }

    public static void startForeground(Service service) {
        if (service == null) {
            return;
        }
        try {
            createChannelIfNeeded(service);
            Notification notification = buildNotification(service);

            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    service.startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
                    return;
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    service.startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                    return;
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            service.startForeground(NOTIFICATION_ID, notification);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void stopForeground(Service service) {
        if (service == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.stopForeground(Service.STOP_FOREGROUND_REMOVE);
            } else {
                service.stopForeground(true);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
