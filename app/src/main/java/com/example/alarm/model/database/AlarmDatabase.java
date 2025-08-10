package com.example.alarm.model.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.example.alarm.model.entities.Alarm;

@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AlarmDatabase extends RoomDatabase {
    public abstract AlarmDao alarmDao();
    private static volatile AlarmDatabase INSTANCE;

    public static AlarmDatabase getDatabase(final Context context) {
        if(INSTANCE == null) {
            synchronized (AlarmDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AlarmDatabase.class, "alarm_database").build();
                }
            }
        }
        return INSTANCE;
    }
}
