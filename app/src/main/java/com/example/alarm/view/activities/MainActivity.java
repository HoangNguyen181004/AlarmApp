package com.example.alarm.view.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        setupBottomNavigation();

        // Yêu cầu quyền thông báo trước khi khởi tạo fragment
        requestNotificationPermission();

        if (savedInstanceState == null) {
            loadFragment(new AlarmFragment());
        }

        // Tạo notification channel nếu có NotificationUtils
        // NotificationUtils.createNotificationChannel(this);
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

    /**
     * Yêu cầu quyền thông báo trên Android 13+ (API level 33+)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                Log.d(TAG, "Requesting notification permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Notification permission already granted");
            }
        } else {
            Log.d(TAG, "Android version < 13, no need to request notification permission");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp
                Log.d(TAG, "Notification permission granted by user");
            } else {
                // Quyền bị từ chối - hiển thị dialog giải thích
                Log.w(TAG, "Notification permission denied by user");
                showNotificationPermissionDialog();
            }
        }
    }

    /**
     * Hiển thị dialog giải thích tại sao cần quyền thông báo
     */
    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cần quyền thông báo")
                .setMessage("Ứng dụng cần quyền thông báo để có thể:\n\n" +
                        "• Báo khi hẹn giờ kết thúc\n" +
                        "• Hiển thị báo thức\n" +
                        "• Thông báo stopwatch đang chạy\n\n" +
                        "Vui lòng cấp quyền trong Cài đặt.")
                .setPositiveButton("Mở Cài đặt", (dialog, which) -> {
                    // Mở settings để user cấp quyền thủ công
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Log.d(TAG, "Opening app settings for manual permission grant");
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening app settings", e);
                    }
                })
                .setNegativeButton("Bỏ qua", (dialog, which) -> {
                    dialog.dismiss();
                    Log.d(TAG, "User chose to skip notification permission");
                })
                .setCancelable(true)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại quyền khi user quay lại từ Settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission is now granted");
            }
        }
    }
}