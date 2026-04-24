package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.RecentReportAdapter;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminDashboard";
    private DrawerLayout drawerLayout;
    private RecyclerView rvReports;
    private com.mobileapplication.streetassist.admin.RecentReportAdapter adapter;
    private List<Map<String, Object>> reportList = new ArrayList<>();
    private FirebaseFirestore db;

    private TextView tvCountTotal, tvCountPending, tvCountInProgress, tvCountResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_dashboard);

        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_dashboard);

        // Fix Sidebar Obscuration
        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Log.d(TAG, "Menu button clicked");
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                Log.e(TAG, "DrawerLayout is null!");
            }
        });

        // Connect "View All" to Reports Management
        findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminReportsActivity.class));
        });

        tvCountTotal = findViewById(R.id.tvCountTotal);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountResolved = findViewById(R.id.tvCountResolved);

        rvReports = findViewById(R.id.rvAdminReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecentReportAdapter(this, reportList);
        rvReports.setAdapter(adapter);

        fetchReports();
    }

    private void fetchReports() {
        // Single listener for all report data to avoid nested listeners and ANRs
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        int total = value.size();
                        int pending = 0, inProgress = 0, resolved = 0;
                        List<Map<String, Object>> allReports = new ArrayList<>();

                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("documentId", doc.getId());
                                allReports.add(data);

                                String status = String.valueOf(data.get("status")).toLowerCase();
                                switch (status) {
                                    case "pending": pending++; break;
                                    case "in progress": inProgress++; break;
                                    case "resolved": resolved++; break;
                                }
                            }
                        }

                        // Update Stats
                        tvCountTotal.setText(String.valueOf(total));
                        tvCountPending.setText(String.valueOf(pending));
                        tvCountInProgress.setText(String.valueOf(inProgress));
                        tvCountResolved.setText(String.valueOf(resolved));

                        // Update Recent Reports List (Top 5)
                        if (!allReports.isEmpty()) {
                            List<Map<String, Object>> recentReports = allReports.subList(0, Math.min(allReports.size(), 5));
                            adapter.updateList(recentReports);
                        } else {
                            adapter.updateList(new ArrayList<>());
                        }
                    }
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already here
        } else if (id == R.id.nav_all_reports) {
            startActivity(new Intent(this, AdminReportsActivity.class));
            finish();
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminAnnouncementsActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            logout();

        } else {
            Toast.makeText(this, "Unknown navigation item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, IntroductionUserLevel.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}