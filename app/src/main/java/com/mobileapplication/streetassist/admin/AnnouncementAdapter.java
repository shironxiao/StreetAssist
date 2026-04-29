package com.mobileapplication.streetassist.admin;

import android.content.Context;
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

    private final Context context;
    private final List<Map<String, Object>> announcementList;

    public AnnouncementAdapter(Context context, List<Map<String, Object>> announcementList) {
        this.context = context;
        this.announcementList = announcementList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> announcement = announcementList.get(position);

        holder.tvTitle.setText(String.valueOf(announcement.get("title")));
        holder.tvCategory.setText(String.valueOf(announcement.get("category")));
        holder.tvSubtitle.setText(String.valueOf(announcement.get("subtitle")));
        holder.tvContact.setText("Contact: " + announcement.get("contact"));
        holder.tvDate.setText("Posted " + announcement.get("date"));

        String imageUrl = (String) announcement.get("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        String id = (String) announcement.get("id");
        holder.btnViewComments.setOnClickListener(v -> {
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showCommentsDialog(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvTitle, tvSubtitle, tvContact, tvDate, btnViewComments;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivAnnouncementImage);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvContact = itemView.findViewById(R.id.tvContact);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnViewComments = itemView.findViewById(R.id.btnViewComments);
        }
    }
}