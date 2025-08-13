package com.example.alarm.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.model.entities.WorldClock;
import com.example.alarm.utils.TimezoneUtils;

public class WorldClockAdapter extends ListAdapter<WorldClock, WorldClockAdapter.WorldClockViewHolder> {

    private OnWorldClockClickListener listener;

    public WorldClockAdapter() {
        super(DIFF_CALLBACK);
    }

    // Cái này dùng để so sánh các đối tượng WorldClock trong danh sách
    // để xác định xem chúng có thay đổi hay không, từ đó cập nhật RecyclerView một cách hiệu quả
    private static final DiffUtil.ItemCallback<WorldClock> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WorldClock>() {
                @Override
                public boolean areItemsTheSame(@NonNull WorldClock oldItem, @NonNull WorldClock newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull WorldClock oldItem, @NonNull WorldClock newItem) {
                    return oldItem.getCityName().equals(newItem.getCityName()) &&
                            oldItem.getTimeZoneId().equals(newItem.getTimeZoneId());
                }
            };


    // Phương thức này được gọi khi RecyclerView cần tạo một ViewHolder mới
    // để hiển thị một mục trong danh sách. Nó sẽ tạo ra một ViewHolder
    // với layout item_world_clock và trả về nó.
    @NonNull
    @Override
    public WorldClockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_world_clock, parent, false);
        return new WorldClockViewHolder(itemView);
    }

    // Phương thức này được gọi khi RecyclerView cần cập nhật một ViewHolder
    // với dữ liệu mới. Nó sẽ lấy WorldClock hiện tại từ danh sách và
    // gọi phương thức bind của ViewHolder để cập nhật giao diện với dữ liệu đó.
    @Override
    public void onBindViewHolder(@NonNull WorldClockViewHolder holder, int position) {
        WorldClock currentClock = getItem(position);
        holder.bind(currentClock);
    }

    // ViewHolder này sẽ giữ các thành phần giao diện của mỗi mục trong danh sách
    // và sẽ được sử dụng để cập nhật giao diện với dữ liệu của WorldClock.
    public class WorldClockViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCityName;
        private TextView tvTimeDifference;
        private TextView tvWorldTime;
        private TextView tvWorldPeriod;

        // Constructor của ViewHolder, nó sẽ ánh xạ các thành phần giao diện
        // và thiết lập các sự kiện click cho mỗi mục trong danh sách.
        // Khi người dùng nhấn vào một mục, nó sẽ gọi phương thức onWorldClockClick
        // hoặc onWorldClockLongClick của listener nếu nó được thiết lập.
        // Nếu không có listener, nó sẽ không làm gì cả.
        public WorldClockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
            tvTimeDifference = itemView.findViewById(R.id.tv_time_difference);
            tvWorldTime = itemView.findViewById(R.id.tv_world_time);
            tvWorldPeriod = itemView.findViewById(R.id.tv_world_period);

            // này là click 1 lần
            // Cái này không cần thiết
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onWorldClockClick(getItem(position));
                }
            });

            // này là click và giữ
            itemView.setOnLongClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onWorldClockLongClick(getItem(position));
                    return true;
                }
                return false;
            });
        }

        // Phương thức này sẽ cập nhật các thành phần giao diện với dữ liệu
        // của WorldClock hiện tại. Nó sẽ lấy tên thành phố, múi giờ,
        // thời gian hiện tại và khoảng thời gian trong múi giờ đó
        // và hiển thị chúng trong các TextView tương ứng.
        public void bind(WorldClock worldClock) {
            tvCityName.setText(worldClock.getCityName());
            tvTimeDifference.setText(TimezoneUtils.getTimeDifference(worldClock.getTimeZoneId()));
            tvWorldTime.setText(TimezoneUtils.getCurrentTimeInTimezone(worldClock.getTimeZoneId()));
            tvWorldPeriod.setText(TimezoneUtils.getCurrentPeriodInTimezone(worldClock.getTimeZoneId()));
        }
    }

    // Listener để xử lý các sự kiện click và long click trên mỗi mục trong danh sách
    public interface OnWorldClockClickListener {
        void onWorldClockClick(WorldClock worldClock);
        void onWorldClockLongClick(WorldClock worldClock);
    }

    // Phương thức này sẽ thiết lập listener để xử lý các sự kiện click
    // và long click trên mỗi mục trong danh sách. Khi người dùng nhấn vào
    // một mục, nó sẽ gọi phương thức onWorldClockClick của listener.
    // Nếu người dùng nhấn và giữ một mục, nó sẽ gọi phương thức
    // onWorldClockLongClick của listener.
    public void setOnWorldClockClickListener(OnWorldClockClickListener listener) {
        this.listener = listener;
    }
}

