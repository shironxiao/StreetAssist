package com.mobileapplication.streetassist.admin;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.mobileapplication.streetassist.R;

public class AdminNotificationBadgeHelper {
    private static final String TAG = "NotifBadgeHelper";
    
    private final Activity activity;
    private View btnNotifications;
    private TextView tvNotificationBadge;
    private ListenerRegistration listenerRegistration;

    public AdminNotificationBadgeHelper(Activity activity) {
        this.activity = activity;
        initViews();
    }

    private void initViews() {
        btnNotifications = activity.findViewById(R.id.btnNotifications);
        tvNotificationBadge = activity.findViewById(R.id.tvNotificationBadge);

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                activity.startActivity(new Intent(activity, AdminNotificationActivity.class));
            });
        }
    }

    public void startListening() {
        if (listenerRegistration != null) {
            return;
        }

        if (tvNotificationBadge == null) {
            return;
        }

        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("admin_notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Notification listener failed: " + error.getMessage());
                        return;
                    }
                    if (value != null && !activity.isFinishing()) {
                        int unread = value.size();
                        if (unread > 0) {
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                            tvNotificationBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
