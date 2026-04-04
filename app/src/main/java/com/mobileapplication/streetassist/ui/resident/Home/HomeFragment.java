package com.mobileapplication.streetassist.ui.resident.Home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.resident.Reports.submit_report_step1;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int MAX_RECENT = 3; // show only latest 3

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView tvWelcomeName;
    private TextView tvInitialsAvatar;
    private CircleImageView ivAvatar;
    private TextView tvNotificationBadge;
    private TextView tvTotalCount, tvPendingCount, tvResolvedCount;
    private TextView tvSeeAll;
    private LinearLayout layoutRecentReports;
    private LinearLayout layoutRecentEmpty;
    private com.google.android.material.card.MaterialCardView cardSubmitReport;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration reportsListener;
    private ListenerRegistration notifListener;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ── Bind views ────────────────────────────────────────────────────────
        tvWelcomeName       = view.findViewById(R.id.tvWelcomeName);
        ivAvatar            = view.findViewById(R.id.ivAvatar);
        tvInitialsAvatar    = view.findViewById(R.id.tvInitialsAvatar);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        tvTotalCount        = view.findViewById(R.id.tvTotalCount);
        tvPendingCount      = view.findViewById(R.id.tvPendingCount);
        tvResolvedCount     = view.findViewById(R.id.tvResolvedCount);
        tvSeeAll            = view.findViewById(R.id.tvSeeAll);
        layoutRecentReports = view.findViewById(R.id.layoutRecentReports);
        layoutRecentEmpty   = view.findViewById(R.id.layoutRecentEmpty);
        cardSubmitReport    = view.findViewById(R.id.cardSubmitReport);

        // ── Submit report card ────────────────────────────────────────────────
        cardSubmitReport.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), submit_report_step1.class)));

        // ── Notification bell ─────────────────────────────────────────────────
        view.findViewById(R.id.ivNotification).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), NotificationActivity.class)));

        // ── See all → navigates to Reports tab ───────────────────────────────
        tvSeeAll.setOnClickListener(v -> {
            // Trigger bottom nav to switch to Reports tab
            if (getActivity() != null) {
                getActivity().findViewById(R.id.report).performClick();
            }
        });

        loadUserProfile();
        listenToReports();
        listenToNotifications();
    }

    // ── Load user profile from Firestore ──────────────────────────────────────

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || getContext() == null) return;

                    String fullName = doc.getString("fullName");
                    String photoUrl = doc.getString("profilePhotoUrl");

                    // First name only for welcome
                    String firstName = fullName != null
                            ? fullName.trim().split("\\s+")[0] : "User";
                    tvWelcomeName.setText("Welcome, " + firstName + "!");

                    // Avatar
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        tvInitialsAvatar.setVisibility(View.GONE);
                        Glide.with(this)
                                .load(photoUrl)
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.circle_bg_light_blue)
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setVisibility(View.GONE);
                        tvInitialsAvatar.setVisibility(View.VISIBLE);
                        if (fullName != null && !fullName.isEmpty()) {
                            String[] parts  = fullName.trim().split("\\s+");
                            String initials = parts.length >= 2
                                    ? String.valueOf(parts[0].charAt(0))
                                    + String.valueOf(parts[parts.length - 1].charAt(0))
                                    : String.valueOf(parts[0].charAt(0));
                            tvInitialsAvatar.setText(initials.toUpperCase());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load profile.", Toast.LENGTH_SHORT).show());
    }

    // ── Real-time listener: reports → stats + recent list ─────────────────────

    private void listenToReports() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        reportsListener = db.collection("reports")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || getContext() == null) return;

                    int total = 0, pending = 0, resolved = 0;

                    // Sort newest first
                    List<DocumentSnapshot> docs = snapshots.getDocuments();
                    docs.sort((a, b) -> {
                        Object tsA = a.get("timestamp");
                        Object tsB = b.get("timestamp");
                        if (tsA instanceof com.google.firebase.Timestamp
                                && tsB instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) tsB)
                                    .compareTo((com.google.firebase.Timestamp) tsA);
                        }
                        return 0;
                    });

                    // Count stats
                    for (DocumentSnapshot doc : docs) {
                        total++;
                        String status = doc.getString("status");
                        if ("Pending".equalsIgnoreCase(status))  pending++;
                        if ("Resolved".equalsIgnoreCase(status)) resolved++;
                    }

                    tvTotalCount.setText(String.valueOf(total));
                    tvPendingCount.setText(String.valueOf(pending));
                    tvResolvedCount.setText(String.valueOf(resolved));

                    // Build recent reports list (max 3)
                    buildRecentReports(docs);
                });
    }

    // ── Build recent report cards dynamically ─────────────────────────────────

    private void buildRecentReports(List<DocumentSnapshot> docs) {
        if (layoutRecentReports == null || getContext() == null) return;
        layoutRecentReports.removeAllViews();

        if (docs.isEmpty()) {
            layoutRecentEmpty.setVisibility(View.VISIBLE);
            layoutRecentReports.setVisibility(View.GONE);
            return;
        }

        layoutRecentEmpty.setVisibility(View.GONE);
        layoutRecentReports.setVisibility(View.VISIBLE);

        int count = Math.min(docs.size(), MAX_RECENT);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

        for (int i = 0; i < count; i++) {
            DocumentSnapshot doc  = docs.get(i);
            Map<String, Object> d = doc.getData();
            if (d == null) continue;

            View card = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_recent_report, layoutRecentReports, false);

            // Report ID
            TextView tvId = card.findViewById(R.id.tvRecentReportId);
            tvId.setText(getString(d, "reportId", doc.getId()));

            // Description
            TextView tvDesc = card.findViewById(R.id.tvRecentDescription);
            tvDesc.setText(getString(d, "description", "No description"));

            // Location
            TextView tvLoc = card.findViewById(R.id.tvRecentLocation);
            tvLoc.setText(getString(d, "locationAddress", "Location not set"));

            // Timestamp
            TextView tvTs = card.findViewById(R.id.tvRecentTimestamp);
            Object ts = d.get("timestamp");
            if (ts instanceof com.google.firebase.Timestamp) {
                tvTs.setText(sdf.format(((com.google.firebase.Timestamp) ts).toDate()));
            } else {
                tvTs.setText("—");
            }

            // Status badge + left color bar
            String status   = getString(d, "status", "Pending");
            TextView tvStat = card.findViewById(R.id.tvRecentStatus);
            View statusBar  = card.findViewById(R.id.statusBar);
            tvStat.setText(status);
            applyStatusStyle(tvStat, statusBar, status);

            layoutRecentReports.addView(card);
        }
    }

    // ── Real-time notification badge listener ─────────────────────────────────

    private void listenToNotifications() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        notifListener = db.collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || getContext() == null) return;

                    int unread = snapshots.size();
                    if (unread > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    // ── Status styling ─────────────────────────────────────────────────────────

    private void applyStatusStyle(TextView badge, View bar, String status) {
        switch (status) {
            case "Pending":
                badge.setBackgroundResource(R.drawable.badge_pending);
                badge.setTextColor(Color.parseColor("#BA7517"));
                bar.setBackgroundColor(Color.parseColor("#FFC107"));
                break;
            case "Verified":
                badge.setBackgroundResource(R.drawable.badge_verified);
                badge.setTextColor(Color.parseColor("#0F6E56"));
                bar.setBackgroundColor(Color.parseColor("#1D9E75"));
                break;
            case "In Progress":
                badge.setBackgroundResource(R.drawable.badge_in_progress);
                badge.setTextColor(Color.parseColor("#185FA5"));
                bar.setBackgroundColor(Color.parseColor("#4169E1"));
                break;
            case "Resolved":
                badge.setBackgroundResource(R.drawable.badge_resolved);
                badge.setTextColor(Color.parseColor("#3B6D11"));
                bar.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            default:
                badge.setBackgroundResource(R.drawable.badge_pending);
                badge.setTextColor(Color.parseColor("#BA7517"));
                bar.setBackgroundColor(Color.parseColor("#FFC107"));
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val != null && !val.toString().isEmpty() ? val.toString() : fallback;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reportsListener != null) reportsListener.remove();
        if (notifListener  != null) notifListener.remove();
    }
}