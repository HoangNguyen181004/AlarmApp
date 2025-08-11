package com.example.alarm.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.view.activities.AddAlarmActivity;
import com.example.alarm.view.adapters.AlarmAdapter;
import com.example.alarm.viewmodel.AlarmViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmFragment extends Fragment {
    private AlarmViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView noAlarmText;
    private AlarmAdapter adapter;
    private CardView cardNextAlarm;
    private TextView nextAlarmTime, nextAlarmName, countdownText;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);

        recyclerView = view.findViewById(R.id.rv_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlarmAdapter(viewModel);
        recyclerView.setAdapter(adapter);

        noAlarmText = view.findViewById(R.id.noAlarmText);
        cardNextAlarm = view.findViewById(R.id.card_next_alarm);
        nextAlarmTime = view.findViewById(R.id.tv_next_alarm_time);
        nextAlarmName = view.findViewById(R.id.tv_next_alarm_name);
        countdownText = view.findViewById(R.id.tv_countdown);

        viewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            executor.execute(() -> {
                updateNextAlarm(alarms);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.submitList(alarms);
                        boolean hasEnabledAlarm = hasEnabledAlarm(alarms);
                        noAlarmText.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
                        cardNextAlarm.setVisibility(hasEnabledAlarm ? View.VISIBLE : View.GONE);
                    });
                }
            });
        });

        FloatingActionButton addFab = view.findViewById(R.id.btn_add_alarm);
        addFab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddAlarmActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private boolean hasEnabledAlarm(List<Alarm> alarms) {
        if (alarms == null) return false;
        for (Alarm alarm : alarms) {
            if (alarm.enabled) return true;
        }
        return false;
    }

    private void updateNextAlarm(List<Alarm> alarms) {
        if (alarms == null || alarms.isEmpty()) {
            return;
        }

        Calendar now = Calendar.getInstance();
        long currentTimeMillis = now.getTimeInMillis();
        Alarm nextAlarm = null;
        long minTimeToNext = Long.MAX_VALUE;

        for (Alarm alarm : alarms) {
            if (!alarm.enabled) continue; // Chỉ xem alarm enabled

            Calendar alarmTime = Calendar.getInstance();
            alarmTime.set(Calendar.HOUR_OF_DAY, alarm.hour);
            alarmTime.set(Calendar.MINUTE, alarm.minute);
            alarmTime.set(Calendar.SECOND, 0);
            alarmTime.set(Calendar.MILLISECOND, 0);

            boolean isRepeating = alarm.repeatDays.contains(true);
            if (isRepeating) {
                for (int i = 0; i < 7; i++) {
                    if (alarm.repeatDays.get(i)) {
                        Calendar temp = (Calendar) alarmTime.clone();
                        temp.set(Calendar.DAY_OF_WEEK, i + 1); // Sunday = 1, Monday = 2, ...
                        if (temp.getTimeInMillis() <= currentTimeMillis) {
                            temp.add(Calendar.WEEK_OF_YEAR, 1);
                        }
                        long timeToAlarm = temp.getTimeInMillis() - currentTimeMillis;
                        if (timeToAlarm < minTimeToNext) {
                            minTimeToNext = timeToAlarm;
                            nextAlarm = alarm;
                        }
                    }
                }
            } else {
                if (alarmTime.getTimeInMillis() <= currentTimeMillis) {
                    alarmTime.add(Calendar.DAY_OF_YEAR, 1);
                }
                long timeToAlarm = alarmTime.getTimeInMillis() - currentTimeMillis;
                if (timeToAlarm < minTimeToNext) {
                    minTimeToNext = timeToAlarm;
                    nextAlarm = alarm;
                }
            }
        }

        if (nextAlarm != null) {
            final Alarm finalNextAlarm = nextAlarm;
            final long finalMinTimeToNext = minTimeToNext;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    nextAlarmTime.setText(String.format("%02d:%02d %s",
                            finalNextAlarm.hour, finalNextAlarm.minute, finalNextAlarm.hour >= 12 ? "PM" : "AM"));
                    nextAlarmName.setText(finalNextAlarm.label.isEmpty() ? "Báo thức" : finalNextAlarm.label);
                    countdownText.setText(formatCountdown(finalMinTimeToNext));
                });
            }
        }
    }

    private String formatCountdown(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("Báo thức sau %d giờ %d phút", hours, minutes + 1);
    }

    @Override
    public void onDestroyView() {
        executor.shutdown();
        super.onDestroyView();
    }
}
