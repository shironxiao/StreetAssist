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
    public SubmitReportStep3Fragment() {}

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
        Bundle args     = getArguments();
        double latitude     = args != null ? args.getDouble("latitude", 0)              : 0;
        double longitude    = args != null ? args.getDouble("longitude", 0)             : 0;
        String locationText = args != null ? args.getString("locationText")             : null;
        String age          = args != null ? args.getString("age")                      : null;
        String sex          = args != null ? args.getString("sex")                      : null;
        String description  = args != null ? args.getString("description")              : null;
        String assistance   = args != null ? args.getString("assistanceDescription")    : null;
        String contact      = args != null ? args.getString("contact")                  : null;
        long   seenAtMillis = args != null ? args.getLong("seenAt", 0)                  : 0;


        // ── Generate report ID and display it ─────────────────────────────────
        String reportId = "RPT-" + new SimpleDateFormat("yyyyMMdd-HHmmss",
                Locale.getDefault()).format(new Date());

        TextView tvReportId = view.findViewById(R.id.tvReportId);
        tvReportId.setText(reportId);

        // ── Save to Firestore immediately on screen load ───────────────────────
        saveToFirestore(reportId, latitude, longitude, locationText,
                age, sex, description, assistance, contact, seenAtMillis);
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
                        Toast.makeText(requireContext(), "Report saved successfully!",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to save report: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}