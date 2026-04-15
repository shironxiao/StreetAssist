package com.mobileapplication.streetassist.ui.resident.Profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

import java.util.HashMap;
import java.util.Map;

public class ProfileEdit extends AppCompatActivity {

    // ── Intent extra keys (used by ProfileFragment to pass current data) ─────
    public static final String EXTRA_FULL_NAME = "extra_full_name";
    public static final String EXTRA_CONTACT   = "extra_contact";
    public static final String EXTRA_EMAIL     = "extra_email";
    public static final String EXTRA_ADDRESS   = "extra_address";

    // ── Editable views ───────────────────────────────────────────────────────
    private TextInputLayout tilFullName, tilContact;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etContact;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;

    // ── View-only views ──────────────────────────────────────────────────────
    private TextInputEditText etEmail, etAddress;

    // ── Original values to detect changes ────────────────────────────────────
    private String originalFullName = "";
    private String originalContact  = "";

    // ── Other ────────────────────────────────────────────────────────────────
    private MaterialButton btnSaveChanges;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        bindViews();
        setupToolbar();
        prefillFromIntent();

        btnSaveChanges.setOnClickListener(v -> attemptSave());
    }

    // =========================================================================
    //  Setup
    // =========================================================================

    private void bindViews() {
        // Editable
        tilFullName        = findViewById(R.id.tilFullName);
        tilContact         = findViewById(R.id.tilContact);
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword     = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etContact          = findViewById(R.id.etContact);
        etCurrentPassword  = findViewById(R.id.etCurrentPassword);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);

        // View-only
        etEmail   = findViewById(R.id.etEmail);
        etAddress = findViewById(R.id.etAddress);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /** Fill all fields from the data ProfileFragment passed via Intent extras */
    private void prefillFromIntent() {
        String fullName = getIntent().getStringExtra(EXTRA_FULL_NAME);
        String contact  = getIntent().getStringExtra(EXTRA_CONTACT);
        String email    = getIntent().getStringExtra(EXTRA_EMAIL);
        String address  = getIntent().getStringExtra(EXTRA_ADDRESS);

        if (fullName != null) {
            etFullName.setText(fullName);
            originalFullName = fullName.trim();
        }
        if (contact != null) {
            etContact.setText(contact);
            originalContact = contact.trim();
        }
        if (email   != null) etEmail.setText(email);       // view-only
        if (address != null) etAddress.setText(address);   // view-only
    }

    // =========================================================================
    //  Validation & Save flow
    // =========================================================================

    private void attemptSave() {
        clearErrors();

        String fullName    = getText(etFullName);
        String contact     = getText(etContact);
        String currentPass = getText(etCurrentPassword);
        String newPass     = getText(etNewPassword);
        String confirmPass = getText(etConfirmPassword);

        // ── Detect what actually changed ───────────────────────────────────
        boolean nameChanged     = !fullName.equals(originalFullName);
        boolean contactChanged  = !contact.equals(originalContact);
        boolean passwordChanged = !newPass.isEmpty();

        // ── Nothing changed at all → inform user and stop ──────────────────
        if (!nameChanged && !contactChanged && !passwordChanged) {
            Toast.makeText(this, "No changes made.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean valid = true;

        // ── Full name must not be empty ────────────────────────────────────
        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            valid = false;
        }

        // ── Contact: optional but must be valid PH format if filled ────────
        if (!contact.isEmpty() && !contact.matches("^(09|\\+639)\\d{9}$")) {
            tilContact.setError("Enter a valid PH number (09XXXXXXXXX)");
            valid = false;
        }

        // ── Password section: only validate when user wants to change it ───
        if (passwordChanged) {
            // Current password is required ONLY when setting a new password
            if (currentPass.isEmpty()) {
                tilCurrentPassword.setError("Current password is required to set a new password");
                valid = false;
            }
            if (newPass.length() < 6) {
                tilNewPassword.setError("Password must be at least 6 characters");
                valid = false;
            }
            if (!newPass.equals(confirmPass)) {
                tilConfirmPassword.setError("Passwords do not match");
                valid = false;
            }
        }

        if (!valid) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this,
                    "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        if (passwordChanged) {
            // Re-authenticate first because we need to change the password
            AuthCredential credential =
                    EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid ->
                            saveProfileToFirestore(user, fullName, contact, newPass))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        tilCurrentPassword.setError("Incorrect password");
                    });
        } else {
            // Name / contact change only — no re-auth needed
            saveProfileToFirestore(user, fullName, contact, "");
        }
    }

    // =========================================================================
    //  Firestore update
    // =========================================================================

    private void saveProfileToFirestore(FirebaseUser user,
                                        String fullName,
                                        String contact,
                                        String newPass) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName",      fullName);
        updates.put("contactNumber", contact);
        updates.put("updatedAt",     Timestamp.now());

        db.collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!newPass.isEmpty()) {
                        // Also update Firebase Auth password
                        user.updatePassword(newPass)
                                .addOnSuccessListener(a -> onSaveSuccess())
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this,
                                            "Profile saved but password update failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        onSaveSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onSaveSuccess() {
        setLoading(false);
        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK); // ProfileFragment will reload on RESULT_OK
        finish();
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilContact.setError(null);
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnSaveChanges.setEnabled(!loading);
        btnSaveChanges.setText(loading ? "Saving…" : "Save Changes");
    }
}