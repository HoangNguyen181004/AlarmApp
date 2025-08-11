package com.example.alarm.view.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.utils.AlarmUtils;
import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.viewmodel.AlarmViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmRingingActivity extends AppCompatActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_alarm);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        int alarmId = getIntent().getIntExtra("ALARM_ID", -1);

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
                    finish(); // Không có alarm, thoát
                }
            });
        });

        Button snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnClickListener(v -> {
            stopRinging();
            if (!alarms.isEmpty()) {
                // Calculate snooze time
                Calendar snoozeTime = Calendar.getInstance();
                snoozeTime.add(Calendar.MINUTE, 5);

                // Use enhanced snooze method
                AlarmUtils.snoozeAlarmWithDisplay(this, alarms.get(0), 5,
                        snoozeTime.get(Calendar.HOUR_OF_DAY),
                        snoozeTime.get(Calendar.MINUTE));
            }
            finish();
        });

        Button dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(v -> {
            stopRinging();
            for (Alarm alarm : alarms) {
                alarm.enabled = false;
                viewModel.update(alarm);
            }
            NotificationUtils.cancelNotification(this, alarmId);
            finish();
        });
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 1000};
        vibrator.vibrate(pattern, 0);
    }

    private boolean isStopped = false;

    private void stopRinging() {
        if (mediaPlayer != null && !isStopped) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace(); // Log lỗi nhưng không crash
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
    protected void onDestroy() {
        stopRinging();
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