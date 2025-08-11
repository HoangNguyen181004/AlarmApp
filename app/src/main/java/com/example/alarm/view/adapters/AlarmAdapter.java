package com.example.alarm.view.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat; // Sửa: Dùng SwitchCompat
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.view.activities.AddAlarmActivity;
import com.example.alarm.viewmodel.AlarmViewModel;

import java.util.List;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {
    private AlarmViewModel viewModel;

    public AlarmAdapter(AlarmViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    private static final DiffUtil.ItemCallback<Alarm> DIFF_CALLBACK = new DiffUtil.ItemCallback<Alarm>() {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = getItem(position);
        holder.bind(alarm, viewModel);
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView, periodTextView, labelTextView, daysTextView;
        SwitchCompat toggleSwitch;
        private int currentAlarmId = -1; // Track current alarm ID

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.tv_alarm_time);
            periodTextView = itemView.findViewById(R.id.tv_alarm_period);
            labelTextView = itemView.findViewById(R.id.tv_alarm_label);
            daysTextView = itemView.findViewById(R.id.tv_alarm_days);
            toggleSwitch = itemView.findViewById(R.id.switch_alarm_enabled);
        }

        public void bind(Alarm alarm, AlarmViewModel viewModel) {
            // Clear previous listener to prevent stale references
            toggleSwitch.setOnCheckedChangeListener(null);

            // Update UI
            timeTextView.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
            periodTextView.setText(alarm.hour >= 12 ? "PM" : "AM");
            labelTextView.setText(alarm.label.isEmpty() ? "Báo thức" : alarm.label);
            daysTextView.setText(getDaysString(alarm.repeatDays));

            // Set switch state WITHOUT triggering listener
            toggleSwitch.setChecked(alarm.enabled);

            // Track current alarm ID
            currentAlarmId = alarm.id;

            // Set new listener that fetches fresh data
            toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Double-check we're still working with the same alarm
                if (currentAlarmId != alarm.id) {
                    return; // Ignore stale events
                }

                // Get fresh alarm data from database to avoid stale state
                new Thread(() -> {
                    Alarm freshAlarm = viewModel.getAlarmById(currentAlarmId);
                    if (freshAlarm != null) {
                        freshAlarm.enabled = isChecked;
                        viewModel.update(freshAlarm);
                    }
                }).start();
            });

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), AddAlarmActivity.class);
                intent.putExtra("ALARM_ID", alarm.id);
                itemView.getContext().startActivity(intent);
            });
        }

        private String getDaysString(List<Boolean> repeatDays) {
            String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 7; i++) {
                if (repeatDays.get(i)) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(days[i]);
                }
            }
            return sb.length() > 0 ? sb.toString() : "Chỉ một lần";
        }
    }
}