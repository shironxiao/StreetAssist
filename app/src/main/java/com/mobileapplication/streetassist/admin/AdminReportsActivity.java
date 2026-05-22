package com.mobileapplication.streetassist.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.AdminLoginActivity;

import android.net.Uri;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AdminReportsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminReports";
    private DrawerLayout drawerLayout;
    private AdminNotificationBadgeHelper badgeHelper;
    private RecyclerView rvReports;
    private RecentReportAdapter adapter;
    private List<Map<String, Object>> reportList = new ArrayList<>();
    private List<Map<String, Object>> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentSearchQuery = "";

    // Cloudinary & Resolution Proof Config
    private static final String CLOUD_NAME = "durqaiei1";
    private static final String UPLOAD_PRESET = "streetassist_unsigned";
    private static final String API_KEY = "938268411726485";
    private static final int PROOF_IMAGE_PICKER_REQUEST = 2001;
    private final List<Uri> selectedProofImageUris = new ArrayList<>();
    private HorizontalScrollView scrollSelectedProofImages;
    private LinearLayout layoutSelectedProofImages;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    private String currentStatusFilter = "All";
    private String currentMunicipalityFilter = "All";
    private String currentBarangayFilter = "All";
    private String currentSortOrder = "Newest";
    private ListenerRegistration reportsListener;

    private FusedLocationProviderClient fusedLocationClient;
    private Location adminLocation;

    private interface TrashMoveCallback {
        void onComplete(boolean success, String errorMessage);
    }

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final Map<String, List<String>> CITY_BARANGAY_MAP = new TreeMap<>();

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
                "Lucrecia", "Magang", "Mancruz (San Juan)", "Pamorangon", "San Isidro"
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
                "Libas", "Mabini", "Macolabo Island", "Malinis",
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_reports);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        badgeHelper = new AdminNotificationBadgeHelper(this);

        // Security Guard
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        // Verify Admin Role
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (!"admin".equalsIgnoreCase(role)) {
                        Toast.makeText(this, "Access Denied: Admin role required", Toast.LENGTH_LONG).show();
                        logout();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Role verification failed", e);
                    Toast.makeText(this, "Error verifying role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_all_reports);

        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        rvReports = findViewById(R.id.rvAllReports);
        rvReports.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecentReportAdapter(this, filteredList);
        adapter.setHeaderListener(new RecentReportAdapter.OnHeaderActionListener() {
            @Override
            public void onSearch(String query) {
                currentSearchQuery = query;
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> applyFilters();
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void onFilterClick() {
                showFilterPopup();
            }

            @Override
            public void onMunicipalityFilterClick() {
                showMunicipalityFilterPopup();
            }

            @Override
            public void onBarangayFilterClick() {
                showBarangayFilterPopup();
            }

            @Override
            public void onSortClick() {
                showSortPopup();
            }

            @Override
            public void onExportPdfClick() {
                exportToPdf();
            }

            @Override
            public void onDeleteSelected(java.util.Set<String> selectedIds) {
                deleteMultipleReports(selectedIds);
            }

            @Override
            public void onRestoreSelected(java.util.Set<String> selectedIds) {}

            @Override
            public void onCancelSelection() {
                adapter.clearSelection();
            }
        });
        rvReports.setAdapter(adapter);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    startActivity(new Intent(AdminReportsActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }
        });

        requestAdminLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (reportsListener == null) {
            reportsListener = fetchAllReports();
        }
        if (badgeHelper != null) {
            badgeHelper.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reportsListener != null) {
            reportsListener.remove();
            reportsListener = null;
        }
        if (badgeHelper != null) {
            badgeHelper.stopListening();
        }
    }

    private ListenerRegistration fetchAllReports() {
        return db.collection("reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        reportList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> data = doc.getData();
                            if (data != null && data.get("deletedAt") == null) {
                                data.put("documentId", doc.getId());
                                reportList.add(data);
                            }
                        }
                        applyFilters();
                        checkAndShowTargetReport();
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkAndShowTargetReport();
    }

    private void checkAndShowTargetReport() {
        String targetReportId = getIntent().getStringExtra("reportId");
        if (targetReportId == null || targetReportId.isEmpty()) return;

        getIntent().removeExtra("reportId");

        for (Map<String, Object> report : reportList) {
            if (targetReportId.equals(report.get("documentId"))) {
                showReportDetails(report);
                return;
            }
        }

        db.collection("reports").document(targetReportId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            data.put("documentId", documentSnapshot.getId());
                            showReportDetails(data);
                        }
                    } else {
                        Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        filteredList.clear();
        for (Map<String, Object> report : reportList) {
            String address = String.valueOf(report.getOrDefault("locationAddress", "")).toLowerCase();
            
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String desc = String.valueOf(report.get("description")).toLowerCase();
                String id = String.valueOf(report.get("reportId")).toLowerCase();
                matchesSearch = desc.contains(currentSearchQuery.toLowerCase()) ||
                        address.contains(currentSearchQuery.toLowerCase()) ||
                        id.contains(currentSearchQuery.toLowerCase());
            }

            boolean matchesStatus = currentStatusFilter.equals("All") || 
                    String.valueOf(report.get("status")).equalsIgnoreCase(currentStatusFilter);

            boolean matchesMunicipality = currentMunicipalityFilter.equals("All") || 
                    address.contains(currentMunicipalityFilter.toLowerCase());

            boolean matchesBarangay = currentBarangayFilter.equals("All") || 
                    address.contains(currentBarangayFilter.toLowerCase());

            if (matchesSearch && matchesStatus && matchesMunicipality && matchesBarangay) {
                filteredList.add(report);
            }
        }

        // Apply Sorting
        if (currentSortOrder.equals("Newest")) {
            filteredList.sort((a, b) -> compareTimestamps(b, a));
        } else if (currentSortOrder.equals("Oldest")) {
            filteredList.sort((a, b) -> compareTimestamps(a, b));
        } else if (currentSortOrder.equals("Distance (Nearest)") && adminLocation != null) {
            filteredList.sort(this::compareDistances);
        } else if (currentSortOrder.equals("Municipality (A-Z)")) {
            filteredList.sort((a, b) -> {
                String m1 = detectMunicipality(String.valueOf(a.getOrDefault("locationAddress", "")));
                String m2 = detectMunicipality(String.valueOf(b.getOrDefault("locationAddress", "")));
                return m1.compareToIgnoreCase(m2);
            });
        }

        adapter.updateList(filteredList);
    }

    private int compareTimestamps(Map<String, Object> a, Map<String, Object> b) {
        Object t1 = a.get("timestamp");
        Object t2 = b.get("timestamp");
        if (t1 instanceof com.google.firebase.Timestamp && t2 instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) t1).compareTo((com.google.firebase.Timestamp) t2);
        }
        return 0;
    }

    private int compareDistances(Map<String, Object> a, Map<String, Object> b) {
        double lat1 = (double) a.getOrDefault("latitude", 0.0);
        double lon1 = (double) a.getOrDefault("longitude", 0.0);
        double lat2 = (double) b.getOrDefault("latitude", 0.0);
        double lon2 = (double) b.getOrDefault("longitude", 0.0);

        float[] res1 = new float[1];
        Location.distanceBetween(adminLocation.getLatitude(), adminLocation.getLongitude(), lat1, lon1, res1);
        float[] res2 = new float[1];
        Location.distanceBetween(adminLocation.getLatitude(), adminLocation.getLongitude(), lat2, lon2, res2);

        return Float.compare(res1[0], res2[0]);
    }

    private String detectMunicipality(String address) {
        if (address == null || address.isEmpty()) return "Unknown";
        for (String m : CITY_BARANGAY_MAP.keySet()) {
            if (address.toLowerCase().contains(m.toLowerCase())) return m;
        }
        return "Unknown";
    }

    private void showFilterPopup() {
        RecyclerView.ViewHolder header = rvReports.findViewHolderForAdapterPosition(0);
        if (header == null) return;
        View anchor = header.itemView.findViewById(R.id.btnFilterStatus);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("All");
        popup.getMenu().add("Pending");
        popup.getMenu().add("In Progress");
        popup.getMenu().add("Resolved");
        popup.setOnMenuItemClickListener(item -> {
            currentStatusFilter = item.getTitle().toString();
            applyFilters();
            ((TextView) anchor.findViewById(R.id.tvFilterLabel)).setText("Status: " + currentStatusFilter);
            return true;
        });
        popup.show();
    }

    private void showMunicipalityFilterPopup() {
        RecyclerView.ViewHolder header = rvReports.findViewHolderForAdapterPosition(0);
        if (header == null) return;
        View anchor = header.itemView.findViewById(R.id.btnFilterMunicipality);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("All");
        for (String m : CITY_BARANGAY_MAP.keySet()) popup.getMenu().add(m);
        popup.setOnMenuItemClickListener(item -> {
            currentMunicipalityFilter = item.getTitle().toString();
            currentBarangayFilter = "All";
            applyFilters();
            ((TextView) anchor.findViewById(R.id.tvMunicipalityLabel)).setText("Muni: " + currentMunicipalityFilter);
            View brgyBtn = header.itemView.findViewById(R.id.btnFilterBarangay);
            ((TextView) brgyBtn.findViewById(R.id.tvBarangayLabel)).setText("Brgy: All");
            return true;
        });
        popup.show();
    }

    private void showBarangayFilterPopup() {
        RecyclerView.ViewHolder header = rvReports.findViewHolderForAdapterPosition(0);
        if (header == null) return;
        View anchor = header.itemView.findViewById(R.id.btnFilterBarangay);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("All");
        if (!currentMunicipalityFilter.equals("All")) {
            List<String> brgys = CITY_BARANGAY_MAP.get(currentMunicipalityFilter);
            if (brgys != null) for (String b : brgys) popup.getMenu().add(b);
        } else {
            popup.getMenu().add("(Select Municipality First)");
        }
        popup.setOnMenuItemClickListener(item -> {
            String sel = item.getTitle().toString();
            if (sel.startsWith("(")) return false;
            currentBarangayFilter = sel;
            applyFilters();
            ((TextView) anchor.findViewById(R.id.tvBarangayLabel)).setText("Brgy: " + (sel.length() > 10 ? sel.substring(0, 8) + ".." : sel));
            return true;
        });
        popup.show();
    }

    private void showSortPopup() {
        RecyclerView.ViewHolder header = rvReports.findViewHolderForAdapterPosition(0);
        if (header == null) return;
        View anchor = header.itemView.findViewById(R.id.btnSortLocation);
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("Newest");
        popup.getMenu().add("Oldest");
        popup.getMenu().add("Distance (Nearest)");
        popup.getMenu().add("Municipality (A-Z)");
        popup.setOnMenuItemClickListener(item -> {
            currentSortOrder = item.getTitle().toString();
            if ("Distance (Nearest)".equals(currentSortOrder) && adminLocation == null) requestAdminLocation();
            applyFilters();
            ((TextView) anchor.findViewById(R.id.tvSortLabel)).setText("Sort: " + currentSortOrder);
            return true;
        });
        popup.show();
    }

    private void requestAdminLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) adminLocation = location;
            if ("Distance (Nearest)".equals(currentSortOrder)) applyFilters();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1002 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestAdminLocation();
        }
    }

    private void exportToPdf() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No reports to export", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();
            android.graphics.Paint titlePaint = new android.graphics.Paint();
            titlePaint.setTextSize(18f); titlePaint.setFakeBoldText(true);
            float y = 40; canvas.drawText("StreetAssist Reports Export", 40, y, titlePaint);
            y += 30; paint.setTextSize(12f); canvas.drawText("Total Reports: " + filteredList.size(), 40, y, paint);
            y += 20; canvas.drawText("Generated on: " + new Date().toString(), 40, y, paint); y += 40;
            for (Map<String, Object> report : filteredList) {
                if (y > 780) {
                    document.finishPage(page);
                    pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo); canvas = page.getCanvas(); y = 40;
                }
                String id = String.valueOf(report.getOrDefault("reportId", "N/A"));
                String desc = String.valueOf(report.getOrDefault("description", ""));
                String loc = String.valueOf(report.getOrDefault("locationAddress", ""));
                String status = String.valueOf(report.getOrDefault("status", ""));
                paint.setFakeBoldText(true); canvas.drawText("ID: " + id, 40, y, paint); y += 15;
                paint.setFakeBoldText(false); canvas.drawText("Status: " + status, 40, y, paint); y += 15;
                canvas.drawText("Location: " + loc, 40, y, paint); y += 15;
                canvas.drawText("Description: " + (desc.length() > 60 ? desc.substring(0, 60) + "..." : desc), 40, y, paint);
                y += 30; canvas.drawLine(40, y - 10, 555, y - 10, paint); y += 10;
            }
            document.finishPage(page);
            java.io.File exportsDir = new java.io.File(getCacheDir(), "exports");
            if (!exportsDir.exists()) {
                exportsDir.mkdirs();
            }
            java.io.File file = new java.io.File(exportsDir, "reports_" + System.currentTimeMillis() + ".pdf");
            document.writeTo(new java.io.FileOutputStream(file));
            document.close();
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf"); share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share PDF Report"));
        } catch (Exception e) { Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }

    private void deleteMultipleReports(java.util.Set<String> selectedIds) {
        if (selectedIds.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Move Reports to Trash")
                .setMessage("Are you sure you want to move the " + selectedIds.size() + " selected reports to Trash?")
                .setPositiveButton("Move to Trash", (dialog, which) -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (String id : selectedIds) {
                        batch.update(db.collection("reports").document(id), "deletedAt", FieldValue.serverTimestamp());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Reports moved to trash", Toast.LENGTH_SHORT).show();
                                adapter.clearSelection();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to move reports: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showReportDetails(Map<String, Object> report) {
        String docId = String.valueOf(report.get("documentId"));
        if (report.get("adminSeenAt") == null) db.collection("reports").document(docId).update("adminSeenAt", FieldValue.serverTimestamp());
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admin_report_details, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ((TextView) view.findViewById(R.id.tvReportId)).setText("ID: " + report.get("reportId"));
        ((TextView) view.findViewById(R.id.tvDetailDescription)).setText(String.valueOf(report.get("description")));
        ((TextView) view.findViewById(R.id.tvDetailLocation)).setText(String.valueOf(report.get("locationAddress")));
        ((TextView) view.findViewById(R.id.tvDetailStatus)).setText(String.valueOf(report.get("status")).toUpperCase());
        updateReporterInfo(report, view.findViewById(R.id.tvReporterName), view.findViewById(R.id.tvReporterContact), view.findViewById(R.id.tvReporterAddress));

        String status = String.valueOf(report.get("status"));
        View layoutUpdateStatusSection = view.findViewById(R.id.layoutUpdateStatusSection);
        View layoutResolutionProof = view.findViewById(R.id.layoutResolutionProof);

        if ("Resolved".equalsIgnoreCase(status)) {
            if (layoutUpdateStatusSection != null) layoutUpdateStatusSection.setVisibility(View.GONE);
            if (layoutResolutionProof != null) {
                layoutResolutionProof.setVisibility(View.VISIBLE);
                String notes = String.valueOf(report.getOrDefault("resolutionNotes", "")).trim();
                TextView tvNotes = view.findViewById(R.id.tvResolutionNotes);
                if (tvNotes != null) {
                    tvNotes.setText(notes.isEmpty() ? "Resolved by Administrator." : notes);
                }
                List<String> proofImages = new java.util.ArrayList<>();
                if (report.get("resolutionImages") instanceof List) {
                    List<?> rawList = (List<?>) report.get("resolutionImages");
                    for (Object item : rawList) {
                        if (item instanceof String) {
                            proofImages.add((String) item);
                        }
                    }
                }
                String proofUrl = String.valueOf(report.getOrDefault("resolutionImageUrl", ""));
                if (proofImages.isEmpty() && !proofUrl.isEmpty()) {
                    proofImages.add(proofUrl);
                }

                View cardPhoto = view.findViewById(R.id.cardProofPhoto);
                com.google.android.material.button.MaterialButton btnViewProof = view.findViewById(R.id.btnViewProof);

                if (!proofImages.isEmpty()) {
                    if (cardPhoto != null) cardPhoto.setVisibility(View.GONE);
                    if (btnViewProof != null) {
                        btnViewProof.setVisibility(View.VISIBLE);
                        btnViewProof.setOnClickListener(v -> showResolutionProofDialog(proofImages));
                    }
                } else {
                    if (cardPhoto != null) cardPhoto.setVisibility(View.GONE);
                    if (btnViewProof != null) btnViewProof.setVisibility(View.GONE);
                }
            }
        } else {
            if (layoutUpdateStatusSection != null) layoutUpdateStatusSection.setVisibility(View.VISIBLE);
            if (layoutResolutionProof != null) layoutResolutionProof.setVisibility(View.GONE);
        }

        view.findViewById(R.id.btnSetInProgress).setOnClickListener(v -> updateReportStatus(docId, "In Progress", dialog));
        view.findViewById(R.id.btnSetResolved).setOnClickListener(v -> showResolveReportDialog(docId, dialog));
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnViewLocation).setOnClickListener(v -> {
            Object lat = report.get("latitude"), lon = report.get("longitude");
            if (lat != null && lon != null) startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon)));
        });
        view.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Move to Trash")
                    .setMessage("Are you sure you want to move this report to Trash?")
                    .setPositiveButton("Move to Trash", (confirmDialog, which) -> {
                        db.collection("reports").document(docId).update("deletedAt", FieldValue.serverTimestamp()).addOnSuccessListener(unused -> dialog.dismiss());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        dialog.show();
    }

    private void updateReportStatus(String docId, String status, android.app.AlertDialog dialog) {
        db.collection("reports").document(docId).update("status", status).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void updateReporterInfo(Map<String, Object> report, TextView tvName, TextView tvContact, TextView tvAddress) {
        tvName.setText(String.valueOf(report.getOrDefault("fullName", "Resident")));
        String contact = String.valueOf(report.getOrDefault("contactNumber", ""));
        if (!contact.isEmpty()) { tvContact.setVisibility(View.VISIBLE); tvContact.setText("Contact: " + contact); }
        tvAddress.setVisibility(View.GONE);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, AdminLoginActivity.class));
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_dashboard) { startActivity(new Intent(this, AdminDashboardActivity.class)); finish(); }
        else if (id == R.id.nav_announcements) startActivity(new Intent(this, AdminAnnouncementsActivity.class));
        else if (id == R.id.nav_notifications) startActivity(new Intent(this, AdminNotificationActivity.class));
        else if (id == R.id.nav_profile) startActivity(new Intent(this, AdminProfileActivity.class));
        else if (id == R.id.nav_trash) startActivity(new Intent(this, AdminTrashActivity.class));
        else if (id == R.id.nav_logout) logout();
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void startActivity(android.content.Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void startActivity(android.content.Intent intent, android.os.Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void showResolveReportDialog(String docId, android.app.AlertDialog detailsDialog) {
        selectedProofImageUris.clear(); // reset
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_resolve_report, null);
        builder.setView(view);
        android.app.AlertDialog resolveDialog = builder.create();
        if (resolveDialog.getWindow() != null) {
            resolveDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        scrollSelectedProofImages = view.findViewById(R.id.scrollSelectedProofImages);
        layoutSelectedProofImages = view.findViewById(R.id.layoutSelectedProofImages);

        view.findViewById(R.id.cardProofImage).setOnClickListener(v -> openProofImagePicker());

        com.google.android.material.textfield.TextInputEditText etNotes = view.findViewById(R.id.etResolutionNotes);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> resolveDialog.dismiss());
        view.findViewById(R.id.btnSubmitResolution).setOnClickListener(v -> {
            String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Confirm Resolution")
                    .setMessage("Are you sure you want to mark this report as Resolved?")
                    .setPositiveButton("Resolve", (confirmDialog, which) -> {
                        Toast.makeText(this, "Resolving report...", Toast.LENGTH_SHORT).show();
                        if (!selectedProofImageUris.isEmpty()) {
                            uploadProofAndResolve(docId, selectedProofImageUris, notes, resolveDialog, detailsDialog);
                        } else {
                            saveResolutionToFirestore(docId, new ArrayList<>(), notes, resolveDialog, detailsDialog);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        resolveDialog.show();
    }

    private void openProofImagePicker() {
        Intent intent = new Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.setAction(android.provider.MediaStore.ACTION_PICK_IMAGES);
            intent.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 10);
        } else {
            intent.setAction(Intent.ACTION_PICK);
            intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        try {
            startActivityForResult(intent, PROOF_IMAGE_PICKER_REQUEST);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(fallback, "Select Proof Images"), PROOF_IMAGE_PICKER_REQUEST);
            } catch (Exception ex) {
                Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProofImagePreview() {
        if (layoutSelectedProofImages == null || scrollSelectedProofImages == null) return;

        layoutSelectedProofImages.removeAllViews();
        if (selectedProofImageUris.isEmpty()) {
            scrollSelectedProofImages.setVisibility(View.GONE);
        } else {
            scrollSelectedProofImages.setVisibility(View.VISIBLE);
            float density = getResources().getDisplayMetrics().density;
            int thumbW = (int) (80 * density);
            int thumbH = (int) (80 * density);
            int margin = (int) (6 * density);
            int radius = (int) (8 * density);

            for (Uri uri : selectedProofImageUris) {
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(thumbW, thumbH);
                params.setMargins(0, 0, margin, 0);
                card.setLayoutParams(params);
                card.setRadius(radius);
                card.setCardElevation(1 * density);
                card.setStrokeWidth((int) (1 * density));
                card.setStrokeColor(0xFFE2E8F0);

                FrameLayout fl = new FrameLayout(this);
                fl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                ImageView iv = new ImageView(this);
                iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageURI(uri);
                fl.addView(iv);

                ImageButton btnDel = new ImageButton(this);
                FrameLayout.LayoutParams delParams = new FrameLayout.LayoutParams((int) (20 * density), (int) (20 * density));
                delParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                delParams.setMargins(0, (int) (2 * density), (int) (2 * density), 0);
                btnDel.setLayoutParams(delParams);
                btnDel.setBackgroundResource(R.drawable.bg_delete_circle);
                btnDel.setImageResource(R.drawable.ic_close);
                btnDel.setPadding(0, 0, 0, 0);
                btnDel.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    btnDel.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFF4444));
                }
                btnDel.setOnClickListener(v -> {
                    selectedProofImageUris.remove(uri);
                    updateProofImagePreview();
                });
                fl.addView(btnDel);

                card.addView(fl);
                layoutSelectedProofImages.addView(card);
            }
        }
    }

    private void uploadProofAndResolve(String docId, List<Uri> imageUris, String notes, android.app.AlertDialog resolveDialog, android.app.AlertDialog detailsDialog) {
        executor.execute(() -> {
            try {
                List<String> uploadedUrls = new java.util.ArrayList<>();
                for (Uri imageUri : imageUris) {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream == null) {
                        continue;
                    }
                    byte[] imageBytes = readAllBytes(inputStream);
                    inputStream.close();

                    String boundary = "----FormBoundary" + System.currentTimeMillis();
                    String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                    HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    java.io.DataOutputStream dos = new java.io.DataOutputStream(conn.getOutputStream());
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                    dos.writeBytes(UPLOAD_PRESET + "\r\n");

                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                    dos.writeBytes(API_KEY + "\r\n");

                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"proof.jpg\"\r\n");
                    dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                    dos.write(imageBytes);
                    dos.writeBytes("\r\n");
                    dos.writeBytes("--" + boundary + "--\r\n");
                    dos.flush();
                    dos.close();

                    int status = conn.getResponseCode();
                    InputStream responseStream = (status == 200) ? conn.getInputStream() : conn.getErrorStream();
                    byte[] responseBytes = readAllBytes(responseStream);
                    responseStream.close();
                    conn.disconnect();

                    org.json.JSONObject json = new org.json.JSONObject(new String(responseBytes));
                    if (status == 200 && json.has("secure_url")) {
                        uploadedUrls.add(json.getString("secure_url"));
                    } else {
                        String error = json.optString("error", "Upload failed");
                        runOnUiThread(() -> Toast.makeText(this, "Upload error: " + error, Toast.LENGTH_LONG).show());
                        return;
                    }
                }

                runOnUiThread(() -> {
                    saveResolutionToFirestore(docId, uploadedUrls, notes, resolveDialog, detailsDialog);
                });
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary upload failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveResolutionToFirestore(String docId, List<String> imageUrls, String notes, android.app.AlertDialog resolveDialog, android.app.AlertDialog detailsDialog) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", "Resolved");
        updates.put("resolutionNotes", notes);
        updates.put("resolutionImageUrl", imageUrls.isEmpty() ? "" : imageUrls.get(0));
        updates.put("resolutionImages", imageUrls);
        updates.put("resolvedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("reports").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report resolved successfully!", Toast.LENGTH_SHORT).show();
                    if (resolveDialog != null) resolveDialog.dismiss();
                    if (detailsDialog != null) detailsDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to resolve report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private byte[] readAllBytes(InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROOF_IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) {
                        selectedProofImageUris.add(uri);
                    }
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                selectedProofImageUris.add(uri);
            }
            updateProofImagePreview();
        }
    }

    private void showResolutionProofDialog(List<String> images) {
        if (images == null || images.isEmpty()) return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_case_closed_proof_gallery);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvGalleryTitle);
        if (tvTitle != null) {
            tvTitle.setText("Resolution Proof Photos");
            tvTitle.setTextColor(android.graphics.Color.parseColor("#3B6D11"));
        }

        LinearLayout layoutImages = dialog.findViewById(R.id.layoutCaseClosedImagesDialog);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseProofDialog);

        float density = getResources().getDisplayMetrics().density;
        int thumbW = (int) (120 * density);
        int thumbH = (int) (120 * density);
        int margin = (int) (10 * density);
        int radius = (int) (12 * density);

        layoutImages.removeAllViews();
        for (String proofUrl : images) {
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(thumbW, thumbH);
            cardParams.setMargins(0, 0, margin, 0);
            card.setLayoutParams(cardParams);
            card.setRadius(radius);
            card.setCardElevation(2 * density);
            card.setStrokeWidth((int) (1 * density));
            card.setStrokeColor(0xFFE2E8F0);
            card.setClickable(true);
            card.setFocusable(true);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            com.bumptech.glide.Glide.with(this).load(proofUrl).placeholder(R.drawable.ic_image_placeholder).into(iv);
            card.addView(iv);

            iv.setOnClickListener(v -> {
                showFullImageDialog(proofUrl);
            });

            layoutImages.addView(card);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showFullImageDialog(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);

        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .fitCenter()
                .into(ivFullscreen);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        ivFullscreen.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
