package com.example.alarm.view.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.alarm.R;
import com.example.alarm.utils.NotificationUtils;
import com.example.alarm.view.fragments.AlarmFragment;
import com.example.alarm.view.fragments.StopwatchFragment;
import com.example.alarm.view.fragments.TimerFragment;
import com.example.alarm.view.fragments.WorldClockFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(new AlarmFragment());
        }

//        NotificationUtils.createNotificationChannel(this);
//
//        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
//        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
//            if (item.getItemId() == R.id.nav_alarm) {
//                loadFragment(new AlarmFragment());
//                return true;
//            }
//            return false;
//        });
//        bottomNavigation.setSelectedItemId(R.id.nav_alarm);
    }

    private void setupViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_alarm) {
                selectedFragment = new AlarmFragment();
            } else if (itemId == R.id.nav_world_clock) {
                selectedFragment = new WorldClockFragment();
            } else if (itemId == R.id.nav_timer) {
                selectedFragment = new TimerFragment();
            } else if (itemId == R.id.nav_stopwatch) {
                selectedFragment = new StopwatchFragment();
            }

            return loadFragment(selectedFragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            return true;
        }
        return false;
    }
}