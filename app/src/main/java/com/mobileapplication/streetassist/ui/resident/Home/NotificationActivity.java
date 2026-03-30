package com.mobileapplication.streetassist.ui.resident.Home;

import android.os.Bundle;
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

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        btnBack.setOnClickListener(v -> finish());

        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    notificationList.clear();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String  title     = doc.getString("title");
                        String  message   = doc.getString("message");
                        Boolean isRead    = doc.getBoolean("isRead");
                        Date    createdAt = doc.getDate("createdAt");

                        notificationList.add(new NotificationItem(
                                doc.getId(),
                                title   != null ? title   : "Notification",
                                message != null ? message : "",
                                isRead  != null && isRead,
                                createdAt));

                        // Mark as read
                        if (Boolean.FALSE.equals(isRead)) {
                            doc.getReference().update("isRead", true);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(
                            notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(
                            notificationList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // =========================================================================
    //  Data model
    // =========================================================================

    public static class NotificationItem {
        public String id, title, message;
        public boolean isRead;
        public Date createdAt;

        public NotificationItem(String id, String title,
                                String message, boolean isRead,
                                Date createdAt) {
            this.id        = id;
            this.title     = title;
            this.message   = message;
            this.isRead    = isRead;
            this.createdAt = createdAt;
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
            h.tvTime.setText(item.createdAt != null
                    ? sdf.format(item.createdAt) : "");

            // Unread = slightly highlighted background
            h.itemView.setBackgroundColor(item.isRead
                    ? 0xFFFFFFFF
                    : 0xFFEDF2FF);

            // Unread dot visibility
            h.dotUnread.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;
            View dotUnread;

            VH(View v) {
                super(v);
                tvTitle   = v.findViewById(R.id.tvNotifTitle);
                tvMessage = v.findViewById(R.id.tvNotifMessage);
                tvTime    = v.findViewById(R.id.tvNotifTime);
                dotUnread = v.findViewById(R.id.dotUnread);
            }
        }
    }
}