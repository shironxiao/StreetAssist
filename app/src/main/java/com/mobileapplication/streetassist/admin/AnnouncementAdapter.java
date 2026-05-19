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
import java.util.Set;
import java.util.HashSet;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    private static final String TAG = "AnnouncementAdapter";
    private final Context context;
    private final List<Map<String, Object>> announcementList;

    private final Set<String> selectedAnnouncementIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface OnSelectionListener {
        void onSelectionChanged(int count);
        void onSelectionModeStarted();
        void onSelectionModeEnded();
    }

    private OnSelectionListener selectionListener;

    public void setSelectionListener(OnSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedAnnouncementIds.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onSelectionModeEnded();
        }
    }

    public Set<String> getSelectedAnnouncementIds() {
        return selectedAnnouncementIds;
    }

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
        String title = (String) announcement.get("title");
        if (name != null && !name.isEmpty()
                && (title == null || !title.equalsIgnoreCase(name))) {
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvName.setText("Subject: " + name);
        } else {
            holder.tvName.setVisibility(View.GONE);
        }

        String age = (String) announcement.get("age");
        String sex = (String) announcement.get("sex");
        if ((age != null && !age.isEmpty()) || (sex != null && !sex.isEmpty())) {
            holder.tvAgeSex.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder();
            if (age != null && !age.isEmpty()) sb.append("Age: ").append(age);
            if (sex != null && !sex.isEmpty()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append("Sex: ").append(sex);
            }
            holder.tvAgeSex.setText(sb.toString());
        } else {
            holder.tvAgeSex.setVisibility(View.GONE);
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

        // Selection UI State
        if (isSelectionMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(selectedAnnouncementIds.contains(id));
        } else {
            holder.cbSelect.setVisibility(View.GONE);
        }

        holder.cbSelect.setOnClickListener(v -> {
            if (id != null) {
                if (holder.cbSelect.isChecked()) {
                    selectedAnnouncementIds.add(id);
                } else {
                    selectedAnnouncementIds.remove(id);
                }
                if (selectionListener != null) {
                    selectionListener.onSelectionChanged(selectedAnnouncementIds.size());
                }
            }
        });

        // Make the whole item clickable to open comments or select
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                holder.cbSelect.performClick();
                return;
            }
            Log.d(TAG, "Item clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showCommentsDialog(id);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                if (id != null) selectedAnnouncementIds.add(id);
                notifyDataSetChanged();
                if (selectionListener != null) {
                    selectionListener.onSelectionModeStarted();
                    selectionListener.onSelectionChanged(selectedAnnouncementIds.size());
                }
                return true;
            }
            return false;
        });

        holder.btnViewComments.setOnClickListener(v -> {
            if (isSelectionMode) {
                holder.cbSelect.performClick();
                return;
            }
            Log.d(TAG, "View Comments clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showCommentsDialog(id);
            }
        });

        holder.btnUpdateStatus.setOnClickListener(v -> {
            if (isSelectionMode) {
                holder.cbSelect.performClick();
                return;
            }
            Log.d(TAG, "Update Status clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showUpdateStatusDialog(id, status);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (isSelectionMode) {
                holder.cbSelect.performClick();
                return;
            }
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
        TextView tvCategory, tvStatusBadge, tvTitle, tvName, tvAgeSex, tvSubtitle, tvContact, tvDate, tvIncidentDateTime, tvLocation;
        View btnViewComments, btnUpdateStatus, btnDelete, containerIncidentInfo;
        android.widget.CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivAnnouncementImage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvName = itemView.findViewById(R.id.tvName);
            tvAgeSex = itemView.findViewById(R.id.tvAgeSex);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvIncidentDateTime = itemView.findViewById(R.id.tvIncidentDateTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            containerIncidentInfo = itemView.findViewById(R.id.containerIncidentInfo);
            btnViewComments = itemView.findViewById(R.id.btnViewComments);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            cbSelect = itemView.findViewById(R.id.cbSelectAnnouncement);
        }
    }
}
