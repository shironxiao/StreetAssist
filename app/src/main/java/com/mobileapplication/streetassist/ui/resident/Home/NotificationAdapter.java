package com.mobileapplication.streetassist.ui.resident.Home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobileapplication.streetassist.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    // =========================================================================
    //  Data model
    // =========================================================================

    public static class NotificationItem {
        public String  id, title, message, status, reportId;
        public boolean isRead;
        public Date    createdAt;

        public NotificationItem(String id, String title, String message,
                                boolean isRead, Date createdAt, String status, String reportId) {
            this.id        = id;
            this.title     = title;
            this.message   = message;
            this.isRead    = isRead;
            this.createdAt = createdAt;
            this.status    = status;
            this.reportId  = reportId;
        }
    }

    // =========================================================================
    //  Adapter
    // =========================================================================

    private final List<NotificationItem> items;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem item = items.get(position);

        h.tvTitle.setText(item.title);
        h.tvMessage.setText(item.message);
        h.tvTime.setText(item.createdAt != null ? sdf.format(item.createdAt) : "");

        // Unread background highlight + dot
        h.itemView.setBackgroundColor(item.isRead ? 0xFFFFFFFF : 0xFFEDF2FF);
        h.dotUnread.setVisibility(item.isRead ? View.GONE : View.VISIBLE);

        // Status chip — show only when status field is present
        if (item.status != null && !item.status.isEmpty()) {
            h.dotStatus.setVisibility(View.VISIBLE);
            h.tvNotifStatus.setVisibility(View.VISIBLE);
            h.tvNotifStatus.setText(item.status);
            applyStatusChipStyle(h.tvNotifStatus, h.dotStatus, item.status);
        } else {
            h.dotStatus.setVisibility(View.GONE);
            h.tvNotifStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================================
    //  Status chip styling
    // =========================================================================

    private void applyStatusChipStyle(TextView chip, View dot, String status) {
        switch (status) {
            case "Pending":
                chip.setBackgroundResource(R.drawable.badge_pending);
                chip.setTextColor(Color.parseColor("#BA7517"));
                dot.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFC107")));
                break;
            case "Verified":
                chip.setBackgroundResource(R.drawable.badge_verified);
                chip.setTextColor(Color.parseColor("#0F6E56"));
                dot.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#1D9E75")));
                break;
            case "In Progress":
                chip.setBackgroundResource(R.drawable.badge_in_progress);
                chip.setTextColor(Color.parseColor("#185FA5"));
                dot.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#4169E1")));
                break;
            case "Resolved":
                chip.setBackgroundResource(R.drawable.badge_resolved);
                chip.setTextColor(Color.parseColor("#3B6D11"));
                dot.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                break;
            default:
                chip.setBackgroundResource(R.drawable.badge_pending);
                chip.setTextColor(Color.parseColor("#BA7517"));
                dot.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFC107")));
        }
    }

    // =========================================================================
    //  ViewHolder
    // =========================================================================

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime, tvNotifStatus;
        View dotUnread, dotStatus;

        VH(View v) {
            super(v);
            tvTitle       = v.findViewById(R.id.tvNotifTitle);
            tvMessage     = v.findViewById(R.id.tvNotifMessage);
            tvTime        = v.findViewById(R.id.tvNotifTime);
            dotUnread     = v.findViewById(R.id.dotUnread);
            tvNotifStatus = v.findViewById(R.id.tvNotifStatus);
            dotStatus     = v.findViewById(R.id.dotStatus);
        }
    }
}