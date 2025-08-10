package com.example.alarm.model.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.alarm.model.entities.Alarm;

import java.util.List;
@Dao
public interface AlarmDao {
    @Insert
    void insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("SELECT * FROM alarm ORDER BY hour, minute")
    LiveData<List<Alarm>> getAllAlarms();

    @Query("SELECT * FROM alarm WHERE hour = :hour AND minute = :minute")
    List<Alarm> getAlarmsByTime(int hour, int minute);
    @Query("SELECT * FROM alarm WHERE id = :id")
    Alarm getAlarmById(int id);
}
