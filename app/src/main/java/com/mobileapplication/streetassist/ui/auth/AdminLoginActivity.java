package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_login_activity);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!validateInputs(email, password)) return;
            loginAdmin(email, password);
        });

        findViewById(R.id.tvBackToSelection).setOnClickListener(v -> {
            finish();
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void loginAdmin(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        showErrorAndReset("Login failed. Please try again.");
                        return;
                    }
                    verifyAdminRole(user.getUid());
                })
                .addOnFailureListener(e -> showErrorAndReset("Invalid credentials."));
    }

    private void verifyAdminRole(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if ("admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, AdminDashboardActivity.class));
                        finish();
                    } else {
                        mAuth.signOut();
                        showErrorAndReset("Access denied. Admin account required.");
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    showErrorAndReset("Failed to verify admin role.");
                });
    }

    private void showErrorAndReset(String message) {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}