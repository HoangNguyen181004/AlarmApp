package com.example.alarm.model.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
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
}
