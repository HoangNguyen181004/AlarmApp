package com.example.alarm.model.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "alarm")
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int hour;
    public int minute;
    public String label;
    public List<Boolean> repeatDays;
    public String ringtoneUri;
    public boolean enabled;

    public Alarm(int id, int hour, int minute, String label, List<Boolean> repeatDays, String ringtoneUri, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.repeatDays = repeatDays;
        this.ringtoneUri = ringtoneUri;
        this.enabled = enabled;
    }
}
