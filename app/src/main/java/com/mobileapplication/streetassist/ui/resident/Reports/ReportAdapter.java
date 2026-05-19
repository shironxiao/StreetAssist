package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.widget.Toast;
import android.app.Dialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import android.widget.Toast;
import com.mobileapplication.streetassist.R;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final Context context;
    private List<Map<String, Object>> reportList;

    public ReportAdapter(Context context, List<Map<String, Object>> reportList) {
        this.context    = context;
        this.reportList = reportList;
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_report_card, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Map<String, Object> report = reportList.get(position);

        holder.tvReportId.setText(getString(report, "reportId", "—"));

        String status = getString(report, "status", "Pending");
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        holder.tvDescription.setText(getString(report, "description", "No description."));
        holder.tvAge.setText("Age: " + getString(report, "approximateAge", "—"));
        holder.tvSex.setText("Sex: " + getString(report, "sex", "—"));
        holder.tvLocation.setText(getString(report, "locationAddress", "Location not set"));

        String assistance = getString(report, "assistanceDescription", "—");
        holder.tvAssistance.setText("Assistance: " +
                (assistance.length() > 40 ? assistance.substring(0, 40) + "…" : assistance));

        holder.tvTimestamp.setText("Submitted: " + formatTimestamp(report.get("timestamp")));

        // Show seenAt on the card
        String seenAtFormatted = formatTimestamp(report.get("seenAt"));
        holder.tvSeenAt.setText("Seen: " + seenAtFormatted);

        holder.tvViewDetails.setOnClickListener(v -> showDetailsBottomSheet(report));

        // Only show Remove button for Resolved reports
        if ("Resolved".equals(status)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setText("Remove");
            holder.btnDelete.setOnClickListener(v -> showRemoveConfirmation(report));
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> showDetailsBottomSheet(report));
    }

    @Override
    public int getItemCount() {
        return reportList == null ? 0 : reportList.size();
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────

    private void showDetailsBottomSheet(Map<String, Object> report) {
        BottomSheetDialog dialog = new BottomSheetDialog(context,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_report_details, null);
        dialog.setContentView(sheetView);

        setText(sheetView, R.id.dialogReportId,    getString(report, "reportId", "—"));

        String status = getString(report, "status", "Pending");
        TextView tvStatus = sheetView.findViewById(R.id.dialogStatus);
        tvStatus.setText(status);
        applyStatusStyle(tvStatus, status);

        setText(sheetView, R.id.dialogAge,         getString(report, "approximateAge", "—"));
        setText(sheetView, R.id.dialogSex,         getString(report, "sex", "—"));
        setText(sheetView, R.id.dialogDescription, getString(report, "description", "No description provided."));
        
        TextView tvLocation = sheetView.findViewById(R.id.dialogLocation);
        tvLocation.setText(getString(report, "locationAddress", "No address available."));
        Object lat = report.get("latitude");
        Object lng = report.get("longitude");
        if (lat != null && lng != null) {
            tvLocation.setOnClickListener(v -> showLocationOnMap(toDouble(lat), toDouble(lng)));
        }

        setText(sheetView, R.id.dialogAssistance,  getString(report, "assistanceDescription", "Not specified."));

        String contact = getString(report, "contactNumber", "").trim();
        setText(sheetView, R.id.dialogContact, contact.isEmpty() ? "Not provided" : contact);

        // Seen at date/time
        setText(sheetView, R.id.dialogSeenAt,      formatTimestamp(report.get("seenAt")));

        // Submitted timestamp
        setText(sheetView, R.id.dialogTimestamp,   formatTimestamp(report.get("timestamp")));

        View layoutProof = sheetView.findViewById(R.id.layoutResidentResolutionProof);
        if ("Resolved".equals(status)) {
            layoutProof.setVisibility(View.VISIBLE);
            String notes = getString(report, "resolutionNotes", "").trim();
            TextView tvNotes = sheetView.findViewById(R.id.tvResidentResolutionNotes);
            tvNotes.setText(notes.isEmpty() ? "Resolved by Administrator." : notes);

            String proofUrl = getString(report, "resolutionImageUrl", "");
            View cardPhoto = sheetView.findViewById(R.id.cardResidentProofPhoto);
            MaterialButton btnViewProof = sheetView.findViewById(R.id.btnViewResidentProof);

            List<String> proofImages = new ArrayList<>();
            if (report.get("resolutionImages") instanceof List) {
                List<?> rawList = (List<?>) report.get("resolutionImages");
                for (Object item : rawList) {
                    if (item instanceof String) {
                        proofImages.add((String) item);
                    }
                }
            }
            if (proofImages.isEmpty() && !proofUrl.isEmpty()) {
                proofImages.add(proofUrl);
            }

            if (!proofImages.isEmpty()) {
                cardPhoto.setVisibility(View.GONE);
                if (btnViewProof != null) {
                    btnViewProof.setVisibility(View.VISIBLE);
                    btnViewProof.setOnClickListener(v -> showResolutionProofDialog(proofImages));
                }
            } else {
                cardPhoto.setVisibility(View.GONE);
                if (btnViewProof != null) {
                    btnViewProof.setVisibility(View.GONE);
                }
            }
        } else {
            layoutProof.setVisibility(View.GONE);
        }

        MaterialButton btnClose = sheetView.findViewById(R.id.dialogBtnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        MaterialButton btnRemove = sheetView.findViewById(R.id.dialogBtnDelete);
        if ("Resolved".equals(status)) {
            btnRemove.setVisibility(View.VISIBLE);
            btnRemove.setText("Remove Report");
            btnRemove.setOnClickListener(v -> {
                dialog.dismiss();
                showRemoveConfirmation(report);
            });
        } else {
            btnRemove.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void showLocationOnMap(double lat, double lng) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_map_view);
        MapView map = dialog.findViewById(R.id.mapViewOnly);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        GeoPoint point = new GeoPoint(lat, lng);
        map.getController().setZoom(17.0);
        map.getController().setCenter(point);
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Sighting Location");
        map.getOverlays().add(marker);
        btnClose.setOnClickListener(va -> dialog.dismiss());
        dialog.show();
    }

    private void showRemoveConfirmation(Map<String, Object> report) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Remove Report")
                .setMessage("This report will be removed from your list, but will remain in our records. Continue?")
                .setPositiveButton("Remove", (dialog, which) -> removeReport(report))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResolutionProofDialog(List<String> images) {
        if (images == null || images.isEmpty()) return;
        
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_case_closed_proof_gallery);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvGalleryTitle);
        if (tvTitle != null) {
            tvTitle.setText("Resolution Proof Photos");
            tvTitle.setTextColor(android.graphics.Color.parseColor("#3B6D11"));
        }

        LinearLayout layoutImages = dialog.findViewById(R.id.layoutCaseClosedImagesDialog);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseProofDialog);

        float density = context.getResources().getDisplayMetrics().density;
        int thumbW = (int) (120 * density);
        int thumbH = (int) (120 * density);
        int margin = (int) (10 * density);
        int radius = (int) (12 * density);

        layoutImages.removeAllViews();
        for (String proofUrl : images) {
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(context);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(thumbW, thumbH);
            cardParams.setMargins(0, 0, margin, 0);
            card.setLayoutParams(cardParams);
            card.setRadius(radius);
            card.setCardElevation(2 * density);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(0xFFE2E8F0);
            card.setClickable(true);
            card.setFocusable(true);

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            com.bumptech.glide.Glide.with(context).load(proofUrl).placeholder(R.drawable.ic_image_placeholder).into(iv);
            card.addView(iv);

            iv.setOnClickListener(v -> {
                showFullImageDialog(proofUrl);
            });

            layoutImages.addView(card);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showFullImageDialog(String imageUrl) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);

        com.bumptech.glide.Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(ivFullscreen);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        ivFullscreen.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void removeReport(Map<String, Object> report) {
        String docId = (String) report.get("reportId");
        if (docId == null) return;

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Instead of deleting, we set a flag to hide it from the resident's view
        db.collection("reports").document(docId)
                .update("isHiddenByResident", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Report removed from view", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error removing report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyStatusStyle(TextView tv, String status) {
        switch (status) {
            case "Pending":
                tv.setBackgroundResource(R.drawable.badge_pending);
                tv.setTextColor(Color.parseColor("#BA7517"));
                break;
            case "Verified":
                tv.setBackgroundResource(R.drawable.badge_verified);
                tv.setTextColor(Color.parseColor("#0F6E56"));
                break;
            case "In Progress":
                tv.setBackgroundResource(R.drawable.badge_in_progress);
                tv.setTextColor(Color.parseColor("#185FA5"));
                break;
            case "Resolved":
                tv.setBackgroundResource(R.drawable.badge_resolved);
                tv.setTextColor(Color.parseColor("#3B6D11"));
                break;
            default:
                tv.setBackgroundResource(R.drawable.badge_pending);
                tv.setTextColor(Color.parseColor("#BA7517"));
                break;
        }
    }

    private String formatTimestamp(Object ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());
        if (ts instanceof com.google.firebase.Timestamp) {
            return sdf.format(((com.google.firebase.Timestamp) ts).toDate());
        } else if (ts instanceof Date) {
            return sdf.format((Date) ts);
        }
        return "—";
    }

    private String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null && !val.toString().isEmpty() ? val.toString() : fallback;
    }

    private void setText(View parent, int viewId, String value) {
        TextView tv = parent.findViewById(viewId);
        if (tv != null) tv.setText(value);
    }

    private double toDouble(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Long)   return ((Long) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportId, tvStatus, tvDescription, tvAge,
                tvSex, tvLocation, tvAssistance, tvTimestamp,
                tvSeenAt, tvViewDetails, btnDelete;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReportId    = itemView.findViewById(R.id.tvReportId);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAge         = itemView.findViewById(R.id.tvAge);
            tvSex         = itemView.findViewById(R.id.tvSex);
            tvLocation    = itemView.findViewById(R.id.tvLocation);
            tvAssistance  = itemView.findViewById(R.id.tvAssistance);
            tvTimestamp   = itemView.findViewById(R.id.tvTimestamp);
            tvSeenAt      = itemView.findViewById(R.id.tvSeenAt);      // new
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);
            btnDelete     = itemView.findViewById(R.id.btnDeleteReport);
        }
    }
}