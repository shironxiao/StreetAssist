package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.ui.resident.Home.HomeFragment;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.resident.Home.HomeFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class submit_report_step3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_report_step3);

        // ── Collect all data passed from Step 1 & 2 ──────────────────────────
        double latitude     = getIntent().getDoubleExtra("latitude", 0);
        double longitude    = getIntent().getDoubleExtra("longitude", 0);
        String locationText = getIntent().getStringExtra("locationText");
        String age          = getIntent().getStringExtra("age");
        String sex          = getIntent().getStringExtra("sex");
        String description  = getIntent().getStringExtra("description");
        String assistance   = getIntent().getStringExtra("assistanceDescription");
        String contact      = getIntent().getStringExtra("contact");

        // ── Generate report ID ────────────────────────────────────────────────
        String reportId = "RPT-" + new SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.getDefault()).format(new Date());

        TextView tvReportId = findViewById(R.id.tvReportId);
        tvReportId.setText(reportId);

        // ── Save to Firestore ─────────────────────────────────────────────────
        saveToFirestore(reportId, latitude, longitude, locationText,
                age, sex, description, assistance, contact);

        // ── Back to Home ──────────────────────────────────────────────────────
        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeFragment.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveToFirestore(String reportId,
                                 double latitude,
                                 double longitude,
                                 String locationText,
                                 String age,
                                 String sex,
                                 String description,
                                 String assistance,
                                 String contact) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = "anonymous";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // ── Firestore document structure ──────────────────────────────────────
        Map<String, Object> report = new HashMap<>();
        report.put("reportId",              reportId);
        report.put("userId",                userId);

        // Location
        report.put("latitude",              latitude);
        report.put("longitude",             longitude);
        report.put("locationAddress",       locationText);

        // Individual details
        report.put("approximateAge",        age);
        report.put("sex",                   sex);
        report.put("description",           description);

        // Assistance
        report.put("assistanceDescription", assistance);

        // Reporter contact
        report.put("contactNumber",         contact != null ? contact : "");

        // Status & timestamp
        report.put("status",                "Pending");
        report.put("timestamp",             new Date());

        // ── Save to "reports" collection ──────────────────────────────────────
        db.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Report saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to save report: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}