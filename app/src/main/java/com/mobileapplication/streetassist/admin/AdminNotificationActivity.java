package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminNotificationActivity extends AppCompatActivity {
    private static final String TAG = "AdminNotification";

    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private ImageButton btnBack;
    private TextView btnClearAll;

    private FirebaseFirestore db;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;

    private com.google.firebase.firestore.ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        // Verify Admin Role
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (!"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Access Denied: Admin role required", Toast.LENGTH_LONG).show();
                        logout();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Role verification failed", e);
                    Toast.makeText(this, "Error verifying role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnBack = findViewById(R.id.btnBack);
        btnClearAll = findViewById(R.id.btnClearAll);

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> clearAllNotifications());
        }

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(AdminNotificationActivity.this, AdminDashboardActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // If we didn't call finish() when starting this activity, 
                // we can just finish() this one to return to the previous activity.
                finish();
            }
        });

        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void startNotificationListener() {
        notificationListener = db.collection("admin_notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Notification listen failed: " + e.getMessage(), e);
                        if (e.getMessage() != null && e.getMessage().contains("INDEX")) {
                            Toast.makeText(this, "Notification system needs a Firestore Index. Check logs.", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (snapshots == null) return;

                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        Boolean isRead = doc.getBoolean("isRead");
                        Date createdAt = doc.getDate("createdAt");
                        String type = doc.getString("type");
                        String referenceId = doc.getString("referenceId");

                        notificationList.add(new NotificationItem(
                                doc.getId(),
                                title != null ? title : "Notification",
                                message != null ? message : "",
                                isRead != null && isRead,
                                createdAt,
                                type != null ? type : "",
                                referenceId != null ? referenceId : ""
                        ));
                    }

                    // Local sort for safety (docs with server timestamps might be null initially)
                    notificationList.sort((n1, n2) -> {
                        if (n1.createdAt == null && n2.createdAt == null) return 0;
                        if (n1.createdAt == null) return -1; // Null is newest (just created)
                        if (n2.createdAt == null) return 1;
                        return n2.createdAt.compareTo(n1.createdAt); // Descending
                    });

                    adapter.notifyDataSetChanged();
                    boolean isEmpty = notificationList.isEmpty();
                    tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    if (btnClearAll != null) {
                        btnClearAll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (notificationListener == null) {
            startNotificationListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Listener is already removed in onStop, but keeping for safety if moved
    }

    public static class NotificationItem {
        public String id;
        public String title;
        public String message;
        public boolean isRead;
        public Date createdAt;
        public String type;
        public String referenceId;

        public NotificationItem(String id, String title, String message, boolean isRead, Date createdAt, String type, String referenceId) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.isRead = isRead;
            this.createdAt = createdAt;
            this.type = type;
            this.referenceId = referenceId;
        }
    }

    public static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
        private final List<NotificationItem> items;
        private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

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

        private void deleteNotification(String id, View contextView) {
            new androidx.appcompat.app.AlertDialog.Builder(contextView.getContext())
                    .setTitle("Delete Notification")
                    .setMessage("Are you sure you want to delete this notification?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("admin_notifications").document(id).delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(contextView.getContext(), "Notification deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(contextView.getContext(), "Failed to delete notification", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NotificationItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);
            holder.tvTime.setText(item.createdAt != null ? sdf.format(item.createdAt) : "");

            holder.itemView.setBackgroundColor(item.isRead ? 0xFFFFFFFF : 0xFFEDF2FF);
            holder.dotUnread.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
            
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> deleteNotification(item.id, v));

            holder.itemView.setOnClickListener(v -> {
                if (v.getContext() instanceof AdminNotificationActivity) {
                    AdminNotificationActivity activity = (AdminNotificationActivity) v.getContext();
                    
                    // Mark as read in Firestore if not already
                    if (!item.isRead) {
                        FirebaseFirestore.getInstance().collection("admin_notifications")
                                .document(item.id).update("isRead", true);
                        item.isRead = true;
                        notifyItemChanged(position);
                    }

                    // Navigate based on type
                    Intent intent;
                    if ("new_report".equals(item.type)) {
                        intent = new Intent(activity, AdminReportsActivity.class);
                        intent.putExtra("reportId", item.referenceId);
                        activity.startActivity(intent);
                    } else if ("new_comment".equals(item.type)) {
                        intent = new Intent(activity, AdminAnnouncementsActivity.class);
                        intent.putExtra("announcementId", item.referenceId);
                        activity.startActivity(intent);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvMessage;
            TextView tvTime;
            View dotUnread, btnDelete;

            VH(View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tvNotifTitle);
                tvMessage = view.findViewById(R.id.tvNotifMessage);
                tvTime = view.findViewById(R.id.tvNotifTime);
                dotUnread = view.findViewById(R.id.dotUnread);
                btnDelete = view.findViewById(R.id.btnDeleteNotif);
            }
        }
    }

    private void clearAllNotifications() {
        if (notificationList.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    db.collection("admin_notifications")
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
                                    if (btnClearAll != null) {
                                        btnClearAll.setVisibility(View.GONE);
                                    }
                                    Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to clear: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to fetch notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                    .update("fcmToken", com.google.firebase.firestore.FieldValue.delete());
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, IntroductionUserLevel.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
