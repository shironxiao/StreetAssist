package com.mobileapplication.streetassist.ui.resident.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView tvWelcomeName;
    private TextView tvInitialsAvatar;
    private CircleImageView ivAvatar;
    private TextView tvNotificationBadge;
    private TextView tvTotalCount, tvPendingCount, tvResolvedCount;
    private com.google.android.material.card.MaterialCardView cardSubmitReport;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        // Bind views
        tvWelcomeName        = view.findViewById(R.id.tvWelcomeName);
        ivAvatar             = view.findViewById(R.id.ivAvatar);
        tvInitialsAvatar     = view.findViewById(R.id.tvInitialsAvatar);
        tvNotificationBadge  = view.findViewById(R.id.tvNotificationBadge);
        tvTotalCount         = view.findViewById(R.id.tvTotalCount);
        tvPendingCount       = view.findViewById(R.id.tvPendingCount);
        tvResolvedCount      = view.findViewById(R.id.tvResolvedCount);
        cardSubmitReport     = view.findViewById(R.id.cardSubmitReport);

        // Notification bell click
        view.findViewById(R.id.ivNotification).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationActivity.class);
            startActivity(intent);
        });

        // Submit report card click
        cardSubmitReport.setOnClickListener(v -> {
            // TODO: navigate to your submit report screen
            Toast.makeText(getContext(), "Open Submit Report", Toast.LENGTH_SHORT).show();
        });

        loadUserProfile();

        // Inside HomeFragment.java - onViewCreated method
        cardSubmitReport.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.mobileapplication.streetassist.ui.resident.Reports.submit_report_step1.class);
            startActivity(intent);
        });
    }

    // =========================================================================
    //  Firestore – load user profile + report stats
    // =========================================================================

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // ── Load user document ───────────────────────────────────────────────
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || getContext() == null) return;

                    String fullName = doc.getString("fullName");
                    String photoUrl = doc.getString("profilePhotoUrl");

                    // Welcome text — first name only
                    String firstName = fullName != null
                            ? fullName.trim().split("\\s+")[0]
                            : "User";
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
                        // Initials fallback
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
                                "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());

        // ── Load report stats for this user ──────────────────────────────────
        db.collection("reports")
                .whereEqualTo("reporterId", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int total    = 0;
                    int pending  = 0;
                    int resolved = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        total++;
                        String status = doc.getString("status");
                        if ("pending".equalsIgnoreCase(status))  pending++;
                        if ("resolved".equalsIgnoreCase(status)) resolved++;
                    }

                    tvTotalCount.setText(String.valueOf(total));
                    tvPendingCount.setText(String.valueOf(pending));
                    tvResolvedCount.setText(String.valueOf(resolved));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load stats: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());

        // ── Load unread notification count ───────────────────────────────────
        db.collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int unread = snapshots.size();
                    if (unread > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                });
    }
}