package com.example.alarm.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.alarm.services.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("ALARM_ID", -1);
        int dayIndex = intent.getIntExtra("DAY_INDEX", -1);

        if (alarmId == -1) return;

        Log.d(TAG, "Alarm triggered for ID: " + alarmId + ", dayIndex: " + dayIndex);

        // Nếu là báo thức lặp lại, đặt lại cho tuần sau
        if (intent.getBooleanExtra("IS_REPEATING", false) && dayIndex != -1) {
            scheduleNextWeek(context, alarmId, dayIndex);
        }

        // Khởi động service để hiển thị báo thức - QUAN TRỌNG: Dùng alarm ID gốc
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("ALARM_ID", alarmId); // ID gốc, không phải request code

        // Truyền thêm thông tin từ intent gốc nếu có
        String alarmLabel = intent.getStringExtra("ALARM_LABEL");
        String alarmTone = intent.getStringExtra("ALARM_TONE");
        boolean vibrate = intent.getBooleanExtra("VIBRATE", true);

        if (alarmLabel != null) {
            serviceIntent.putExtra("ALARM_LABEL", alarmLabel);
        }
        if (alarmTone != null) {
            serviceIntent.putExtra("ALARM_TONE", alarmTone);
        }
        serviceIntent.putExtra("VIBRATE", vibrate);

        // Truyền thông tin snooze nếu có
        if (intent.getBooleanExtra("IS_SNOOZE", false)) {
            serviceIntent.putExtra("IS_SNOOZE", true);
            serviceIntent.putExtra("SNOOZE_HOUR", intent.getIntExtra("SNOOZE_HOUR", -1));
            serviceIntent.putExtra("SNOOZE_MINUTE", intent.getIntExtra("SNOOZE_MINUTE", -1));
            serviceIntent.putExtra("SNOOZE_LABEL", intent.getStringExtra("SNOOZE_LABEL"));
        }

        // Đảm bảo service được start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    // Đặt lại báo thức cho tuần sau
    private void scheduleNextWeek(Context context, int alarmId, int dayIndex) {
        new Thread(() -> {
            try {
                // Tạo intent cho tuần sau
                Intent nextIntent = new Intent(context, AlarmReceiver.class);
                nextIntent.putExtra("ALARM_ID", alarmId); // ID gốc
                nextIntent.putExtra("IS_REPEATING", true);
                nextIntent.putExtra("DAY_INDEX", dayIndex);

                // Tính thời gian cho tuần sau (+ 7 ngày)
                long nextWeekTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L);

                // Đặt alarm cho tuần sau - QUAN TRỌNG: Dùng request code đúng
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if (alarmManager != null) {
                    // Request code phải giống với logic trong AlarmUtils
                    int requestCode = alarmId * 10 + dayIndex;

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode, // Dùng request code đúng
                            nextIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    try {
                        // Android 12+ cần quyền SCHEDULE_EXACT_ALARM
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        nextWeekTime,
                                        pendingIntent
                                );
                                Log.d(TAG, "Scheduled next week alarm (exact) for ID: " + alarmId + ", day: " + dayIndex);
                            } else {
                                Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM trên Android 12+");
                            }
                        } else {
                            // Android 11 trở xuống vẫn chạy bình thường
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    nextWeekTime,
                                    pendingIntent
                            );
                            Log.d(TAG, "Scheduled next week alarm for ID: " + alarmId + ", day: " + dayIndex);
                        }
                    } catch (SecurityException se) {
                        Log.e(TAG, "SecurityException khi đặt exact alarm: " + se.getMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scheduling next week: " + e.getMessage());
            }
        }).start();
    }
}