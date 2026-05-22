package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;
import com.mobileapplication.streetassist.utils.SessionManager;
import android.widget.CheckBox;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private MaterialButton btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private final Handler loginTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable loginTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_login_activity);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Auto-login check
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            verifyAdminRole(currentUser.getUid());
        }

        // Restore remembered email
        if (sessionManager.isRememberMeChecked("admin")) {
            etEmail.setText(sessionManager.getSavedEmail("admin"));
            cbRememberMe.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!validateInputs(email, password)) return;
            loginAdmin(email, password);
        });

        findViewById(R.id.tvBackToSelection).setOnClickListener(v -> {
            startActivity(new Intent(AdminLoginActivity.this, IntroductionUserLevel.class));
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
        if (!hasInternetConnection()) {
            Toast.makeText(this, "No internet connection. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        loginTimeoutRunnable = () -> {
            if (!isFinishing()) {
                mAuth.signOut();
                showErrorAndReset("Login timed out. Check your connection and try again.");
            }
        };
        loginTimeoutHandler.postDelayed(loginTimeoutRunnable, 15000);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save or clear remembered email
                        sessionManager.setRememberMe("admin", cbRememberMe.isChecked(), email);

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            verifyAdminRole(user.getUid());
                        }
                    } else {
                        String errorMsg = getErrorMessage(task.getException());
                        showErrorAndReset(errorMsg);
                    }
                });
    }

    private String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email address.";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_USER_NOT_FOUND":
                case "INVALID_LOGIN_CREDENTIALS":
                    return "Incorrect email or password.";
                case "ERROR_USER_DISABLED":
                    return "This account has been disabled.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many attempts. Please try again later.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Network error. Check your connection.";
                default:
                    return exception.getMessage();
            }
        }
        return (exception != null) ? exception.getMessage() : "Login failed. Please try again.";
    }

    private void verifyAdminRole(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if ("admin".equalsIgnoreCase(role)) {
                        loginTimeoutHandler.removeCallbacks(loginTimeoutRunnable);
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
        loginTimeoutHandler.removeCallbacks(loginTimeoutRunnable);
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    @Override
    protected void onDestroy() {
        loginTimeoutHandler.removeCallbacks(loginTimeoutRunnable);
        super.onDestroy();
    }
}