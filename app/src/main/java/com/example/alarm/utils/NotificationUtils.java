package com.example.alarm.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.alarm.R;
import com.example.alarm.receiver.AlarmNotificationActionReceiver;
import com.example.alarm.view.activities.AlarmRingingActivity;

public class NotificationUtils {
    public static final String CHANNEL_ID = "alarm_channel_app_v2"; // Kênh chính cho notification hiển thị
    public static final String HIDDEN_CHANNEL_ID = "alarm_service_hidden"; // Kênh ẩn cho foreground service
    private static final String CHANNEL_NAME = "Báo thức Ứng dụng";
    private static final String HIDDEN_CHANNEL_NAME = "Dịch vụ báo thức";

    public static final String ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS";
    public static final String ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE";
    public static final String EXTRA_ALARM_ID = "com.example.alarm.EXTRA_ALARM_ID";

    // Request code cơ sở để tránh trùng lặp
    public static final int REQUEST_CODE_OPEN_ACTIVITY_BASE = 1000;
    public static final int REQUEST_CODE_DISMISS_BASE = 2000;
    public static final int REQUEST_CODE_SNOOZE_BASE = 3000;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Kênh chính cho notification hiển thị
            NotificationChannel mainChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            mainChannel.setDescription("Thông báo khi báo thức ứng dụng reo");
            mainChannel.setSound(null, null); // QUAN TRỌNG: Không có âm thanh từ kênh
            mainChannel.enableVibration(false); // QUAN TRỌNG: Không rung từ kênh
            mainChannel.setBypassDnd(true); // Bỏ qua chế độ không làm phiền
            mainChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Hiển thị trên lockscreen
            mainChannel.setShowBadge(true);
            manager.createNotificationChannel(mainChannel);

            // Kênh ẩn cho foreground service (không hiển thị với người dùng)
            NotificationChannel hiddenChannel = new NotificationChannel(HIDDEN_CHANNEL_ID, HIDDEN_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            hiddenChannel.setDescription("Dịch vụ nền cho báo thức");
            hiddenChannel.setSound(null, null);
            hiddenChannel.enableVibration(false);
            hiddenChannel.setShowBadge(false);
            hiddenChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET); // Ẩn trên lockscreen
            manager.createNotificationChannel(hiddenChannel);
        }
    }

    // Notification chính cho màn hình bật (có thể tương tác)
    public static Notification buildAlarmNotification(Context context, int alarmId, String timeStr, String label) {
        // Intent khi nhấn vào nội dung thông báo (để mở AlarmRingingActivity)
        Intent openActivityIntent = new Intent(context, AlarmRingingActivity.class);
        openActivityIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        openActivityIntent.putExtra("ALARM_LABEL", label);
        openActivityIntent.putExtra("ALARM_TIME_STR", timeStr);
        openActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(context,
                REQUEST_CODE_OPEN_ACTIVITY_BASE + alarmId,
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent và PendingIntent cho hành động "Tắt"
        Intent dismissIntent = new Intent(context, AlarmNotificationActionReceiver.class);
        dismissIntent.setAction(ACTION_DISMISS);
        dismissIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context,
                REQUEST_CODE_DISMISS_BASE + alarmId,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent và PendingIntent cho hành động "Báo lại"
        Intent snoozeIntent = new Intent(context, AlarmNotificationActionReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context,
                REQUEST_CODE_SNOOZE_BASE + alarmId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Báo thức - " + timeStr)
                .setContentText(label != null && !label.isEmpty() ? label : "Đã đến giờ!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Thông báo không thể vuốt đi
                .setAutoCancel(false) // Không tự hủy khi nhấn
                .setFullScreenIntent(openPendingIntent, true) // QUAN TRỌNG: Để hiển thị Activity khi màn hình khóa
                .setContentIntent(openPendingIntent) // Khi nhấn vào nội dung thông báo
                .addAction(0, "Tắt", dismissPendingIntent)
                .addAction(0, "Báo lại 5 phút", snoozePendingIntent)
                .setDeleteIntent(dismissPendingIntent) // Nếu thông báo bị hủy bằng cách khác
                .build();
    }

    // Notification ẩn cho foreground service (màn hình tắt)
    public static Notification buildHiddenServiceNotification(Context context, int alarmId, String timeStr, String label) {
        // Intent đơn giản để mở activity khi cần
        Intent openActivityIntent = new Intent(context, AlarmRingingActivity.class);
        openActivityIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        openActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent openPendingIntent = PendingIntent.getActivity(context,
                REQUEST_CODE_OPEN_ACTIVITY_BASE + alarmId + 10000, // Offset để tránh conflict
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, HIDDEN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Dịch vụ báo thức")
                .setContentText("Đang chạy trong nền")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(openPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Ẩn trên lock screen
                .build();
    }

    public static void showNotification(Context context, int alarmId, String timeStr, String label) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        Notification notification = buildAlarmNotification(context, alarmId, timeStr, label);
        manager.notify(alarmId, notification);
    }

    public static void cancelNotification(Context context, int alarmId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(alarmId);
        }
    }
}