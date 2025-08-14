package com.example.alarm.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.alarm.R;
import com.example.alarm.utils.TimeUtils;
import com.example.alarm.view.activities.MainActivity;

import java.util.List;

public class TimerService extends Service {

    public static final String ACTION_START_TIMER = "START_TIMER";
    public static final String ACTION_PAUSE_TIMER = "PAUSE_TIMER";
    public static final String ACTION_STOP_TIMER = "STOP_TIMER";
    public static final String EXTRA_DELAY_MILLIS = "delay_millis";
    public static final String TIMER_FINISHED_ACTION = "com.example.alarm.TIMER_FINISHED";
    public static final String TIMER_TICK_ACTION = "com.example.alarm.TIMER_TICK";
    public static final String EXTRA_REMAINING_MILLIS = "remaining_millis";

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final int FINISHED_NOTIFICATION_ID = 999;
    private static final String CHANNEL_ID = "TimerChannel";
    private static final String FINISHED_CHANNEL_ID = "TimerFinishedChannel";
    private static final String TAG = "TimerService";

    private CountDownTimer countDownTimer;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private Handler handler;
    private long totalMillis = 0;
    private boolean hasTimerFinished = false; // Thêm biến để track trạng thái

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
        Log.d(TAG, "TimerService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called with action: " + (intent != null ? intent.getAction() : "null"));
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_START_TIMER.equals(action)) {
                long delayMillis = intent.getLongExtra(EXTRA_DELAY_MILLIS, 0);
                Log.d(TAG, "Starting timer with " + delayMillis + " ms");
                startTimer(delayMillis);
            } else if (ACTION_PAUSE_TIMER.equals(action)) {
                Log.d(TAG, "Pausing timer");
                pauseTimer();
            } else if (ACTION_STOP_TIMER.equals(action)) {
                Log.d(TAG, "Stopping timer");
                stopTimer();
            }
        }

        return START_STICKY;
    }

    private void startTimer(long millis) {
        totalMillis = millis;
        hasTimerFinished = false; // Reset trạng thái khi start timer mới

        // Hủy timer cũ nếu có
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Tạo timer mới với cập nhật mỗi giây
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Cập nhật thông báo với thời gian còn lại
                updateOngoingNotification(millisUntilFinished);

                // Gửi broadcast để cập nhật UI (chỉ khi fragment active)
                Intent tickIntent = new Intent(TIMER_TICK_ACTION);
                tickIntent.putExtra(EXTRA_REMAINING_MILLIS, millisUntilFinished);
                sendBroadcast(tickIntent);
                Log.d(TAG, "Timer tick: " + millisUntilFinished + " ms remaining");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer finished");
                onTimerFinished();
            }
        };

        countDownTimer.start();

        // Bắt đầu foreground service với thông báo
        Notification notification = createOngoingNotification(millis);
        if (notification != null) {
            startForeground(ONGOING_NOTIFICATION_ID, notification);
            Log.d(TAG, "Foreground service started with ongoing notification");
        } else {
            Log.e(TAG, "Notification is null, cannot start foreground");
            stopSelf();
        }
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopForeground(true);
        hasTimerFinished = false; // Reset trạng thái khi pause
        Log.d(TAG, "Timer paused, foreground stopped");
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopForeground(true);
        notificationManager.cancel(FINISHED_NOTIFICATION_ID);
        hasTimerFinished = false; // Reset trạng thái khi stop
        stopSelf();
        Log.d(TAG, "Timer stopped, service stopped");
    }

    private void onTimerFinished() {
        // Kiểm tra để tránh gọi nhiều lần
        if (hasTimerFinished) {
            Log.d(TAG, "Timer already finished, ignoring duplicate call");
            return;
        }

        hasTimerFinished = true;

        // Dừng timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Dừng foreground service
        stopForeground(true);

        // Kiểm tra xem app có đang ở foreground không
        boolean isAppInForeground = isAppInForeground();
        Log.d(TAG, "App is in foreground: " + isAppInForeground);

        // Chỉ phát âm thanh và rung + hiển thị notification khi app KHÔNG ở foreground
        // Khi app ở foreground, UI sẽ xử lý hiển thị và âm thanh thông qua broadcast
        if (!isAppInForeground) {
            playAlarmSoundAndVibration();
            showFinishedNotification();
            Log.d(TAG, "Timer finished - app in background: played sound and showed notification");
        } else {
            Log.d(TAG, "Timer finished - app in foreground: skipped sound and notification, UI will handle it");
        }

        // Luôn gửi broadcast để fragment biết (nếu đang active)
        Intent broadcast = new Intent(TIMER_FINISHED_ACTION);
        sendBroadcast(broadcast);

        // Dừng service sau 30 giây hoặc khi user tương tác
        handler.postDelayed(() -> stopSelf(), 30000);
    }

    private void playAlarmSoundAndVibration() {
        try {
            // Phát âm thanh
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            if (alarmUri != null && audioManager != null) {
                // Tăng âm lượng notification lên mức tối đa tạm thời
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

                if (currentVolume < maxVolume / 2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxVolume, 0);
                }

                Log.d(TAG, "Playing alarm sound: " + alarmUri);
            }

            // Rung điện thoại
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Pattern: rung 1s, nghỉ 0.5s, rung 1s, nghỉ 0.5s, rung 2s
                    long[] pattern = {0, 1000, 500, 1000, 500, 2000};
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                    vibrator.vibrate(effect);
                } else {
                    long[] pattern = {0, 1000, 500, 1000, 500, 2000};
                    vibrator.vibrate(pattern, -1);
                }
                Log.d(TAG, "Vibration started");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound or vibration", e);
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Kênh cho thông báo đang chạy
            NotificationChannel ongoingChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Timer Ongoing",
                    NotificationManager.IMPORTANCE_LOW
            );
            ongoingChannel.setDescription("Hiển thị thời gian đếm ngược");
            ongoingChannel.setSound(null, null);
            ongoingChannel.enableVibration(false);
            ongoingChannel.setShowBadge(false);
            ongoingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(ongoingChannel);
            Log.d(TAG, "Created ongoing notification channel");

            // Kênh cho thông báo kết thúc - QUAN TRỌNG: IMPORTANCE_HIGH để có âm thanh
            NotificationChannel finishedChannel = new NotificationChannel(
                    FINISHED_CHANNEL_ID,
                    "Timer Finished",
                    NotificationManager.IMPORTANCE_HIGH  // QUAN TRỌNG!
            );
            finishedChannel.setDescription("Thông báo khi hẹn giờ hoàn thành");

            // Sử dụng âm thanh báo thức thay vì notification thường
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)  // Sử dụng ALARM thay vì NOTIFICATION
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build();

            finishedChannel.setSound(alarmUri, audioAttributes);
            finishedChannel.enableVibration(true);
            finishedChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 2000});
            finishedChannel.setShowBadge(true);
            finishedChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            finishedChannel.setBypassDnd(true); // Bỏ qua chế độ không làm phiền

            notificationManager.createNotificationChannel(finishedChannel);
            Log.d(TAG, "Created finished notification channel with alarm sound: " + alarmUri);
        }
    }

    private Notification createOngoingNotification(long remainingMillis) {
        String timeText = TimeUtils.formatMillisToMinutesSeconds(remainingMillis);

        // Tạo intent để mở lại app khi tap vào thông báo
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo action buttons
        Intent pauseIntent = new Intent(this, TimerService.class);
        pauseIntent.setAction(ACTION_PAUSE_TIMER);
        PendingIntent pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, TimerService.class);
        stopIntent.setAction(ACTION_STOP_TIMER);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                2,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Bộ đếm ngược đang chạy")
                .setContentText("Thời gian còn lại: " + timeText)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_pause, "Tạm dừng", pausePendingIntent)
                .addAction(R.drawable.ic_stop, "Dừng", stopPendingIntent)
                .setProgress(100, calculateProgress(remainingMillis), false);

        return builder.build();
    }

    private void updateOngoingNotification(long remainingMillis) {
        Notification notification = createOngoingNotification(remainingMillis);
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
    }

    private void showFinishedNotification() {
        // Tạo intent để mở lại app
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                3,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Sử dụng âm thanh báo thức
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        long[] vibrationPattern = {0, 1000, 500, 1000, 500, 2000};

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FINISHED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("HẸN GIỜ ĐÃ KẾT THÚC!")
                .setContentText("Bộ đếm ngược của bạn đã hoàn thành!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Bộ đếm ngược của bạn đã hoàn thành!\nChạm để mở ứng dụng."))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)  // Ưu tiên cao nhất
                .setCategory(NotificationCompat.CATEGORY_ALARM)  // Là báo thức
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(alarmUri)
                .setVibrate(vibrationPattern)
                .setAutoCancel(true)
                .setOngoing(false)
                .setFullScreenIntent(pendingIntent, true)  // Hiển thị full screen trên lock screen
                .setProgress(0, 0, false);

        try {
            notificationManager.notify(FINISHED_NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Finished notification shown with ID: " + FINISHED_NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error showing finished notification", e);
        }
    }

    private int calculateProgress(long remainingMillis) {
        if (totalMillis <= 0) return 0;
        return (int) (((double) (totalMillis - remainingMillis) / totalMillis) * 100);
    }

    private boolean isAppInForeground() {
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (activityManager == null) return false;

            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) return false;

            String packageName = getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (packageName.equals(appProcess.processName)) {
                    boolean isForeground = appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                    Log.d(TAG, "App process found, importance: " + appProcess.importance + ", isForeground: " + isForeground);
                    return isForeground;
                }
            }

            Log.d(TAG, "App process not found in running processes");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if app is in foreground", e);
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        notificationManager.cancel(FINISHED_NOTIFICATION_ID);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        hasTimerFinished = false; // Reset trạng thái khi destroy
        Log.d(TAG, "TimerService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}