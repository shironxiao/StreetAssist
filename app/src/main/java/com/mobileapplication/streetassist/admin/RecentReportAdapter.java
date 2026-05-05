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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecentReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final Context context;
    private List<Map<String, Object>> reportList;
    private OnHeaderActionListener headerListener;
    
    private final Set<String> selectedReportIds = new HashSet<>();
    private boolean isSelectionMode = false;

    public interface OnHeaderActionListener {
        void onSearch(String query);
        void onFilterClick();
        void onExportClick();
        void onDeleteSelected(Set<String> selectedIds);
        void onRestoreSelected(Set<String> selectedIds);
        void onCancelSelection();
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
        if (context instanceof AdminDashboardActivity) {
            return TYPE_ITEM;
        }
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.header_admin_reports, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_admin_report, parent, false);
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

            if (isSelectionMode) {
                h.btnCancelSelection.setVisibility(View.VISIBLE);
                h.btnCancelSelection.setOnClickListener(v -> {
                    if (headerListener != null) headerListener.onCancelSelection();
                });

                if (!selectedReportIds.isEmpty()) {
                    h.btnDeleteSelected.setVisibility(View.VISIBLE);
                    ((com.google.android.material.button.MaterialButton) h.btnDeleteSelected).setText(
                            context.getString(R.string.delete_selected, selectedReportIds.size()));
                    h.btnDeleteSelected.setOnClickListener(v -> {
                        if (headerListener != null) headerListener.onDeleteSelected(new HashSet<>(selectedReportIds));
                    });

                    if (context instanceof AdminTrashActivity) {
                        h.btnRestoreSelected.setVisibility(View.VISIBLE);
                        h.btnRestoreSelected.setOnClickListener(v -> {
                            if (headerListener != null) headerListener.onRestoreSelected(new HashSet<>(selectedReportIds));
                        });
                    } else {
                        h.btnRestoreSelected.setVisibility(View.GONE);
                    }
                } else {
                    h.btnDeleteSelected.setVisibility(View.GONE);
                    h.btnRestoreSelected.setVisibility(View.GONE);
                }
            } else {
                h.btnCancelSelection.setVisibility(View.GONE);
                h.btnDeleteSelected.setVisibility(View.GONE);
                h.btnRestoreSelected.setVisibility(View.GONE);
            }

            // Fix: Remove previous listener to prevent accumulation and infinite loops
            if (h.etSearch.getTag() instanceof android.text.TextWatcher) {
                h.etSearch.removeTextChangedListener((android.text.TextWatcher) h.etSearch.getTag());
            }

            android.text.TextWatcher watcher = new android.text.TextWatcher() {
                private String lastText = "";
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String newText = s.toString();
                    if (!newText.equals(lastText)) {
                        lastText = newText;
                        if (headerListener != null) headerListener.onSearch(newText);
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            };
            h.etSearch.addTextChangedListener(watcher);
            h.etSearch.setTag(watcher);
        } else if (holder instanceof ViewHolder) {
            int adapterPos = holder.getAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;

            int dataIndex = (context instanceof AdminDashboardActivity) ? adapterPos : adapterPos - 1;
            if (dataIndex < 0 || dataIndex >= reportList.size()) return;

            Map<String, Object> report = reportList.get(dataIndex);
            ViewHolder itemHolder = (ViewHolder) holder;

            Object docIdObj = report.get("documentId");
            String docId = docIdObj != null ? String.valueOf(docIdObj) : null;

            Object reportIdObj = report.get("reportId");
            String id = reportIdObj != null ? String.valueOf(reportIdObj) : null;
            String displayId = (id != null) ? (id.startsWith("RPT-") ? id : "RPT-" + id) : "—";
            itemHolder.tvId.setText(displayId);

            Object descObj = report.get("description");
            itemHolder.tvDescription.setText(descObj != null ? String.valueOf(descObj) : "No description");

            Object locObj = report.get("locationAddress");
            itemHolder.tvLocation.setText(locObj != null ? String.valueOf(locObj) : "Unknown location");

            Object statusObj = report.get("status");
            String status = statusObj != null ? String.valueOf(statusObj) : "Pending";
            itemHolder.tvStatus.setText(status);
            applyStatusStyle(itemHolder, status);

            // Handle Read Status and NEW Tag using adminSeenAt
            Object adminSeenAt = report.get("adminSeenAt");
            if (adminSeenAt == null) {
                itemHolder.viewReadStatus.setVisibility(View.VISIBLE);
                itemHolder.tvNewTag.setVisibility(View.VISIBLE);
            } else {
                itemHolder.viewReadStatus.setVisibility(View.GONE);
                itemHolder.tvNewTag.setVisibility(View.GONE);
            }

            // Format timestamp
            Object ts = report.get("timestamp");
            if (ts instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) ts).toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());
                itemHolder.tvTimestamp.setText(sdf.format(date));
            } else {
                itemHolder.tvTimestamp.setText("—");
            }

            // Selection Logic
            if (isSelectionMode && !(context instanceof AdminDashboardActivity)) {
                itemHolder.cbSelect.setVisibility(View.VISIBLE);
                itemHolder.cbSelect.setChecked(selectedReportIds.contains(docId));
            } else {
                itemHolder.cbSelect.setVisibility(View.GONE);
            }

            itemHolder.cbSelect.setOnClickListener(v -> {
                if (docId != null) {
                    if (itemHolder.cbSelect.isChecked()) {
                        selectedReportIds.add(docId);
                    } else {
                        selectedReportIds.remove(docId);
                    }
                    updateDeleteButtonState();
                }
            });

            itemHolder.itemView.setOnClickListener(v -> {
                if (isSelectionMode && !(context instanceof AdminDashboardActivity)) {
                    itemHolder.cbSelect.performClick();
                    return;
                }
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    int clickedIndex = (context instanceof AdminDashboardActivity) ? currentPos : currentPos - 1;
                    if (clickedIndex >= 0 && clickedIndex < reportList.size()) {
                        Map<String, Object> currentReport = reportList.get(clickedIndex);
                        if (context instanceof com.mobileapplication.streetassist.admin.AdminReportsActivity) {
                            ((com.mobileapplication.streetassist.admin.AdminReportsActivity) context).showReportDetails(currentReport);
                        } else if (context instanceof com.mobileapplication.streetassist.admin.AdminTrashActivity) {
                            ((com.mobileapplication.streetassist.admin.AdminTrashActivity) context).showReportDetails(currentReport);
                        } else if (context instanceof AdminDashboardActivity) {
                            String docIdToPass = (String) currentReport.get("documentId");
                            android.content.Intent intent = new android.content.Intent(context, com.mobileapplication.streetassist.admin.AdminReportsActivity.class);
                            intent.putExtra("reportId", docIdToPass);
                            context.startActivity(intent);
                        }
                    }
                }
            });

            itemHolder.itemView.setOnLongClickListener(v -> {
                if (context instanceof AdminDashboardActivity) return false;
                if (!isSelectionMode) {
                    isSelectionMode = true;
                    if (docId != null) selectedReportIds.add(docId);
                    notifyDataSetChanged();
                    updateDeleteButtonState();
                    return true;
                }
                return false;
            });
        }
    }

    private void updateDeleteButtonState() {
        // This will be handled via the header update
        notifyItemChanged(0);
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedReportIds.clear();
        notifyDataSetChanged();
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
        if (context instanceof AdminDashboardActivity) {
            return reportList.size();
        }
        return reportList.size() + 1; // +1 for header
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        View btnExport, btnFilter, btnDeleteSelected, btnCancelSelection, btnRestoreSelected;
        android.widget.EditText etSearch;
        TextView tvShowingResults;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            btnExport = itemView.findViewById(R.id.btnExport);
            btnFilter = itemView.findViewById(R.id.btnFilterStatus);
            btnDeleteSelected = itemView.findViewById(R.id.btnDeleteSelected);
            btnCancelSelection = itemView.findViewById(R.id.btnCancelSelection);
            btnRestoreSelected = itemView.findViewById(R.id.btnRestoreSelected);
            etSearch = itemView.findViewById(R.id.etSearch);
            tvShowingResults = itemView.findViewById(R.id.tvShowingResults);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View statusBar, viewReadStatus;
        TextView tvId, tvDescription, tvLocation, tvTimestamp, tvStatus, tvNewTag;
        android.widget.CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusBar = itemView.findViewById(R.id.statusBar);
            viewReadStatus = itemView.findViewById(R.id.viewReadStatus);
            tvId = itemView.findViewById(R.id.tvRecentReportId);
            tvDescription = itemView.findViewById(R.id.tvRecentDescription);
            tvLocation = itemView.findViewById(R.id.tvRecentLocation);
            tvTimestamp = itemView.findViewById(R.id.tvRecentTimestamp);
            tvStatus = itemView.findViewById(R.id.tvRecentStatus);
            cbSelect = itemView.findViewById(R.id.cbSelectReport);
            tvNewTag = itemView.findViewById(R.id.tvNewTag);
        }
    }
}
