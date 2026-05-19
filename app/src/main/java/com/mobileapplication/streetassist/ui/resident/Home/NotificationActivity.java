package com.mobileapplication.streetassist.ui.resident.Home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private TextView btnClearAll;
    private TextView btnMarkRead;
    private View cardMarkRead;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private com.google.firebase.firestore.ListenerRegistration notificationListenerRegistration;

    private final List<NotificationItem> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty         = findViewById(R.id.tvEmpty);
        btnBack         = findViewById(R.id.btnBack);
        btnClearAll     = findViewById(R.id.btnClearAll);
        btnMarkRead     = findViewById(R.id.btnMarkRead);
        cardMarkRead    = findViewById(R.id.cardMarkRead);

        btnBack.setOnClickListener(v -> finish());
        btnClearAll.setOnClickListener(v -> clearAllNotifications());
        btnMarkRead.setOnClickListener(v -> markAllAsRead());

        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
        }
    }

    private void loadNotifications() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        // 1. Fetch all existing report IDs for this user to ensure sync
        db.collection("reports")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(reportSnapshots -> {
                    java.util.Set<String> existingReportIds = new java.util.HashSet<>();
                    for (QueryDocumentSnapshot reportDoc : reportSnapshots) {
                        // Use the reportId field if it exists, otherwise use document ID
                        String rId = reportDoc.getString("reportId");
                        if (rId == null) rId = reportDoc.getId();
                        existingReportIds.add(rId);
                    }

                    // 2. Load notifications and filter dynamically in real-time
                    if (notificationListenerRegistration != null) {
                        notificationListenerRegistration.remove();
                    }

                    notificationListenerRegistration = db.collection("notifications")
                            .whereEqualTo("userId", uid)
                            .addSnapshotListener((snapshots, error) -> {
                                if (error != null) {
                                    Toast.makeText(this, "Sync error: " + error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (snapshots != null) {
                                    notificationList.clear();
                                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                                    boolean needsCleanup = false;

                                    for (QueryDocumentSnapshot doc : snapshots) {
                                        String reportId = doc.getString("reportId");

                                        // If notification is linked to a report that no longer exists, delete it from DB
                                        if (reportId != null && !existingReportIds.contains(reportId)) {
                                            batch.delete(doc.getReference());
                                            needsCleanup = true;
                                            continue;
                                        }

                                        String title = doc.getString("title");
                                        String message = doc.getString("message");
                                        Boolean isRead = doc.getBoolean("isRead");
                                        Date createdAt = doc.getDate("createdAt");
                                        String status = doc.getString("status");

                                        notificationList.add(new NotificationItem(
                                                doc.getId(),
                                                title != null ? title : "Notification",
                                                message != null ? message : "",
                                                isRead != null && isRead,
                                                createdAt,
                                                status,
                                                reportId));
                                    }

                                    if (needsCleanup) {
                                        batch.commit();
                                    }

                                    // Manual sort: newest first
                                    notificationList.sort((a, b) -> {
                                        if (a.createdAt == null || b.createdAt == null) return 0;
                                        return b.createdAt.compareTo(a.createdAt);
                                    });

                                    adapter.notifyDataSetChanged();
                                    tvEmpty.setVisibility(
                                            notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                                    rvNotifications.setVisibility(
                                            notificationList.isEmpty() ? View.GONE : View.VISIBLE);
                                    btnClearAll.setVisibility(
                                            notificationList.isEmpty() ? View.GONE : View.VISIBLE);

                                    int unreadCount = 0;
                                    for (NotificationItem item : notificationList) {
                                        if (!item.isRead) {
                                            unreadCount++;
                                        }
                                    }
                                    updateMarkAllAsReadUI(unreadCount);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to sync reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMarkAllAsReadUI(int unreadCount) {
        btnMarkRead.setText("Mark All as Read (" + unreadCount + ")");
        if (unreadCount > 0) {
            btnMarkRead.setEnabled(true);
            btnMarkRead.setTextColor(Color.parseColor("#1B2559"));
            cardMarkRead.setAlpha(1.0f);
            cardMarkRead.setVisibility(View.VISIBLE);
        } else {
            btnMarkRead.setEnabled(false);
            btnMarkRead.setTextColor(Color.parseColor("#888888"));
            cardMarkRead.setAlpha(0.6f);
            cardMarkRead.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) return;

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        for (NotificationItem item : notificationList) {
                            item.isRead = true;
                        }
                        adapter.notifyDataSetChanged();
                        updateMarkAllAsReadUI(0);
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        batch.update(doc.getReference(), "isRead", true);
                    }

                    batch.commit().addOnSuccessListener(unused -> {
                        for (NotificationItem item : notificationList) {
                            item.isRead = true;
                        }
                        adapter.notifyDataSetChanged();
                        updateMarkAllAsReadUI(0);
                        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to mark as read: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching unread notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearAllNotifications() {
        if (notificationList.isEmpty()) return;

        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Clear Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    db.collection("notifications")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener(snapshots -> {
                                if (snapshots.isEmpty()) return;

                                com.google.firebase.firestore.WriteBatch batch = db.batch();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                                    batch.delete(doc.getReference());
                                }

                                batch.commit().addOnSuccessListener(unused -> {
                                    notificationList.clear();
                                    adapter.notifyDataSetChanged();
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    rvNotifications.setVisibility(View.GONE);
                                    btnClearAll.setVisibility(View.GONE);
                                    btnMarkRead.setVisibility(View.GONE);
                                    Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to clear: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

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
    //  RecyclerView Adapter
    // =========================================================================

    public static class NotificationAdapter
            extends RecyclerView.Adapter<NotificationAdapter.VH> {

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

            h.itemView.setOnClickListener(v -> {
                if (!item.isRead) {
                    FirebaseFirestore.getInstance().collection("notifications")
                            .document(item.id).update("isRead", true);
                    item.isRead = true;
                }

                android.content.Intent intent = new android.content.Intent(v.getContext(), com.mobileapplication.streetassist.ui.resident.ResidentMainActivity.class);
                intent.putExtra("nav_to_report", true);
                intent.putExtra("filter_status", item.status);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                v.getContext().startActivity(intent);
                if (v.getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) v.getContext()).finish();
                }
            });
        }

        private void applyStatusChipStyle(TextView chip, View dot, String status) {
            switch (status) {
                case "Pending":
                    chip.setBackgroundResource(R.drawable.badge_pending);
                    chip.setTextColor(Color.parseColor("#BA7517"));
                    dot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#FFC107")));
                    break;
                case "Verified":
                    chip.setBackgroundResource(R.drawable.badge_verified);
                    chip.setTextColor(Color.parseColor("#0F6E56"));
                    dot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#1D9E75")));
                    break;
                case "In Progress":
                    chip.setBackgroundResource(R.drawable.badge_in_progress);
                    chip.setTextColor(Color.parseColor("#185FA5"));
                    dot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#4169E1")));
                    break;
                case "Resolved":
                    chip.setBackgroundResource(R.drawable.badge_resolved);
                    chip.setTextColor(Color.parseColor("#3B6D11"));
                    dot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#4CAF50")));
                    break;
                default:
                    chip.setBackgroundResource(R.drawable.badge_pending);
                    chip.setTextColor(Color.parseColor("#BA7517"));
                    dot.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#FFC107")));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

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
}