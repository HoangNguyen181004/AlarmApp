package com.example.alarm.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static String formatMillisToTime(long millis) {
        int minutes = (int) (millis / 60000);
        int seconds = (int) ((millis / 1000) % 60);
        int centis = (int) ((millis / 10) % 100);
        return String.format("%02d:%02d.%02d", minutes, seconds, centis);
    }

    public static String formatMillisToMinutesSeconds(long millis) {
        int minutes = (int) (millis / 60000);
        int seconds = (int) ((millis / 1000) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }
}
