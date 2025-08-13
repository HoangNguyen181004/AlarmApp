package com.example.alarm.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.utils.TimeUtils;

public class LapAdapter extends ListAdapter<LapAdapter.Lap, LapAdapter.LapViewHolder> {

    public static class Lap {
        public int number;
        public long diffTime;
        public long elapsedTime;

        public Lap(int number, long diffTime, long elapsedTime) {
            this.number = number;
            this.diffTime = diffTime;
            this.elapsedTime = elapsedTime;
        }
    }

    public LapAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Lap> DIFF_CALLBACK = new DiffUtil.ItemCallback<Lap>() {
        // cai này dùng để so sánh các đối tượng Lap trong danh sách
        // để xác định xem chúng có thay đổi hay không, từ đó cập nhật RecyclerView
        @Override
        public boolean areItemsTheSame(@NonNull Lap oldItem, @NonNull Lap newItem) {
            return oldItem.number == newItem.number;
        }

        // Phương thức này so sánh nội dung của hai đối tượng Lap
        // để xác định xem chúng có giống nhau hay không.
        // Nếu thời gian chênh lệch (diffTime) và thời gian đã trôi qua (elapsedTime)
        // của hai đối tượng Lap giống nhau, thì chúng được coi là giống nhau.
        @Override
        public boolean areContentsTheSame(@NonNull Lap oldItem, @NonNull Lap newItem) {
            return oldItem.diffTime == newItem.diffTime && oldItem.elapsedTime == newItem.elapsedTime;
        }
    };


    // Phương thức này được gọi khi RecyclerView cần tạo một ViewHolder mới
    @NonNull
    @Override
    public LapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lap_time, parent, false);
        return new LapViewHolder(view);
    }

    // Phương thức này được gọi khi RecyclerView cần cập nhật một ViewHolder
    // với dữ liệu mới. Nó sẽ lấy Lap hiện tại từ danh sách và
    // cập nhật các thành phần giao diện trong ViewHolder với dữ liệu đó.
    @Override
    public void onBindViewHolder(@NonNull LapViewHolder holder, int position) {
        Lap lap = getItem(position);
        holder.tvLapNumber.setText("Vòng lặp " + lap.number); // Hiển thị số vòng lặp
        holder.tvLapTimeDiff.setText(TimeUtils.formatMillisToTime(lap.diffTime)); // Hiển thị thời gian chênh lệch của vòng lặp
        holder.tvElapsedTime.setText("Thời gian đã trôi qua: " + TimeUtils.formatMillisToTime(lap.elapsedTime));
    }

    public static class LapViewHolder extends RecyclerView.ViewHolder {
        TextView tvLapNumber, tvLapTimeDiff, tvElapsedTime;

        public LapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLapNumber = itemView.findViewById(R.id.tv_lap_number);
            tvLapTimeDiff = itemView.findViewById(R.id.tv_lap_time_diff);
            tvElapsedTime = itemView.findViewById(R.id.tv_elapsed_time);
        }
    }
}
