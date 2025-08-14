package com.example.alarm.viewmodel;

import android.os.CountDownTimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TimerViewModel extends ViewModel {

    private MutableLiveData<Long> remainingMillis = new MutableLiveData<>(0L);
    private MutableLiveData<Long> totalMillis = new MutableLiveData<>(0L);
    private MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> isFinished = new MutableLiveData<>(false);

    private CountDownTimer countDownTimer;
    private long pausedMillis = 0L;

    public LiveData<Long> getRemainingMillis() {
        return remainingMillis;
    }

    public LiveData<Long> getTotalMillis() {
        return totalMillis;
    }

    public LiveData<Boolean> getIsRunning() {
        return isRunning;
    }

    public LiveData<Boolean> getIsFinished() {
        return isFinished;
    }

    public void start(long millis) {
        totalMillis.setValue(millis);
        remainingMillis.setValue(millis);
        isFinished.setValue(false);
        createTimer(millis);
        countDownTimer.start();
        isRunning.setValue(true);
    }

    public void resume() {
        if (pausedMillis > 0) {
            createTimer(pausedMillis);
            countDownTimer.start();
            isRunning.setValue(true);
        }
    }

    public void pause() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isRunning.setValue(false);
            pausedMillis = remainingMillis.getValue() != null ? remainingMillis.getValue() : 0L;
        }
    }

    public void reset() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning.setValue(false);
        isFinished.setValue(false);
        remainingMillis.setValue(0L);
        totalMillis.setValue(0L);
        pausedMillis = 0L;
    }

    public void onTimerFinished() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning.setValue(false);
        isFinished.setValue(true);
        remainingMillis.setValue(0L);
    }

    public void updateRemainingMillis(long millis) {
        remainingMillis.postValue(millis);
    }

    private void createTimer(long millis) {
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis.postValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                remainingMillis.postValue(0L);
                isRunning.postValue(false);
                isFinished.postValue(true);
            }
        };
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}