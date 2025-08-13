package com.example.alarm.model.entities;

public class TimezoneInfo {
    private String cityName;
    private String timeZoneId;
    private String displayName;
    private int offsetMinutes;

    public TimezoneInfo(String cityName, String timeZoneId, String displayName, int offsetMinutes) {
        this.cityName = cityName;
        this.timeZoneId = timeZoneId;
        this.displayName = displayName;
        this.offsetMinutes = offsetMinutes;
    }

    public String getCityName() {
        return cityName;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public String getDisplayName() {
        return displayName;
    }

//    public int getOffsetMinutes() {
//        return offsetMinutes;
//    }
//
//    public void setCityName(String cityName) {
//        this.cityName = cityName;
//    }
//
//    public void setTimeZoneId(String timeZoneId) {
//        this.timeZoneId = timeZoneId;
//    }
//
//    public void setDisplayName(String displayName) {
//        this.displayName = displayName;
//    }
//
//    public void setOffsetMinutes(int offsetMinutes) {
//        this.offsetMinutes = offsetMinutes;
//    }
}

