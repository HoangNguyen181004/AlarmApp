package com.example.alarm.model.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.alarm.model.database.WorldClockDatabase;
import com.example.alarm.model.database.WorldClockDao;
import com.example.alarm.model.entities.WorldClock;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldClockRepository {

    private WorldClockDao worldClockDao;
    private LiveData<List<WorldClock>> allWorldClocks;
    private ExecutorService databaseExecutor;

    public WorldClockRepository(Application application) {
        WorldClockDatabase database = WorldClockDatabase.getDatabase(application);
        worldClockDao = database.worldClockDao();
        allWorldClocks = worldClockDao.getAllWorldClocks();
        databaseExecutor = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<WorldClock>> getAllWorldClocks() {
        return allWorldClocks;
    }

    public void insertWorldClock(WorldClock worldClock) {
        databaseExecutor.execute(() -> {
            int maxOrder = worldClockDao.getMaxOrderIndex();
            worldClock.setOrderIndex(maxOrder + 1);
            worldClockDao.insertWorldClock(worldClock);
        });
    }

    public void deleteWorldClock(WorldClock worldClock) {
        databaseExecutor.execute(() -> {
            worldClockDao.deleteWorldClock(worldClock);
        });
    }

    public void updateWorldClock(WorldClock worldClock) {
        databaseExecutor.execute(() -> {
            worldClockDao.updateWorldClock(worldClock);
        });
    }
}

