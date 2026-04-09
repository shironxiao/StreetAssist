package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final Context context;
    private List<Map<String, Object>> reportList;
    private final FirebaseFirestore db;

    public ReportAdapter(Context context, List<Map<String, Object>> reportList) {
        this.context    = context;
        this.reportList = reportList;
        this.db         = FirebaseFirestore.getInstance();
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

        // ── Bind card fields ──────────────────────────────────────────────────
        String reportId = getString(report, "reportId", "—");
        holder.tvReportId.setText(reportId);

        String status = getString(report, "status", "Pending");
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        // Check if IDs exist in item_report_card.xml
        if (holder.tvDescription != null) holder.tvDescription.setText(getString(report, "description", "No description."));
        if (holder.tvAge != null) holder.tvAge.setText("Age: " + getString(report, "approximateAge", "—"));
        if (holder.tvSex != null) holder.tvSex.setText("Sex: " + getString(report, "sex", "—"));
        if (holder.tvLocation != null) holder.tvLocation.setText(getString(report, "locationAddress", "Location not set"));

        if (holder.tvAssistance != null) {
            String assistance = getString(report, "assistanceDescription", "—");
            holder.tvAssistance.setText("Assistance: " +
                    (assistance.length() > 40 ? assistance.substring(0, 40) + "…" : assistance));
        }

        if (holder.tvTimestamp != null) {
            String formattedTime = formatTimestamp(report.get("timestamp"));
            holder.tvTimestamp.setText("Submitted: " + formattedTime);
        }

        // ── View Details click → BottomSheet ──────────────────────────────────
        if (holder.tvViewDetails != null) {
            holder.tvViewDetails.setOnClickListener(v -> showDetailsBottomSheet(report));
        }
        holder.itemView.setOnClickListener(v -> showDetailsBottomSheet(report));
    }

    @Override
    public int getItemCount() {
        return reportList == null ? 0 : reportList.size();
    }

    // ── Bottom Sheet Dialog ────────────────────────────────────────────────────

    private void showDetailsBottomSheet(Map<String, Object> report) {
        BottomSheetDialog dialog = new BottomSheetDialog(context,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_report_details, null);
        dialog.setContentView(sheetView);

        // ── Populate all fields ───────────────────────────────────────────────

        // Image
        ImageView ivDetail = sheetView.findViewById(R.id.dialogImage);
        if (ivDetail != null) {
            String imageUrl = getString(report, "imageUrl", "");
            if (!imageUrl.isEmpty()) {
                Glide.with(context).load(imageUrl).placeholder(R.drawable.logo).into(ivDetail);
            }
        }

        // Report ID & Timestamp
        setText(sheetView, R.id.dialogReportId, getString(report, "reportId", "—"));
        setText(sheetView, R.id.dialogTimestamp, formatTimestamp(report.get("timestamp")));

        // Status
        String status = getString(report, "status", "Pending");
        TextView tvStatus = sheetView.findViewById(R.id.dialogStatus);
        if (tvStatus != null) {
            tvStatus.setText(status);
            applyStatusStyle(tvStatus, status);
        }

        // Individual info
        setText(sheetView, R.id.dialogAge, getString(report, "approximateAge", "—"));
        setText(sheetView, R.id.dialogSex, getString(report, "sex", "—"));
        setText(sheetView, R.id.dialogDescription, getString(report, "description", "No description provided."));
        setText(sheetView, R.id.dialogReporterName, getString(report, "reporterName", "Anonymous"));

        // Location
        setText(sheetView, R.id.dialogLocation, getString(report, "locationAddress", "No address available."));

        // Coordinates
        Object lat = report.get("latitude");
        Object lng = report.get("longitude");
        setText(sheetView, R.id.dialogLatitude,
                lat != null ? String.format(Locale.getDefault(), "Lat: %.5f", toDouble(lat)) : "Lat: —");
        setText(sheetView, R.id.dialogLongitude,
                lng != null ? String.format(Locale.getDefault(), "Lng: %.5f", toDouble(lng)) : "Lng: —");

        // Assistance
        setText(sheetView, R.id.dialogAssistance, getString(report, "assistanceDescription", "Not specified."));

        // Reporter Contact
        setText(sheetView, R.id.dialogContact, getString(report, "contactNumber", "Not provided"));

        // ── Status Update Buttons (ADMIN ONLY) ──────────────────────────────
        String docId = (String) report.get("documentId");
        if (docId != null) {
            setupStatusButton(sheetView, R.id.btnStatusPending, docId, "Pending", dialog);
            setupStatusButton(sheetView, R.id.btnStatusInProgress, docId, "In Progress", dialog);
            setupStatusButton(sheetView, R.id.btnStatusVerified, docId, "Verified", dialog);
            setupStatusButton(sheetView, R.id.btnStatusResolved, docId, "Resolved", dialog);
        }

        // Close button
        View btnClose = sheetView.findViewById(R.id.dialogBtnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupStatusButton(View parent, int btnId, String docId, String newStatus, BottomSheetDialog dialog) {
        View btn = parent.findViewById(btnId);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                db.collection("reports").document(docId)
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                        });
            });
        }
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

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportId, tvStatus, tvDescription, tvAge,
                tvSex, tvLocation, tvAssistance, tvTimestamp, tvViewDetails, tvReportTitle, tvReporterName;

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
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);
            tvReportTitle = itemView.findViewById(R.id.tvReportTitle);
            tvReporterName = itemView.findViewById(R.id.tvReporterName);
        }
    }
}