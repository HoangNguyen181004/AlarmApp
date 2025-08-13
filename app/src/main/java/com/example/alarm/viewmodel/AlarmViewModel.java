package com.example.alarm.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.alarm.model.entities.Alarm;
import com.example.alarm.model.repository.AlarmRepository;
import com.example.alarm.utils.AlarmUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmViewModel extends AndroidViewModel {
    private AlarmRepository repository;
    private LiveData<List<Alarm>> allAlarms;

    public AlarmViewModel(Application application) {
        super(application);
        repository = new AlarmRepository(application);
        allAlarms = repository.getAllAlarms();
    }

    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void insert(Alarm alarm) {
        repository.insert(alarm);
        AlarmUtils.scheduleAlarm(getApplication(), alarm);
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
        if(alarm.enabled) {
            AlarmUtils.scheduleAlarm(getApplication(), alarm);
        } else {
            AlarmUtils.cancelAlarm(getApplication(), alarm);
        }
    }

    public void delete(Alarm alarm) {
        repository.delete(alarm);
        AlarmUtils.cancelAlarm(getApplication(), alarm);
    }
    public Alarm getAlarmById(int id) {
        return repository.getAlarmById(id);
    }
}
