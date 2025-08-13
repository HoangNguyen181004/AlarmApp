package com.example.alarm.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.alarm.model.entities.WorldClock;
import com.example.alarm.model.repository.WorldClockRepository;

import java.util.List;

// Chức năng chính của cái này là cầu nối giữa UI (giao diện người dùng) và dữ liệu
// Nó sẽ lấy dữ liệu từ repository và cung cấp nó cho UI thông qua LiveData
// Khi dữ liệu thay đổi, UI sẽ tự động cập nhật mà không cần phải gọi lại
public class WorldClockViewModel extends AndroidViewModel {

    private WorldClockRepository repository;
    private LiveData<List<WorldClock>> allWorldClocks;


    public WorldClockViewModel(@NonNull Application application) {
        super(application);
        repository = new WorldClockRepository(application);
        allWorldClocks = repository.getAllWorldClocks();
    }

    public LiveData<List<WorldClock>> getAllWorldClocks() {
        return allWorldClocks;
    }


    public void insertWorldClock(WorldClock worldClock) {
        repository.insertWorldClock(worldClock);
    }

    public void deleteWorldClock(WorldClock worldClock) {
        repository.deleteWorldClock(worldClock);
    }

//    public void updateWorldClock(WorldClock worldClock) {
//        repository.updateWorldClock(worldClock);
//    }
}

