package com.example.alarm.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alarm.R;
import com.example.alarm.model.entities.TimezoneInfo;

import java.util.ArrayList;
import java.util.List;

public class TimezoneSelectorAdapter extends RecyclerView.Adapter<TimezoneSelectorAdapter.TimezoneViewHolder> {

    private List<TimezoneInfo> timezones;
    private List<TimezoneInfo> filteredTimezones;
    private OnTimezoneSelectedListener listener;

    public TimezoneSelectorAdapter() {
        timezones = new ArrayList<>();
        filteredTimezones = new ArrayList<>();
    }

    //
    @NonNull
    @Override
    public TimezoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timezone_selector, parent, false);
        return new TimezoneViewHolder(itemView);
    }

    // Phương thức này được gọi khi RecyclerView cần cập nhật một ViewHolder
    @Override
    public void onBindViewHolder(@NonNull TimezoneViewHolder holder, int position) {
        TimezoneInfo timezone = filteredTimezones.get(position);
        holder.bind(timezone);
    }

    // Phương thức này trả về số lượng mục trong danh sách
    @Override
    public int getItemCount() {
        return filteredTimezones.size();
    }

    // Thiết lập danh sách múi giờ và làm mới dữ liệu
    public void setTimezones(List<TimezoneInfo> timezones) {
        this.timezones = timezones;
        this.filteredTimezones = new ArrayList<>(timezones);
        notifyDataSetChanged();
    }

    // Phương thức này sẽ lọc danh sách múi giờ dựa trên truy vấn tìm kiếm
    // Nếu truy vấn rỗng, nó sẽ hiển thị tất cả các múi giờ
    // Nếu có truy vấn, nó sẽ lọc các múi giờ có tên thành phố hoặc ID múi giờ chứa truy vấn đó
    // Sau đó, nó sẽ thông báo cho RecyclerView rằng dữ liệu đã thay đổi để cập nhật giao diện
    // của nó.
    public void filter(String query) {
        filteredTimezones.clear();
        if (query.isEmpty()) {
            filteredTimezones.addAll(timezones);
        } else {
            // Tìm kiếm theo tên thành phố hoặc ID múi giờ
            String lowerCaseQuery = query.toLowerCase();
            for (TimezoneInfo timezone : timezones) {
                if (timezone.getCityName().toLowerCase().contains(lowerCaseQuery) ||
                        timezone.getTimeZoneId().toLowerCase().contains(lowerCaseQuery)) {
                    filteredTimezones.add(timezone);
                }
            }
        }
        notifyDataSetChanged();
    }

    // ViewHolder này sẽ giữ các thành phần giao diện của mỗi mục trong danh sách
    // và sẽ được sử dụng để cập nhật giao diện với dữ liệu của TimezoneInfo

    public class TimezoneViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCityName;
        private TextView tvTimezoneInfo;

        // Constructor của ViewHolder, nó sẽ ánh xạ các thành phần giao diện
        // và thiết lập các sự kiện click cho mỗi mục trong danh sách.
        public TimezoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
            tvTimezoneInfo = itemView.findViewById(R.id.tv_timezone_info);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onTimezoneSelected(filteredTimezones.get(position));
                }
            });
        }

        // Phương thức này sẽ cập nhật các thành phần giao diện với dữ liệu
        // của TimezoneInfo hiện tại. Nó sẽ lấy tên thành phố và thông tin múi giờ
        // và hiển thị chúng trong các TextView tương ứng.
        // Ví dụ: "Hà Nội (GMT+7:00)"
        public void bind(TimezoneInfo timezone) {
            tvCityName.setText(timezone.getCityName());
            tvTimezoneInfo.setText(timezone.getDisplayName());
        }
    }

    // Listener để xử lý sự kiện khi người dùng chọn một múi giờ
    public interface OnTimezoneSelectedListener {
        void onTimezoneSelected(TimezoneInfo timezone);
    }

    // Phương thức này sẽ thiết lập listener để xử lý sự kiện khi người dùng chọn một múi giờ.
    public void setOnTimezoneSelectedListener(OnTimezoneSelectedListener listener) {
        this.listener = listener;
    }
}

