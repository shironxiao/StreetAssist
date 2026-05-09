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

import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminNotificationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "AdminNotification";

    private DrawerLayout drawerLayout;
    private RecyclerView rvNotifications;
    private TextView tvEmpty;
    private ImageButton btnMenu;

    private FirebaseFirestore db;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private NotificationAdapter adapter;

    private com.google.firebase.firestore.ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        // No specific menu item for notifications in the drawer yet, 
        // but we can uncheck others or just leave it.
        
        // Fix Sidebar Obscuration
        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnMenu = findViewById(R.id.btnMenu);

        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    startActivity(new Intent(AdminNotificationActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }
        });

        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void startNotificationListener() {
        notificationListener = db.collection("admin_notifications")
                .limit(100) // Increase limit to fetch recent ones even without order
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Notification listen failed: " + e.getMessage(), e);
                        if (e.getMessage().contains("INDEX")) {
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

                    // Sort locally to handle documents missing the createdAt field
                    notificationList.sort((n1, n2) -> {
                        if (n1.createdAt == null && n2.createdAt == null) return 0;
                        if (n1.createdAt == null) return 1;
                        if (n2.createdAt == null) return -1;
                        return n2.createdAt.compareTo(n1.createdAt); // Descending
                    });

                    adapter.notifyDataSetChanged();
                    boolean isEmpty = notificationList.isEmpty();
                    tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            NotificationItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);
            holder.tvTime.setText(item.createdAt != null ? sdf.format(item.createdAt) : "");

            holder.itemView.setBackgroundColor(item.isRead ? 0xFFFFFFFF : 0xFFEDF2FF);
            holder.dotUnread.setVisibility(item.isRead ? View.GONE : View.VISIBLE);

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
            View dotUnread;

            VH(View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tvNotifTitle);
                tvMessage = view.findViewById(R.id.tvNotifMessage);
                tvTime = view.findViewById(R.id.tvNotifTime);
                dotUnread = view.findViewById(R.id.dotUnread);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (id == R.id.nav_all_reports) {
            startActivity(new Intent(this, AdminReportsActivity.class));
            finish();
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, AdminAnnouncementsActivity.class));
            finish();
        } else if (id == R.id.nav_trash) {
            startActivity(new Intent(this, AdminTrashActivity.class));
            finish();
        } else if (id == R.id.nav_notifications) {
            // Already here
        } else if (id == R.id.nav_logout) {
            logout();
        }

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void logout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
