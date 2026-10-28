package com.example.smartdecision.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.example.smartdecision.activities.SchoolDetailActivity;
import com.example.smartdecision.models.School;
import android.content.Intent;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class OwnerSchoolAdapter extends RecyclerView.Adapter<OwnerSchoolAdapter.ViewHolder> {

    private final Context context;
    private final List<School> schoolList;

    public OwnerSchoolAdapter(Context context, List<School> schoolList) {
        this.context = context;
        this.schoolList = schoolList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_owner_school, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        School school = schoolList.get(position);
        holder.tvName.setText(school.name);
        holder.tvCity.setText(school.city);
        
        String status = school.status != null ? school.status : "pending";
        holder.tvStatus.setText(status.toUpperCase());

        // Display level
        if (holder.tvLevel != null) {
            String level = school.schoolLevel;
            if (level == null || level.isEmpty() || level.equalsIgnoreCase("Unknown") || level.equalsIgnoreCase("Not Specified")) {
                level = school.inferSchoolLevel();
            }
            holder.tvLevel.setText(level);
        }

        // Status Background Styling
        if ("approved".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_success_bg);
        } else if ("rejected".equalsIgnoreCase(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_rejected_bg);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_pending_bg);
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
            holder.ivSchool.setVisibility(View.VISIBLE);
            holder.cardInitial.setVisibility(View.GONE);
            Glide.with(context)
                    .load(displayUrl)
                    .placeholder(R.drawable.spinner_background)
                    .into(holder.ivSchool);
        } else {
            holder.ivSchool.setVisibility(View.GONE);
            holder.cardInitial.setVisibility(View.VISIBLE);
            if (school.getName() != null && !school.getName().isEmpty()) {
                holder.textInitial.setText(school.getName().substring(0, 1).toUpperCase());
            } else {
                holder.textInitial.setText("S");
            }
        }

        // Edit Functionality
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.smartdecision.activities.AddSchoolActivity.class);
            intent.putExtra("edit_school", school);
            context.startActivity(intent);
        });

        // Delete Functionality
        holder.btnDelete.setOnClickListener(v -> showDeleteDialog(school, position));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SchoolDetailActivity.class);
            intent.putExtra("school", school);
            context.startActivity(intent);
        });
    }

    private void showDeleteDialog(School school, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete School")
                .setMessage("Are you sure you want to remove " + school.name + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference("Schools")
                            .child(school.id)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "School deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return schoolList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSchool, btnDelete, btnEdit;
        TextView tvName, tvCity, tvStatus, textInitial, tvLevel;
        View cardInitial;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSchool = itemView.findViewById(R.id.ivSchoolItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteSchool);
            btnEdit = itemView.findViewById(R.id.btnEditSchool);
            tvName = itemView.findViewById(R.id.tvSchoolName);
            tvCity = itemView.findViewById(R.id.tvSchoolCity);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            textInitial = itemView.findViewById(R.id.textInitial);
            cardInitial = itemView.findViewById(R.id.cardInitial);
            tvLevel = itemView.findViewById(R.id.tvSchoolLevel);
        }
    }
}
