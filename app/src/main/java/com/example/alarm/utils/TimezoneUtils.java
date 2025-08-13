package com.example.alarm.utils;

import com.example.alarm.model.entities.TimezoneInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimezoneUtils {

    // Phương thức này trả về danh sách tất cả các timezone phổ biến với tên thành phố
    // Mỗi timezone sẽ bao gồm tên thành phố, ID timezone, tên hiển thị
    // và độ lệch giờ tính bằng phút so với GMT
    public static List<TimezoneInfo> getAllTimezones() {
        List<TimezoneInfo> timezones = new ArrayList<>();

        // Danh sách các timezone phổ biến với tên thành phố
        String[][] commonTimezones = {
                // Asia
                {"Hà Nội", "Asia/Ho_Chi_Minh"},
                {"Tokyo", "Asia/Tokyo"},
                {"Seoul", "Asia/Seoul"},
                {"Bangkok", "Asia/Bangkok"},
                {"Singapore", "Asia/Singapore"},
                {"Bắc Kinh", "Asia/Shanghai"},
                {"Đài Bắc", "Asia/Taipei"},
                {"Mumbai", "Asia/Kolkata"},
                {"Delhi", "Asia/Kolkata"},
                {"Karachi", "Asia/Karachi"},
                {"Dubai", "Asia/Dubai"},
                {"Riyadh", "Asia/Riyadh"},
                {"Tehran", "Asia/Tehran"},

                // Europe
                {"Moscow", "Europe/Moscow"},
                {"Istanbul", "Europe/Istanbul"},
                {"Athens", "Europe/Athens"},
                {"Berlin", "Europe/Berlin"},
                {"Paris", "Europe/Paris"},
                {"Rome", "Europe/Rome"},
                {"Madrid", "Europe/Madrid"},
                {"Lisbon", "Europe/Lisbon"},
                {"London", "Europe/London"},
                {"Warsaw", "Europe/Warsaw"},
                {"Stockholm", "Europe/Stockholm"},

                // Americas
                {"New York", "America/New_York"},
                {"Chicago", "America/Chicago"},
                {"Denver", "America/Denver"},
                {"Los Angeles", "America/Los_Angeles"},
                {"Mexico City", "America/Mexico_City"},
                {"São Paulo", "America/Sao_Paulo"},
                {"Buenos Aires", "America/Argentina/Buenos_Aires"},
                {"Santiago", "America/Santiago"},
                {"Lima", "America/Lima"},
                {"Bogotá", "America/Bogota"},
                {"Toronto", "America/Toronto"},
                {"Vancouver", "America/Vancouver"},

                // Pacific & Oceania
                {"Sydney", "Australia/Sydney"},
                {"Melbourne", "Australia/Melbourne"},
                {"Auckland", "Pacific/Auckland"},
                {"Fiji", "Pacific/Fiji"},
                {"Honolulu", "Pacific/Honolulu"},

                // Africa
                {"Cairo", "Africa/Cairo"},
                {"Johannesburg", "Africa/Johannesburg"},
                {"Nairobi", "Africa/Nairobi"},
                {"Lagos", "Africa/Lagos"},
                {"Casablanca", "Africa/Casablanca"}
        };


        // Duyệt qua danh sách các timezone và tạo đối tượng TimezoneInfo
        // với tên thành phố, ID timezone, tên hiển thị và độ lệch giờ
        for (String[] timezone : commonTimezones) {
            String cityName = timezone[0];
            String timezoneId = timezone[1];

            TimeZone tz = TimeZone.getTimeZone(timezoneId);
            int offsetMillis = tz.getRawOffset();
            int offsetMinutes = offsetMillis / (1000 * 60);

            // Tạo tên hiển thị cho timezone
            // Ví dụ: "Hà Nội (GMT+7:00)"
            String displayName = String.format(Locale.getDefault(),
                    "%s (GMT%+d:%02d)",
                    timezoneId,
                    offsetMinutes / 60,
                    Math.abs(offsetMinutes % 60)
            );
            // Tạo đối tượng TimezoneInfo và thêm vào danh sách
            timezones.add(new TimezoneInfo(cityName, timezoneId, displayName, offsetMinutes));
        }

        // Sắp xếp theo tên thành phố
        Collections.sort(timezones, (t1, t2) -> t1.getCityName().compareToIgnoreCase(t2.getCityName()));

        return timezones;
    }

    // Các phương thức dưới đây sẽ trả về thời gian hiện tại, khoảng thời gian
    // và khoảng cách thời gian giữa múi giờ hiện tại và múi giờ được chỉ
    // định bởi timezoneId. Nếu có lỗi xảy ra, chúng sẽ trả về giá trị
    // mặc định là "00:00" hoặc "0 giờ".
    public static String getCurrentTimeInTimezone(String timezoneId) {
        try {
            TimeZone timezone = TimeZone.getTimeZone(timezoneId);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            sdf.setTimeZone(timezone);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "00:00";
        }
    }

    // Trả về khoảng thời gian hiện tại trong múi giờ được chỉ định (AM/PM)
    public static String getCurrentPeriodInTimezone(String timezoneId) {
        try {
            TimeZone timezone = TimeZone.getTimeZone(timezoneId);
            SimpleDateFormat sdf = new SimpleDateFormat("a", Locale.getDefault());
            sdf.setTimeZone(timezone);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "AM";
        }
    }

    // Trả về khoảng thời gian giữa múi giờ hiện tại và múi giờ được chỉ định
    public static String getTimeDifference(String timezoneId) {
        try {
            TimeZone localTimezone = TimeZone.getDefault();
            TimeZone targetTimezone = TimeZone.getTimeZone(timezoneId);

            long currentTime = System.currentTimeMillis();
            int localOffset = localTimezone.getOffset(currentTime);
            int targetOffset = targetTimezone.getOffset(currentTime);

            int diffMinutes = (targetOffset - localOffset) / (1000 * 60);
            int diffHours = diffMinutes / 60;
            int remainingMinutes = Math.abs(diffMinutes % 60);

            if (diffHours == 0 && remainingMinutes == 0) {
                return "Cùng giờ";
            }

            String sign = diffHours >= 0 ? "+" : "";
            if (remainingMinutes == 0) {
                return String.format(Locale.getDefault(), "%s%d giờ", sign, diffHours);
            } else {
                return String.format(Locale.getDefault(), "%s%d:%02d giờ", sign, diffHours, remainingMinutes);
            }
        } catch (Exception e) {
            return "0 giờ";
        }
    }
}

