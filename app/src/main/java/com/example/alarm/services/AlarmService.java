package com.example.alarm.services;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.view.activities.AlarmRingingActivity;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    public static final String ACTION_STOP_ALARM = "com.example.alarm.STOP_ALARM";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Handler autoStopHandler;
    private Runnable autoStopRunnable;

    // Auto stop after 2 minutes if no action
    private static final long AUTO_STOP_DELAY = 2 * 60 * 1000; // 2 phút
    private static final int FOREGROUND_NOTIFICATION_ID = 9999; // ID riêng cho foreground service

    private int currentAlarmId = -1;
    private boolean isScreenOnMode = false; // Flag để track mode hiện tại

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (ACTION_STOP_ALARM.equals(action)) {
                int alarmId = intent.getIntExtra("ALARM_ID", -1);
                Log.d(TAG, "Stopping alarm service for ID: " + alarmId);
                stopAlarmRinging();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            int alarmId = intent.getIntExtra("ALARM_ID", -1);
            String alarmLabel = intent.getStringExtra("ALARM_LABEL");
            String alarmTone = intent.getStringExtra("ALARM_TONE");
            boolean vibrate = intent.getBooleanExtra("VIBRATE", true);

            if (alarmId != -1) {
                currentAlarmId = alarmId;

                // Kiểm tra màn hình có bật không
                boolean isScreenOn = isScreenOn();
                isScreenOnMode = isScreenOn;
                Log.d(TAG, "Screen is on: " + isScreenOn);

                String timeStr = getCurrentTimeString();

                if (!isScreenOn) {
                    // Màn hình tắt - sử dụng notification ẩn và hiển thị Activity
                    Notification hiddenNotification = NotificationUtils.buildHiddenServiceNotification(
                            this, alarmId, timeStr, alarmLabel != null ? alarmLabel : "Báo thức");
                    startForeground(FOREGROUND_NOTIFICATION_ID, hiddenNotification);

                    startAlarmActivity(intent);
                    Log.d(TAG, "Screen off - started activity with hidden notification");
                } else {
                    // Màn hình bật - hiển thị notification đầy đủ
                    Notification fullNotification = NotificationUtils.buildAlarmNotification(
                            this, alarmId, timeStr, alarmLabel != null ? alarmLabel : "Báo thức");
                    startForeground(FOREGROUND_NOTIFICATION_ID, fullNotification);
                    Log.d(TAG, "Screen on - showing full notification");
                }

                // Bắt đầu phát chuông báo thức
                startAlarmRinging(alarmTone, vibrate);

                // Đặt auto stop
                setupAutoStop();
            }
        }

        return START_NOT_STICKY;
    }

    private void startAlarmActivity(Intent serviceIntent) {
        Intent alarmIntent = new Intent(this, AlarmRingingActivity.class);
        alarmIntent.putExtra("ALARM_ID", serviceIntent.getIntExtra("ALARM_ID", -1));

        // Pass snooze display info if available
        if (serviceIntent.getBooleanExtra("IS_SNOOZE", false)) {
            alarmIntent.putExtra("IS_SNOOZE", true);
            alarmIntent.putExtra("SNOOZE_HOUR", serviceIntent.getIntExtra("SNOOZE_HOUR", -1));
            alarmIntent.putExtra("SNOOZE_MINUTE", serviceIntent.getIntExtra("SNOOZE_MINUTE", -1));
            alarmIntent.putExtra("SNOOZE_LABEL", serviceIntent.getStringExtra("SNOOZE_LABEL"));
        }

        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        try {
            startActivity(alarmIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting alarm activity", e);
        }
    }

    private void startAlarmRinging(String ringtoneUri, boolean shouldVibrate) {
        try {
            // Start MediaPlayer
            if (ringtoneUri != null && !ringtoneUri.isEmpty()) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, Uri.parse(ringtoneUri));
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "Started alarm sound");
            }

            // Start Vibration
            if (shouldVibrate) {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    long[] pattern = {0, 1000, 1000}; // Rung 1s, nghỉ 1s
                    vibrator.vibrate(pattern, 0);
                    Log.d(TAG, "Started vibration");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting alarm: " + e.getMessage());
        }
    }

    private void stopAlarmRinging() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "Stopped alarm sound");
            }

            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
                Log.d(TAG, "Stopped vibration");
            }

            // Cancel auto stop
            if (autoStopHandler != null && autoStopRunnable != null) {
                autoStopHandler.removeCallbacks(autoStopRunnable);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping alarm: " + e.getMessage());
        }
    }

    private void setupAutoStop() {
        if (autoStopHandler == null) {
            autoStopHandler = new Handler();
        }

        autoStopRunnable = () -> {
            Log.d(TAG, "Auto stopping alarm after timeout");
            stopAlarmRinging();

            // Hủy notification user-visible nếu có
            if (currentAlarmId != -1) {
                NotificationUtils.cancelNotification(this, currentAlarmId);
            }

            stopForeground(true);
            stopSelf();
        };

        autoStopHandler.postDelayed(autoStopRunnable, AUTO_STOP_DELAY);
        Log.d(TAG, "Auto stop scheduled in " + (AUTO_STOP_DELAY / 1000) + " seconds");
    }

    private boolean isScreenOn() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            return powerManager.isInteractive();
        }
        return true; // Default to true if can't determine
    }

    private String getCurrentTimeString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AlarmService destroyed");
        stopAlarmRinging();

        // Hủy notification user-visible nếu có
        if (currentAlarmId != -1) {
            NotificationUtils.cancelNotification(this, currentAlarmId);
        }

        stopForeground(true);
        super.onDestroy();
    }
}