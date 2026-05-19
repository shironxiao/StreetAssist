package com.mobileapplication.streetassist.admin;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

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
            if (incidentDate != null && !incidentDate.isEmpty()) dateTime += "Incident Date: " + incidentDate;
            if (incidentTime != null && !incidentTime.isEmpty()) dateTime += (dateTime.isEmpty() ? "" : " at ") + incidentTime;

            holder.tvIncidentDateTime.setVisibility(dateTime.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvIncidentDateTime.setText(dateTime);

            if (locationAddress != null && !locationAddress.isEmpty()) {
                holder.tvLocation.setVisibility(View.VISIBLE);
                holder.tvLocation.setText("Location: " + locationAddress);
            } else {
                holder.tvLocation.setVisibility(View.GONE);
            }
        } else {
            holder.containerIncidentInfo.setVisibility(View.GONE);
        }

        String imageUrl = (String) announcement.get("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_image_placeholder).centerCrop().into(holder.ivImage);
            holder.ivImage.setOnClickListener(v -> {
                if (isSelectionMode) {
                    holder.cbSelect.performClick();
                    return;
                }
                showImagePreviewDialog(imageUrl);
            });
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_image_placeholder);
            holder.ivImage.setOnClickListener(null);
        }

        // Render multiple announcement images under subtitle
        Object announcementImagesObj = announcement.get("announcementImages");
        if (announcementImagesObj instanceof List) {
            List<String> announcementImages = (List<String>) announcementImagesObj;
            if (announcementImages != null && !announcementImages.isEmpty()) {
                holder.scrollAnnouncementImages.setVisibility(View.VISIBLE);
                holder.layoutAnnouncementImages.removeAllViews();

                float density = context.getResources().getDisplayMetrics().density;
                int thumbW = (int) (80 * density);
                int thumbH = (int) (80 * density);
                int margin = (int) (6 * density);
                int radius = (int) (8 * density);

                for (String extraUrl : announcementImages) {
                    com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(thumbW, thumbH);
                    params.setMargins(0, 0, margin, 0);
                    card.setLayoutParams(params);
                    card.setRadius(radius);
                    card.setCardElevation(1 * density);
                    card.setStrokeWidth((int) (1 * density));
                    card.setStrokeColor(0xFFE2E8F0);
                    card.setClickable(true);
                    card.setFocusable(true);

                    ImageView iv = new ImageView(context);
                    iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(context)
                            .load(extraUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(iv);
                    card.addView(iv);

                    iv.setOnClickListener(imgV -> {
                        if (isSelectionMode) {
                            holder.cbSelect.performClick();
                            return;
                        }
                        showImagePreviewDialog(extraUrl);
                    });

                    holder.layoutAnnouncementImages.addView(card);
                }
            } else {
                holder.scrollAnnouncementImages.setVisibility(View.GONE);
            }
        } else {
            holder.scrollAnnouncementImages.setVisibility(View.GONE);
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

        holder.btnEdit.setOnClickListener(v -> {
            if (isSelectionMode) {
                holder.cbSelect.performClick();
                return;
            }
            Log.d(TAG, "Edit clicked: " + id);
            if (context instanceof AdminAnnouncementsActivity) {
                ((AdminAnnouncementsActivity) context).showEditAnnouncementDialog(id, announcement);
            }
        });

        // Case Closed Proof Section
        if ("Case Closed".equalsIgnoreCase(status)) {
            holder.layoutCaseClosedProof.setVisibility(View.VISIBLE);
            holder.layoutCaseClosedImages.removeAllViews();

            float density = context.getResources().getDisplayMetrics().density;
            int thumbH = (int) (90 * density); // matches the HorizontalScrollView height in xml
            int thumbW = (int) (90 * density);
            int thumbMargin = (int) (8 * density);
            int thumbRadius = (int) (10 * density);

            Object proofObj = announcement.get("caseClosedImages");
            int proofCount = 0;
            if (proofObj instanceof List) {
                List<String> proofImages = (List<String>) proofObj;
                proofCount = proofImages.size();
                for (String proofUrl : proofImages) {
                    com.google.android.material.card.MaterialCardView card =
                            new com.google.android.material.card.MaterialCardView(context);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(thumbW, thumbH);
                    cardParams.setMargins(0, 0, thumbMargin, 0);
                    card.setLayoutParams(cardParams);
                    card.setRadius(thumbRadius);
                    card.setCardElevation(2 * density);
                    card.setStrokeWidth((int) (1 * density));
                    card.setStrokeColor(0xFFD1FAE5); // light green border
                    card.setClickable(true);
                    card.setFocusable(true);
                    card.setUseCompatPadding(false);

                    ImageView iv = new ImageView(context);
                    iv.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(context)
                            .load(proofUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(iv);
                    card.addView(iv);

                    iv.setOnClickListener(v -> {
                        if (isSelectionMode) {
                            holder.cbSelect.performClick();
                            return;
                        }
                        showImagePreviewDialog(proofUrl);
                    });

                    holder.layoutCaseClosedImages.addView(card);
                }
            }

            // Update proof count badge
            if (holder.tvProofCount != null) {
                holder.tvProofCount.setText(proofCount + (proofCount == 1 ? " photo" : " photos"));
            }
            // Show divider only when there are images
            if (holder.proofDivider != null) {
                holder.proofDivider.setVisibility(proofCount > 0 ? View.VISIBLE : View.GONE);
            }

            holder.btnAddProofImage.setOnClickListener(v -> {
                if (context instanceof AdminAnnouncementsActivity) {
                    ((AdminAnnouncementsActivity) context).startProofImagePicker(id);
                }
            });
        } else {
            holder.layoutCaseClosedProof.setVisibility(View.GONE);
        }
    }

    private void showImagePreviewDialog(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        
        ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);
        
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(ivFullscreen);
                
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (ivFullscreen != null) {
            ivFullscreen.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return announcementList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvStatusBadge, tvTitle, tvName, tvAgeSex, tvSubtitle, tvContact, tvDate, tvIncidentDateTime, tvLocation;
        View btnViewComments, btnUpdateStatus, btnDelete, btnEdit, containerIncidentInfo;
        android.widget.CheckBox cbSelect;

        View layoutCaseClosedProof;
        LinearLayout layoutCaseClosedImages;
        com.google.android.material.button.MaterialButton btnAddProofImage;
        TextView tvProofCount;
        View proofDivider;

        View scrollAnnouncementImages;
        LinearLayout layoutAnnouncementImages;

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
            btnEdit = itemView.findViewById(R.id.btnEdit);
            cbSelect = itemView.findViewById(R.id.cbSelectAnnouncement);

            layoutCaseClosedProof = itemView.findViewById(R.id.layoutCaseClosedProof);
            layoutCaseClosedImages = itemView.findViewById(R.id.layoutCaseClosedImages);
            btnAddProofImage = itemView.findViewById(R.id.btnAddProofImage);
            tvProofCount = itemView.findViewById(R.id.tvProofCount);
            proofDivider = itemView.findViewById(R.id.proofDivider);

            scrollAnnouncementImages = itemView.findViewById(R.id.scrollAnnouncementImages);
            layoutAnnouncementImages = itemView.findViewById(R.id.layoutAnnouncementImages);
        }
    }
}
