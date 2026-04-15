package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;
public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvBackToSelection;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_login_activity);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etEmail         = findViewById(R.id.etEmail);
        etPassword      = findViewById(R.id.etPassword);
        btnLogin        = findViewById(R.id.btnLogin);
        tvBackToSelection = findViewById(R.id.tvBackToSelection);

        // Autofill credentials
        etEmail.setText("augorioa@gmail.com");
        etPassword.setText("123");

        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) loginAdmin();
        });

        setupSwitchUserSpannable();
    }

    private boolean validateInputs() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Admin Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void loginAdmin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // ── DEVELOPER BYPASS ──
        // This allows you to enter the dashboard with the specific credentials you provided
        if (email.equals("augorioa@gmail.com") && password.equals("123")) {
            Toast.makeText(this, "Admin access granted.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verifying Admin…");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            verifyAdminRole(user.getUid());
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login as Admin");
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void verifyAdminRole(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this, (DocumentSnapshot doc) -> {
                    if (doc.exists() && "admin".equals(doc.getString("role"))) {
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login as Admin");
                        Toast.makeText(this, "Access Denied: Not an Admin account.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login as Admin");
                    Toast.makeText(this, "Error verifying role: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupSwitchUserSpannable() {
        String fullText = "Not an Admin? Switch User";
        SpannableString ss = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(AdminLoginActivity.this, IntroductionUserLevel.class));
                finish();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#2F80ED"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(clickableSpan, 15, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvBackToSelection.setText(ss);
        tvBackToSelection.setMovementMethod(LinkMovementMethod.getInstance());
    }
}