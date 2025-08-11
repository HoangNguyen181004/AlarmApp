package com.example.alarm.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.example.alarm.model.entities.Alarm;
import com.example.alarm.receiver.AlarmReceiver;

import java.util.Calendar;

public class AlarmUtils {
    private static final String TAG = "AlarmUtils";

    public static void scheduleAlarm(Context context, Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule alarm for ID: " + alarm.id);
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarm.id);
        int requestCode = alarm.id;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerTime = calendar.getTimeInMillis();

        try {
            // Kiểm tra quyền từ Android S (API 31) trở lên
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        Log.d(TAG, "Exact alarm scheduled with setExactAndAllowWhileIdle for ID: " + alarm.id + " at " + triggerTime);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        Log.d(TAG, "Exact alarm scheduled with setExact for ID: " + alarm.id + " at " + triggerTime);
                    }
                } else {

                    Log.w(TAG, "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission. Alarm ID: " + alarm.id);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    Log.d(TAG, "Legacy exact alarm scheduled with setExactAndAllowWhileIdle for ID: " + alarm.id + " at " + triggerTime);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    Log.d(TAG, "Legacy exact alarm scheduled with setExact for ID: " + alarm.id + " at " + triggerTime);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while scheduling exact alarm for ID: " + alarm.id + ". Missing SCHEDULE_EXACT_ALARM permission or other security issue.", e);
        }
    }

    public static void cancelAlarm(Context context, Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot cancel alarm for ID: " + alarm.id);
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarm.id);
        int requestCode = alarm.id;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Canceled alarm with ID: " + alarm.id);
    }


    public static void snoozeAlarm(Context context, Alarm alarm, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);

        Alarm snoozedAlarm = new Alarm(
                alarm.id,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                "Snooze: " + alarm.label,
                null,
                alarm.ringtoneUri,
                true
        );

        Log.d(TAG, "Snoozing alarm ID: " + alarm.id + " for " + minutes + " minutes.");
        scheduleAlarm(context, snoozedAlarm);
    }

    public static void snoozeAlarmWithDisplay(Context context, Alarm alarm, int minutes, int displayHour, int displayMinute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot snooze alarm for ID: " + alarm.id);
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("ALARM_ID", alarm.id);

        // Add snooze display info to intent
        intent.putExtra("IS_SNOOZE", true);
        intent.putExtra("SNOOZE_HOUR", displayHour);
        intent.putExtra("SNOOZE_MINUTE", displayMinute);
        intent.putExtra("SNOOZE_LABEL", "Snooze: " + (alarm.label.isEmpty() ? "Báo thức" : alarm.label));

        int requestCode = alarm.id + 10000; // Different request code for snooze to avoid conflicts

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);
        long triggerTime = calendar.getTimeInMillis();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        Log.d(TAG, "Snooze alarm scheduled with setExactAndAllowWhileIdle for ID: " + alarm.id + " at " + triggerTime + " (Display: " + displayHour + ":" + displayMinute + ")");
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                        Log.d(TAG, "Snooze alarm scheduled with setExact for ID: " + alarm.id + " at " + triggerTime + " (Display: " + displayHour + ":" + displayMinute + ")");
                    }
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms for snooze. App needs SCHEDULE_EXACT_ALARM permission. Alarm ID: " + alarm.id);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    Log.d(TAG, "Legacy snooze alarm scheduled with setExactAndAllowWhileIdle for ID: " + alarm.id + " at " + triggerTime + " (Display: " + displayHour + ":" + displayMinute + ")");
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                    Log.d(TAG, "Legacy snooze alarm scheduled with setExact for ID: " + alarm.id + " at " + triggerTime + " (Display: " + displayHour + ":" + displayMinute + ")");
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while scheduling snooze alarm for ID: " + alarm.id + ". Missing SCHEDULE_EXACT_ALARM permission or other security issue.", e);
        }
    }
}
