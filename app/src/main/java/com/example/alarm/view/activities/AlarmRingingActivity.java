package com.example.alarm.view.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.services.AlarmService;
import com.example.alarm.utils.AlarmUtils;
import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.viewmodel.AlarmViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmRingingActivity extends AppCompatActivity {
    private static final String TAG = "AlarmRingingActivity";

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private List<Alarm> alarms = new ArrayList<>();
    private AlarmViewModel viewModel;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Add fields to handle snooze display
    private boolean isSnoozeAlarm = false;
    private int displayHour = -1;
    private int displayMinute = -1;
    private String displayLabel = "";

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "AlarmRingingActivity onCreate");
        super.onCreate(savedInstanceState);

        // Thiết lập window flags để hiển thị trên lock screen
        setupWindowFlags();

        // Acquire wake lock để giữ màn hình sáng
        acquireWakeLock();

        setContentView(R.layout.activity_alarm);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        int alarmId = getIntent().getIntExtra("ALARM_ID", -1);
        Log.d(TAG, "Alarm ID: " + alarmId);

        // Check if this is a snooze alarm with custom time
        isSnoozeAlarm = getIntent().getBooleanExtra("IS_SNOOZE", false);
        displayHour = getIntent().getIntExtra("SNOOZE_HOUR", -1);
        displayMinute = getIntent().getIntExtra("SNOOZE_MINUTE", -1);
        displayLabel = getIntent().getStringExtra("SNOOZE_LABEL");

        TextView timeText = findViewById(R.id.alarmTimeText);
        TextView labelText = findViewById(R.id.alarmLabelText);

        // Load alarm async để tránh main thread
        executor.execute(() -> {
            Alarm alarm = viewModel.getAlarmById(alarmId);
            runOnUiThread(() -> {
                if (alarm != null) {
                    alarms.add(alarm);

                    // Display snooze time if available, otherwise original time
                    if (isSnoozeAlarm && displayHour != -1 && displayMinute != -1) {
                        timeText.setText(String.format("%02d:%02d", displayHour, displayMinute));
                        labelText.setText(displayLabel != null ? displayLabel :
                                (alarm.label.isEmpty() ? "Báo thức" : "Snooze: " + alarm.label));
                    } else {
                        timeText.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
                        labelText.setText(alarm.label.isEmpty() ? "Báo thức" : alarm.label);
                    }

                    startRinging(alarm.ringtoneUri);
                } else {
                    Log.e(TAG, "No alarm found for ID: " + alarmId);
                    finish(); // Không có alarm, thoát
                }
            });
        });

        Button snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnClickListener(v -> {
            Log.d(TAG, "Snooze button clicked");
            stopRinging();

            // Tắt service
            Intent serviceIntent = new Intent(this, AlarmService.class);
            serviceIntent.setAction(AlarmService.ACTION_STOP_ALARM);
            serviceIntent.putExtra("ALARM_ID", alarmId);
            startService(serviceIntent);

            if (!alarms.isEmpty()) {
                // Calculate snooze time
                Calendar snoozeTime = Calendar.getInstance();
                snoozeTime.add(Calendar.MINUTE, 5);

                // Use enhanced snooze method
                AlarmUtils.snoozeAlarmWithDisplay(this, alarms.get(0), 5,
                        snoozeTime.get(Calendar.HOUR_OF_DAY),
                        snoozeTime.get(Calendar.MINUTE));
            }
            NotificationUtils.cancelNotification(this, alarmId);
            finish();
        });

        Button dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(v -> {
            Log.d(TAG, "Dismiss button clicked");
            stopRinging();

            // Tắt service
            Intent serviceIntent = new Intent(this, AlarmService.class);
            serviceIntent.setAction(AlarmService.ACTION_STOP_ALARM);
            serviceIntent.putExtra("ALARM_ID", alarmId);
            startService(serviceIntent);

            for (Alarm alarm : alarms) {
                if (alarm.repeatDays == null || !hasRepeatDays(alarm.repeatDays)) {
                    alarm.enabled = false;
                    viewModel.update(alarm);
                }
            }
//            NotificationUtils.cancelNotification(this, alarmId);
            finish();
        });
    }

    // Helper method để kiểm tra có repeat days không
    private boolean hasRepeatDays(java.util.List<Boolean> repeatDays) {
        if (repeatDays == null || repeatDays.size() != 7) return false;
        for (Boolean day : repeatDays) {
            if (day != null && day) return true;
        }
        return false;
    }

    private void setupWindowFlags() {
        // Đối với Android 8.1+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            // Đối với Android cũ hơn
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // Các flags bổ sung để đảm bảo activity hiển thị
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Đặt độ sáng tối đa
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        window.setAttributes(params);
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "AlarmApp:AlarmWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes max
            Log.d(TAG, "Wake lock acquired");
        }
    }

    private void startRinging(String ringtoneUriStr) {
        Uri ringtoneUri = Uri.parse(ringtoneUriStr);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, ringtoneUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.d(TAG, "Started ringing in activity");
        } catch (Exception e) {
            Log.e(TAG, "Error starting media player", e);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 1000, 1000};
            vibrator.vibrate(pattern, 0);
            Log.d(TAG, "Started vibration in activity");
        }
    }

    private boolean isStopped = false;

    private void stopRinging() {
        Log.d(TAG, "Stopping ringing");

        if (mediaPlayer != null && !isStopped) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping media player", e);
            } finally {
                try {
                    mediaPlayer.release();
                } catch (Exception ignore) {
                }
                mediaPlayer = null;
                isStopped = true;
            }
        }

        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignore) {
            }
            vibrator = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called - but staying active");
        // Không làm gì ở đây, để activity tiếp tục hoạt động
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");

        stopRinging();

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.onDestroy();
    }
}