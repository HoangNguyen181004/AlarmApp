package com.example.alarm.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.view.activities.AlarmRingingActivity;

public class AlarmService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int alarmId = intent.getIntExtra("ALARM_ID", -1);
            String alarmLabel = intent.getStringExtra("ALARM_LABEL");

            if (alarmId != -1) {
                // Start AlarmRingingActivity
                Intent alarmIntent = new Intent(this, AlarmRingingActivity.class);
                alarmIntent.putExtra("ALARM_ID", alarmId);

                // Pass snooze display info if available
                if (intent.getBooleanExtra("IS_SNOOZE", false)) {
                    alarmIntent.putExtra("IS_SNOOZE", true);
                    alarmIntent.putExtra("SNOOZE_HOUR", intent.getIntExtra("SNOOZE_HOUR", -1));
                    alarmIntent.putExtra("SNOOZE_MINUTE", intent.getIntExtra("SNOOZE_MINUTE", -1));
                    alarmIntent.putExtra("SNOOZE_LABEL", intent.getStringExtra("SNOOZE_LABEL"));
                }

                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(alarmIntent);

                // Hiển thị noti
                NotificationUtils.showNotification(this, alarmId, "Alarm", alarmLabel);
            }
        }

        return START_NOT_STICKY;
    }
}