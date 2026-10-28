package com.example.smartdecision.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.models.Review;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviews;
    private OnReviewDeleteListener deleteListener;
    private boolean isAdmin;
    private String currentUserId;

    public interface OnReviewDeleteListener {
        void onDelete(Review review);
    }

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void setConfig(boolean isAdmin, String currentUserId, OnReviewDeleteListener listener) {
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        this.deleteListener = listener;
        notifyDataSetChanged();
    }

    public void setAdmin(boolean isAdmin, OnReviewDeleteListener listener) {
        setConfig(isAdmin, null, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvName.setText(review.userName);
        holder.tvComment.setText(review.comment);
        holder.ratingBar.setRating(review.rating);
        holder.tvDate.setText(DateUtils.getRelativeTimeSpanString(review.timestamp));

        boolean isOwnReview = currentUserId != null && currentUserId.equals(review.userId);

        if (isAdmin || isOwnReview) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(review);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvComment, tvDate;
        RatingBar ratingBar;
        View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReviewerName);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            ratingBar = itemView.findViewById(R.id.reviewRating);
            btnDelete = itemView.findViewById(R.id.btnDeleteReview);
        }
    }
}
