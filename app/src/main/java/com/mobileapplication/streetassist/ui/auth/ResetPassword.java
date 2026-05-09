package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

public class ResetPassword extends AppCompatActivity {

    private TextInputEditText etVerifyEmail;
    private View sectionVerify, sectionReset;
    private MaterialButton btnVerify, btnDone;
    private ImageButton btnBack;
    private android.widget.TextView tvBackToLogin;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String targetUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind Views
        etVerifyEmail = findViewById(R.id.etVerifyEmail);
        sectionVerify = findViewById(R.id.sectionVerify);
        sectionReset = findViewById(R.id.sectionReset);
        
        btnVerify = findViewById(R.id.btnVerify);
        btnDone = findViewById(R.id.btnDone);
        btnBack = findViewById(R.id.btnBack);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> verifyIdentity());
        
        btnDone.setOnClickListener(v -> finish());
    }

    private void verifyIdentity() {
        String email = etVerifyEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etVerifyEmail.setError("Please enter your email address");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etVerifyEmail.setError("Please enter a valid email");
            return;
        }

        btnVerify.setEnabled(false);
        btnVerify.setText("Sending...");

        // SIMPLIFIED WAY: Send email directly via Firebase Auth
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    sectionVerify.setVisibility(View.GONE);
                    sectionReset.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("Send Reset Link");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
