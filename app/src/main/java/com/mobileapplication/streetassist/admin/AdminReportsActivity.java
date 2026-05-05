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
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private com.google.firebase.firestore.ListenerRegistration reportsListener;

    private interface TrashMoveCallback {
        void onComplete(boolean success, String errorMessage);
    }

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

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
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> applyFilters();
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void onFilterClick() {
                showFilterPopup();
            }

            @Override
            public void onExportClick() {
                exportFilteredReports();
            }

            @Override
            public void onDeleteSelected(java.util.Set<String> selectedIds) {
                new android.app.AlertDialog.Builder(AdminReportsActivity.this)
                        .setTitle("Move Selected to Trash")
                        .setMessage("Move " + selectedIds.size() + " selected reports to Trash?")
                        .setPositiveButton("Move to Trash", (d, which) -> {
                            deleteMultipleReports(selectedIds);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onRestoreSelected(java.util.Set<String> selectedIds) {
                // Restore action is supported in AdminTrashActivity.
            }

            @Override
            public void onCancelSelection() {
                adapter.clearSelection();
            }
        });
        rvReports.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reportsListener == null) {
            reportsListener = fetchAllReports();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reportsListener != null) {
            reportsListener.remove();
            reportsListener = null;
        }
    }

    private void checkForIntentExtras() {
        String targetId = getIntent().getStringExtra("reportId");
        if (targetId != null && !targetId.isEmpty()) {
            getIntent().removeExtra("reportId"); // Clear it so it only shows once
            db.collection("reports").document(targetId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> data = documentSnapshot.getData();
                            if (data != null) {
                                data.put("documentId", documentSnapshot.getId());
                                showReportDetails(data);
                            }
                        }
                    });
        }
    }

    private com.google.firebase.firestore.ListenerRegistration fetchAllReports() {
        return db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        reportList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                // Only active reports should appear in Reports screen.
                                if (data.get("deletedAt") != null) continue;
                                data.put("documentId", doc.getId());
                                reportList.add(data);
                            }
                        }
                        applyFilters();
                        // Handle opening a specific report if we came from a notification
                        checkForIntentExtras();
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
        // Update adminSeenAt if not already set for admin tracking
        if (report.get("adminSeenAt") == null) {
            String docId = (String) report.get("documentId");
            if (docId != null) {
                db.collection("reports").document(docId)
                        .update("adminSeenAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            }
        }

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
        TextView tvSubject = dialogView.findViewById(R.id.tvDetailSubject);
        TextView tvAssistance = dialogView.findViewById(R.id.tvDetailAssistance);
        TextView tvName = dialogView.findViewById(R.id.tvReporterName);
        TextView tvContact = dialogView.findViewById(R.id.tvReporterContact);
        TextView tvAddress = dialogView.findViewById(R.id.tvReporterAddress);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnInProgress = dialogView.findViewById(R.id.btnSetInProgress);
        Button btnResolved = dialogView.findViewById(R.id.btnSetResolved);
        Button btnMap = dialogView.findViewById(R.id.btnViewLocation);
        ImageButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        String docId = String.valueOf(report.get("documentId"));
        String reportId = String.valueOf(report.get("reportId"));
        String status = String.valueOf(report.get("status"));

        tvId.setText(reportId.startsWith("RPT-") ? reportId : "RPT-" + reportId);
        tvDesc.setText(String.valueOf(report.getOrDefault("description", "No description provided")));
        tvLoc.setText(String.valueOf(report.getOrDefault("locationAddress", "Unknown Location")));
        tvStatus.setText(status.toUpperCase());

        // Set Subject and Assistance details
        String age = String.valueOf(report.getOrDefault("approximateAge", "N/A"));
        String sex = String.valueOf(report.getOrDefault("sex", "N/A"));
        tvSubject.setText("Age: " + age + ", Sex: " + sex);

        String assistance = String.valueOf(report.getOrDefault("assistanceDescription", "No assistance details provided"));
        tvAssistance.setText(assistance);

        // Fetch Reporter Details
        updateReporterInfo(report, tvName, tvContact, tvAddress);

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

        btnDelete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Move to Trash")
                    .setMessage("Move this report to Trash? You can restore it later.")
                    .setPositiveButton("Move to Trash", (d, which) -> {
                        deleteReport(docId, dialog);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    private void updateReporterInfo(Map<String, Object> report, TextView tvName, TextView tvContact, TextView tvAddress) {
        String reporterId = report.get("userId") != null ? String.valueOf(report.get("userId")) : "";
        String reportEmail = report.get("userEmail") != null ? String.valueOf(report.get("userEmail")) : "";
        String reportContact = report.get("contactNumber") != null ? String.valueOf(report.get("contactNumber")) : "";
        String reportFullName = report.get("fullName") != null ? String.valueOf(report.get("fullName")) : "";

        // 1. Initial display from report data
        if (!reportContact.isEmpty() && !reportContact.equalsIgnoreCase("null")) {
            tvContact.setText("Contact: " + reportContact);
            tvContact.setVisibility(android.view.View.VISIBLE);
        } else {
            tvContact.setVisibility(android.view.View.GONE);
        }
        
        tvAddress.setVisibility(android.view.View.GONE);

        // Priority: Use full name from report if it exists, otherwise email, otherwise "Resident"
        if (!reportFullName.isEmpty() && !reportFullName.equalsIgnoreCase("null")) {
            tvName.setText(reportFullName);
        } else if (!reportEmail.isEmpty() && !reportEmail.equalsIgnoreCase("null")) {
            tvName.setText(reportEmail);
        } else {
            tvName.setText("Resident");
        }

        // 2. Fetch additional user details if not anonymous to get the latest fullName
        if (!reporterId.isEmpty() && !reporterId.equalsIgnoreCase("anonymous") && !reporterId.equalsIgnoreCase("null")) {
            if (reporterId.contains("@")) {
                // Case where email might be stored in userId field
                db.collection("users").whereEqualTo("email", reporterId).get()
                        .addOnSuccessListener(qs -> {
                            if (!qs.isEmpty()) {
                                populateUserDetails(qs.getDocuments().get(0), tvName, tvContact, tvAddress);
                            }
                        });
            } else {
                // Priority 1: Lookup by UID
                db.collection("users").document(reporterId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                populateUserDetails(doc, tvName, tvContact, tvAddress);
                            } else if (reportEmail.contains("@")) {
                                // Priority 2: Lookup by Email if UID not found
                                db.collection("users").whereEqualTo("email", reportEmail).get()
                                        .addOnSuccessListener(qs -> {
                                            if (!qs.isEmpty()) {
                                                populateUserDetails(qs.getDocuments().get(0), tvName, tvContact, tvAddress);
                                            }
                                        });
                            }
                        });
            }
        } else if (reportEmail.contains("@")) {
            db.collection("users").whereEqualTo("email", reportEmail).get()
                    .addOnSuccessListener(qs -> {
                        if (!qs.isEmpty()) {
                            populateUserDetails(qs.getDocuments().get(0), tvName, tvContact, tvAddress);
                        }
                    });
        }
    }

    private void populateUserDetails(com.google.firebase.firestore.DocumentSnapshot doc, TextView tvName, TextView tvContact, TextView tvAddress) {
        // 1. Resolve Name
        String name = doc.getString("fullName");
        if (name == null || name.isEmpty()) name = doc.getString("fullname");
        if (name == null || name.isEmpty()) name = doc.getString("username");
        if (name == null || name.isEmpty()) name = doc.getString("email");
        tvName.setText(name != null && !name.isEmpty() ? name : "Resident");

        // 2. Resolve Contact (overwrites report contact if available in profile)
        String contact = doc.getString("contactNumber");
        if (contact == null || contact.isEmpty()) contact = doc.getString("phoneNumber");
        if (contact != null && !contact.isEmpty()) {
            tvContact.setText("Contact: " + contact);
            tvContact.setVisibility(android.view.View.VISIBLE);
        }

        // 3. Resolve Address
        Object addrObj = doc.get("address");
        if (addrObj instanceof Map) {
            Map<String, Object> addrMap = (Map<String, Object>) addrObj;
            String brgy = String.valueOf(addrMap.getOrDefault("barangay", ""));
            String city = String.valueOf(addrMap.getOrDefault("city", ""));
            if (!brgy.isEmpty() || !city.isEmpty()) {
                String fullAddr = brgy + (brgy.isEmpty() || city.isEmpty() ? "" : ", ") + city;
                tvAddress.setText("Address: " + fullAddr);
                tvAddress.setVisibility(android.view.View.VISIBLE);
            }
        } else if (addrObj instanceof String && !((String) addrObj).isEmpty()) {
            tvAddress.setText("Address: " + addrObj);
            tvAddress.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void deleteMultipleReports(java.util.Set<String> selectedIds) {
        if (selectedIds.isEmpty()) return;
        java.util.List<String> idList = new java.util.ArrayList<>(selectedIds);
        processTrashMoveSequentially(idList, 0, 0, 0);
    }

    private void deleteReport(String docId, android.app.AlertDialog detailsDialog) {
        moveReportToTrash(docId, (success, errorMessage) -> {
            if (success) {
                Toast.makeText(this, "Report moved to Trash", Toast.LENGTH_SHORT).show();
                showRestoreSnackbar(java.util.Collections.singleton(docId), "Report moved to Trash");
                if (detailsDialog != null) detailsDialog.dismiss();
            } else {
                Toast.makeText(this, "Failed to move report to Trash: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processTrashMoveSequentially(java.util.List<String> ids, int index, int successCount, int failCount) {
        if (index >= ids.size()) {
            if (failCount == 0) {
                Toast.makeText(this, successCount + " reports moved to Trash", Toast.LENGTH_SHORT).show();
                showRestoreSnackbar(new java.util.HashSet<>(ids), successCount + " reports moved to Trash");
            } else {
                Toast.makeText(this, "Moved " + successCount + ", failed " + failCount, Toast.LENGTH_LONG).show();
            }
            adapter.clearSelection();
            return;
        }

        moveReportToTrash(ids.get(index), (success, errorMessage) -> {
            int nextSuccess = success ? successCount + 1 : successCount;
            int nextFail = success ? failCount : failCount + 1;
            processTrashMoveSequentially(ids, index + 1, nextSuccess, nextFail);
        });
    }

    private void moveReportToTrash(String docId, TrashMoveCallback callback) {
        if (docId == null || docId.trim().isEmpty() || "null".equalsIgnoreCase(docId)) {
            callback.onComplete(false, "Invalid report id");
            return;
        }

        com.google.firebase.firestore.DocumentReference reportRef = db.collection("reports").document(docId);

        reportRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists() || snapshot.getData() == null) {
                        callback.onComplete(false, "Report not found");
                        return;
                    }

                    reportRef.update("deletedAt", com.google.firebase.Timestamp.now())
                            .addOnSuccessListener(unused -> callback.onComplete(true, null))
                            .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    private void showRestoreSnackbar(java.util.Set<String> docIds, String message) {
        if (docIds == null || docIds.isEmpty()) return;
        Snackbar.make(rvReports, message, Snackbar.LENGTH_LONG)
                .setAction("Restore", v -> restoreReports(docIds))
                .show();
    }

    private void restoreReports(java.util.Set<String> docIds) {
        com.google.firebase.firestore.WriteBatch batch = db.batch();
        for (String id : docIds) {
            if (id != null && !id.trim().isEmpty()) {
                batch.update(db.collection("reports").document(id), "deletedAt", FieldValue.delete());
            }
        }
        batch.commit()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Report restored", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to restore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void exportFilteredReports() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No reports to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String csv = buildCsvFromReports(filteredList);
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            java.io.File exportDir = new java.io.File(getCacheDir(), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                Toast.makeText(this, "Failed to prepare export folder", Toast.LENGTH_LONG).show();
                return;
            }

            java.io.File csvFile = new java.io.File(exportDir, "reports_export_" + timestamp + ".csv");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(csvFile)) {
                fos.write(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            android.net.Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "StreetAssist Reports Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Exported " + filteredList.size() + " reports.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Export reports"));
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String buildCsvFromReports(List<Map<String, Object>> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("Document ID,Report ID,Status,Description,Location,Latitude,Longitude,Timestamp,User ID\n");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (Map<String, Object> report : reports) {
            String documentId = safeString(report.get("documentId"));
            String reportId = safeString(report.get("reportId"));
            String status = safeString(report.get("status"));
            String description = safeString(report.get("description"));
            String location = safeString(report.get("locationAddress"));
            String latitude = safeString(report.get("latitude"));
            String longitude = safeString(report.get("longitude"));
            String userId = safeString(report.get("userId"));

            String timestamp = "";
            Object ts = report.get("timestamp");
            if (ts instanceof com.google.firebase.Timestamp) {
                timestamp = sdf.format(((com.google.firebase.Timestamp) ts).toDate());
            } else if (ts != null) {
                timestamp = ts.toString();
            }

            sb.append(csvCell(documentId)).append(',')
                    .append(csvCell(reportId)).append(',')
                    .append(csvCell(status)).append(',')
                    .append(csvCell(description)).append(',')
                    .append(csvCell(location)).append(',')
                    .append(csvCell(latitude)).append(',')
                    .append(csvCell(longitude)).append(',')
                    .append(csvCell(timestamp)).append(',')
                    .append(csvCell(userId))
                    .append('\n');
        }

        return sb.toString();
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String csvCell(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
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
        } else if (id == R.id.nav_trash) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminTrashActivity.class));
            finish();
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminNotificationActivity.class));
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