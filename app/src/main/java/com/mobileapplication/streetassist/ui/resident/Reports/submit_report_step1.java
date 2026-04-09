package com.mobileapplication.streetassist.ui.resident.Reports;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mobileapplication.streetassist.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class submit_report_step1 extends AppCompatActivity {

    // Map
    private MapView mapView;
    private Marker selectedMarker;
    private GeoPoint selectedPoint;
    private boolean isMapExpanded = false;
    private static final int MAP_HEIGHT_NORMAL   = 200;
    private static final int MAP_HEIGHT_EXPANDED = 400;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private boolean isManualPin = false;

    // Date & Time
    private Calendar selectedDateTime = null;

    // Views
    private MaterialButton btnUseMyLocation;
    private TextView tvSelectedLocation;
    private TextInputEditText etSearch, etAge, etDescription, etDateTimePicker;
    private AutoCompleteTextView actvSex;
    private MaterialCardView mapCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this,
                PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.submit_report_step1);

        // Bind views
        mapView            = findViewById(R.id.mapView);
        mapCard            = findViewById(R.id.mapCard);
        btnUseMyLocation   = findViewById(R.id.btnUseMyLocation);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        etSearch           = findViewById(R.id.etSearch);
        etAge              = findViewById(R.id.etAge);
        etDescription      = findViewById(R.id.etDescription);
        actvSex            = findViewById(R.id.actvSex);
        etDateTimePicker   = findViewById(R.id.etDateTimePicker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        setupSexDropdown();
        setupSearch();
        setupDateTimePicker();
        setupButtons();
    }

    // ─── DATE & TIME ──────────────────────────────────────────────────────────

    private void setupDateTimePicker() {
        // Tapping the field opens date picker → then time picker
        etDateTimePicker.setOnClickListener(v -> openDatePicker());
        etDateTimePicker.setFocusable(false);

        // "Use current date & time" button
        MaterialButton btnUseNow = findViewById(R.id.btnUseCurrentDateTime);
        btnUseNow.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            updateDateTimeDisplay();
        });
    }

    private void openDatePicker() {
        Calendar cal = selectedDateTime != null ? selectedDateTime : Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            if (selectedDateTime == null) selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            openTimePicker(); // Chain into time picker after date is set
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openTimePicker() {
        Calendar cal = selectedDateTime != null ? selectedDateTime : Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            selectedDateTime.set(Calendar.SECOND, 0);
            updateDateTimeDisplay();
        },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false).show();
    }

    private void updateDateTimeDisplay() {
        if (selectedDateTime == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  ·  hh:mm a", Locale.getDefault());
        etDateTimePicker.setText(sdf.format(selectedDateTime.getTime()));
    }

    // ─── MAP SETUP ────────────────────────────────────────────────────────────

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        GeoPoint defaultPoint = new GeoPoint(14.6760, 121.0437);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(defaultPoint);

        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                dropMarker(p, true);
                return true;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        }));

        FloatingActionButton btnExpand = findViewById(R.id.btnExpandMap);
        btnExpand.setOnClickListener(v -> toggleMapExpand());

        getCurrentLocation(false);
    }

    private void dropMarker(GeoPoint point, boolean isManual) {
        if (selectedMarker != null) mapView.getOverlays().remove(selectedMarker);

        selectedPoint = point;
        isManualPin   = isManual;

        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(point);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle(isManual ? "Pinned Location" : "Your Location");

        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin_red);
        if (icon != null) selectedMarker.setIcon(icon);

        mapView.getOverlays().add(selectedMarker);
        mapView.getController().animateTo(point);
        mapView.invalidate();

        findViewById(R.id.tvMapHint).setVisibility(android.view.View.GONE);

        if (isManual) {
            btnUseMyLocation.setText("Use this location");
            btnUseMyLocation.setIcon(
                    ContextCompat.getDrawable(this, android.R.drawable.ic_menu_add));
        } else {
            btnUseMyLocation.setText("Use my location");
            btnUseMyLocation.setIcon(
                    ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation));
        }

        reverseGeocode(point);
    }

    private void toggleMapExpand() {
        isMapExpanded = !isMapExpanded;
        int heightPx = dpToPx(isMapExpanded ? MAP_HEIGHT_EXPANDED : MAP_HEIGHT_NORMAL);
        ViewGroup.LayoutParams params = mapCard.getLayoutParams();
        params.height = heightPx;
        mapCard.setLayoutParams(params);

        FloatingActionButton btnExpand = findViewById(R.id.btnExpandMap);
        btnExpand.setImageDrawable(ContextCompat.getDrawable(this,
                isMapExpanded
                        ? android.R.drawable.ic_menu_close_clear_cancel
                        : android.R.drawable.ic_menu_zoom));
    }

    // ─── GEOCODING ────────────────────────────────────────────────────────────

    private void reverseGeocode(GeoPoint point) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        point.getLatitude(), point.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i));
                        if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                    }
                    runOnUiThread(() -> tvSelectedLocation.setText(sb.toString()));
                } else {
                    runOnUiThread(() -> tvSelectedLocation.setText(
                            String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                    point.getLatitude(), point.getLongitude())));
                }
            } catch (IOException e) {
                runOnUiThread(() -> tvSelectedLocation.setText(
                        String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                point.getLatitude(), point.getLongitude())));
            }
        }).start();
    }

    private void searchLocation(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());
                    runOnUiThread(() -> {
                        dropMarker(point, true);
                        mapView.getController().setZoom(17.0);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Location not found.", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Search failed. Check your connection.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── CURRENT LOCATION ─────────────────────────────────────────────────────

    private void getCurrentLocation(boolean showToastIfFail) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                dropMarker(point, false);
                mapView.getController().setZoom(17.0);
            } else if (showToastIfFail) {
                Toast.makeText(this, "Could not get location. Try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── SETUP HELPERS ────────────────────────────────────────────────────────

    private void setupSexDropdown() {
        String[] sexOptions = {"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, sexOptions);
        actvSex.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText() != null
                        ? etSearch.getText().toString().trim() : "";
                searchLocation(query);
                return true;
            }
            return false;
        });
    }

    private void setupButtons() {
        btnUseMyLocation.setOnClickListener(v -> {
            if (isManualPin && selectedPoint != null) {
                saveAndProceed();
            } else {
                getCurrentLocation(true);
            }
        });

        MaterialButton btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            if (!validateForm()) return;
            saveAndProceed();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ─── VALIDATION + NAVIGATION ──────────────────────────────────────────────

    private boolean validateForm() {
        if (selectedPoint == null) {
            Toast.makeText(this, "Please select a location on the map.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etAge.getText() == null || etAge.getText().toString().trim().isEmpty()) {
            etAge.setError("Please enter approximate age.");
            etAge.requestFocus();
            return false;
        }
        if (actvSex.getText() == null || actvSex.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select sex.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDescription.getText() == null || etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError("Please enter a description.");
            etDescription.requestFocus();
            return false;
        }
        if (selectedDateTime == null) {
            Toast.makeText(this, "Please select the date and time when the individual was seen.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveAndProceed() {
        if (!validateForm()) return;

        String locationText = tvSelectedLocation.getText().toString();

        // Pass date/time as a long (milliseconds) to Step 2 → Step 3
        Intent intent = new Intent(this, submit_report_step2.class);
        intent.putExtra("latitude",        selectedPoint.getLatitude());
        intent.putExtra("longitude",       selectedPoint.getLongitude());
        intent.putExtra("locationText",    locationText);
        intent.putExtra("age",             etAge.getText().toString().trim());
        intent.putExtra("sex",             actvSex.getText().toString().trim());
        intent.putExtra("description",     etDescription.getText().toString().trim());
        intent.putExtra("seenAt",          selectedDateTime.getTimeInMillis());
        startActivity(intent);
    }

    // ─── PERMISSIONS ──────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation(true);
        }
    }

    // ─── OSMDroid LIFECYCLE ───────────────────────────────────────────────────

    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }

    // ─── UTILITY ──────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}