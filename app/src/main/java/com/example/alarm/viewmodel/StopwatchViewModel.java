package com.example.alarm.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alarm.view.adapters.LapAdapter;

import java.util.ArrayList;
import java.util.List;

public class StopwatchViewModel extends ViewModel {

    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>(0L);
    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> hasStarted = new MutableLiveData<>(false);
    private MutableLiveData<List<LapAdapter.Lap>> laps = new MutableLiveData<>(new ArrayList<>());

    private long startTime = 0L;
    private long pauseTime = 0L;
    private long lastLapTime = 0L;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = SystemClock.elapsedRealtime() - startTime + pauseTime;
            elapsedTime.postValue(currentTime);
            handler.postDelayed(this, 5); // update mỗi 10ms
        }
    };

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public LiveData<Boolean> getIsRunning() {
        return isRunning;
    }

    public LiveData<Boolean> getHasStarted() {
        return hasStarted;
    }

    public LiveData<List<LapAdapter.Lap>> getLaps() {
        return laps;
    }

    // Phương thức để bắt đầu đồng hồ bấm giờ
    public void start() {
        if (!Boolean.TRUE.equals(isRunning.getValue())) {
            if (!Boolean.TRUE.equals(hasStarted.getValue())) { // Nếu chưa bắt đầu, reset thời gian
                startTime = SystemClock.elapsedRealtime();
                pauseTime = 0L;
                lastLapTime = 0L;
                hasStarted.setValue(true);
            } else { // Nếu đã bắt đầu, chỉ cần tiếp tục từ thời gian đã dừng
                startTime = SystemClock.elapsedRealtime(); // Lấy thời gian hiện tại
                pauseTime = elapsedTime.getValue() != null ? elapsedTime.getValue() : 0L;
            }
            isRunning.setValue(true);
            handler.post(updateRunnable);
        }
    }

    // Phương thức để tạm dừng đồng hồ bấm giờ
    public void pause() {
        if (Boolean.TRUE.equals(isRunning.getValue())) { // Chỉ tạm dừng nếu đang chạy
            handler.removeCallbacks(updateRunnable);
            isRunning.setValue(false);
            pauseTime = elapsedTime.getValue() != null ? elapsedTime.getValue() : 0L;
        }
    }

    // Phương thức để reset đồng hồ bấm giờ
    public void reset() {
        handler.removeCallbacks(updateRunnable);
        isRunning.setValue(false);
        hasStarted.setValue(false);
        elapsedTime.setValue(0L);
        pauseTime = 0L;
        lastLapTime = 0L;
        laps.setValue(new ArrayList<>());
    }

    // Phương thức để ghi lại một vòng (lap)
    public void lap() {
        if (Boolean.TRUE.equals(isRunning.getValue())) { // Chỉ ghi lại vòng nếu đang chạy
            long currentElapsed = elapsedTime.getValue() != null ? elapsedTime.getValue() : 0L;
            long diff = currentElapsed - lastLapTime;
            lastLapTime = currentElapsed;

            List<LapAdapter.Lap> currentLaps = laps.getValue() != null ? new ArrayList<>(laps.getValue()) : new ArrayList<>();
            int lapNumber = currentLaps.size() + 1;
            currentLaps.add(0, new LapAdapter.Lap(lapNumber, diff, currentElapsed)); // Thêm vòng mới vào đầu danh sách
            laps.setValue(currentLaps);
        }
    }

    // Phương thức để lấy thời gian của vòng trước đó
    public double getPreviousLapTime() {
        List<LapAdapter.Lap> currentLaps = laps.getValue();
        if (currentLaps != null && !currentLaps.isEmpty()) {
            return currentLaps.get(0).diffTime;
        }
        return 0;
    }

    // Phương thức để lấy thời gian của vòng nhanh nhất, chậm nhất và trung bình
    public double getFastestLapTime() {
        List<LapAdapter.Lap> currentLaps = laps.getValue();
        if (currentLaps != null && !currentLaps.isEmpty()) {
            return currentLaps.stream().mapToLong(lap -> lap.diffTime).min().orElse(0L);
        }
        return 0;
    }

    public double getSlowestLapTime() {
        List<LapAdapter.Lap> currentLaps = laps.getValue();
        if (currentLaps != null && !currentLaps.isEmpty()) {
            return currentLaps.stream().mapToLong(lap -> lap.diffTime).max().orElse(0L);
        }
        return 0;
    }

    public double getAverageLapTime() {
        List<LapAdapter.Lap> currentLaps = laps.getValue();
        if (currentLaps != null && !currentLaps.isEmpty()) {
            return currentLaps.stream().mapToLong(lap -> lap.diffTime).average().orElse(0.0);
        }
        return 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(updateRunnable);
    }
}
