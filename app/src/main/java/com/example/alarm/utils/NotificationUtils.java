package com.example.alarm.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.alarm.R;
import com.example.alarm.view.activities.AlarmRingingActivity;

public class NotificationUtils {
    private static final String CHANNEL_ID = "alarm_channel";
    private static final String CHANNEL_NAME = "Alarm Notifications";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for alarm triggers");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    public static void showNotification(Context context, int alarmId, String title, String content) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);

        Intent intent = new Intent(context, AlarmRingingActivity.class);
        intent.putExtra("ALARM_ID", alarmId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }
        builder.setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        manager.notify(alarmId, builder.build());
    }

    public static void cancelNotification(Context context, int alarmId) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.cancel(alarmId);
    }
}