package com.example.alarm.model.repository;

import androidx.lifecycle.LiveData;
import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import com.example.alarm.model.database.AlarmDao;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.model.database.AlarmDatabase;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmRepository {
    private AlarmDao alarmDao;
    private LiveData<List<Alarm>> allAlarms;
    private ExecutorService executorService;

    public AlarmRepository(Application application) {
        AlarmDatabase database = AlarmDatabase.getDatabase(application);
        this.alarmDao = database.alarmDao();
        this.allAlarms = alarmDao.getAllAlarms();
        this.executorService = Executors.newFixedThreadPool(2); // Hoặc newSingleThreadExecutor()
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void insert(final Alarm alarm) {
        executorService.execute(() -> alarmDao.insert(alarm));
    }

    public void update(final Alarm alarm) {
        executorService.execute(() -> alarmDao.update(alarm));
    }

    public void delete(final Alarm alarm) {
        executorService.execute(() -> alarmDao.delete(alarm));
    }

    // Các hàm này giờ trực tiếp trả về LiveData từ DAO
    public List<Alarm> getAlarmsByTime(int hour, int minute) {
        return alarmDao.getAlarmsByTime(hour, minute);
    }

    public Alarm getAlarmById(int id) {
        return alarmDao.getAlarmById(id);
    }
}
