package com.mobileapplication.streetassist.admin;

import android.content.Context;
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
import java.util.Map;

public class RecentReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Context context;
    private List<Map<String, Object>> reportList;
    private OnHeaderActionListener headerListener;

    public interface OnHeaderActionListener {
        void onSearch(String query);
        void onFilterClick();
        void onExportClick();
    }

    public RecentReportAdapter(Context context, List<Map<String, Object>> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    public void setHeaderListener(OnHeaderActionListener listener) {
        this.headerListener = listener;
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.reportList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.header_admin_reports, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_recent_report, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.btnExport.setOnClickListener(v -> {
                if (headerListener != null) headerListener.onExportClick();
            });
            h.btnFilter.setOnClickListener(v -> {
                if (headerListener != null) headerListener.onFilterClick();
            });
            h.etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (headerListener != null) headerListener.onSearch(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        } else if (holder instanceof ViewHolder) {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION || adapterPos <= 0) return;

            Map<String, Object> report = reportList.get(adapterPos - 1);
            ViewHolder itemHolder = (ViewHolder) holder;

            Object reportIdObj = report.get("reportId");
            String id = reportIdObj != null ? String.valueOf(reportIdObj) : null;
            itemHolder.tvId.setText("RPT-" + (id != null ? id : "—"));

            Object descObj = report.get("description");
            itemHolder.tvDescription.setText(descObj != null ? String.valueOf(descObj) : "No description");

            Object locObj = report.get("locationAddress");
            itemHolder.tvLocation.setText(locObj != null ? String.valueOf(locObj) : "Unknown location");

            Object statusObj = report.get("status");
            String status = statusObj != null ? String.valueOf(statusObj) : "Pending";
            itemHolder.tvStatus.setText(status);

            // Format timestamp
            Object ts = report.get("timestamp");
            if (ts instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) ts).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());
                itemHolder.tvTimestamp.setText(sdf.format(date));
            } else {
                itemHolder.tvTimestamp.setText("—");
            }

            // Apply status styles
            applyStatusStyle(itemHolder, status);

            itemHolder.itemView.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION && currentPos > 0) {
                    Map<String, Object> currentReport = reportList.get(currentPos - 1);
                    if (context instanceof com.mobileapplication.streetassist.admin.AdminReportsActivity) {
                        ((com.mobileapplication.streetassist.admin.AdminReportsActivity) context).showReportDetails(currentReport);
                    }
                }
            });
        }
    }

    private void applyStatusStyle(ViewHolder holder, String status) {
        if (status == null || status.equalsIgnoreCase("null")) {
            status = "Pending";
        }

        switch (status.toLowerCase()) {
            case "pending":
                holder.statusBar.setBackgroundColor(Color.parseColor("#FFC107"));
                holder.tvStatus.setBackgroundResource(R.drawable.badge_pending);
                holder.tvStatus.setTextColor(Color.parseColor("#BA7517"));
                break;
            case "in progress":
                holder.statusBar.setBackgroundColor(Color.parseColor("#185FA5"));
                holder.tvStatus.setBackgroundResource(R.drawable.badge_in_progress);
                holder.tvStatus.setTextColor(Color.parseColor("#185FA5"));
                break;
            case "resolved":
                holder.statusBar.setBackgroundColor(Color.parseColor("#3B6D11"));
                holder.tvStatus.setBackgroundResource(R.drawable.badge_resolved);
                holder.tvStatus.setTextColor(Color.parseColor("#3B6D11"));
                break;
            default:
                holder.statusBar.setBackgroundColor(Color.parseColor("#888888"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size() + 1; // +1 for header
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        View btnExport, btnFilter;
        android.widget.EditText etSearch;
        TextView tvShowingResults;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            btnExport = itemView.findViewById(R.id.btnExport);
            btnFilter = itemView.findViewById(R.id.btnFilterStatus);
            etSearch = itemView.findViewById(R.id.etSearch);
            tvShowingResults = itemView.findViewById(R.id.tvShowingResults);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View statusBar;
        TextView tvId, tvDescription, tvLocation, tvTimestamp, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusBar = itemView.findViewById(R.id.statusBar);
            tvId = itemView.findViewById(R.id.tvRecentReportId);
            tvDescription = itemView.findViewById(R.id.tvRecentDescription);
            tvLocation = itemView.findViewById(R.id.tvRecentLocation);
            tvTimestamp = itemView.findViewById(R.id.tvRecentTimestamp);
            tvStatus = itemView.findViewById(R.id.tvRecentStatus);
        }
    }
}
