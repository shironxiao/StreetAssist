package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminTrashActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminTrash";
    private DrawerLayout drawerLayout;
    private RecyclerView rvTrash;
    private RecentReportAdapter adapter;
    private List<Map<String, Object>> trashList = new ArrayList<>();
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration trashListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_trash);

        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_trash);

        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
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
                    startActivity(new Intent(AdminTrashActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }
        });

        rvTrash = findViewById(R.id.rvTrashReports);
        rvTrash.setLayoutManager(new LinearLayoutManager(this));

        // Using RecentReportAdapter for consistency, but we might want to disable selection or change actions
        adapter = new RecentReportAdapter(this, trashList);
        adapter.setHeaderListener(new RecentReportAdapter.OnHeaderActionListener() {
            @Override
            public void onSearch(String query) {
                // Search/filter can be added later for Trash screen.
            }

            @Override
            public void onFilterClick() {
                Toast.makeText(AdminTrashActivity.this, "No filters available in Trash yet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onExportClick() {
                Toast.makeText(AdminTrashActivity.this, "Export is not available in Trash", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteSelected(Set<String> selectedIds) {
                permanentlyDeleteSelected(selectedIds);
            }

            @Override
            public void onRestoreSelected(Set<String> selectedIds) {
                restoreSelectedReports(selectedIds);
            }

            @Override
            public void onCancelSelection() {
                adapter.clearSelection();
            }
        });
        rvTrash.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (trashListener == null) {
            trashListener = fetchTrashReports();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (trashListener != null) {
            trashListener.remove();
            trashListener = null;
        }
    }

    private com.google.firebase.firestore.ListenerRegistration fetchTrashReports() {
        return db.collection("reports")
                .whereGreaterThan("deletedAt", new com.google.firebase.Timestamp(new java.util.Date(0)))
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        trashList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("documentId", doc.getId());
                                trashList.add(data);
                            }
                        }
                        adapter.updateList(trashList);
                    }
                });
    }

    public void showReportDetails(Map<String, Object> report) {
        // Update adminSeenAt if not already set
        if (report.get("adminSeenAt") == null) {
            String docId = (String) report.get("documentId");
            if (docId != null) {
                db.collection("reports").document(docId)
                        .update("adminSeenAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            }
        }

        String reportId = String.valueOf(report.get("reportId"));
        String docId = String.valueOf(report.get("documentId"));
        new android.app.AlertDialog.Builder(this)
                .setTitle("RPT-" + reportId)
                .setMessage("Choose an action for this deleted report.")
                .setPositiveButton("Restore", (dialog, which) -> restoreSelectedReports(java.util.Collections.singleton(docId)))
                .setNegativeButton("Delete Permanently", (dialog, which) -> permanentlyDeleteSelected(java.util.Collections.singleton(docId)))
                .setNeutralButton("Close", null)
                .show();
    }

    private void restoreSelectedReports(Set<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) return;

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (String id : selectedIds) {
            batch.update(db.collection("reports").document(id), "deletedAt", FieldValue.delete());
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Selected reports restored", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to restore reports: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void permanentlyDeleteSelected(Set<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) return;

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (String id : selectedIds) {
            batch.delete(db.collection("reports").document(id));
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Selected reports deleted permanently", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete reports: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            // Already here
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, AdminNotificationActivity.class));
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
}