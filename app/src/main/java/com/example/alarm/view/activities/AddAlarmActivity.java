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

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class AddAlarmActivity extends AppCompatActivity {
    private TimePicker timePicker;
    private TextInputEditText labelEditText;
    private CheckBox[] dayCheckBoxes;
    private TextView ringtoneNameTextView;
    private Uri ringtoneUri;
    private AlarmViewModel viewModel;

    private final ActivityResultLauncher<Intent> ringtonePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (ringtoneUri != null) {
                        ringtoneNameTextView.setText(RingtoneManager.getRingtone(this, ringtoneUri).getTitle(this));
                    } else {
                        ringtoneNameTextView.setText("Nhạc mặc định");
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

        dayCheckBoxes = new CheckBox[]{
                findViewById(R.id.checkSun), findViewById(R.id.checkMon),
                findViewById(R.id.checkTue), findViewById(R.id.checkWed),
                findViewById(R.id.checkThu), findViewById(R.id.checkFri),
                findViewById(R.id.checkSat)
        };

        Button selectRingtoneButton = findViewById(R.id.selectRingtoneButton);
        selectRingtoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Chọn nhạc báo thức");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);
            ringtonePickerLauncher.launch(intent);
        });

        Button saveButton = findViewById(R.id.saveAlarmButton);
        saveButton.setOnClickListener(v -> saveAlarm());

        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String label = labelEditText.getText().toString();

        List<Boolean> repeatDays = new ArrayList<>();
        for (CheckBox checkBox : dayCheckBoxes) {
            repeatDays.add(checkBox.isChecked());
        }

        String ringtone = (ringtoneUri != null) ? ringtoneUri.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();

        Alarm alarm = new Alarm(0, hour, minute, label, repeatDays, ringtone, true);
        viewModel.insert(alarm); // Lưu và lên lịch
        finish();
    }
}
