package com.example.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.alarm.model.database.AlarmDatabase;
import com.example.alarm.model.database.AlarmDao;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.services.AlarmService;
import com.example.alarm.utils.AlarmUtils;
import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.viewmodel.AlarmViewModel;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmNotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmNotificationAction";
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AlarmViewModel viewModel;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int alarmId = intent.getIntExtra(NotificationUtils.EXTRA_ALARM_ID, -1);

        Log.d(TAG, "Received action: " + action + " for alarm ID: " + alarmId);

        if (alarmId == -1) return;

        if (NotificationUtils.ACTION_DISMISS.equals(action)) {
            handleDismiss(context, alarmId);
        } else if (NotificationUtils.ACTION_SNOOZE.equals(action)) {
            handleSnooze(context, alarmId);
        }
    }

    private void handleDismiss(Context context, int alarmId) {
        // Tắt service và notification
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.setAction(AlarmService.ACTION_STOP_ALARM);
        serviceIntent.putExtra("ALARM_ID", alarmId);
        context.startService(serviceIntent);

        NotificationUtils.cancelNotification(context, alarmId);

        // Tắt alarm trong database (background thread)
        executor.execute(() -> {
            try {
                AlarmDatabase db = AlarmDatabase.getDatabase(context.getApplicationContext());
                Alarm alarm = db.alarmDao().getAlarmById(alarmId);
                if (alarm != null) {
                    // Chỉ tắt alarm nếu không phải alarm lặp lại
                    if (alarm.repeatDays == null || !hasRepeatDays(alarm.repeatDays)) {
                        alarm.enabled = false;
                        db.alarmDao().update(alarm);
                        Log.d(TAG, "DISABLED one-time alarm ID: " + alarmId);

                        // THÊM: Gửi broadcast để UI cập nhật
                        Intent updateIntent = new Intent("com.example.alarm.ACTION_ALARM_STATE_CHANGED");
                        updateIntent.putExtra("ALARM_ID", alarmId);
                        updateIntent.putExtra("ENABLED", false);
                        context.sendBroadcast(updateIntent);
                        Log.d(TAG, "Sent broadcast for UI update");
                    } else {
                        Log.d(TAG, "Repeating alarm ID: " + alarmId + " - keeping enabled");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling dismiss: " + e.getMessage());
            }
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

    private void handleSnooze(Context context, int alarmId) {
        // Tắt service hiện tại
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.setAction(AlarmService.ACTION_STOP_ALARM);
        serviceIntent.putExtra("ALARM_ID", alarmId);
        context.startService(serviceIntent);

        NotificationUtils.cancelNotification(context, alarmId);

        // Đặt snooze (background thread)
        executor.execute(() -> {
            try {
                AlarmDatabase db = AlarmDatabase.getDatabase(context.getApplicationContext());
                Alarm alarm = db.alarmDao().getAlarmById(alarmId);
                if (alarm != null) {
                    Calendar snoozeTime = Calendar.getInstance();
                    snoozeTime.add(Calendar.MINUTE, 5);

                    AlarmUtils.snoozeAlarmWithDisplay(context, alarm, 5,
                            snoozeTime.get(Calendar.HOUR_OF_DAY),
                            snoozeTime.get(Calendar.MINUTE));

                    Log.d(TAG, "Snoozed alarm ID: " + alarmId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error snoozing alarm: " + e.getMessage());
            }
        });
    }
}