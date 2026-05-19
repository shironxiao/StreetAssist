package com.mobileapplication.streetassist.ui.resident.Reports;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class SubmitReportStep1Fragment extends Fragment {

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

    // Static local locations for instant, offline suggestions
    private static final String[] LOCAL_LOCATIONS = {
        "Quezon City", "Manila", "Caloocan", "Las Piñas", "Makati", "Malabon", 
        "Mandaluyong", "Marikina", "Muntinlupa", "Navotas", "Parañaque", "Pasay", 
        "Pasig", "Pateros", "San Juan", "Taguig", "Valenzuela", 
        "Commonwealth, Quezon City", "Diliman, Quezon City", "Cubao, Quezon City", 
        "Katipunan, Quezon City", "Fairview, Quezon City", "Novaliches, Quezon City", 
        "Batasan Hills, Quezon City", "Loyola Heights, Quezon City", 
        "Tondo, Manila", "Sampaloc, Manila", "Ermita, Manila", "Malate, Manila", 
        "Binondo, Manila", "Quiapo, Manila", "Intramuros, Manila", 
        "Bonifacio Global City, Taguig", "Ortigas Center, Pasig", "Ayala Avenue, Makati", 
        "Greenhills, San Juan", "Eastwood City, Quezon City", "Araneta Center, Quezon City", 
        "UP Diliman, Quezon City", "Ateneo de Manila, Quezon City"
    };

    // Search Autocomplete Suggestion fields
    private android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;
    private NoFilterAdapter<String> searchSuggestionsAdapter;
    private final java.util.List<Address> currentSuggestionAddresses = new java.util.ArrayList<>();
    private boolean isSelectingSuggestion = false;

    // Views
    private MaterialButton btnUseMyLocation;
    private TextView tvSelectedLocation;
    private AutoCompleteTextView etSearch;
    private TextInputEditText etAge, etDescription, etDateTimePicker;
    private AutoCompleteTextView actvSex;
    private MaterialCardView mapCard;

    public SubmitReportStep1Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        return inflater.inflate(R.layout.fragment_submit_report_step1, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        mapView            = view.findViewById(R.id.mapView);
        mapCard            = view.findViewById(R.id.mapCard);
        btnUseMyLocation   = view.findViewById(R.id.btnUseMyLocation);
        tvSelectedLocation = view.findViewById(R.id.tvSelectedLocation);
        etSearch           = view.findViewById(R.id.etSearch);
        etAge              = view.findViewById(R.id.etAge);
        etDescription      = view.findViewById(R.id.etDescription);
        actvSex            = view.findViewById(R.id.actvSex);
        etDateTimePicker   = view.findViewById(R.id.etDateTimePicker);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupMap();
        setupSexDropdown();
        setupSearch();
        setupDateTimePicker();
        setupButtons();

        View nestedScrollView = view.findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            nestedScrollView.setOnTouchListener((v, event) -> {
                hideKeyboardAndClearFocus();
                return false;
            });
        }
    }

    // ─── DATE & TIME ──────────────────────────────────────────────────────────

    private void setupDateTimePicker() {
        etDateTimePicker.setOnClickListener(v -> openDatePicker());
        etDateTimePicker.setFocusable(false);

        MaterialButton btnUseNow = requireView().findViewById(R.id.btnUseCurrentDateTime);
        btnUseNow.setOnClickListener(v -> {
            selectedDateTime = Calendar.getInstance();
            updateDateTimeDisplay();
        });
    }

    private void openDatePicker() {
        Calendar cal = selectedDateTime != null ? selectedDateTime : Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            if (selectedDateTime == null) selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            openTimePicker();
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openTimePicker() {
        Calendar cal = selectedDateTime != null ? selectedDateTime : Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
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
        mapView.setBuiltInZoomControls(true);
        if (mapView.getZoomController() != null) {
            mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.ALWAYS);
        }

        // Prevent NestedScrollView from stealing touch events while interacting with the MapView
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case android.view.MotionEvent.ACTION_DOWN:
                    hideKeyboardAndClearFocus();
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

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

        FloatingActionButton btnExpand = requireView().findViewById(R.id.btnExpandMap);
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

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location_pin_red);
        if (icon != null) selectedMarker.setIcon(icon);

        mapView.getOverlays().add(selectedMarker);
        mapView.getController().animateTo(point);
        mapView.invalidate();

        requireView().findViewById(R.id.tvMapHint).setVisibility(View.GONE);

        reverseGeocode(point);
    }

    private void toggleMapExpand() {
        isMapExpanded = !isMapExpanded;
        int heightPx = dpToPx(isMapExpanded ? MAP_HEIGHT_EXPANDED : MAP_HEIGHT_NORMAL);
        ViewGroup.LayoutParams params = mapCard.getLayoutParams();
        params.height = heightPx;
        mapCard.setLayoutParams(params);

        FloatingActionButton btnExpand = requireView().findViewById(R.id.btnExpandMap);
        btnExpand.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                isMapExpanded
                        ? android.R.drawable.ic_menu_close_clear_cancel
                        : android.R.drawable.ic_menu_zoom));
    }

    // ─── GEOCODING ────────────────────────────────────────────────────────────

    private void reverseGeocode(GeoPoint point) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        point.getLatitude(), point.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i));
                        if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                    }
                    requireActivity().runOnUiThread(() ->
                            tvSelectedLocation.setText(sb.toString()));
                } else {
                    requireActivity().runOnUiThread(() ->
                            tvSelectedLocation.setText(
                                    String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                            point.getLatitude(), point.getLongitude())));
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        tvSelectedLocation.setText(
                                String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                        point.getLatitude(), point.getLongitude())));
            }
        }).start();
    }

    private void searchLocation(String query) {
        if (query.isEmpty()) return;
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> results = null;

                if (selectedPoint != null) {
                    double lowerLeftLat = selectedPoint.getLatitude() - 1.0;
                    double lowerLeftLng = selectedPoint.getLongitude() - 1.0;
                    double upperRightLat = selectedPoint.getLatitude() + 1.0;
                    double upperRightLng = selectedPoint.getLongitude() + 1.0;
                    try {
                        results = geocoder.getFromLocationName(query, 1, lowerLeftLat, lowerLeftLng, upperRightLat, upperRightLng);
                    } catch (Exception e) {
                        // ignore and try fallback
                    }
                }

                if (results == null || results.isEmpty()) {
                    results = geocoder.getFromLocationName(query, 1);
                }

                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());
                    requireActivity().runOnUiThread(() -> {
                        dropMarker(point, true);
                        mapView.getController().setZoom(17.0);
                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Location not found.",
                                    Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Search failed. Check your connection.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ─── CURRENT LOCATION ─────────────────────────────────────────────────────

    private void getCurrentLocation(boolean showToastIfFail) {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
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
                Toast.makeText(requireContext(), "Could not get location. Try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── SETUP HELPERS ────────────────────────────────────────────────────────

    private void setupSexDropdown() {
        String[] sexOptions = {"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, sexOptions);
        actvSex.setAdapter(adapter);
    }

    private List<String> getLocalSuggestions(String query) {
        List<String> matches = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        for (String loc : LOCAL_LOCATIONS) {
            if (loc.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                matches.add(loc);
            }
        }
        return matches;
    }

    private String getAddressString(Address addr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
            sb.append(addr.getAddressLine(i));
            if (i < addr.getMaxAddressLineIndex()) sb.append(", ");
        }
        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                    addr.getLatitude(), addr.getLongitude());
        }
    }

    private void setupSearch() {
        searchSuggestionsAdapter = new NoFilterAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        etSearch.setAdapter(searchSuggestionsAdapter);
        etSearch.setThreshold(1); // Start showing suggestions after 1 character

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isSelectingSuggestion) {
                    return;
                }
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    currentSuggestionAddresses.clear();
                    searchSuggestionsAdapter.clear();
                    return;
                }

                searchRunnable = () -> fetchSearchSuggestions(query);
                searchHandler.postDelayed(searchRunnable, 300); // 300ms debounce
            }
        });

        // When a suggestion is clicked
        etSearch.setOnItemClickListener((parent, view1, position, id) -> {
            if (position < searchSuggestionsAdapter.getCount()) {
                isSelectingSuggestion = true;
                String selectedText = parent.getItemAtPosition(position).toString();
                Address address = null;
                for (Address addr : currentSuggestionAddresses) {
                    if (getAddressString(addr).equalsIgnoreCase(selectedText)) {
                        address = addr;
                        break;
                    }
                }

                if (address != null) {
                    GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());
                    dropMarker(point, true);
                    mapView.getController().setZoom(17.0);
                    etSearch.setText(selectedText, false);
                } else {
                    // For static local suggestions, perform geocoded lookup to pan/pin
                    searchLocation(selectedText);
                    etSearch.setText(selectedText, false);
                }
                etSearch.dismissDropDown();
                isSelectingSuggestion = false;
            }
        });

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

    private void fetchSearchSuggestions(String query) {
        // 1. Get instant offline suggestions from static LOCAL_LOCATIONS list
        List<String> localMatches = getLocalSuggestions(query);

        requireActivity().runOnUiThread(() -> {
            if (isAdded() && etSearch != null) {
                searchSuggestionsAdapter.clear();
                searchSuggestionsAdapter.addAll(localMatches);
                searchSuggestionsAdapter.notifyDataSetChanged();
                if (etSearch.hasFocus() && !localMatches.isEmpty()) {
                    etSearch.showDropDown();
                }
            }
        });

        // 2. Query Geocoder in background for additional dynamic suggestions
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> results = null;

                if (selectedPoint != null) {
                    double lowerLeftLat = selectedPoint.getLatitude() - 1.0;
                    double lowerLeftLng = selectedPoint.getLongitude() - 1.0;
                    double upperRightLat = selectedPoint.getLatitude() + 1.0;
                    double upperRightLng = selectedPoint.getLongitude() + 1.0;
                    try {
                        results = geocoder.getFromLocationName(query, 5, lowerLeftLat, lowerLeftLng, upperRightLat, upperRightLng);
                    } catch (Exception e) {
                        // ignore and try fallback
                    }
                }

                if (results == null || results.isEmpty()) {
                    results = geocoder.getFromLocationName(query, 5);
                }

                if (results != null) {
                    final List<Address> finalResults = results;
                    List<String> geocodedTexts = new ArrayList<>();
                    for (Address addr : finalResults) {
                        geocodedTexts.add(getAddressString(addr));
                    }

                    requireActivity().runOnUiThread(() -> {
                        if (isAdded() && etSearch != null) {
                            currentSuggestionAddresses.clear();
                            currentSuggestionAddresses.addAll(finalResults);

                            // Combine local matches and geocoded results, avoiding duplicates
                            List<String> combined = new ArrayList<>(localMatches);
                            for (String geoText : geocodedTexts) {
                                if (!combined.contains(geoText)) {
                                    combined.add(geoText);
                                }
                            }

                            searchSuggestionsAdapter.clear();
                            searchSuggestionsAdapter.addAll(combined);
                            searchSuggestionsAdapter.notifyDataSetChanged();

                            if (etSearch.hasFocus() && !combined.isEmpty()) {
                                etSearch.showDropDown();
                            }
                        }
                    });
                }
            } catch (IOException e) {
                // Ignore background suggestion errors
            }
        }).start();
    }

    private void setupButtons() {
        btnUseMyLocation.setOnClickListener(v -> {
            getCurrentLocation(true);
        });

        MaterialButton btnNext = requireView().findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            if (!validateForm()) return;
            saveAndProceed();
        });

        // Replace requireActivity().onBackPressed() with:
        requireView().findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    // ─── VALIDATION + NAVIGATION ──────────────────────────────────────────────

    private boolean validateForm() {
        if (selectedPoint == null) {
            Toast.makeText(requireContext(), "Please select a location on the map.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etAge.getText() == null || etAge.getText().toString().trim().isEmpty()) {
            etAge.setError("Please enter approximate age.");
            etAge.requestFocus();
            return false;
        }
        if (actvSex.getText() == null || actvSex.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please select sex.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (etDescription.getText() == null || etDescription.getText().toString().trim().isEmpty()) {
            etDescription.setError("Please enter a description.");
            etDescription.requestFocus();
            return false;
        }
        if (selectedDateTime == null) {
            Toast.makeText(requireContext(),
                    "Please select the date and time when the individual was seen.",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveAndProceed() {
        if (!validateForm()) return;

        String locationText = tvSelectedLocation.getText().toString();

        Bundle args = new Bundle();
        args.putDouble("latitude",      selectedPoint.getLatitude());
        args.putDouble("longitude",     selectedPoint.getLongitude());
        args.putString("locationText",  locationText);
        args.putString("age",           etAge.getText().toString().trim());
        args.putString("sex",           actvSex.getText().toString().trim());
        args.putString("description",   etDescription.getText().toString().trim());
        args.putLong("seenAt",          selectedDateTime.getTimeInMillis());

        Navigation.findNavController(requireView())
                .navigate(R.id.submitReportStep2Fragment, args); // ← passes Bundle as nav args
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
    public void onResume() { super.onResume(); mapView.onResume(); }

    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }

    // ─── UTILITY ──────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboardAndClearFocus() {
        if (etSearch != null) {
            etSearch.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        }
    }

    public static class NoFilterAdapter<T> extends ArrayAdapter<T> {
        private final List<T> items;

        public NoFilterAdapter(android.content.Context context, int resource, List<T> objects) {
            super(context, resource, objects);
            this.items = objects;
        }

        @NonNull
        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = items;
                    results.count = items.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    return resultValue == null ? "" : resultValue.toString();
                }
            };
        }
    }
}