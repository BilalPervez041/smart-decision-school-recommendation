package com.example.smartdecision.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CityStatAdapter extends RecyclerView.Adapter<CityStatAdapter.ViewHolder> {

    private List<String> cityNames;
    private Map<String, Integer> cityCounts;

    public CityStatAdapter(Map<String, Integer> cityCounts) {
        this.cityCounts = cityCounts;
        this.cityNames = new ArrayList<>(cityCounts.keySet());
    }

    public void updateData(Map<String, Integer> newCityCounts) {
        this.cityCounts = newCityCounts;
        this.cityNames = new ArrayList<>(cityCounts.keySet());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String city = cityNames.get(position);
        Integer count = cityCounts.get(city);
        holder.tvCityName.setText(city);
        holder.tvSchoolCount.setText(String.valueOf(count != null ? count : 0));
    }

    @Override
    public int getItemCount() {
        return cityNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityName, tvSchoolCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tvCityName);
            tvSchoolCount = itemView.findViewById(R.id.tvSchoolCount);
        }
    }
}
