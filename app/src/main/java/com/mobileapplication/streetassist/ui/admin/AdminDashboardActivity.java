package com.mobileapplication.streetassist.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;
import com.mobileapplication.streetassist.ui.resident.Reports.ReportAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private RecyclerView rvReports;
    private ReportAdapter adapter;
    private List<Map<String, Object>> reportList = new ArrayList<>();
    private FirebaseFirestore db;

    // Count TextViews
    private TextView tvCountPending, tvCountInProgress, tvCountVerified, tvCountResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_dashboard);

        db = FirebaseFirestore.getInstance();
        
        // Initialize Count Views
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountVerified = findViewById(R.id.tvCountVerified);
        tvCountResolved = findViewById(R.id.tvCountResolved);

        rvReports = findViewById(R.id.rvAdminReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ReportAdapter(this, reportList);
        rvReports.setAdapter(adapter);

        // Exit / Logout button
        Button btnExit = findViewById(R.id.btnExitAdmin);
        Log.d(TAG, "btnExitAdmin found: " + (btnExit != null));
        if (btnExit != null) {
            btnExit.setOnClickListener(v -> {
                Log.d(TAG, "Logout button clicked!");
                try {
                    FirebaseAuth.getInstance().signOut();
                    Log.d(TAG, "Firebase sign out successful");
                    Intent intent = new Intent(AdminDashboardActivity.this, IntroductionUserLevel.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error during logout: " + e.getMessage(), e);
                    Toast.makeText(AdminDashboardActivity.this, "Logout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Log.e(TAG, "btnExitAdmin is NULL! Check layout XML.");
        }

        fetchReports();
    }

    private void fetchReports() {
        db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        reportList.clear();
                        int pending = 0, inProgress = 0, verified = 0, resolved = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("documentId", doc.getId()); // Store ID for updates
                                reportList.add(data);

                                // Update counters
                                String status = String.valueOf(data.get("status")).toLowerCase();
                                switch (status) {
                                    case "pending": pending++; break;
                                    case "in progress": inProgress++; break;
                                    case "verified": verified++; break;
                                    case "resolved": resolved++; break;
                                }
                            }
                        }
                        
                        // Update UI counts
                        tvCountPending.setText(String.valueOf(pending));
                        tvCountInProgress.setText(String.valueOf(inProgress));
                        tvCountVerified.setText(String.valueOf(verified));
                        tvCountResolved.setText(String.valueOf(resolved));

                        adapter.updateList(reportList);
                    }
                });
    }
}