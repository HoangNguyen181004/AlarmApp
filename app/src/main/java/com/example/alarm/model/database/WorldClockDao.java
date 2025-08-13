package com.example.alarm.model.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.alarm.model.entities.WorldClock;

import java.util.List;

@Dao
public interface WorldClockDao {

    @Query("SELECT * FROM world_clocks ORDER BY orderIndex ASC")
    LiveData<List<WorldClock>> getAllWorldClocks();

    @Insert
    long insertWorldClock(WorldClock worldClock);

    @Update
    void updateWorldClock(WorldClock worldClock);

    @Delete
    void deleteWorldClock(WorldClock worldClock);

    @Query("DELETE FROM world_clocks WHERE id = :id")
    void deleteWorldClockById(int id);

    @Query("SELECT COUNT(*) FROM world_clocks")
    int getWorldClockCount();

    @Query("SELECT MAX(orderIndex) FROM world_clocks")
    int getMaxOrderIndex();
}

