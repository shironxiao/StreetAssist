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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvTermsLink, tvLoginPrompt;
    private ImageButton btnBack;
    private TextInputEditText etFullName, etEmail, etContactNumber, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerCity, spinnerBarangay;
    private boolean hasReadTerms = false;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Fixed values ──────────────────────────────────────────────────────────
    private static final String FIXED_REGION   = "REGION V (Bicol Region)";
    private static final String FIXED_PROVINCE = "Camarines Norte";

    // ── Hardcoded PSGC data for Camarines Norte ───────────────────────────────
    private static final Map<String, List<String>> CITY_BARANGAY_MAP = new HashMap<>();

    static {
        CITY_BARANGAY_MAP.put("Basud", Arrays.asList(
                "Aguit-it", "Backong", "Bagaobawan", "Calangcawan Norte", "Calangcawan Sur",
                "Culayculay", "Dagang", "Gahonon", "Gubat Norte", "Gubat Sur",
                "Ignit", "Kaibigan", "Langa-langa", "Laniton", "Lastic",
                "Mabini", "Manlimonsito", "Matango", "Mocong", "Oloapaen",
                "Ombao Heights", "Ombao Tibang", "Omboy", "Pagsangahan", "Pambuhan",
                "Pinagwarasan", "Plaridel", "Poblacion", "Salvacion", "San Isidro",
                "San Roque", "Santa Rosa Norte", "Santa Rosa Sur", "Taba-taba",
                "Tacad", "Taisan", "Tambongon", "Tenerife", "Yapak"
        ));

        CITY_BARANGAY_MAP.put("Capalonga", Arrays.asList(
                "Alayao", "Binawangan", "Calabaca", "Calagbagang", "Catabaguangan",
                "Catioan", "Del Pilar", "Gilong", "Guayabo", "Ligñon",
                "Mabini", "Magsaysay", "Mantalongon", "Milagrosa", "Plaridel",
                "Poblacion", "Quirino", "Roosevelt", "Salvacion", "San Antonio",
                "San Francisco", "San Isidro", "Santa Cruz", "Santa Elena",
                "Santa Maria", "Santo Niño", "Sinagapos", "Vista Hermosa"
        ));

        CITY_BARANGAY_MAP.put("Daet", Arrays.asList(
                "Alawihao", "Awitan", "Bagasbas", "Barangay I (Pob.)", "Barangay II (Pob.)",
                "Barangay III (Pob.)", "Barangay IV (Pob.)", "Barangay V (Pob.)",
                "Barangay VI (Pob.)", "Barangay VII (Pob.)", "Barangay VIII (Pob.)",
                "Bibirao", "Borabod", "Calasgasan", "Camambugan", "Cobangbang (Sto. Niño)",
                "Dogongan", "Garcia", "Gahonon", "Gubat", "Lag-on",
                "Lucrecia", "Lag-on", "Magang", "Mancruz (San Juan)",
                "Pamorangon", "San Isidro"
        ));

        CITY_BARANGAY_MAP.put("San Lorenzo Ruiz", Arrays.asList(
                "Alegria", "Anahawan", "Anonang", "Bagong Silang", "Calangcawan",
                "Guinabonan", "Iligan", "Inductan", "Km. 891 Pob. (Tulay)",
                "Lamon", "Mabilo I", "Mabilo II", "Nakalaya", "Northern Poblacion",
                "Placer", "Salvacion", "San Antonio", "San Francisco", "San Isidro",
                "San Jose", "San Martin", "San Pedro", "Santa Cruz",
                "Santa Elena", "Santiago", "Southern Poblacion", "Talahib",
                "Talisay", "Tamban", "Tambo", "Tandoc", "Tison"
        ));

        CITY_BARANGAY_MAP.put("San Vicente", Arrays.asList(
                "Bugtong na Pulo", "Calwit", "Labnig", "Mabini", "Madlawon",
                "Pag-asa", "Poblacion", "San Antonio", "San Francisco",
                "San Isidro", "San Ramon", "Santa Cruz", "Santa Elena",
                "Santo Niño", "Taguilid"
        ));

        CITY_BARANGAY_MAP.put("Santa Elena", Arrays.asList(
                "Angga", "Bactas", "Binanwaanan", "Bulhao", "Busak",
                "Caawigan", "Caayunan", "Calabaca", "Calagbagang", "Calaocan",
                "Camambugan", "Candawan", "Catabaguangan", "Cataroan", "Caugmayan",
                "Cayucay", "Del Pilar", "Guadalupe", "Hawak", "Itulan",
                "Laniton", "Lastic", "Mabini", "Magsaysay",
                "Manlimonsito", "Matango", "Mocong", "Oloapaen",
                "Pagsangahan", "Pambuhan", "Pinagwarasan", "Plaridel",
                "Poblacion", "Puro", "Salvacion", "San Antonio",
                "San Francisco", "San Isidro", "San Jose", "San Martin",
                "San Miguel", "San Pedro", "San Ramon", "San Roque",
                "Santa Cruz", "Santa Elena", "Santo Niño", "Tacad",
                "Taisan", "Talisay", "Tambongon", "Tenerife"
        ));

        CITY_BARANGAY_MAP.put("Jose Panganiban", Arrays.asList(
                "Bagong Bayan", "Calero", "Dahican", "Dayhagan", "Estacion",
                "Lag-on", "Larap", "Loreña", "Luyos", "Mabini",
                "Mabungabon", "Managpi", "Manaringon", "Mercedes", "Napaod",
                "Parang", "Placer", "Poblacion I", "Poblacion II", "Poblacion III",
                "Port Junction Norte", "Port Junction Sur", "Santa Milagrosa",
                "Tacay", "Tambo", "Trinidad", "Viñas", "Wawa"
        ));

        CITY_BARANGAY_MAP.put("Labo", Arrays.asList(
                "Abella", "Agusigin", "Balangcawan Norte", "Balangcawan Sur", "Balite",
                "Bautista", "Bayabas", "Bena", "Binanuahan East", "Binanuahan West",
                "Bulacan", "Caayunan", "Calibunan", "Camambugan", "Candawan",
                "Capalogan", "Catabaguangan", "Catioan", "Codon", "Colacling",
                "Colomio", "Corucao", "Del Pilar", "Gahonon", "Guadalupe",
                "Guinabonan", "Herrera", "Hoyohoy", "Imelda", "Inauayan",
                "J. Milan (Catanggalan)", "Kaibigan", "Lag-on", "Lictingtung",
                "Ligñon", "Lumbangan", "Luna Norte", "Luna Sur", "Mabini",
                "Mabolo", "Macabug", "Magang", "Magsaysay", "Manuangan",
                "Maria", "Masalong Norte", "Masalong Sur", "Mataque", "Mercedes",
                "Napaod", "Niabonan", "Obaliw Recto", "Ocampo", "Ola Norte",
                "Ola Sur", "Osmeña", "Oyon", "Pag-asa", "Palong",
                "Pancucuran", "Pawili", "Plaridel", "Poblacion", "Pola",
                "Pood", "Quezon", "Quirino", "Roosevelt", "Rosario",
                "Salvacion", "San Antonio Norte", "San Antonio Sur", "San Isidro",
                "San Lorenzo", "San Miguel", "San Pablo Norte", "San Pablo Sur",
                "San Patricio Norte", "San Patricio Sur", "San Ramon",
                "San Vicente", "Santa Cruz", "Sapang Palay", "Sumaoy",
                "Tamban", "Tulay", "Tungmalaong", "Vega", "Villasol"
        ));

        CITY_BARANGAY_MAP.put("Mercedes", Arrays.asList(
                "Apuao", "Barangay I (Pob.)", "Barangay II (Pob.)", "Barangay III (Pob.)",
                "Barangay IV (Pob.)", "Barangay V (Pob.)", "Barangay VI (Pob.)",
                "Barangay VII (Pob.)", "Boot", "Casagsagan", "Comadaycaday",
                "Comadogcadog", "Daculang Bolo", "Daguit", "Danao",
                "Guayabo", "Himanag", "Lagha", "Lanot", "Lañgon",
                "Lañgon", "Libas", "Mabini", "Macolabo Island", "Malinis",
                "Maot", "Masikla", "Matnog", "Mobo", "Nacawit",
                "Pambuhan", "Patag", "Patrol", "Quinapaguian", "Salingogon",
                "Sirangan", "Taba", "Tawig", "Tugos", "Yabo"
        ));

        CITY_BARANGAY_MAP.put("Paracale", Arrays.asList(
                "Awitan", "Bagumbayan", "Bakal Norte", "Bakal Sur", "Batobalani",
                "Calaburnay", "Capacuan", "Casagsagan", "Caypandan", "Colasi",
                "Gahonon", "Guinabonan", "Jose Panganiban", "Lag-on", "Larap",
                "Luklukan Norte", "Luklukan Sur", "Mabini", "Madlawon",
                "Mananao", "Mancuartira", "Mangkasuy", "Maot", "Masalong",
                "Minalabac", "Nakalaya", "Norte", "Obo", "Pag-asa",
                "Pangarairan", "Peñafrancia", "Poblacion", "Tabugon",
                "Tagas", "Talisay", "Tambong", "Tigbinan", "Tulay Na Lupa"
        ));

        CITY_BARANGAY_MAP.put("Talisay", Arrays.asList(
                "Bagong Bayan", "Bautista", "Calasag", "Catagbacan", "Codon",
                "Hampas", "Laniton", "Limaong", "Mabini", "Magang",
                "Mataque", "Maugat East", "Maugat West", "Pag-asa", "Poblacion",
                "Salvacion", "San Antonio", "San Isidro", "San Jose",
                "San Miguel", "San Pablo", "San Roque", "Santa Cruz",
                "Santo Niño", "Tapihan", "Tulatula"
        ));

        CITY_BARANGAY_MAP.put("Vinzons", Arrays.asList(
                "Alaban", "Algaran", "Balagba", "Binobong", "Burabod",
                "Cagbanaba", "Calabagas", "Calangcawan Norte", "Calangcawan Sur",
                "Cawayan Pola", "Cawayan Sapa", "Colasi", "Del Pilar",
                "Gubat Norte", "Gubat Sur", "Himaao", "Indangan",
                "La Purisima", "Labo", "Laga", "Mabini", "Masalong",
                "Maulawin", "Nakalaya", "Pag-asa", "Pambuhan", "Pinit",
                "Pob. I (Barangay I)", "Pob. II (Barangay II)", "Pob. III (Barangay III)",
                "Pob. IV (Barangay IV)", "Potot", "Sabang", "Salvacion",
                "San Antonio", "San Francisco", "San Isidro", "San Jose",
                "San Pascual", "Santa Cruz", "Santo Niño", "Taisan",
                "Tambongon", "Tulay Na Lupa"
        ));

        CITY_BARANGAY_MAP.put("Tulay Na Lupa", Arrays.asList(
                "Calabasa", "Mabini", "Pag-asa (Pob.)", "Poblacion",
                "San Antonio", "San Francisco", "San Isidro", "San Jose",
                "Santa Cruz", "Santa Elena", "Santo Niño", "Villa Aurora",
                "Villa Hermosa"
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Bind views
        cbTerms           = findViewById(R.id.cbTerms);
        btnRegister       = findViewById(R.id.btnRegister);
        tvTermsLink       = findViewById(R.id.tvTermsLink);
        tvLoginPrompt     = findViewById(R.id.tvLoginPrompt);
        btnBack           = findViewById(R.id.btnBack);
        etFullName        = findViewById(R.id.etFullName);
        etEmail           = findViewById(R.id.etEmail);
        etContactNumber   = findViewById(R.id.etContactNumber);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerCity       = findViewById(R.id.spinnerCity);
        spinnerBarangay   = findViewById(R.id.spinnerBarangay);

        spinnerBarangay.setEnabled(false);
        
        // Prevent checking terms unless they've been read
        cbTerms.setOnClickListener(v -> {
            if (!hasReadTerms) {
                cbTerms.setChecked(false);
                Toast.makeText(this, "Please read the Terms and Privacy Policy first.", Toast.LENGTH_SHORT).show();
                showTermsDialog();
            }
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupTermsSpannable();
        setupLoginSpannable();

        // Populate cities immediately — no network call needed
        List<String> cities = Arrays.asList(
                "Basud", "Capalonga", "Daet", "Jose Panganiban", "Labo",
                "Mercedes", "Paracale", "San Lorenzo Ruiz", "San Vicente",
                "Santa Elena", "Talisay", "Tulay Na Lupa", "Vinzons"
        );
        setAdapter(spinnerCity, cities, true);
        spinnerCity.setEnabled(true);

        // City → Barangay cascade
        spinnerCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);

            spinnerBarangay.setText("", false);
            spinnerBarangay.setEnabled(false);

            List<String> barangays = CITY_BARANGAY_MAP.get(selectedCity);
            if (barangays != null) {
                setAdapter(spinnerBarangay, barangays, false);
                spinnerBarangay.setEnabled(true);
                spinnerBarangay.setHint("Select barangay");
            }
        });

        // Register button
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) registerUser();
        });
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private void setAdapter(AutoCompleteTextView view, List<String> items, boolean filter) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, items);
        view.setAdapter(adapter);
        view.setThreshold(filter ? 1 : 0);
    }

    // =========================================================================
    //  Validation
    // =========================================================================

    private boolean validateInputs() {
        String fullName      = etFullName.getText().toString().trim();
        String email         = etEmail.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String password      = etPassword.getText().toString();
        String confirmPass   = etConfirmPassword.getText().toString();
        String city          = spinnerCity.getText().toString().trim();
        String barangay      = spinnerBarangay.getText().toString().trim();

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
        if (contactNumber.isEmpty()) {
            etContactNumber.setError("Contact number is required");
            etContactNumber.requestFocus();
            return false;
        }
        if (!contactNumber.matches("^09\\d{9}$")) {
            etContactNumber.setError("Enter a valid 11-digit number starting with 09 (e.g. 09XXXXXXXXX)");
            etContactNumber.requestFocus();
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
        if (!password.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!hasReadTerms) {
            Toast.makeText(this, "Please read the Terms and Privacy Policy first.", Toast.LENGTH_LONG).show();
            showTermsDialog();
            return false;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "You must accept the Terms and Privacy Policy to continue.", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // =========================================================================
    //  Firebase — Auth + Firestore
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
        String fullName      = etFullName.getText().toString().trim();
        String email         = etEmail.getText().toString().trim();
        String contactNumber = etContactNumber.getText().toString().trim();
        String city          = spinnerCity.getText().toString().trim();
        String barangay      = spinnerBarangay.getText().toString().trim();

        Map<String, Object> address = new HashMap<>();
        address.put("region",   FIXED_REGION);
        address.put("province", FIXED_PROVINCE);
        address.put("city",     city);
        address.put("barangay", barangay);

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid",             uid);
        userDoc.put("fullName",        fullName);
        userDoc.put("email",           email);
        userDoc.put("contactNumber",   contactNumber);
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
                    
                    // Sign out so user has to log in manually
                    mAuth.signOut();

                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
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
            @Override
            public void onClick(@NonNull View w) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
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
                        "TERMS OF SERVICE\n\n" +
                        "1. Acceptance of Terms\n" +
                        "By creating an account or accessing StreetAssist, you agree to be bound by these Terms of Service. If you do not agree, please do not use the application.\n\n" +
                        "2. Purpose and Service Description\n" +
                        "StreetAssist is a community reporting application designed to assist in highlighting community welfare and street sighting concerns. The service is provided to facilitate reporting and community support coordination.\n\n" +
                        "3. User Conduct & Accuracy\n" +
                        "You agree to submit only truthful, accurate, and constructive information. False reporting, malicious submissions, or any form of harassment within the platform is strictly prohibited and will lead to immediate account termination.\n\n" +
                        "4. Platform Availability\n" +
                        "StreetAssist reserves the right to modify, suspend, or discontinue any aspect of the application at any time without notice.\n\n" +
                        "──────────────────────────────────\n\n" +
                        "PRIVACY POLICY\n\n" +
                        "1. Information We Collect\n" +
                        "We collect personal information necessary for identification and community service facilitation, including your full name, email address, phone number, and geographic location address when a report is submitted.\n\n" +
                        "2. Use of Data\n" +
                        "Your information is utilized solely to manage user accounts, verify reporting details, and coordinate sightings. Location data is only accessed when you actively pin or submit a sighting.\n\n" +
                        "3. Information Sharing\n" +
                        "We do not sell, trade, or distribute your personal data to third-parties. Information may only be disclosed if required by law or local public safety authorities.\n\n" +
                        "4. Security Measures\n" +
                        "We implement standard technical and organizational security measures to protect your personal data from unauthorized access, loss, or alteration.\n")
                .setPositiveButton("I Have Read & Understand", (d, w) -> {
                    hasReadTerms = true;
                    cbTerms.setEnabled(true);
                    cbTerms.setChecked(true);
                    d.dismiss();
                })
                .show();
    }
}