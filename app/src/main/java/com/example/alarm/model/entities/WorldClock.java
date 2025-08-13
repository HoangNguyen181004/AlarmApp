package com.example.alarm.model.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "world_clocks")
public class WorldClock {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String cityName;
    private String timeZoneId;
    private int orderIndex;

    public WorldClock() {}

    public WorldClock(String cityName, String timeZoneId, int orderIndex) {
        this.cityName = cityName;
        this.timeZoneId = timeZoneId;
        this.orderIndex = orderIndex;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}

