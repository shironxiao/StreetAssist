package com.mobileapplication.streetassist.ui.resident.Profile;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileEdit extends AppCompatActivity {

    // ── Intent extra keys ───────────────────────────────────────────────────
    public static final String EXTRA_FULL_NAME = "extra_full_name";
    public static final String EXTRA_CONTACT   = "extra_contact";
    public static final String EXTRA_EMAIL     = "extra_email";

    // ── PSGC Data (Camarines Norte) ──────────────────────────────────────────
    private static final Map<String, List<String>> CITY_BARANGAY_MAP = new HashMap<>();
    static {
        CITY_BARANGAY_MAP.put("Basud", Arrays.asList("Aguit-it", "Backong", "Bagaobawan", "Calangcawan Norte", "Calangcawan Sur", "Culayculay", "Dagang", "Gahonon", "Gubat Norte", "Gubat Sur", "Ignit", "Kaibigan", "Langa-langa", "Laniton", "Lastic", "Mabini", "Manlimonsito", "Matango", "Mocong", "Oloapaen", "Ombao Heights", "Ombao Tibang", "Omboy", "Pagsangahan", "Pambuhan", "Pinagwarasan", "Plaridel", "Poblacion", "Salvacion", "San Isidro", "San Roque", "Santa Rosa Norte", "Santa Rosa Sur", "Taba-taba", "Tacad", "Taisan", "Tambongon", "Tenerife", "Yapak"));
        CITY_BARANGAY_MAP.put("Capalonga", Arrays.asList("Alayao", "Binawangan", "Calabaca", "Calagbagang", "Catabaguangan", "Catioan", "Del Pilar", "Gilong", "Guayabo", "Ligñon", "Mabini", "Magsaysay", "Mantalongon", "Milagrosa", "Plaridel", "Poblacion", "Quirino", "Roosevelt", "Salvacion", "San Antonio", "San Francisco", "San Isidro", "Santa Cruz", "Santa Elena", "Santa Maria", "Santo Niño", "Sinagapos", "Vista Hermosa"));
        CITY_BARANGAY_MAP.put("Daet", Arrays.asList("Alawihao", "Awitan", "Bagasbas", "Barangay I (Pob.)", "Barangay II (Pob.)", "Barangay III (Pob.)", "Barangay IV (Pob.)", "Barangay V (Pob.)", "Barangay VI (Pob.)", "Barangay VII (Pob.)", "Barangay VIII (Pob.)", "Bibirao", "Borabod", "Calasgasan", "Camambugan", "Cobangbang (Sto. Niño)", "Dogongan", "Garcia", "Gahonon", "Gubat", "Lag-on", "Lucrecia", "Magang", "Mancruz (San Juan)", "Pamorangon", "San Isidro"));
        CITY_BARANGAY_MAP.put("Jose Panganiban", Arrays.asList("Bagong Bayan", "Calero", "Dahican", "Dayhagan", "Estacion", "Lag-on", "Larap", "Loreña", "Luyos", "Mabini", "Mabungabon", "Managpi", "Manaringon", "Mercedes", "Napaod", "Parang", "Placer", "Poblacion I", "Poblacion II", "Poblacion III", "Port Junction Norte", "Port Junction Sur", "Santa Milagrosa", "Tacay", "Tambo", "Trinidad", "Viñas", "Wawa"));
        CITY_BARANGAY_MAP.put("Labo", Arrays.asList("Abella", "Agusigin", "Balangcawan Norte", "Balangcawan Sur", "Balite", "Bautista", "Bayabas", "Bena", "Binanuahan East", "Binanuahan West", "Bulacan", "Caayunan", "Calibunan", "Camambugan", "Candawan", "Capalogan", "Catabaguangan", "Catioan", "Codon", "Colacling", "Colomio", "Corucao", "Del Pilar", "Gahonon", "Guadalupe", "Guinabonan", "Herrera", "Hoyohoy", "Imelda", "Inauayan", "J. Milan (Catanggalan)", "Kaibigan", "Lag-on", "Lictingtung", "Ligñon", "Lumbangan", "Luna Norte", "Luna Sur", "Mabini", "Mabolo", "Macabug", "Magang", "Magsaysay", "Manuangan", "Maria", "Masalong Norte", "Masalong Sur", "Mataque", "Mercedes", "Napaod", "Niabonan", "Obaliw Recto", "Ocampo", "Ola Norte", "Ola Sur", "Osmeña", "Oyon", "Pag-asa", "Palong", "Pancucuran", "Pawili", "Plaridel", "Poblacion", "Pola", "Pood", "Quezon", "Quirino", "Roosevelt", "Rosario", "Salvacion", "San Antonio Norte", "San Antonio Sur", "San Isidro", "San Lorenzo", "San Miguel", "San Pablo Norte", "San Pablo Sur", "San Patricio Norte", "San Patricio Sur", "San Ramon", "San Vicente", "Santa Cruz", "Sapang Palay", "Sumaoy", "Tamban", "Tulay", "Tungmalaong", "Vega", "Villasol"));
        CITY_BARANGAY_MAP.put("Mercedes", Arrays.asList("Apuao", "Barangay I (Pob.)", "Barangay II (Pob.)", "Barangay III (Pob.)", "Barangay IV (Pob.)", "Barangay V (Pob.)", "Barangay VI (Pob.)", "Barangay VII (Pob.)", "Boot", "Casagsagan", "Comadaycaday", "Comadogcadog", "Daculang Bolo", "Daguit", "Danao", "Guayabo", "Himanag", "Lagha", "Lanot", "Lañgon", "Libas", "Mabini", "Macolabo Island", "Malinis", "Maot", "Masikla", "Matnog", "Mobo", "Nacawit", "Pambuhan", "Patag", "Patrol", "Quinapaguian", "Salingogon", "Sirangan", "Taba", "Tawig", "Tugos", "Yabo"));
        CITY_BARANGAY_MAP.put("Paracale", Arrays.asList("Awitan", "Bagumbayan", "Bakal Norte", "Bakal Sur", "Batobalani", "Calaburnay", "Capacuan", "Casagsagan", "Caypandan", "Colasi", "Gahonon", "Guinabonan", "Jose Panganiban", "Lag-on", "Larap", "Luklukan Norte", "Luklukan Sur", "Mabini", "Madlawon", "Mananao", "Mancuartira", "Mangkasuy", "Maot", "Masalong", "Minalabac", "Nakalaya", "Norte", "Obo", "Pag-asa", "Pangarairan", "Peñafrancia", "Poblacion", "Tabugon", "Tagas", "Talisay", "Tambong", "Tigbinan", "Tulay Na Lupa"));
        CITY_BARANGAY_MAP.put("San Lorenzo Ruiz", Arrays.asList("Alegria", "Anahawan", "Anonang", "Bagong Silang", "Calangcawan", "Guinabonan", "Iligan", "Inductan", "Km. 891 Pob. (Tulay)", "Lamon", "Mabilo I", "Mabilo II", "Nakalaya", "Northern Poblacion", "Placer", "Salvacion", "San Antonio", "San Francisco", "San Isidro", "San Jose", "San Martin", "San Pedro", "Santa Cruz", "Santa Elena", "Santiago", "Southern Poblacion", "Talahib", "Talisay", "Tamban", "Tambo", "Tandoc", "Tison"));
        CITY_BARANGAY_MAP.put("San Vicente", Arrays.asList("Bugtong na Pulo", "Calwit", "Labnig", "Mabini", "Madlawon", "Pag-asa", "Poblacion", "San Antonio", "San Francisco", "San Isidro", "San Ramon", "Santa Cruz", "Santa Elena", "Santo Niño", "Taguilid"));
        CITY_BARANGAY_MAP.put("Santa Elena", Arrays.asList("Angga", "Bactas", "Binanwaanan", "Bulhao", "Busak", "Caawigan", "Caayunan", "Calabaca", "Calagbagang", "Calaocan", "Camambugan", "Candawan", "Catabaguangan", "Cataroan", "Caugmayan", "Cayucay", "Del Pilar", "Guadalupe", "Hawak", "Itulan", "Laniton", "Lastic", "Mabini", "Magsaysay", "Manlimonsito", "Matango", "Mocong", "Oloapaen", "Pagsangahan", "Pambuhan", "Pinagwarasan", "Plaridel", "Poblacion", "Puro", "Salvacion", "San Antonio", "San Francisco", "San Isidro", "San Jose", "San Martin", "San Miguel", "San Pedro", "San Ramon", "San Roque", "Santa Cruz", "Santa Elena", "Santo Niño", "Tacad", "Taisan", "Talisay", "Tambongon", "Tenerife"));
        CITY_BARANGAY_MAP.put("Talisay", Arrays.asList("Bagong Bayan", "Bautista", "Calasag", "Catagbacan", "Codon", "Hampas", "Laniton", "Limaong", "Mabini", "Magang", "Mataque", "Maugat East", "Maugat West", "Pag-asa", "Poblacion", "Salvacion", "San Antonio", "San Isidro", "San Jose", "San Miguel", "San Pablo", "San Roque", "Santa Cruz", "Santo Niño", "Tapihan", "Tulatula"));
        CITY_BARANGAY_MAP.put("Vinzons", Arrays.asList("Alaban", "Algaran", "Balagba", "Binobong", "Burabod", "Cagbanaba", "Calabagas", "Calangcawan Norte", "Calangcawan Sur", "Cawayan Pola", "Cawayan Sapa", "Colasi", "Del Pilar", "Gubat Norte", "Gubat Sur", "Himaao", "Indangan", "La Purisima", "Labo", "Laga", "Mabini", "Masalong", "Maulawin", "Nakalaya", "Pag-asa", "Pambuhan", "Pinit", "Pob. I (Barangay I)", "Pob. II (Barangay II)", "Pob. III (Barangay III)", "Pob. IV (Barangay IV)", "Potot", "Sabang", "Salvacion", "San Antonio", "San Francisco", "San Isidro", "San Jose", "San Pascual", "Santa Cruz", "Santo Niño", "Taisan", "Tambongon", "Tulay Na Lupa"));
        CITY_BARANGAY_MAP.put("Tulay Na Lupa", Arrays.asList("Calabasa", "Mabini", "Pag-asa (Pob.)", "Poblacion", "San Antonio", "San Francisco", "San Isidro", "San Jose", "Santa Cruz", "Santa Elena", "Santo Niño", "Villa Aurora", "Villa Hermosa"));
    }

    // ── Views ────────────────────────────────────────────────────────────────
    private TextInputLayout tilFullName, tilContact, tilEmail;
    private TextInputLayout tilCity, tilBarangay;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etContact, etEmail;
    private AutoCompleteTextView spinnerCity, spinnerBarangay;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;

    // ── Data ─────────────────────────────────────────────────────────────────
    private String originalFullName = "";
    private String originalContact  = "";
    private String originalEmail    = "";
    private String originalCity     = "";
    private String originalBarangay = "";

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
        setupSpinners();
        prefillFromIntent();

        btnSaveChanges.setOnClickListener(v -> attemptSave());
    }

    private void bindViews() {
        tilFullName        = findViewById(R.id.tilFullName);
        tilContact         = findViewById(R.id.tilContact);
        tilEmail           = findViewById(R.id.tilEmail);
        tilCity            = findViewById(R.id.tilCity);
        tilBarangay        = findViewById(R.id.tilBarangay);
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword     = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        
        etFullName         = findViewById(R.id.etFullName);
        etContact          = findViewById(R.id.etContact);
        etEmail            = findViewById(R.id.etEmail);
        spinnerCity        = findViewById(R.id.spinnerCity);
        spinnerBarangay    = findViewById(R.id.spinnerBarangay);
        
        etCurrentPassword  = findViewById(R.id.etCurrentPassword);
        etNewPassword      = findViewById(R.id.etNewPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        List<String> cities = new ArrayList<>(CITY_BARANGAY_MAP.keySet());
        Collections.sort(cities);
        
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, cities);
        spinnerCity.setAdapter(cityAdapter);

        spinnerCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            updateBarangaySpinner(selectedCity, "");
        });
    }

    private void updateBarangaySpinner(String city, String preselect) {
        List<String> barangays = CITY_BARANGAY_MAP.get(city);
        if (barangays != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, barangays);
            spinnerBarangay.setAdapter(adapter);
            spinnerBarangay.setEnabled(true);
            if (!preselect.isEmpty()) {
                spinnerBarangay.setText(preselect, false);
            } else {
                spinnerBarangay.setText("", false);
            }
        } else {
            spinnerBarangay.setEnabled(false);
            spinnerBarangay.setText("", false);
        }
    }

    private void prefillFromIntent() {
        String fullName = getIntent().getStringExtra(EXTRA_FULL_NAME);
        String contact  = getIntent().getStringExtra(EXTRA_CONTACT);
        String email    = getIntent().getStringExtra(EXTRA_EMAIL);
        String city     = getIntent().getStringExtra("extra_city");
        String barangay = getIntent().getStringExtra("extra_barangay");

        if (fullName != null) { etFullName.setText(fullName); originalFullName = fullName.trim(); }
        if (contact  != null) { etContact.setText(contact);   originalContact  = contact.trim(); }
        if (email    != null) { etEmail.setText(email);     originalEmail    = email.trim(); }
        
        if (city != null) {
            spinnerCity.setText(city, false);
            originalCity = city.trim();
            updateBarangaySpinner(city, barangay != null ? barangay : "");
            if (barangay != null) originalBarangay = barangay.trim();
        }
    }

    private void attemptSave() {
        clearErrors();

        String fullName    = etFullName.getText().toString().trim();
        String contact     = etContact.getText().toString().trim();
        String email       = etEmail.getText().toString().trim();
        String city        = spinnerCity.getText().toString().trim();
        String barangay    = spinnerBarangay.getText().toString().trim();
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass     = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        boolean nameChanged     = !fullName.equals(originalFullName);
        boolean contactChanged  = !contact.equals(originalContact);
        boolean emailChanged    = !email.equals(originalEmail);
        boolean addressChanged  = !city.equals(originalCity) || !barangay.equals(originalBarangay);
        boolean passwordChanged = !newPass.isEmpty();

        if (!nameChanged && !contactChanged && !emailChanged && !addressChanged && !passwordChanged) {
            Toast.makeText(this, "No changes made.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fullName.isEmpty()) { tilFullName.setError("Required"); return; }
        if (email.isEmpty()) { tilEmail.setError("Required"); return; }
        if (city.isEmpty()) { tilCity.setError("Required"); return; }
        if (barangay.isEmpty()) { tilBarangay.setError("Required"); return; }
        
        if ((emailChanged || passwordChanged) && currentPass.isEmpty()) {
            tilCurrentPassword.setError("Current password required to change email or password");
            return;
        }

        if (passwordChanged) {
            if (newPass.length() < 6) { tilNewPassword.setError("Min 6 chars"); return; }
            if (!newPass.equals(confirmPass)) { tilConfirmPassword.setError("Mismatch"); return; }
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        setLoading(true);

        if (emailChanged || passwordChanged) {
            // Re-authenticate using the ORIGINAL email and the provided current password
            AuthCredential credential = EmailAuthProvider.getCredential(originalEmail, currentPass);
            
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        if (emailChanged) {
                            // verifyBeforeUpdateEmail is the recommended way in modern Firebase
                            user.verifyBeforeUpdateEmail(email)
                                    .addOnSuccessListener(a -> {
                                        if (passwordChanged) {
                                            user.updatePassword(newPass)
                                                    .addOnSuccessListener(p -> saveToFirestore(user, fullName, contact, email, city, barangay))
                                                    .addOnFailureListener(e -> handleError("Password update failed: " + e.getMessage()));
                                        } else {
                                            // Email verification sent, now update Firestore
                                            saveToFirestore(user, fullName, contact, email, city, barangay);
                                            Toast.makeText(this, "Verification email sent to " + email, Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> handleError("Email update failed: " + e.getMessage()));
                        } else if (passwordChanged) {
                            user.updatePassword(newPass)
                                    .addOnSuccessListener(p -> saveToFirestore(user, fullName, contact, email, city, barangay))
                                    .addOnFailureListener(e -> handleError("Password update failed: " + e.getMessage()));
                        }
                    })
                    .addOnFailureListener(e -> handleError("Re-authentication failed: " + e.getLocalizedMessage()));
        } else {
            saveToFirestore(user, fullName, contact, email, city, barangay);
        }
    }

    private void saveToFirestore(FirebaseUser user, String name, String contact, String email, String city, String barangay) {
        Map<String, Object> address = new HashMap<>();
        address.put("city", city);
        address.put("barangay", barangay);
        address.put("province", "Camarines Norte");
        address.put("region", "REGION V (Bicol Region)");

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("contactNumber", contact);
        updates.put("email", email);
        updates.put("address", address);
        updates.put("updatedAt", Timestamp.now());

        db.collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> handleError("Firestore update failed: " + e.getMessage()));
    }

    private void handleError(String msg) {
        setLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void clearErrors() {
        tilFullName.setError(null); tilContact.setError(null); tilEmail.setError(null);
        tilCity.setError(null); tilBarangay.setError(null);
        tilCurrentPassword.setError(null); tilNewPassword.setError(null); tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnSaveChanges.setEnabled(!loading);
        btnSaveChanges.setText(loading ? "Saving…" : "Save Changes");
    }
}