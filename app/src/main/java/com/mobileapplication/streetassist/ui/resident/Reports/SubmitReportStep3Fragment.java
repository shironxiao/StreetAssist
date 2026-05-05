package com.mobileapplication.streetassist.ui.resident.Reports;

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

public class SubmitReportStep3Fragment extends Fragment {
    private boolean reportSaved = false;

    public SubmitReportStep3Fragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_submit_report_step3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Retrieve all data from Step 2 via Bundle ──────────────────────────
        Bundle args = getArguments();
        double latitude = args != null ? args.getDouble("latitude", 0) : 0;
        double longitude = args != null ? args.getDouble("longitude", 0) : 0;
        String locationText = args != null ? args.getString("locationText") : null;
        String age = args != null ? args.getString("age") : null;
        String sex = args != null ? args.getString("sex") : null;
        String description = args != null ? args.getString("description") : null;
        String assistance = args != null ? args.getString("assistanceDescription") : null;
        String contact = args != null ? args.getString("contact") : null;
        long seenAtMillis = args != null ? args.getLong("seenAt", 0) : 0;

        // ── Generate report ID and display it ─────────────────────────────────
        String reportId = "RPT-" + new SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.getDefault()).format(new Date());

        TextView tvReportId = view.findViewById(R.id.tvReportId);
        tvReportId.setText(reportId);

        // ── Save to Firestore only once ───────────────────────────────────────
        if (!reportSaved) {
            reportSaved = true;
            saveToFirestore(reportId, latitude, longitude, locationText,
                    age, sex, description, assistance, contact, seenAtMillis);
        }
        // ── Back to Home button ───────────────────────────────────────────────
        MaterialButton btnBackToHome = view.findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ResidentMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void saveToFirestore(String reportId,
            double latitude, double longitude,
            String locationText,
            String age, String sex, String description,
            String assistance, String contact,
            long seenAtMillis) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            proceedWithSave(db, reportId, "anonymous", "", "Anonymous Resident",
                    latitude, longitude, locationText, age, sex, description, assistance, contact, seenAtMillis);
            return;
        }

        // Fetch User Full Name from Firestore before saving the report
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fullName = documentSnapshot.getString("fullName");
                    if (fullName == null || fullName.isEmpty())
                        fullName = documentSnapshot.getString("username");
                    if (fullName == null || fullName.isEmpty())
                        fullName = "Resident";

                    String email = documentSnapshot.getString("email");
                    if (email == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                        email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    }

                    proceedWithSave(db, reportId, uid, email != null ? email : "", fullName,
                            latitude, longitude, locationText, age, sex, description, assistance, contact, seenAtMillis);
                })
                .addOnFailureListener(e -> {
                    String email = "";
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    }
                    proceedWithSave(db, reportId, uid, email != null ? email : "", "Resident",
                            latitude, longitude, locationText, age, sex, description, assistance, contact, seenAtMillis);
                });
    }

    private void proceedWithSave(FirebaseFirestore db, String reportId,
            String userId, String userEmail, String fullName,
            double latitude, double longitude,
            String locationText,
            String age, String sex, String description,
            String assistance, String contact,
            long seenAtMillis) {

        // Only set seenAt if it was actually provided
        Timestamp seenAtTimestamp = null;
        if (seenAtMillis > 0) {
            seenAtTimestamp = new Timestamp(new Date(seenAtMillis));
        }

        Map<String, Object> report = new HashMap<>();
        report.put("reportId", reportId);
        report.put("userId", userId);
        report.put("userEmail", userEmail);
        report.put("fullName", fullName); // Storing fullName directly for Admin view
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("locationAddress", locationText);
        report.put("approximateAge", age);
        report.put("sex", sex);
        report.put("description", description);
        report.put("assistanceDescription", assistance);
        report.put("contactNumber", contact != null ? contact : "");
        report.put("seenAt", seenAtTimestamp);
        report.put("status", "Pending");
        report.put("timestamp", new Date());

        db.collection("reports")
                .document(reportId)
                .set(report)
                .addOnSuccessListener(unused -> {
                    // Create notification regardless of UI state to ensure Admin receives it
                    createNotification(db, reportId, locationText, fullName);

                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Report saved successfully!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to save report: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createNotification(FirebaseFirestore db, String reportId, String location, String userName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "New Report Submitted");
        notification.put("message", userName + " submitted a new report (" + reportId + ") at " + location);
        notification.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        notification.put("isRead", false);
        notification.put("type", "new_report");
        notification.put("referenceId", reportId);

        db.collection("admin_notifications").add(notification)
                .addOnSuccessListener(docRef -> android.util.Log.d("STREET_ASSIST_DEBUG",
                        "Admin notification created with ID: " + docRef.getId()))
                .addOnFailureListener(e -> android.util.Log.e("STREET_ASSIST_DEBUG", "Error creating admin notification: " + e.getMessage()));
    }
}