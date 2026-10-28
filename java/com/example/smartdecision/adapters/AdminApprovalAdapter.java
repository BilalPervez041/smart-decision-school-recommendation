package com.example.smartdecision.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.example.smartdecision.models.School;
import java.util.List;

public class AdminApprovalAdapter extends RecyclerView.Adapter<AdminApprovalAdapter.ViewHolder> {

    private Context context;
    private List<School> schoolList;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApprove(School school);
        void onReject(School school);
    }

    public AdminApprovalAdapter(Context context, List<School> schoolList, OnActionClickListener listener) {
        this.context = context;
        this.schoolList = schoolList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        School school = schoolList.get(position);
        school.cleanAllFields();
        
        holder.tvName.setText(school.getName());
        holder.tvCityArea.setText(school.getCity() + ", " + school.getLocationArea());
        holder.tvLevel.setText(school.schoolLevel);
        
        String details = "Type: " + school.getType() + " | Gender: " + school.getGender() + "\n" +
                         "Fee: " + school.getDynamicFeeDisplay(null) + "\n" +
                         "Contact: " + (school.contact != null ? school.contact : "N/A") + "\n" +
                         "Address: " + school.getAddress();
        holder.tvDetails.setText(details);

        // Determine which image to show (Logo preferred, fallback to cover/image)
        String displayImageUrl = null;
        if (school.logoUrl != null && !school.logoUrl.isEmpty() && !school.logoUrl.equals("Unknown")) {
            displayImageUrl = school.logoUrl;
        } else if (school.coverUrl != null && !school.coverUrl.isEmpty() && !school.coverUrl.equals("Unknown")) {
            displayImageUrl = school.coverUrl;
        } else if (school.imageUrl != null && !school.imageUrl.isEmpty() && !school.imageUrl.equals("Unknown")) {
            displayImageUrl = school.imageUrl;
        }

        if (displayImageUrl != null) {
            holder.ivSchool.setVisibility(View.VISIBLE);
            holder.cardInitial.setVisibility(View.GONE);
            Glide.with(context)
                    .load(displayImageUrl)
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

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(school));
        holder.btnReject.setOnClickListener(v -> listener.onReject(school));
    }

    @Override
    public int getItemCount() {
        return schoolList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSchool;
        TextView tvName, tvCityArea, tvLevel, tvDetails, textInitial;
        View cardInitial;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSchool = itemView.findViewById(R.id.ivSchoolImage);
            tvName = itemView.findViewById(R.id.tvSchoolName);
            tvCityArea = itemView.findViewById(R.id.tvCityArea);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            textInitial = itemView.findViewById(R.id.textInitial);
            cardInitial = itemView.findViewById(R.id.cardInitial);
            btnApprove = findViewById(R.id.btnApprove);
            btnReject = findViewById(R.id.btnReject);
        }

        private <T extends View> T findViewById(int id) {
            return itemView.findViewById(id);
        }
    }
}
