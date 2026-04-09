package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;

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
        setText(sheetView, R.id.dialogLocation,    getString(report, "locationAddress", "No address available."));

        Object lat = report.get("latitude");
        Object lng = report.get("longitude");
        setText(sheetView, R.id.dialogLatitude,
                lat != null ? String.format(Locale.getDefault(), "%.5f", toDouble(lat)) : "—");
        setText(sheetView, R.id.dialogLongitude,
                lng != null ? String.format(Locale.getDefault(), "%.5f", toDouble(lng)) : "—");

        setText(sheetView, R.id.dialogAssistance,  getString(report, "assistanceDescription", "Not specified."));

        String contact = getString(report, "contactNumber", "").trim();
        setText(sheetView, R.id.dialogContact, contact.isEmpty() ? "Not provided" : contact);

        // Seen at date/time
        setText(sheetView, R.id.dialogSeenAt,      formatTimestamp(report.get("seenAt")));

        // Submitted timestamp
        setText(sheetView, R.id.dialogTimestamp,   formatTimestamp(report.get("timestamp")));

        MaterialButton btnClose = sheetView.findViewById(R.id.dialogBtnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
                tvSeenAt, tvViewDetails;

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
        }
    }
}