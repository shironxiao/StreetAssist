package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.resident.ResidentMainActivity;

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

        double latitude     = getIntent().getDoubleExtra("latitude", 0);
        double longitude    = getIntent().getDoubleExtra("longitude", 0);
        String locationText = getIntent().getStringExtra("locationText");
        String age          = getIntent().getStringExtra("age");
        String sex          = getIntent().getStringExtra("sex");
        String description  = getIntent().getStringExtra("description");
        String assistance   = getIntent().getStringExtra("assistanceDescription");
        String contact      = getIntent().getStringExtra("contact");
        long   seenAtMillis = getIntent().getLongExtra("seenAt", 0);

        String reportId = "RPT-" + new SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.getDefault()).format(new Date());

        TextView tvReportId = findViewById(R.id.tvReportId);
        tvReportId.setText(reportId);

        saveToFirestore(reportId, latitude, longitude, locationText,
                age, sex, description, assistance, contact, seenAtMillis);

        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            // ✅ Clears the entire report submission back stack and goes home
            Intent intent = new Intent(this, ResidentMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveToFirestore(String reportId,
                                 double latitude, double longitude,
                                 String locationText,
                                 String age, String sex, String description,
                                 String assistance, String contact,
                                 long seenAtMillis) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String userId = "anonymous";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        Timestamp seenAtTimestamp = new Timestamp(new Date(seenAtMillis));

        Map<String, Object> report = new HashMap<>();
        report.put("reportId",              reportId);
        report.put("userId",                userId);
        report.put("latitude",              latitude);
        report.put("longitude",             longitude);
        report.put("locationAddress",       locationText);
        report.put("approximateAge",        age);
        report.put("sex",                   sex);
        report.put("description",           description);
        report.put("assistanceDescription", assistance);
        report.put("contactNumber",         contact != null ? contact : "");
        report.put("seenAt",                seenAtTimestamp);
        report.put("status",                "Pending");
        report.put("timestamp",             new Date());

        db.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Report saved successfully!",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save report: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}