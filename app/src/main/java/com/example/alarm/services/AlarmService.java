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
        int alarmId = intent.getIntExtra("ALARM_ID", -1);
        if (alarmId != -1) {
            // Hiển thị noti
            NotificationUtils.showNotification(this, alarmId, "Alarm", "Time to wake up!");

            // Khởi động ringing activity
            Intent ringingIntent = new Intent(this, AlarmRingingActivity.class);
            ringingIntent.putExtra("ALARM_ID", alarmId);
            ringingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(ringingIntent);
        }
        return START_NOT_STICKY;
    }
}
