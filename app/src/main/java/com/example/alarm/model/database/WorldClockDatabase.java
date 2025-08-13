package com.example.alarm.model.database;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.alarm.model.entities.WorldClock;

@Database(
        entities = {
                WorldClock.class,
        },
        version = 6,
        exportSchema = false
)
public abstract class WorldClockDatabase extends RoomDatabase {

    private static volatile WorldClockDatabase INSTANCE;

    public abstract WorldClockDao worldClockDao();

    public static WorldClockDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WorldClockDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            WorldClockDatabase.class,
                            "world_clock_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}

