package com.mobileapplication.streetassist.admin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mobileapplication.streetassist.R;

import java.util.List;
import java.util.Map;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private static final String TAG = "AnnouncementAdapter";
    private final Context context;
    private final List<Map<String, Object>> announcementList;

    public AnnouncementAdapter(Context context, List<Map<String, Object>> announcementList) {
        this.context = context;
        this.announcementList = announcementList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> announcement = announcementList.get(position);
        String id = (String) announcement.get("id");
        String category = String.valueOf(announcement.get("category"));
        String status = (String) announcement.get("status");

        holder.tvTitle.setText(String.valueOf(announcement.get("title")));
        holder.tvCategory.setText(category);
        holder.tvSubtitle.setText(String.valueOf(announcement.get("subtitle")));
        holder.tvContact.setText(String.valueOf(announcement.get("contact")));
        holder.tvDate.setText("Posted " + announcement.get("date"));

        String name = (String) announcement.get("name");
        if (name != null && !name.isEmpty()) {
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvName.setText("Subject: " + name);
        } else {
            holder.tvName.setVisibility(View.GONE);
        }

        // Status logic for all announcements
        holder.tvStatusBadge.setVisibility(View.VISIBLE);
        holder.tvStatusBadge.setText(status != null ? status : "Verified by Police");
        holder.btnUpdateStatus.setVisibility(View.VISIBLE);

        // Bind Incident Info
        String incidentDate = (String) announcement.get("incidentDate");
        String incidentTime = (String) announcement.get("incidentTime");
        String locationAddress = (String) announcement.get("locationAddress");

        boolean hasIncidentInfo = (incidentDate != null && !incidentDate.isEmpty())
                || (locationAddress != null && !locationAddress.isEmpty());

        if (hasIncidentInfo) {
            holder.containerIncidentInfo.setVisibility(View.VISIBLE);
            String dateTime = "";
            if (incidentDate != null && !incidentDate.isEmpty()) dateTime += "📅 " + incidentDate;
            if (incidentTime != null && !incidentTime.isEmpty()) dateTime += (dateTime.isEmpty() ? "" : " at ") + incidentTime;

            holder.tvIncidentDateTime.setVisibility(dateTime.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvIncidentDateTime.setText(dateTime);

            if (locationAddress != null && !locationAddress.isEmpty()) {
                holder.tvLocation.setVisibility(View.VISIBLE);
                holder.tvLocation.setText("📍 " + locationAddress);
            } else {
                holder.tvLocation.setVisibility(View.GONE);
            }
        } else {
            holder.containerIncidentInfo.setVisibility(View.GONE);
        }

        String imageUrl = (String) announcement.get("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_image_placeholder).centerCrop().into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Make the whole item clickable to open comments
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showCommentsDialog(id);
            }
        });

        holder.btnViewComments.setOnClickListener(v -> {
            Log.d(TAG, "View Comments clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showCommentsDialog(id);
            }
        });

        holder.btnUpdateStatus.setOnClickListener(v -> {
            Log.d(TAG, "Update Status clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showUpdateStatusDialog(id, status);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            Log.d(TAG, "Delete clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).deleteAnnouncement(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvStatusBadge, tvTitle, tvName, tvSubtitle, tvContact, tvDate, tvIncidentDateTime, tvLocation;
        View btnViewComments, btnUpdateStatus, btnDelete, containerIncidentInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivAnnouncementImage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvIncidentDateTime = itemView.findViewById(R.id.tvIncidentDateTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            containerIncidentInfo = itemView.findViewById(R.id.containerIncidentInfo);
            btnViewComments = itemView.findViewById(R.id.btnViewComments);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
