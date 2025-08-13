package com.example.alarm.model.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class Converters {
    @TypeConverter
    public static String fromBooleanList(List<Boolean> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static List<Boolean> toBooleanList(String json) {
        Type listType = new TypeToken<List<Boolean>>() {}.getType();
        return new Gson().fromJson(json, listType);
    }

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static String fromIntegerList(List<Integer> value) {
        if (value == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(value);
    }

    @TypeConverter
    public static List<Integer> toIntegerList(String value) {
        if (value == null) {
            return null;
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromStringList(List<String> value) {
        if (value == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(value);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null) {
            return null;
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }
}
