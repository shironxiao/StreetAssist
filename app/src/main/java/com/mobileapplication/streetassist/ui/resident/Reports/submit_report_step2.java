package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

public class submit_report_step2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_report_step2);

        TextInputEditText etAssistanceDescription = findViewById(R.id.etAssistanceDescription);
        TextInputEditText etContactNumber         = findViewById(R.id.etContactNumber);

        double latitude     = getIntent().getDoubleExtra("latitude", 0);
        double longitude    = getIntent().getDoubleExtra("longitude", 0);
        String locationText = getIntent().getStringExtra("locationText");
        String age          = getIntent().getStringExtra("age");
        String sex          = getIntent().getStringExtra("sex");
        String description  = getIntent().getStringExtra("description");
        long   seenAt       = getIntent().getLongExtra("seenAt", 0);

        // ── Auto-fill contact number from the user's Firestore profile ────────
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String savedContact = doc.getString("contactNumber");
                            if (savedContact != null && !savedContact.isEmpty()) {
                                etContactNumber.setText(savedContact);
                            }
                        }
                    });
        }

        // ── Next button ───────────────────────────────────────────────────────
        MaterialButton btnNext = findViewById(R.id.btnNextStep2);
        btnNext.setOnClickListener(v -> {
            String assistanceDesc = etAssistanceDescription.getText() != null
                    ? etAssistanceDescription.getText().toString().trim() : "";

            if (assistanceDesc.isEmpty()) {
                etAssistanceDescription.setError("Please describe the assistance needed.");
                etAssistanceDescription.requestFocus();
                return;
            }

            String contact = etContactNumber.getText() != null
                    ? etContactNumber.getText().toString().trim() : "";

            Intent intent = new Intent(this, submit_report_step3.class);
            intent.putExtra("latitude",              latitude);
            intent.putExtra("longitude",             longitude);
            intent.putExtra("locationText",          locationText);
            intent.putExtra("age",                   age);
            intent.putExtra("sex",                   sex);
            intent.putExtra("description",           description);
            intent.putExtra("assistanceDescription", assistanceDesc);
            intent.putExtra("contact",               contact);
            intent.putExtra("seenAt",                seenAt);
            startActivity(intent);
        });

        // ── Back button ───────────────────────────────────────────────────────
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}