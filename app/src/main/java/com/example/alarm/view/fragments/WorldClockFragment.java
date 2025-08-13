package com.example.alarm.view.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.alarm.R;
import com.example.alarm.model.entities.WorldClock;
import com.example.alarm.view.activities.TimezoneSelectorActivity;
import com.example.alarm.view.adapters.WorldClockAdapter;
import com.example.alarm.viewmodel.WorldClockViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WorldClockFragment extends Fragment implements WorldClockAdapter.OnWorldClockClickListener {
    private ActivityResultLauncher<Intent> timezoneLauncher;

    private WorldClockViewModel viewModel;
    private WorldClockAdapter adapter;
    private RecyclerView recyclerView;
    private FloatingActionButton btnAddTimezone;

    private Handler updateHandler;
    private Runnable updateRunnable;

    // Tạo một instance mới của WorldClockFragment
    // để sử dụng trong MainActivity hoặc khi cần khởi tạo fragment này
    public static WorldClockFragment newInstance() {
        return new WorldClockFragment();
    }


    // Tạo giao diện cho fragment (lấy layout từ XML)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_world_clock, container, false);
    }

    // Khi fragment đã được tạo xong, chúng ta sẽ khởi tạo các view và thiết lập các thành phần cần thiết
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        timezoneLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String cityName = data.getStringExtra("city_name");
                            String timezoneId = data.getStringExtra("timezone_id");

                            if (cityName != null && timezoneId != null) {
                                WorldClock newWorldClock = new WorldClock(cityName, timezoneId, 0);
                                viewModel.insertWorldClock(newWorldClock);
                            }
                        }
                    }
                }
        );
        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
        startTimeUpdater();
    }

    // Tìm kiếm các view trong layout và khởi tạo chúng
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_world_clocks);
        btnAddTimezone = view.findViewById(R.id.btn_add_timezone);
    }

    // Thiết lập RecyclerView với adapter và layout manager
    // Adapter sẽ quản lý danh sách các múi giờ thế giới
    // và hiển thị chúng trong RecyclerView
    // LayoutManager sẽ xác định cách hiển thị các mục trong RecyclerView
    // Ở đây chúng ta sử dụng LinearLayoutManager để hiển thị các mục theo
    // dạng danh sách dọc
    private void setupRecyclerView() {
        adapter = new WorldClockAdapter();
        adapter.setOnWorldClockClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // Thiết lập ViewModel để quản lý dữ liệu múi giờ thế giới
    // ViewModel sẽ lấy dữ liệu từ cơ sở dữ liệu và cung cấp cho adapter
    // Khi dữ liệu thay đổi, adapter sẽ tự động cập nhật danh sách hiển thị
    // Chúng ta sử dụng LiveData để quan sát sự thay đổi của dữ liệu
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(WorldClockViewModel.class);
        viewModel.getAllWorldClocks().observe(getViewLifecycleOwner(), worldClocks -> {
            adapter.submitList(worldClocks);
        });
    }

    // Thiết lập các sự kiện click cho nút thêm múi giờ (nút add ở góc dưới bên phải)
    // Khi người dùng click vào nút này, chúng ta sẽ mở Activity để chọn múi giờ
    // Người dùng sẽ chọn thành phố và múi giờ, sau đó Activity sẽ trả về
    // kết quả
    private void setupClickListeners() {
        btnAddTimezone.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TimezoneSelectorActivity.class);
            timezoneLauncher.launch(intent);
        });
    }

    // Bắt đầu một Handler để cập nhật thời gian mỗi giây
    // Handler sẽ chạy một Runnable để cập nhật adapter, từ đó làm mới giao diện
    // Chúng ta sẽ sử dụng Handler và Runnable để thực hiện việc này
    private void startTimeUpdater() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                updateHandler.postDelayed(this, 1000);
            }
        };
        updateHandler.post(updateRunnable);
    }

    // Cái này không cần thiết
    // cái này là click 1 lần
    @Override
    public void onWorldClockClick(WorldClock worldClock) {
        // Có thể thêm tính năng xem chi tiết timezone
    }


    // Long click để xóa múi giờ trong danh sách hiển thị
    @Override
    public void onWorldClockLongClick(WorldClock worldClock) {
        showDeleteConfirmationDialog(worldClock);
    }

    // Hiển thị cửa sổ xác nhận xóa múi giờ
    private void showDeleteConfirmationDialog(WorldClock worldClock) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa múi giờ")
                .setMessage("Bạn có muốn xóa " + worldClock.getCityName() + " không ?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteWorldClock(worldClock);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }
}

