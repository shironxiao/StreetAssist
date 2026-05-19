package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.resident.ResidentMainActivity;
import com.mobileapplication.streetassist.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnGetStarted;
    private CheckBox cbRememberMe;
    private TextView tvSignUpPrompt, tvForgotPassword, tvContinueAsGuest;
    private ImageButton btnBack;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Session Manager ──────────────────────────────────────────────────────
    private SessionManager sessionManager;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // Firebase & Session
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Views
        etEmail         = findViewById(R.id.etEmail);
        etPassword      = findViewById(R.id.etPassword);
        btnGetStarted   = findViewById(R.id.btnGetStarted);
        cbRememberMe    = findViewById(R.id.cbRememberMe);
        tvSignUpPrompt  = findViewById(R.id.tvSignUpPrompt);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvContinueAsGuest = findViewById(R.id.tvContinueAsGuest);
        btnBack          = findViewById(R.id.btnBack);

        // ── Auto-login: if user is already signed in, skip to main ───────────
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateByRole(currentUser.getUid());
            return;
        }

        // ── Restore remembered email via SessionManager ──────────────────────
        if (sessionManager.isRememberMeChecked()) {
            etEmail.setText(sessionManager.getSavedEmail());
            cbRememberMe.setChecked(true);
        }

        // ── Listeners ────────────────────────────────────────────────────────
        btnGetStarted.setOnClickListener(v -> {
            if (validateInputs()) loginUser();
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        tvContinueAsGuest.setOnClickListener(v -> continueAsGuest());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, IntroductionUserLevel.class));
            finish();
        });

        setupSignUpSpannable();
    }

    // =========================================================================
    //  Validation
    // =========================================================================

    private boolean validateInputs() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        return true;
    }

    // =========================================================================
    //  Firebase Login
    // =========================================================================

    private void loginUser() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnGetStarted.setEnabled(false);
        btnGetStarted.setText("Signing in…");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save or clear remembered email
                        saveRememberMe(email);

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            navigateByRole(user.getUid());
                        }
                    } else {
                        btnGetStarted.setEnabled(true);
                        btnGetStarted.setText("Login");

                        // Map common Firebase errors to friendly messages
                        String errorMsg = "Login failed. Please try again.";
                        if (task.getException() != null) {
                            String raw = task.getException().getMessage();
                            if (raw != null) {
                                if (raw.contains("no user record") ||
                                        raw.contains("identifier")) {
                                    errorMsg = "No account found with this email.";
                                } else if (raw.contains("password is invalid") ||
                                        raw.contains("incorrect")) {
                                    errorMsg = "Incorrect password. Please try again.";
                                } else if (raw.contains("blocked") ||
                                        raw.contains("too many")) {
                                    errorMsg = "Too many attempts. Please try again later.";
                                } else if (raw.contains("network")) {
                                    errorMsg = "Network error. Check your connection.";
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // =========================================================================
    //  Firestore – fetch user role then navigate
    // =========================================================================

    /**
     * Reads the user's Firestore document to get their role,
     * then routes them to the correct activity.
     */
    private void navigateByRole(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this, (DocumentSnapshot doc) -> {
                    if (doc.exists()) {
                        String role     = doc.getString("role");
                        String fullName = doc.getString("fullName");

                        if ("admin".equals(role)) {
                            mAuth.signOut();
                            btnGetStarted.setEnabled(true);
                            btnGetStarted.setText("Login");
                            Toast.makeText(this, "Access denied. Resident account required.", Toast.LENGTH_LONG).show();
                        } else {
                            // Greet the user
                            Toast.makeText(this,
                                    "Welcome back, " + (fullName != null ? fullName : "User") + "!",
                                    Toast.LENGTH_SHORT).show();

                            // Route to resident main
                            Intent intent = new Intent(this, ResidentMainActivity.class);

                            // Pass useful user data to the next screen via Intent extras
                            intent.putExtra("uid",      uid);
                            intent.putExtra("fullName", fullName);
                            intent.putExtra("email",    doc.getString("email"));
                            intent.putExtra("role",     role);

                            // Pass address fields
                            if (doc.contains("address")) {
                                intent.putExtra("region",   doc.getString("address.region"));
                                intent.putExtra("province", doc.getString("address.province"));
                                intent.putExtra("city",     doc.getString("address.city"));
                                intent.putExtra("barangay", doc.getString("address.barangay"));
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }

                    } else {
                        // Auth account exists but Firestore doc is missing
                        btnGetStarted.setEnabled(true);
                        btnGetStarted.setText("Login");
                        Toast.makeText(this,
                                "Account data not found. Please contact support.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                })
                .addOnFailureListener(e -> {
                    btnGetStarted.setEnabled(true);
                    btnGetStarted.setText("Login");
                    Toast.makeText(this,
                            "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                });
    }

    // =========================================================================
    //  Forgot Password
    // =========================================================================

    private void handleForgotPassword() {
        startActivity(new Intent(this, ResetPassword.class));
    }

    // =========================================================================
    //  Remember Me
    // =========================================================================

    private void saveRememberMe(String email) {
        sessionManager.setRememberMe(cbRememberMe.isChecked(), email);
    }

    private void continueAsGuest() {
        Intent intent = new Intent(this, ResidentMainActivity.class);
        intent.putExtra("is_guest", true);
        intent.putExtra("role", "guest");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =========================================================================
    //  Spannable – "Sign up"
    // =========================================================================

    private void setupSignUpSpannable() {
        String fullText = "Don't have an Account ? Sign up";
        SpannableString ss = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(clickableSpan, 24, 31, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSignUpPrompt.setText(ss);
        tvSignUpPrompt.setMovementMethod(LinkMovementMethod.getInstance());
        tvSignUpPrompt.setHighlightColor(Color.TRANSPARENT);
    }
}