package com.example.smartdecision.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.example.smartdecision.activities.SchoolDetailActivity;
import com.example.smartdecision.models.School;
import java.util.List;

public class SchoolAdapter extends RecyclerView.Adapter<SchoolAdapter.SchoolViewHolder> {

    private List<School> schoolList;
    private OnSchoolClickListener listener;
    private String selectedGrade = "Any";

    public interface OnSchoolClickListener {
        void onSchoolClick(School school);
    }

    public SchoolAdapter(List<School> schoolList, OnSchoolClickListener listener) {
        this.schoolList = schoolList;
        this.listener = listener;
    }

    public void updateList(List<School> newList) {
        this.schoolList = newList;
        notifyDataSetChanged();
    }

    public void setSelectedGrade(String grade) {
        this.selectedGrade = grade;
    }

    @NonNull
    @Override
    public SchoolViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_school, parent, false);
        return new SchoolViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SchoolViewHolder holder, int position) {
        School school = schoolList.get(position);
        holder.name.setText(school.getName());
        holder.textRatingNumber.setText(String.format("(%.1f)", school.getRating()));
        
        // Admission Status
        if (holder.textAdmissionStatus != null) {
            if (school.admissionOpen) {
                holder.textAdmissionStatus.setText("ADMISSION OPEN");
                holder.textAdmissionStatus.setBackgroundResource(R.drawable.bg_badge_green);
            } else {
                holder.textAdmissionStatus.setText("ADMISSION CLOSED");
                holder.textAdmissionStatus.setBackgroundResource(R.drawable.bg_badge_red);
            }
        }

        // Display School Level (e.g. Nursery to Matric)
        if (holder.textLevel != null) {
            String level = school.schoolLevel;
            if (level == null || level.isEmpty() || level.equalsIgnoreCase("Unknown") || level.equalsIgnoreCase("Not Specified")) {
                level = school.inferSchoolLevel();
            }
            holder.textLevel.setText(level);
        }

        // Image logic: Prefer logoUrl, fallback to coverUrl/imageUrl (old behavior), then initials.
        String displayUrl = null;
        if (school.logoUrl != null && !school.logoUrl.isEmpty() && !school.logoUrl.equals("Unknown")) {
            displayUrl = school.logoUrl;
        } else if (school.coverUrl != null && !school.coverUrl.isEmpty() && !school.coverUrl.equals("Unknown")) {
            displayUrl = school.coverUrl;
        } else if (school.imageUrl != null && !school.imageUrl.isEmpty() && !school.imageUrl.equals("Unknown")) {
            displayUrl = school.imageUrl;
        }

        if (displayUrl != null) {
            holder.imgSchool.setVisibility(View.VISIBLE);
            holder.textInitial.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(displayUrl)
                    .placeholder(R.drawable.spinner_background)
                    .into(holder.imgSchool);
        } else {
            holder.imgSchool.setVisibility(View.GONE);
            holder.textInitial.setVisibility(View.VISIBLE);
            if (school.getName() != null && !school.getName().isEmpty()) {
                holder.textInitial.setText(school.getName().substring(0, 1).toUpperCase());
            } else {
                holder.textInitial.setText("S");
            }
        }

        String feeDisplay = school.getDynamicFeeDisplay(selectedGrade);
        if (!feeDisplay.equals("N/A")) {
            // Remove labels like (Nursery to Inter) from feeDisplay for cleaner look if needed
            // But let's keep it consistent with school's logic
            String cleanFee = feeDisplay.replaceAll("\\s*\\([^)]*\\)", "");
            holder.fee.setText("Fee: " + cleanFee + " PKR");
        } else {
            holder.fee.setText("Fee: N/A");
        }
        
        // Accurate Distance Display
        if (school.getDistance() >= 0) {
            holder.distance.setText(String.format("%.1f km away", school.getDistance()));
        } else {
            holder.distance.setText("Calculating...");
        }
        
        // Medium Display
        String medium = school.getMedium();
        holder.textMedium.setText(medium);
        if (medium.toLowerCase().contains("english")) {
            holder.textMedium.setTextColor(Color.parseColor("#2E7D32")); // Professional Green
        } else if (medium.toLowerCase().contains("urdu")) {
            holder.textMedium.setTextColor(Color.parseColor("#C62828")); // Professional Red
        } else {
            holder.textMedium.setTextColor(Color.GRAY);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSchoolClick(school);
            } else {
                Intent intent = new Intent(v.getContext(), SchoolDetailActivity.class);
                intent.putExtra("school", school);
                intent.putExtra("selectedGrade", selectedGrade);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return schoolList.size();
    }

    static class SchoolViewHolder extends RecyclerView.ViewHolder {
        TextView name, fee, distance, textInitial, textMedium, textRatingNumber, textLevel, textAdmissionStatus;
        ImageView imgSchool;
        View cardInitialContainer;

        public SchoolViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textSchoolName);
            fee = itemView.findViewById(R.id.textFee);
            distance = itemView.findViewById(R.id.textDistance);
            textInitial = itemView.findViewById(R.id.textInitial);
            textMedium = itemView.findViewById(R.id.textMedium);
            textRatingNumber = itemView.findViewById(R.id.textRatingNumber);
            imgSchool = itemView.findViewById(R.id.imgSchool);
            textLevel = itemView.findViewById(R.id.textLevel);
            textAdmissionStatus = itemView.findViewById(R.id.textAdmissionStatus);
            cardInitialContainer = itemView.findViewById(R.id.cardInitialContainer);
        }
    }
}
