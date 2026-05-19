package com.mobileapplication.streetassist.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

import java.util.HashMap;
import java.util.Map;

public class AdminRegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etContactNumber, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ImageButton btnBack;

    private FirebaseFirestore db;

    // Secondary Firebase Auth details to prevent session logout
    private static final String APP_ID = "1:16419213959:android:4aa060effcec94b6591e22";
    private static final String API_KEY = "AIzaSyCZn41JP-GJpTWtIcj_IDBN8KarqkPQUN4";
    private static final String PROJECT_ID = "streetassist-8a9d3";
    private static final String STORAGE_BUCKET = "streetassist-8a9d3.firebasestorage.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register);

        db = FirebaseFirestore.getInstance();

        // Bind Views
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etContactNumber = findViewById(R.id.etContactNumber);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerNewAdmin();
            }
        });
    }

    private boolean validateInputs() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contact = etContactNumber.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(contact)) {
            etContactNumber.setError("Contact number is required");
            etContactNumber.requestFocus();
            return false;
        }

        if (!contact.matches("^09\\d{9}$")) {
            etContactNumber.setError("Enter a valid PH contact number (e.g., 09XXXXXXXXX)");
            etContactNumber.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void registerNewAdmin() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String contact = etContactNumber.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating admin account...");

        // Setup Secondary Firebase App to avoid logging out the current admin session
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(APP_ID)
                .setApiKey(API_KEY)
                .setProjectId(PROJECT_ID)
                .setStorageBucket(STORAGE_BUCKET)
                .build();

        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("TempAdminRegister");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.initializeApp(this, options, "TempAdminRegister");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String newAdminUid = task.getResult().getUser().getUid();
                        saveAdminToFirestore(newAdminUid, fullName, email, contact, secondaryAuth);
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Admin Account");
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Auth registration failed.";
                        Toast.makeText(AdminRegisterActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveAdminToFirestore(String uid, String fullName, String email, String contact, FirebaseAuth secondaryAuth) {
        Map<String, Object> address = new HashMap<>();
        address.put("region", "REGION V (Bicol Region)");
        address.put("province", "Camarines Norte");
        address.put("city", "Daet");
        address.put("barangay", "Barangay I (Pob.)");

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("fullName", fullName);
        userDoc.put("email", email);
        userDoc.put("contactNumber", contact);
        userDoc.put("address", address);
        userDoc.put("role", "admin");
        userDoc.put("isVerified", true);
        userDoc.put("isActive", true);
        userDoc.put("profilePhotoUrl", null);
        userDoc.put("createdAt", Timestamp.now());
        userDoc.put("updatedAt", Timestamp.now());

        db.collection("users")
                .document(uid)
                .set(userDoc)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminRegisterActivity.this, "Admin account registered successfully!", Toast.LENGTH_SHORT).show();
                    // Clean secondary auth session
                    secondaryAuth.signOut();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Try to clean up authentication user since saving firestore failed
                    if (secondaryAuth.getCurrentUser() != null) {
                        secondaryAuth.getCurrentUser().delete();
                    }
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Admin Account");
                    Toast.makeText(AdminRegisterActivity.this, "Firestore save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
