package com.example.alarm.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.alarm.model.entities.Alarm;
import com.example.alarm.receiver.AlarmReceiver;

import java.util.Calendar;
import java.util.List;

public class AlarmUtils {
    private static final String TAG = "AlarmUtils";

    public static void scheduleAlarm(Context context, Alarm alarm) {
        // Hủy alarm cũ trước
        cancelAlarm(context, alarm);

        if (!alarm.enabled) return;

        // Kiểm tra có lặp lại không
        if (hasRepeatDays(alarm.repeatDays)) {
            scheduleRepeatingAlarm(context, alarm);
        } else {
            scheduleOneTimeAlarm(context, alarm);
        }
    }

    // Báo thức 1 lần
    private static void scheduleOneTimeAlarm(Context context, Alarm alarm) {
        Calendar nextTime = Calendar.getInstance();
        nextTime.set(Calendar.HOUR_OF_DAY, alarm.hour);
        nextTime.set(Calendar.MINUTE, alarm.minute);
        nextTime.set(Calendar.SECOND, 0);
        nextTime.set(Calendar.MILLISECOND, 0);

        // Nếu thời gian đã qua, đặt cho ngày mai
        if (nextTime.getTimeInMillis() <= System.currentTimeMillis()) {
            nextTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        setAlarm(context, alarm, alarm.id, nextTime.getTimeInMillis(), false, -1);
        Log.d(TAG, "Scheduled one-time alarm ID: " + alarm.id + " at " + formatTime(nextTime));
    }

    // Báo thức lặp lại
    private static void scheduleRepeatingAlarm(Context context, Alarm alarm) {
        Calendar now = Calendar.getInstance();

        // Đặt alarm cho mỗi ngày được chọn
        for (int day = 0; day < 7; day++) {
            if (alarm.repeatDays.get(day)) {
                Calendar nextTime = getNextTimeForDay(alarm.hour, alarm.minute, day);
                int requestCode = alarm.id * 10 + day; // ID riêng cho mỗi ngày
                setAlarm(context, alarm, requestCode, nextTime.getTimeInMillis(), true, day);
                Log.d(TAG, "Scheduled repeat alarm ID: " + alarm.id + " for " +
                        getDayName(day) + " at " + formatTime(nextTime));
            }
        }
    }

    // Tính thời gian báo thức tiếp theo cho ngày cụ thể
    private static Calendar getNextTimeForDay(int hour, int minute, int dayIndex) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Chuyển từ index (0=CN, 1=T2...) sang Calendar (1=CN, 2=T2...)
        int targetDay = (dayIndex == 0) ? Calendar.SUNDAY : dayIndex + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);

        // Tính số ngày cần thêm
        int daysToAdd = (targetDay - currentDay + 7) % 7;

        // Nếu cùng ngày nhưng giờ đã qua
        if (daysToAdd == 0 && calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            daysToAdd = 7; // Tuần sau
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return calendar;
    }

    // Đặt alarm thực tế - CẬP NHẬT để truyền thông tin alarm đầy đủ
    private static void setAlarm(Context context, Alarm alarm, int requestCode, long triggerTime, boolean isRepeating, int dayIndex) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarm.id); // ID gốc của alarm
        intent.putExtra("IS_REPEATING", isRepeating);

        // QUAN TRỌNG: Truyền thông tin alarm đầy đủ
        intent.putExtra("ALARM_LABEL", alarm.label);
        intent.putExtra("ALARM_TONE", alarm.ringtoneUri);

        if (isRepeating && dayIndex != -1) {
            intent.putExtra("DAY_INDEX", dayIndex); // Ngày trong tuần
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "No exact alarm permission");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot schedule alarm: " + e.getMessage());
        }
    }

    // Hủy alarm
    public static void cancelAlarm(Context context, Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Hủy alarm chính
        cancelSingleAlarm(context, alarmManager, alarm.id);

        // Hủy tất cả alarm lặp lại
        for (int day = 0; day < 7; day++) {
            cancelSingleAlarm(context, alarmManager, alarm.id * 10 + day);
        }

        // Hủy snooze
        cancelSingleAlarm(context, alarmManager, alarm.id + 10000);

        Log.d(TAG, "Canceled alarm ID: " + alarm.id);
    }

    private static void cancelSingleAlarm(Context context, AlarmManager alarmManager, int requestCode) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    // Snooze - CẬP NHẬT để truyền thông tin alarm
    public static void snoozeAlarmWithDisplay(Context context, Alarm alarm, int minutes,
                                              int displayHour, int displayMinute) {
        Calendar snoozeTime = Calendar.getInstance();
        snoozeTime.add(Calendar.MINUTE, minutes);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarm.id);
        intent.putExtra("IS_SNOOZE", true);
        intent.putExtra("SNOOZE_HOUR", displayHour);
        intent.putExtra("SNOOZE_MINUTE", displayMinute);
        intent.putExtra("SNOOZE_LABEL", "Snooze: " + (alarm.label.isEmpty() ? "Báo thức" : alarm.label));

        // Truyền thông tin alarm để có thể phát nhạc
        intent.putExtra("ALARM_LABEL", alarm.label);
        intent.putExtra("ALARM_TONE", alarm.ringtoneUri);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarm.id + 10000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            snoozeTime.getTimeInMillis(), pendingIntent);
                }
                Log.d(TAG, "Snoozed alarm ID: " + alarm.id + " for " + minutes + " minutes");
            } catch (SecurityException e) {
                Log.e(TAG, "Cannot schedule snooze: " + e.getMessage());
            }
        }
    }

    // Helper methods
    private static boolean hasRepeatDays(List<Boolean> repeatDays) {
        if (repeatDays == null || repeatDays.size() != 7) return false;
        for (Boolean day : repeatDays) {
            if (day != null && day) return true;
        }
        return false;
    }

    private static String getDayName(int dayIndex) {
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        return days[dayIndex];
    }

    private static String formatTime(Calendar cal) {
        return String.format("%02d:%02d %s",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                getDayName((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7));
    }
}