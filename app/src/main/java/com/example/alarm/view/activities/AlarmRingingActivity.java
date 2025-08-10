package com.example.alarm.view.activities;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.utils.NotificationUtils;

import java.util.List;

public class AlarmRingingActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private List<Alarm> alarms; // xử lý trùng alarm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_alarm);

        int alarmID = getIntent().getIntExtra("ALARM_ID", -1);

        TextView timeText = findViewById(R.id.alarmTimeText);
        TextView labelText = findViewById(R.id.alarmLabelText);
        timeText.setText(String.format("%0.2d:%0.2d", alarms.get(0).hour, alarms.get(0).minute));
        labelText.setText(alarms.get(0).label);

        startRinging(alarms.get(0).ringtoneUri);

        Button snoozeButton = findViewById(R.id.snoozeButton);
        snoozeButton.setOnClickListener(v -> {
            stopRinging();
            AlarmUtils.snoozeAlarm(this, alarms.get(0), 5); // Snooze 5 phút
            finish();
        });

        Button dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(v -> {
            stopRinging();
            for (Alarm alarm : alarms) {
                alarm.enabled = false;
                // update qua viewmodel
            }
            NotificationUtils.cancalNotification(this, alarmID);
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

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 1000};
        vibrator.vibrate(pattern, 0);
    }

    private void stopRinging() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    protected void onDestroy() {
        stopRinging();
        super.onDestroy();
    }
}
