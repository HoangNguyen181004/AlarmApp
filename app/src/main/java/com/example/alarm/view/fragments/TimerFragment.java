package com.example.alarm.view.fragments;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarm.R;
import com.example.alarm.services.TimerService;
import com.example.alarm.utils.TimeUtils;
import com.example.alarm.viewmodel.TimerViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class TimerFragment extends Fragment {

    private TimerViewModel viewModel;
    private NumberPicker npHours, npMinutes, npSeconds;
    private TextView tvTimerTime, tvRemainingLabel;
    private ProgressBar circularProgress;
    private ExtendedFloatingActionButton btnStartPause, btnReset;
    private View cardTimePicker, cardPresets, countdownContainer;
    private Ringtone ringtone;
    private Handler ringtoneHandler;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "TimerChannel";
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // BroadcastReceiver để nhận thông báo từ TimerService
    private BroadcastReceiver timerFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.TIMER_FINISHED_ACTION.equals(intent.getAction())) {
                // Timer đã kết thúc từ service
                viewModel.onTimerFinished();
                showTimerFinishedDialog();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        // thiết lập các view từ layout
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

        // number pickers
        npHours.setMinValue(0);
        npHours.setMaxValue(23);
        npMinutes.setMinValue(0);
        npMinutes.setMaxValue(59);
        npSeconds.setMinValue(0);
        npSeconds.setMaxValue(59);

        // mấy cái nút chọn sẵn thời gian đếm ngược
        setupPresetChips(view);

        // ringtone - đổi thành notification sound
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getContext(), ringtoneUri);
        ringtoneHandler = new Handler(Looper.getMainLooper());

        // notification
        createNotificationChannel();

        // request quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted && Boolean.TRUE.equals(viewModel.getIsRunning().getValue())) {
                showNotification();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);

        viewModel.getRemainingMillis().observe(getViewLifecycleOwner(), this::updateTimeDisplay);
        viewModel.getIsRunning().observe(getViewLifecycleOwner(), this::updateButtonStates);
        viewModel.getTotalMillis().observe(getViewLifecycleOwner(), total -> {
            if (total > 0) {
                updateUIForCountdown();
                showNotification();
            } else {
                updateUIForSetup();
                cancelNotification();
            }
        });
        viewModel.getIsFinished().observe(getViewLifecycleOwner(), isFinished -> {
            if (isFinished) {
                playRingtone();
                // Không cần showTimerFinishedDialog() ở đây vì service sẽ gọi
            }
        });

        // nút bấm
        btnStartPause.setOnClickListener(v -> {
            if (viewModel.getIsRunning().getValue() != null && viewModel.getIsRunning().getValue()) {
                // Tạm dừng
                viewModel.pause();
                stopTimerService();
                cancelNotification();
            } else if (viewModel.getTotalMillis().getValue() != null && viewModel.getTotalMillis().getValue() > 0) {
                // Tiếp tục
                Long remaining = viewModel.getRemainingMillis().getValue();
                if (remaining != null && remaining > 0) {
                    viewModel.resume();
                    startTimerService(remaining);
                    showNotification();
                }
            } else {
                // Bắt đầu mới
                long millis = getSelectedMillis();
                if (millis > 0) {
                    viewModel.start(millis);
                    startTimerService(millis);
                    showNotification();
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            viewModel.reset();
            stopTimerService();
            cancelNotification();
            stopRingtone();
        });

        // cập nhật giao diện ban đầu
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
        IntentFilter filter = new IntentFilter(TimerService.TIMER_FINISHED_ACTION);
        requireContext().registerReceiver(
                timerFinishedReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
        );


    }

    @Override
    public void onPause() {
        super.onPause();
        // Hủy đăng ký receiver
        try {
            requireContext().unregisterReceiver(timerFinishedReceiver);
        } catch (Exception e) {
            // Ignore if not registered
        }
    }

    // Bắt đầu timer service
    private void startTimerService(long delayMillis) {
        Intent serviceIntent = new Intent(getContext(), TimerService.class);
        serviceIntent.setAction(TimerService.ACTION_START_TIMER);
        serviceIntent.putExtra(TimerService.EXTRA_DELAY_MILLIS, delayMillis);
        requireContext().startService(serviceIntent);
    }

    // Dừng timer service
    private void stopTimerService() {
        Intent serviceIntent = new Intent(getContext(), TimerService.class);
        requireContext().stopService(serviceIntent);
    }

    // cái này là để thiết lập các chip chọn sẵn thời gian
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
    }

    // cái này là để thiết lập giá trị cho các picker
    private void setPickerValues(int hours, int minutes, int seconds) {
        npHours.setValue(hours);
        npMinutes.setValue(minutes);
        npSeconds.setValue(seconds);
    }

    // cái này là để lấy thời gian đã chọn từ các picker (chuyển đổi sang ms)
    private long getSelectedMillis() {
        int hours = npHours.getValue();
        int minutes = npMinutes.getValue();
        int seconds = npSeconds.getValue();
        return (hours * 3600L + minutes * 60L + seconds) * 1000L;
    }

    private void updateTimeDisplay(Long millis) {
        if (millis == null || millis <= 0) {
            tvTimerTime.setText("00:00");
            circularProgress.setProgress(100);
            return;
        }
        String formatted = TimeUtils.formatMillisToMinutesSeconds(millis);
        tvTimerTime.setText(formatted);

        // Cập nhật progress bar
        long total = viewModel.getTotalMillis().getValue() != null ? viewModel.getTotalMillis().getValue() : 1L;
        int progress = (int) (((double) millis / total) * 100);
        circularProgress.setProgress(100 - progress);
    }

    private void updateButtonStates(Boolean isRunning) {
        if (isRunning == null) isRunning = false;
        if (viewModel.getTotalMillis().getValue() != null && viewModel.getTotalMillis().getValue() > 0) {
            btnReset.setVisibility(View.VISIBLE);
            if (isRunning) {
                btnStartPause.setIconResource(R.drawable.ic_pause);
                btnStartPause.setContentDescription("Pause Timer");
            } else {
                btnStartPause.setIconResource(R.drawable.ic_start);
                btnStartPause.setContentDescription("Resume Timer");
            }
        } else {
            btnReset.setVisibility(View.GONE);
            btnStartPause.setIconResource(R.drawable.ic_start);
            btnStartPause.setContentDescription("Start Timer");
        }
    }

    // cập nhật giao diện khi đang đếm ngược hoặc thiết lập
    private void updateUIForCountdown() {
        cardTimePicker.setVisibility(View.GONE);
        cardPresets.setVisibility(View.GONE);
        countdownContainer.setVisibility(View.VISIBLE);
        tvRemainingLabel.setVisibility(View.VISIBLE);
    }

    private void updateUIForSetup() {
        cardTimePicker.setVisibility(View.VISIBLE);
        cardPresets.setVisibility(View.VISIBLE);
        countdownContainer.setVisibility(View.GONE);
        tvRemainingLabel.setVisibility(View.GONE);
        setPickerValues(0, 0, 0);
    }

    private void playRingtone() {
        if (ringtone != null && !ringtone.isPlaying()) {
            ringtone.play();
            // Giảm thời gian từ 60s xuống 3s cho notification sound
            ringtoneHandler.postDelayed(() -> stopRingtone(), 3_000);
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtoneHandler.removeCallbacksAndMessages(null);
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
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    stopRingtone();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    // tạo notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Notification";
            String description = "Channel for timer notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(null, audioAttributes);
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Hiện notification khi timer đang chạy
    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Bộ đếm ngược")
                .setContentText("Bộ đếm ngược của bạn đang chạy")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setAutoCancel(false);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void cancelNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Chỉ dừng service nếu timer đã reset, không phải khi thoát app
        if (viewModel.getTotalMillis().getValue() == null || viewModel.getTotalMillis().getValue() == 0) {
            stopTimerService();
        }
        stopRingtone();
        cancelNotification();
    }
}