package com.example.alarm.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;  // Thêm import này nếu chưa có
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.model.entities.TimezoneInfo;
import com.example.alarm.utils.TimezoneUtils;
import com.example.alarm.view.adapters.TimezoneSelectorAdapter;

import java.util.List;

public class TimezoneSelectorActivity extends AppCompatActivity implements TimezoneSelectorAdapter.OnTimezoneSelectedListener {

    private TimezoneSelectorAdapter adapter;
    private SearchView searchView;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timezone_selector);

        setupViews();
        loadTimezones();
    }

    private void setupViews() {
        // Tìm và set listener cho nút back
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.rv_timezones);

        // Setup RecyclerView
        adapter = new TimezoneSelectorAdapter();
        adapter.setOnTimezoneSelectedListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void loadTimezones() {
        List<TimezoneInfo> timezones = TimezoneUtils.getAllTimezones();
        adapter.setTimezones(timezones);
    }

    @Override
    public void onTimezoneSelected(TimezoneInfo timezone) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("city_name", timezone.getCityName());
        resultIntent.putExtra("timezone_id", timezone.getTimeZoneId());
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
