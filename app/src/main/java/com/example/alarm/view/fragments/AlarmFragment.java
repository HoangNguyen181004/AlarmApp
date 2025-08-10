package com.example.alarm.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.view.activities.AddAlarmActivity;
import com.example.alarm.view.adapters.AlarmAdapter;
import com.example.alarm.viewmodel.AlarmViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AlarmFragment extends Fragment {
    private AlarmViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView noAlarmText;
    private AlarmAdapter adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(AlarmViewModel.class);

        recyclerView = view.findViewById(R.id.rv_alarms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlarmAdapter(viewModel);
        recyclerView.setAdapter(adapter);

        noAlarmText = view.findViewById(R.id.noAlarmText);

        viewModel.getAllAlarms().observe(getViewLifecycleOwner(), alarms -> {
            adapter.submitList(alarms);
            noAlarmText.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        });

        FloatingActionButton addFab = view.findViewById(R.id.btn_add_alarm);
        addFab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddAlarmActivity.class);
            startActivity(intent);
        });

        return view;
    }
}
