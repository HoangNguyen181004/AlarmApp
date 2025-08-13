package com.example.alarm.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.view.adapters.LapAdapter;
import com.example.alarm.viewmodel.StopwatchViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class StopwatchFragment extends Fragment {

    private StopwatchViewModel viewModel;
    private TextView tvStopwatchTime;
    private ExtendedFloatingActionButton btnStartPause, btnResetLap;
    private RecyclerView rvLapTimes;
    private LapAdapter lapAdapter;
    private View progressCard;
    private TextView previousTimeText, fastestTimeText, slowestTimeText, averageTimeText;
    private android.widget.ProgressBar previousProgressBar, fastestProgressBar, slowestProgressBar, averageProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stopwatch, container, false);

        // phần này lấy các view từ layout
        // và ánh xạ chúng với các biến tương ứng
        tvStopwatchTime = view.findViewById(R.id.tv_stopwatch_time);
        btnStartPause = view.findViewById(R.id.btn_start_pause);
        btnResetLap = view.findViewById(R.id.btn_reset_lap);
        rvLapTimes = view.findViewById(R.id.rv_lap_times);
        progressCard = view.findViewById(R.id.progress_card);
        previousTimeText = view.findViewById(R.id.previous_time_text);
        fastestTimeText = view.findViewById(R.id.fastest_time_text);
        slowestTimeText = view.findViewById(R.id.slowest_time_text);
        averageTimeText = view.findViewById(R.id.average_time_text);
        previousProgressBar = view.findViewById(R.id.previous_progress_bar);
        fastestProgressBar = view.findViewById(R.id.fastest_progress_bar);
        slowestProgressBar = view.findViewById(R.id.slowest_progress_bar);
        averageProgressBar = view.findViewById(R.id.average_progress_bar);

        // Thiết lập RecyclerView cho danh sách thời gian vòng
        rvLapTimes.setLayoutManager(new LinearLayoutManager(getContext()));
        lapAdapter = new LapAdapter();
        rvLapTimes.setAdapter(lapAdapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // cái này khởi tạo ViewModel
        // để quản lý dữ liệu và logic của stopwatch
        viewModel = new ViewModelProvider(requireActivity()).get(StopwatchViewModel.class);

        // Thiết lập các quan sát (observers) để cập nhật giao diện khi dữ liệu thay đổi
        viewModel.getElapsedTime().observe(getViewLifecycleOwner(), this::updateTimeDisplay);
        viewModel.getIsRunning().observe(getViewLifecycleOwner(), isRunning -> {
            updateButtonStates(isRunning);
            btnResetLap.setEnabled(viewModel.getHasStarted().getValue() != null && viewModel.getHasStarted().getValue());
        });
        viewModel.getLaps().observe(getViewLifecycleOwner(), laps -> {
            lapAdapter.submitList(laps);
            // Cập nhật RecyclerView với danh sách thời gian vòng
            if (!laps.isEmpty()) { // nếu có thời gian vòng thì hiển thị progressCard
                progressCard.setVisibility(View.VISIBLE);
                updateProgressBars();
                rvLapTimes.scrollToPosition(0); // cuộn lên đầu danh sách
            } else { // nếu không có thời gian vòng
                // ẩn progressCard
                progressCard.setVisibility(View.GONE);
            }
        });
        // Quan sát hasStarted để cập nhật trạng thái của nút reset/lap
        viewModel.getHasStarted().observe(getViewLifecycleOwner(), hasStarted -> {
            btnResetLap.setEnabled(hasStarted);
        });

        // Thiết lập các sự kiện click cho nút Start/Pause và Reset/Lap
        btnStartPause.setOnClickListener(v -> {
            if (viewModel.getIsRunning().getValue() != null && viewModel.getIsRunning().getValue()) {
                viewModel.pause();
            } else {
                viewModel.start();
            }
        });

        // Nút Reset/Lap sẽ reset nếu không chạy, hoặc lap nếu đang chạy
        btnResetLap.setOnClickListener(v -> {
            if (viewModel.getIsRunning().getValue() != null && viewModel.getIsRunning().getValue()) {
                viewModel.lap();
            } else {
                viewModel.reset();
            }
        });

        // cập nhật giao diện ban đầu
        updateButtonStates(viewModel.getIsRunning().getValue());
        updateTimeDisplay(viewModel.getElapsedTime().getValue());
        updateProgressBars();
    }

    // Phương thức này cập nhật hiển thị thời gian trên TextView
    private void updateTimeDisplay(Long millis) {
        if (millis == null) millis = 0L;
        int minutes = (int) (millis / 60000);
        int seconds = (int) ((millis / 1000) % 60);
        int centis = (int) ((millis / 10) % 100);
        tvStopwatchTime.setText(String.format("%02d:%02d.%02d", minutes, seconds, centis));
    }

    // cập nhật trạng thái của các nút Start/Pause và Reset/Lap
    private void updateButtonStates(Boolean isRunning) {
        if (isRunning == null) isRunning = false;
        if (isRunning) {
            btnStartPause.setIconResource(R.drawable.ic_pause);
            btnStartPause.setContentDescription("Pause Stopwatch");
            btnResetLap.setIconResource(R.drawable.ic_lap);
            btnResetLap.setContentDescription("Lap");
        } else {
            btnStartPause.setIconResource(R.drawable.ic_start);
            btnStartPause.setContentDescription("Start or Resume Stopwatch");
            btnResetLap.setIconResource(R.drawable.ic_stop);
            btnResetLap.setContentDescription("Reset");
        }
    }

    // Phương thức này cập nhật các thanh tiến trình dựa trên thời gian vòng
    private void updateProgressBars() {
        // Lấy các thời gian vòng từ ViewModel
        double previous = viewModel.getPreviousLapTime();
        double fastest = viewModel.getFastestLapTime();
        double slowest = viewModel.getSlowestLapTime();
        double average = viewModel.getAverageLapTime();

        // logic để xử lý
        double maxLap = Math.max(slowest, 1);
        int prevProgress = (int) (previous / maxLap * 100);
        int fastProgress = (int) (fastest / maxLap * 100);
        int slowProgress = (int) (slowest / maxLap * 100);
        int avgProgress = (int) (average / maxLap * 100);

        previousProgressBar.setProgress(prevProgress);
        fastestProgressBar.setProgress(fastProgress);
        slowestProgressBar.setProgress(slowProgress);
        averageProgressBar.setProgress(avgProgress);

        previousTimeText.setText(String.format("%.2f", previous / 1000.0));
        fastestTimeText.setText(String.format("%.2f", fastest / 1000.0));
        slowestTimeText.setText(String.format("%.2f", slowest / 1000.0));
        averageTimeText.setText(String.format("%.2f", average / 1000.0));
    }
}
