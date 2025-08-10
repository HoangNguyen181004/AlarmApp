package com.example.alarm.view.activities;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.alarm.R;
import com.example.alarm.model.entities.Alarm;
import com.example.alarm.viewmodel.AlarmViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAlarmActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TextInputEditText labelEditText;
    private CheckBox[] dayCheckBoxes;
    private TextView ringtoneNameTextView;
    private Button selectRingtoneButton;
    private Button deleteButton, cancelButton, saveButton;
    private Uri ringtoneUri;
    private AlarmViewModel viewModel;
    private Alarm existingAlarm;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (ringtoneUri != null) {
                        ringtoneNameTextView.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
                    } else {
                        ringtoneNameTextView.setText(getString(R.string.default_ringtone));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        timePicker = findViewById(R.id.timePicker);
        labelEditText = findViewById(R.id.alarmLabelEditText);
        ringtoneNameTextView = findViewById(R.id.ringtoneNameTextView);
        selectRingtoneButton = findViewById(R.id.selectRingtoneButton);
        deleteButton = findViewById(R.id.buttonDeleteAlarm);
        cancelButton = findViewById(R.id.cancelButton);
        saveButton = findViewById(R.id.saveAlarmButton);

        dayCheckBoxes = new CheckBox[]{
                findViewById(R.id.checkSun), findViewById(R.id.checkMon),
                findViewById(R.id.checkTue), findViewById(R.id.checkWed),
                findViewById(R.id.checkThu), findViewById(R.id.checkFri),
                findViewById(R.id.checkSat)
        };

        // Kiểm tra edit hay add mới
        int alarmId = getIntent().getIntExtra("ALARM_ID", -1);
        if (alarmId != -1) {
            executor.execute(() -> {
                existingAlarm = viewModel.getAlarmById(alarmId); // Dòng 76: Chạy trên background
                runOnUiThread(() -> {
                    if (existingAlarm != null) {
                        setTitle(R.string.edit_alarm);
                        loadAlarmData(existingAlarm);
                        deleteButton.setEnabled(true);
                    } else {
                        setTitle(R.string.add_alarm);
                        deleteButton.setEnabled(false);
                    }
                });
            });
        } else {
            setTitle(R.string.add_alarm);
            deleteButton.setEnabled(false);
        }

        selectRingtoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_ringtone));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
            ringtonePickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> saveOrUpdateAlarm());
        cancelButton.setOnClickListener(v -> finish());
        deleteButton.setOnClickListener(v -> {
            if (existingAlarm != null) {
                viewModel.delete(existingAlarm);
            }
            finish();
        });
    }

    private void loadAlarmData(Alarm alarm) {
        timePicker.setHour(alarm.hour);
        timePicker.setMinute(alarm.minute);
        labelEditText.setText(alarm.label);
        for (int i = 0; i < dayCheckBoxes.length; i++) {
            dayCheckBoxes[i].setChecked(alarm.repeatDays.get(i));
        }
        ringtoneUri = Uri.parse(alarm.ringtoneUri);
        ringtoneNameTextView.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
    }

    private void saveOrUpdateAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String label = labelEditText.getText().toString();

        List<Boolean> repeatDays = new ArrayList<>();
        for (CheckBox checkBox : dayCheckBoxes) {
            repeatDays.add(checkBox.isChecked());
        }

        String ringtone = (ringtoneUri != null) ? ringtoneUri.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();

        if (existingAlarm != null) {
            existingAlarm.hour = hour;
            existingAlarm.minute = minute;
            existingAlarm.label = label;
            existingAlarm.repeatDays = repeatDays;
            existingAlarm.ringtoneUri = ringtone;
            viewModel.update(existingAlarm);
        } else {
            Alarm alarm = new Alarm(0, hour, minute, label, repeatDays, ringtone, true); // Không set ID, để Room tự generate
            viewModel.insert(alarm);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}