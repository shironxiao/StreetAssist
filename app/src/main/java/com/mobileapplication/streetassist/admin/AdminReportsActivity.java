package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminReportsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminReports";
    private DrawerLayout drawerLayout;
    private RecyclerView rvReports;
    private RecentReportAdapter adapter;
    private List<Map<String, Object>> reportList = new ArrayList<>();
    private List<Map<String, Object>> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentSearchQuery = "";
    private String currentStatusFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_reports);

        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_all_reports);

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

        rvReports = findViewById(R.id.rvAllReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new com.mobileapplication.streetassist.admin.RecentReportAdapter(this, filteredList);
        adapter.setHeaderListener(new com.mobileapplication.streetassist.admin.RecentReportAdapter.OnHeaderActionListener() {
            @Override
            public void onSearch(String query) {
                currentSearchQuery = query;
                applyFilters();
            }

            @Override
            public void onFilterClick() {
                showFilterPopup();
            }

            @Override
            public void onExportClick() {
                Toast.makeText(AdminReportsActivity.this, "Exporting " + filteredList.size() + " reports...", Toast.LENGTH_SHORT).show();
            }
        });
        rvReports.setAdapter(adapter);

        fetchAllReports();
    }

    private void fetchAllReports() {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        reportList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("documentId", doc.getId());
                                reportList.add(data);
                            }
                        }
                        applyFilters();
                    }
                });
    }

    private void applyFilters() {
        filteredList.clear();
        for (Map<String, Object> report : reportList) {
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String desc = String.valueOf(report.get("description")).toLowerCase();
                String loc = String.valueOf(report.get("locationAddress")).toLowerCase();
                String id = String.valueOf(report.get("reportId")).toLowerCase();
                matchesSearch = desc.contains(currentSearchQuery.toLowerCase()) ||
                        loc.contains(currentSearchQuery.toLowerCase()) ||
                        id.contains(currentSearchQuery.toLowerCase());
            }

            boolean matchesStatus = true;
            if (!currentStatusFilter.equals("All")) {
                String status = String.valueOf(report.get("status"));
                matchesStatus = status.equalsIgnoreCase(currentStatusFilter);
            }

            if (matchesSearch && matchesStatus) {
                filteredList.add(report);
            }
        }
        adapter.updateList(filteredList);
    }

    private void showFilterPopup() {
        RecyclerView.ViewHolder headerHolder = rvReports.findViewHolderForAdapterPosition(0);
        if (!(headerHolder instanceof RecentReportAdapter.HeaderViewHolder)) return;

        android.view.View filterBtn = ((RecentReportAdapter.HeaderViewHolder) headerHolder).btnFilter;
        if (filterBtn == null) return;

        PopupMenu popup = new PopupMenu(this, filterBtn);
        popup.getMenu().add("All");
        popup.getMenu().add("Pending");
        popup.getMenu().add("In Progress");
        popup.getMenu().add("Resolved");

        popup.setOnMenuItemClickListener(item -> {
            currentStatusFilter = item.getTitle().toString();
            applyFilters();

            android.widget.TextView filterLabel = filterBtn.findViewById(R.id.tvFilterLabel);
            if (filterLabel != null) filterLabel.setText(currentStatusFilter);

            return true;
        });
        popup.show();
    }

    public void showReportDetails(Map<String, Object> report) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_report_details, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvId = dialogView.findViewById(R.id.tvReportId);
        TextView tvStatus = dialogView.findViewById(R.id.tvDetailStatus);
        TextView tvTime = dialogView.findViewById(R.id.tvDetailTime);
        TextView tvDesc = dialogView.findViewById(R.id.tvDetailDescription);
        TextView tvLoc = dialogView.findViewById(R.id.tvDetailLocation);
        TextView tvName = dialogView.findViewById(R.id.tvReporterName);
        TextView tvContact = dialogView.findViewById(R.id.tvReporterContact);
        TextView tvAddress = dialogView.findViewById(R.id.tvReporterAddress);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnInProgress = dialogView.findViewById(R.id.btnSetInProgress);
        Button btnResolved = dialogView.findViewById(R.id.btnSetResolved);
        Button btnMap = dialogView.findViewById(R.id.btnViewLocation);

        String docId = String.valueOf(report.get("documentId"));
        String reportId = String.valueOf(report.get("reportId"));
        String status = String.valueOf(report.get("status"));

        tvId.setText("RPT-" + reportId);
        tvDesc.setText(String.valueOf(report.get("description")));
        tvLoc.setText(String.valueOf(report.get("locationAddress")));
        tvStatus.setText(status.toUpperCase());

        // Fetch Reporter Details
        String reporterId = String.valueOf(report.get("userId"));
        if (reporterId != null && !reporterId.equals("anonymous")) {
            db.collection("users").document(reporterId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String contact = documentSnapshot.getString("contactNumber");
                            Map<String, Object> addressMap = (Map<String, Object>) documentSnapshot.get("address");

                            tvName.setText(fullName != null ? fullName : "Anonymous");

                            if (contact != null && !contact.isEmpty()) {
                                tvContact.setText(getString(R.string.reporter_contact) + " " + contact);
                                tvContact.setVisibility(android.view.View.VISIBLE);
                            }

                            if (addressMap != null) {
                                String city = (String) addressMap.get("city");
                                String brgy = (String) addressMap.get("barangay");
                                if (city != null && brgy != null) {
                                    tvAddress.setText(getString(R.string.reporter_address) + " " + brgy + ", " + city);
                                    tvAddress.setVisibility(android.view.View.VISIBLE);
                                }
                            }
                        } else {
                            tvName.setText(getString(R.string.anonymous));
                        }
                    })
                    .addOnFailureListener(e -> tvName.setText(getString(R.string.anonymous)));
        } else {
            tvName.setText(getString(R.string.anonymous));
        }

        // Format Time
        Object ts = report.get("timestamp");
        if (ts instanceof com.google.firebase.Timestamp) {
            java.util.Date date = ((com.google.firebase.Timestamp) ts).toDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault());
            tvTime.setText(sdf.format(date));
        }

        // Hide update buttons if already resolved
        if ("Resolved".equalsIgnoreCase(status)) {
            btnInProgress.setVisibility(android.view.View.GONE);
            btnResolved.setVisibility(android.view.View.GONE);
        } else if ("In Progress".equalsIgnoreCase(status)) {
            btnInProgress.setVisibility(android.view.View.GONE);
        }

        btnInProgress.setOnClickListener(v -> updateReportStatus(docId, "In Progress", dialog));
        btnResolved.setOnClickListener(v -> updateReportStatus(docId, "Resolved", dialog));
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnMap.setOnClickListener(v -> {
            Object lat = report.get("latitude");
            Object lon = report.get("longitude");
            if (lat != null && lon != null) {
                String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(Incident)";
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)));
            }
        });

        dialog.show();
    }

    private void updateReportStatus(String docId, String newStatus, android.app.AlertDialog dialog) {
        db.collection("reports").document(docId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (id == R.id.nav_all_reports) {
            // Already here
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminAnnouncementsActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            logout();
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