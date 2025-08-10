package com.example.alarm.model.repository;

import androidx.lifecycle.LiveData;

import com.example.alarm.model.database.AlarmDao;
import com.example.alarm.model.entities.Alarm;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmRepository {
    private AlarmDao alarmDao;
    private LiveData<List<Alarm>> allAlarms;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AlarmRepository(AlarmDao alarmDao) {
        this.alarmDao = alarmDao;
        allAlarms = alarmDao.getAllAlarms();
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void insert(Alarm alarm) {
        executorService.execute(() -> alarmDao.insert(alarm));
    }

    public void update(Alarm alarm) {
        executorService.execute(() -> alarmDao.update(alarm));
    }

    public void delete(Alarm alarm) {
        executorService.execute(() -> alarmDao.delete(alarm));
    }

    public List<Alarm> getAlarmsByTime(int hour, int minute) {
        return alarmDao.getAlarmsByTime(hour, minute);
    }
}
