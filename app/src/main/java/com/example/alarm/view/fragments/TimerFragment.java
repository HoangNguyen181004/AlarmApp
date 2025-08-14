package com.example.alarm.view.fragments;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarm.R;
import com.example.alarm.services.TimerService;
import com.example.alarm.utils.TimeUtils;
import com.example.alarm.viewmodel.TimerViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class TimerFragment extends Fragment {

    private static final String TAG = "TimerFragment";
    private TimerViewModel viewModel;
    private NumberPicker npHours, npMinutes, npSeconds;
    private TextView tvTimerTime, tvRemainingLabel;
    private ProgressBar circularProgress;
    private ExtendedFloatingActionButton btnStartPause, btnReset;
    private View cardTimePicker, cardPresets, countdownContainer;
    private Ringtone ringtone;
    private Handler ringtoneHandler;

    // BroadcastReceiver để nhận thông báo từ TimerService
    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.TIMER_TICK_ACTION.equals(intent.getAction())) {
                long remainingMillis = intent.getLongExtra(TimerService.EXTRA_REMAINING_MILLIS, 0);
                viewModel.updateRemainingMillis(remainingMillis);
                Log.d(TAG, "Received tick broadcast: " + remainingMillis + " ms remaining");
            } else if (TimerService.TIMER_FINISHED_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "Received finished broadcast");
                viewModel.onTimerFinished();
                showTimerFinishedDialog();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        // Thiết lập các view từ layout
        npHours = view.findViewById(R.id.np_hours);
        npMinutes = view.findViewById(R.id.np_minutes);
        npSeconds = view.findViewById(R.id.np_seconds);
        tvTimerTime = view.findViewById(R.id.tv_timer_time);
        tvRemainingLabel = view.findViewById(R.id.tv_remaining_label);
        circularProgress = view.findViewById(R.id.circular_progress);
        btnStartPause = view.findViewById(R.id.btn_start_pause);
        btnReset = view.findViewById(R.id.btn_reset);
        cardTimePicker = view.findViewById(R.id.card_time_picker);
        cardPresets = view.findViewById(R.id.card_presets);
        countdownContainer = view.findViewById(R.id.countdown_container);

        // Number pickers
        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        // Thiết lập các chip chọn sẵn thời gian đếm ngược
        setupPresetChips(view);

        // Ringtone
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getContext(), ringtoneUri);
        ringtoneHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Ringtone initialized with URI: " + ringtoneUri);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        viewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);

        viewModel.getRemainingMillis().observe(getViewLifecycleOwner(), this::updateTimeDisplay);
        viewModel.getIsRunning().observe(getViewLifecycleOwner(), this::updateButtonStates);
        viewModel.getTotalMillis().observe(getViewLifecycleOwner(), total -> {
            if (total != null && total > 0) {
                updateUIForCountdown();
                Log.d(TAG, "UI updated for countdown with total: " + total + " ms");
            } else {
                updateUIForSetup();
                Log.d(TAG, "UI updated for setup");
            }
        });
        viewModel.getIsFinished().observe(getViewLifecycleOwner(), isFinished -> {
            if (isFinished) {
                playRingtone();
                Log.d(TAG, "Timer finished, playing ringtone");
            }
        });

        // Nút bấm
        btnStartPause.setOnClickListener(v -> {
            if (viewModel.getIsRunning().getValue() != null && viewModel.getIsRunning().getValue()) {
                // Tạm dừng
                viewModel.pause();
                stopTimerService();
                Log.d(TAG, "Timer paused");
            } else if (viewModel.getTotalMillis().getValue() != null && viewModel.getTotalMillis().getValue() > 0) {
                // Tiếp tục
                Long remaining = viewModel.getRemainingMillis().getValue();
                if (remaining != null && remaining > 0) {
                    viewModel.resume();
                    startTimerService(remaining);
                    Log.d(TAG, "Timer resumed with " + remaining + " ms");
                }
            } else {
                // Bắt đầu mới
                long millis = getSelectedMillis();
                if (millis > 0) {
                    viewModel.start(millis);
                    startTimerService(millis);
                    Log.d(TAG, "Timer started with " + millis + " ms");
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            viewModel.reset();
            stopTimerService();
            stopRingtone();
            Log.d(TAG, "Timer reset");
        });

        // Cập nhật giao diện ban đầu
        updateButtonStates(viewModel.getIsRunning().getValue());
        updateTimeDisplay(viewModel.getRemainingMillis().getValue());
        if (viewModel.getTotalMillis().getValue() != null && viewModel.getTotalMillis().getValue() > 0) {
            updateUIForCountdown();
        } else {
            updateUIForSetup();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Đăng ký receiver để nhận thông báo từ service
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.TIMER_TICK_ACTION);
        filter.addAction(TimerService.TIMER_FINISHED_ACTION);
        requireContext().registerReceiver(
                timerReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
        );
        Log.d(TAG, "BroadcastReceiver registered");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Hủy đăng ký receiver
        try {
            requireContext().unregisterReceiver(timerReceiver);
            Log.d(TAG, "BroadcastReceiver unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }

    // Bắt đầu timer service
    private void startTimerService(long delayMillis) {
        Intent serviceIntent = new Intent(getContext(), TimerService.class);
        serviceIntent.setAction(TimerService.ACTION_START_TIMER);
        serviceIntent.putExtra(TimerService.EXTRA_DELAY_MILLIS, delayMillis);
        requireContext().startForegroundService(serviceIntent);
        Log.d(TAG, "Started TimerService with " + delayMillis + " ms");
    }

    // Dừng timer service
    private void stopTimerService() {
        Intent serviceIntent = new Intent(getContext(), TimerService.class);
        requireContext().stopService(serviceIntent);
        Log.d(TAG, "Stopped TimerService");
    }

    // Thiết lập các chip chọn sẵn thời gian
    private void setupPresetChips(View view) {
        Chip chip1min = view.findViewById(R.id.chip_1min);
        Chip chip3min = view.findViewById(R.id.chip_3min);
        Chip chip5min = view.findViewById(R.id.chip_5min);
        Chip chip10min = view.findViewById(R.id.chip_10min);
        Chip chip15min = view.findViewById(R.id.chip_15min);
        Chip chip20min = view.findViewById(R.id.chip_20min);

        chip1min.setOnClickListener(v -> setPickerValues(0, 1, 0));
        chip3min.setOnClickListener(v -> setPickerValues(0, 3, 0));
        chip5min.setOnClickListener(v -> setPickerValues(0, 5, 0));
        chip10min.setOnClickListener(v -> setPickerValues(0, 10, 0));
        chip15min.setOnClickListener(v -> setPickerValues(0, 15, 0));
        chip20min.setOnClickListener(v -> setPickerValues(0, 20, 0));
        Log.d(TAG, "Preset chips initialized");
    }

    // Thiết lập giá trị cho các picker
    private void setPickerValues(int hours, int minutes, int seconds) {
        npHours.setValue(hours);
        npMinutes.setValue(minutes);
        npSeconds.setValue(seconds);
        Log.d(TAG, "Picker values set to " + hours + "h " + minutes + "m " + seconds + "s");
    }

    // Lấy thời gian đã chọn từ các picker (chuyển đổi sang ms)
    private long getSelectedMillis() {
        int hours = npHours.getValue();
        int minutes = npMinutes.getValue();
        int seconds = npSeconds.getValue();
        long millis = (hours * 3600L + minutes * 60L + seconds) * 1000L;
        Log.d(TAG, "Selected time: " + millis + " ms");
        return millis;
    }

    private void updateTimeDisplay(Long millis) {
        if (millis == null || millis <= 0) {
            tvTimerTime.setText("00:00");
            circularProgress.setProgress(100);
            Log.d(TAG, "Time display updated to 00:00");
            return;
        }
        String formatted = TimeUtils.formatMillisToMinutesSeconds(millis);
        tvTimerTime.setText(formatted);

        // Cập nhật progress bar
        long total = viewModel.getTotalMillis().getValue() != null ? viewModel.getTotalMillis().getValue() : 1L;
        int progress = (int) (((double) millis / total) * 100);
        circularProgress.setProgress(100 - progress);
        Log.d(TAG, "Time display updated to " + formatted + ", progress: " + progress);
    }

    private void updateButtonStates(Boolean isRunning) {
        if (isRunning == null) isRunning = false;
        if (viewModel.getTotalMillis().getValue() != null && viewModel.getTotalMillis().getValue() > 0) {
            btnReset.setVisibility(View.VISIBLE);
            if (isRunning) {
                btnStartPause.setIconResource(R.drawable.ic_pause);
                btnStartPause.setContentDescription("Pause Timer");
                Log.d(TAG, "Button state: Pause");
            } else {
                btnStartPause.setIconResource(R.drawable.ic_start);
                btnStartPause.setContentDescription("Resume Timer");
                Log.d(TAG, "Button state: Resume");
            }
        } else {
            btnReset.setVisibility(View.GONE);
            btnStartPause.setIconResource(R.drawable.ic_start);
            btnStartPause.setContentDescription("Start Timer");
            Log.d(TAG, "Button state: Start");
        }
    }

    private void updateUIForCountdown() {
        cardTimePicker.setVisibility(View.GONE);
        cardPresets.setVisibility(View.GONE);
        countdownContainer.setVisibility(View.VISIBLE);
        tvRemainingLabel.setVisibility(View.VISIBLE);
        Log.d(TAG, "UI switched to countdown mode");
    }

    private void updateUIForSetup() {
        cardTimePicker.setVisibility(View.VISIBLE);
        cardPresets.setVisibility(View.VISIBLE);
        countdownContainer.setVisibility(View.GONE);
        tvRemainingLabel.setVisibility(View.GONE);
        setPickerValues(0, 0, 0);
        Log.d(TAG, "UI switched to setup mode");
    }

    private void playRingtone() {
        if (ringtone != null && !ringtone.isPlaying()) {
            ringtone.play();
            ringtoneHandler.postDelayed(() -> stopRingtone(), 3_000);
            Log.d(TAG, "Playing ringtone for 3 seconds");
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtoneHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Ringtone stopped");
    }

    private void showTimerFinishedDialog() {
        long totalMillis = viewModel.getTotalMillis().getValue() != null ? viewModel.getTotalMillis().getValue() : 0L;
        int hours = (int) (totalMillis / 3_600_000);
        int minutes = (int) ((totalMillis % 3_600_000) / 60_000);
        int seconds = (int) ((totalMillis % 60_000) / 1000);

        // Hiển thị dialog thông báo hẹn giờ đã kết thúc
        new AlertDialog.Builder(requireContext())
                .setTitle("Hẹn giờ đã kết thúc!")
                .setMessage("Bộ hẹn giờ đã hoàn thành!")
                .setPositiveButton("Đặt lại", (dialog, which) -> {
                    stopRingtone();
                    viewModel.reset();
                    setPickerValues(hours, minutes, seconds);
                    updateUIForSetup();
                    Log.d(TAG, "Timer reset from dialog");
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    stopRingtone();
                    dialog.dismiss();
                    Log.d(TAG, "Dialog dismissed");
                })
                .setCancelable(false)
                .show();
        Log.d(TAG, "Showing timer finished dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Chỉ dừng service nếu timer đã reset
        if (viewModel.getTotalMillis().getValue() == null || viewModel.getTotalMillis().getValue() == 0) {
            stopTimerService();
        }
        stopRingtone();
        Log.d(TAG, "onDestroyView called");
    }
}