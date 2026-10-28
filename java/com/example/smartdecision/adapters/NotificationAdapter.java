package com.example.smartdecision.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.models.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onApprove(Notification notification);
        void onReject(Notification notification);
        void onDelete(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notif = notifications.get(position);
        holder.tvTitle.setText(notif.title);
        holder.tvMessage.setText(notif.message);
        holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(notif.timestamp));

        if ("owner".equals(notif.type) && notif.applicationId != null) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnApprove.setOnClickListener(v -> listener.onApprove(notif));
            holder.btnReject.setOnClickListener(v -> listener.onReject(notif));
        } else {
            holder.layoutActions.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            listener.onDelete(notif);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        View layoutActions, btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            layoutActions = itemView.findViewById(R.id.layoutActionButtons);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
