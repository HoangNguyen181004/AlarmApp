package com.example.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.alarm.services.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("ALARM_ID", -1);
        if (alarmId != -1) {
            // Kiểm tra trùng alarm và gộp nếu cần (lấy từ repository)
            // Ở đây giản hóa: khởi động service để hiển thị notification và activity
            Intent serviceIntent = new Intent(context, AlarmService.class);
            serviceIntent.putExtra("ALARM_ID", alarmId);

            // Pass snooze display info if available
            if (intent.getBooleanExtra("IS_SNOOZE", false)) {
                serviceIntent.putExtra("IS_SNOOZE", true);
                serviceIntent.putExtra("SNOOZE_HOUR", intent.getIntExtra("SNOOZE_HOUR", -1));
                serviceIntent.putExtra("SNOOZE_MINUTE", intent.getIntExtra("SNOOZE_MINUTE", -1));
                serviceIntent.putExtra("SNOOZE_LABEL", intent.getStringExtra("SNOOZE_LABEL"));
            }

            context.startService(serviceIntent);
        }
    }
}