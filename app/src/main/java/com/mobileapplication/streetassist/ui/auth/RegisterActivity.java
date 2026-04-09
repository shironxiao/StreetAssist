package com.mobileapplication.streetassist.ui.auth;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.mobileapplication.streetassist.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvTermsLink, tvLoginPrompt;
    private ImageButton btnBack;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerRegion, spinnerProvince, spinnerCity, spinnerBarangay;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── PSGC state ───────────────────────────────────────────────────────────
    private final Map<String, String> regionCodeMap   = new HashMap<>();
    private final Map<String, String> provinceCodeMap = new HashMap<>();
    private final Map<String, String> cityCodeMap     = new HashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final String BASE_URL = "https://psgc.cloud/api";

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Views
        cbTerms           = findViewById(R.id.cbTerms);
        btnRegister       = findViewById(R.id.btnRegister);
        tvTermsLink       = findViewById(R.id.tvTermsLink);
        tvLoginPrompt     = findViewById(R.id.tvLoginPrompt);
        btnBack           = findViewById(R.id.btnBack);
        etFullName        = findViewById(R.id.etFullName);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRegion     = findViewById(R.id.spinnerRegion);
        spinnerProvince   = findViewById(R.id.spinnerProvince);
        spinnerCity       = findViewById(R.id.spinnerCity);
        spinnerBarangay   = findViewById(R.id.spinnerBarangay);

        // Disable dependent dropdowns until parent is selected
        spinnerProvince.setEnabled(false);
        spinnerCity.setEnabled(false);
        spinnerBarangay.setEnabled(false);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupTermsSpannable();
        setupLoginSpannable();
        loadRegions();

        // ── Cascade: Region → Province ───────────────────────────────────────
        spinnerRegion.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRegion = (String) parent.getItemAtPosition(position);
            String regionCode     = regionCodeMap.get(selectedRegion);

            // Reset all dependent fields
            spinnerProvince.setText("", false);
            spinnerCity.setText("", false);
            spinnerBarangay.setText("", false);
            spinnerProvince.setEnabled(false);
            spinnerCity.setEnabled(false);
            spinnerBarangay.setEnabled(false);
            provinceCodeMap.clear();
            cityCodeMap.clear();

            if (regionCode != null) loadProvinces(regionCode);
        });

        // ── Cascade: Province → City ─────────────────────────────────────────
        spinnerProvince.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvince = (String) parent.getItemAtPosition(position);
            String provinceCode     = provinceCodeMap.get(selectedProvince);

            spinnerCity.setText("", false);
            spinnerBarangay.setText("", false);
            spinnerCity.setEnabled(false);
            spinnerBarangay.setEnabled(false);
            cityCodeMap.clear();

            if (provinceCode != null) loadCities(provinceCode);
        });

        // ── Cascade: City → Barangay ─────────────────────────────────────────
        spinnerCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            String cityCode     = cityCodeMap.get(selectedCity);

            spinnerBarangay.setText("", false);
            spinnerBarangay.setEnabled(false);

            if (cityCode != null) loadBarangays(cityCode);
        });

        // ── Register ─────────────────────────────────────────────────────────
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) registerUser();
        });
    }

    // =========================================================================
    //  PSGC API calls
    // =========================================================================

    /** Step 1 – load all regions */
    private void loadRegions() {
        executor.execute(() -> {
            try {
                JSONArray arr      = fetchJsonArray(BASE_URL + "/regions");
                List<String> names = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name    = obj.getString("name");
                    String code    = obj.getString("code");
                    names.add(name);
                    regionCodeMap.put(name, code);
                }

                runOnUiThread(() -> setAdapter(spinnerRegion, names, true));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Failed to load regions: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** Step 2 – load provinces for the chosen region */
    private void loadProvinces(String regionCode) {
        runOnUiThread(() -> spinnerProvince.setHint("Loading…"));

        executor.execute(() -> {
            try {
                JSONArray arr      = fetchJsonArray(
                        BASE_URL + "/regions/" + regionCode + "/provinces");
                List<String> names = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name    = obj.getString("name");
                    String code    = obj.getString("code");
                    names.add(name);
                    provinceCodeMap.put(name, code);
                }

                runOnUiThread(() -> {
                    setAdapter(spinnerProvince, names, true);
                    spinnerProvince.setEnabled(true);
                    spinnerProvince.setHint("Select province");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    spinnerProvince.setHint("Select province");
                    Toast.makeText(this,
                            "Failed to load provinces: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** Step 3 – load cities/municipalities for the chosen province */
    private void loadCities(String provinceCode) {
        runOnUiThread(() -> spinnerCity.setHint("Loading…"));

        executor.execute(() -> {
            try {
                JSONArray arr      = fetchJsonArray(
                        BASE_URL + "/provinces/" + provinceCode + "/cities-municipalities");
                List<String> names = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name    = obj.getString("name");
                    String code    = obj.getString("code");
                    names.add(name);
                    cityCodeMap.put(name, code);
                }

                runOnUiThread(() -> {
                    setAdapter(spinnerCity, names, true);
                    spinnerCity.setEnabled(true);
                    spinnerCity.setHint("Select city / municipality");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    spinnerCity.setHint("Select city / municipality");
                    Toast.makeText(this,
                            "Failed to load cities: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** Step 4 – load barangays for the chosen city/municipality */
    private void loadBarangays(String cityCode) {
        runOnUiThread(() -> spinnerBarangay.setHint("Loading…"));

        executor.execute(() -> {
            try {
                JSONArray arr      = fetchJsonArray(
                        BASE_URL + "/cities-municipalities/" + cityCode + "/barangays");
                List<String> names = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    names.add(arr.getJSONObject(i).getString("name"));
                }

                runOnUiThread(() -> {
                    setAdapter(spinnerBarangay, names, false);
                    spinnerBarangay.setEnabled(true);
                    spinnerBarangay.setHint("Select barangay");
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    spinnerBarangay.setHint("Select barangay");
                    Toast.makeText(this,
                            "Failed to load barangays: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private JSONArray fetchJsonArray(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + status);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        return new JSONArray(sb.toString());
    }

    private void setAdapter(AutoCompleteTextView view,
                            List<String> items,
                            boolean filter) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, items);
        view.setAdapter(adapter);
        view.setThreshold(filter ? 1 : 0);
    }

    // =========================================================================
    //  Validation
    // =========================================================================

    private boolean validateInputs() {
        String fullName        = etFullName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String password        = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        String region          = spinnerRegion.getText().toString().trim();
        String province        = spinnerProvince.getText().toString().trim();
        String city            = spinnerCity.getText().toString().trim();
        String barangay        = spinnerBarangay.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return false;
        }
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
        if (region.isEmpty()) {
            spinnerRegion.setError("Please select a region");
            spinnerRegion.requestFocus();
            return false;
        }
        if (province.isEmpty()) {
            spinnerProvince.setError("Please select a province");
            spinnerProvince.requestFocus();
            return false;
        }
        if (city.isEmpty()) {
            spinnerCity.setError("Please select a city / municipality");
            spinnerCity.requestFocus();
            return false;
        }
        if (barangay.isEmpty()) {
            spinnerBarangay.setError("Please select a barangay");
            spinnerBarangay.requestFocus();
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
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this,
                    "You must accept the Terms and Privacy Policy to continue",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // =========================================================================
    //  Firebase – Auth + Firestore
    // =========================================================================

    private void registerUser() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account…");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) saveUserToFirestore(user.getUid());
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Account");
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed. Please try again.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid) {
        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String region   = spinnerRegion.getText().toString().trim();
        String province = spinnerProvince.getText().toString().trim();
        String city     = spinnerCity.getText().toString().trim();
        String barangay = spinnerBarangay.getText().toString().trim();

        String regionCode   = regionCodeMap.get(region);
        String provinceCode = provinceCodeMap.get(province);
        String cityCode     = cityCodeMap.get(city);

        Map<String, Object> address = new HashMap<>();
        address.put("region",       region);
        address.put("regionCode",   regionCode);
        address.put("province",     province);
        address.put("provinceCode", provinceCode);
        address.put("city",         city);
        address.put("cityCode",     cityCode);
        address.put("barangay",     barangay);

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid",             uid);
        userDoc.put("fullName",        fullName);
        userDoc.put("email",           email);
        userDoc.put("address",         address);
        userDoc.put("role",            "resident");
        userDoc.put("isVerified",      false);
        userDoc.put("isActive",        true);
        userDoc.put("profilePhotoUrl", null);
        userDoc.put("createdAt",       Timestamp.now());
        userDoc.put("updatedAt",       Timestamp.now());

        db.collection("users")
                .document(uid)
                .set(userDoc)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT).show();
                    // TODO: navigate to MainActivity
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Roll back auth account so the user can retry cleanly
                    FirebaseUser current = mAuth.getCurrentUser();
                    if (current != null) current.delete();

                    btnRegister.setEnabled(true);
                    btnRegister.setText("Create Account");
                    Toast.makeText(this,
                            "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // =========================================================================
    //  Spannables & Terms dialog
    // =========================================================================

    private void setupLoginSpannable() {
        String text = "Already have an account? Log in";
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new ClickableSpan() {
            @Override public void onClick(@NonNull View w) { finish(); }
            @Override public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        }, 25, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLoginPrompt.setText(ss);
        tvLoginPrompt.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupTermsSpannable() {
        String text = "I accept the terms and privacy policy";
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new ClickableSpan() {
            @Override public void onClick(@NonNull View w) { showTermsDialog(); }
            @Override public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        }, 13, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTermsLink.setText(ss);
        tvTermsLink.setMovementMethod(LinkMovementMethod.getInstance());
        tvTermsLink.setHighlightColor(Color.TRANSPARENT);
    }

    private void showTermsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Terms & Privacy Policy")
                .setMessage(
                        "Terms of Service – StreetAssist\n" +
                                "By using StreetAssist you agree to:\n\n" +
                                "1. Purpose – report homelessness concerns.\n" +
                                "2. User Responsibility – provide accurate info; no false reports.\n" +
                                "3. Appropriate Use – no harassment.\n" +
                                "4. Availability – student project, may change.\n" +
                                "5. Acceptance – registering means you agree.\n\n" +
                                "──────────────────────\n\n" +
                                "Privacy Policy – StreetAssist\n" +
                                "1. We collect: Name, Email, Location, Reports.\n" +
                                "2. Used for identification and reporting.\n" +
                                "3. Not shared with third parties.\n" +
                                "4. Location used only during report submission.\n" +
                                "5. Questions? streetassist.support@example.com")
                .setPositiveButton("I Understand", (d, w) -> d.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}