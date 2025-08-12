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
    public static final String CHANNEL_ID = "alarm_channel_app_v2"; // Đảm bảo ID kênh là duy nhất
    private static final String CHANNEL_NAME = "Báo thức Ứng dụng";

    public static final String ACTION_DISMISS = "com.example.alarm.ACTION_DISMISS";
    public static final String ACTION_SNOOZE = "com.example.alarm.ACTION_SNOOZE";
    public static final String EXTRA_ALARM_ID = "com.example.alarm.EXTRA_ALARM_ID";

    // Request code cơ sở để tránh trùng lặp
    public static final int REQUEST_CODE_OPEN_ACTIVITY_BASE = 1000;
    public static final int REQUEST_CODE_DISMISS_BASE = 2000;
    public static final int REQUEST_CODE_SNOOZE_BASE = 3000;


    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo khi báo thức ứng dụng reo");
            channel.setSound(null, null); // QUAN TRỌNG: Không có âm thanh từ kênh
            channel.enableVibration(false); // QUAN TRỌNG: Không rung từ kênh
            channel.setBypassDnd(true); // Bỏ qua chế độ không làm phiền
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Hiển thị trên lockscreen
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // alarmId: ID của báo thức
    // timeStr: Chuỗi thời gian hiển thị (ví dụ: "08:00")
    // label: Nhãn của báo thức
    public static Notification buildAlarmNotification(Context context, int alarmId, String timeStr, String label) {
        // Intent khi nhấn vào nội dung thông báo (để mở AlarmRingingActivity)
        Intent openActivityIntent = new Intent(context, AlarmRingingActivity.class);
        openActivityIntent.putExtra(EXTRA_ALARM_ID, alarmId);
        // Truyền thêm thông tin mà Activity có thể cần
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
        // Nếu bạn muốn truyền thêm thông tin cho snooze (ví dụ: các chi tiết của báo thức gốc),
        // bạn có thể thêm chúng vào snoozeIntent ở đây.
        // Ví dụ: snoozeIntent.putExtra("ORIGINAL_ALARM_LABEL", label);
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
                // .setUsesChronometer(true) // Cân nhắc nếu bạn muốn hiển thị thời gian trôi qua
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
