package com.example.alarm.services;

import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.alarm.R;

public class TimerService extends Service {

    public static final String ACTION_START_TIMER = "START_TIMER";
    public static final String EXTRA_DELAY_MILLIS = "delay_millis";
    public static final String TIMER_FINISHED_ACTION = "com.example.alarm.TIMER_FINISHED";

    private Handler handler = new Handler();
    private Runnable timerRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START_TIMER.equals(intent.getAction())) {
            long delayMillis = intent.getLongExtra(EXTRA_DELAY_MILLIS, 0);

            // Hủy timer cũ nếu có
            if (timerRunnable != null) {
                handler.removeCallbacks(timerRunnable);
            }

            // Tạo timer mới
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    // Timer kết thúc
                    playNotificationSound();
                    showNotification();

                    // Gửi broadcast để fragment biết
                    Intent broadcast = new Intent(TIMER_FINISHED_ACTION);
                    sendBroadcast(broadcast);

                    stopSelf(); // Dừng service
                }
            };

            handler.postDelayed(timerRunnable, delayMillis);
        }

        return START_NOT_STICKY;
    }

    private void playNotificationSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, soundUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TimerChannel")
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Timer đã kết thúc!")
                .setContentText("Bộ đếm ngược đã hoàn thành")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(999, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}