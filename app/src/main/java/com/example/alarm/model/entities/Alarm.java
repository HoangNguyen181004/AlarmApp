package com.example.alarm.model.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Kiểm tra xem có phải cùng một đối tượng không
        if (o == null || getClass() != o.getClass()) return false; // Kiểm tra null và kiểu lớp
        Alarm alarm = (Alarm) o; // Ép kiểu đối tượng
        // So sánh tất cả các trường quan trọng để xác định sự bằng nhau về nội dung
        return id == alarm.id &&
                hour == alarm.hour &&
                minute == alarm.minute &&
                enabled == alarm.enabled &&
                Objects.equals(label, alarm.label) && // Dùng Objects.equals cho các đối tượng có thể null
                Objects.equals(repeatDays, alarm.repeatDays);
    }

    @Override
    public int hashCode() {
        // Tạo mã băm dựa trên các trường đã sử dụng trong phương thức equals()
        return Objects.hash(id, hour, minute, label, repeatDays, enabled);
    }
}
