package com.mobileapplication.streetassist.ui.resident.News;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.core.util.Pair;

import android.preference.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.RegisterActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecyclerView rvAnnouncements;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AnnouncementAdapter adapter;
    private final List<Announcement> allAnnouncements = new ArrayList<>();
    private final List<Announcement> filteredAnnouncements = new ArrayList<>();
    private boolean isGuestMode = false;
    private EditText etSearchNews;
    private MaterialButton btnDatePicker;
    private Chip chipDateFilter;
    private Long startDate = null, endDate = null;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // ── Pagination variables ────────────────────────────────────────────────
    private DocumentSnapshot lastVisible;
    private boolean isLastItemReached = false;
    private boolean isLoadingMore = false;
    private static final int PAGE_SIZE = 10;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize OSMDroid configuration
        org.osmdroid.config.Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    // Lifecycle for OSMDroid
    @Override
    public void onResume() {
        super.onResume();
        org.osmdroid.config.Configuration.getInstance().load(requireContext(),
                PreferenceManager.getDefaultSharedPreferences(requireContext()));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        isGuestMode = requireActivity().getIntent().getBooleanExtra("is_guest", false);

        rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        etSearchNews = view.findViewById(R.id.etSearchNews);
        btnDatePicker = view.findViewById(R.id.btnDatePicker);
        chipDateFilter = view.findViewById(R.id.chipDateFilter);

        adapter = new AnnouncementAdapter(filteredAnnouncements);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAnnouncements.setAdapter(adapter);

        setupSearchAndFilter();
        setupInfiniteScroll();
        loadAnnouncements(true);
    }

    private void setupInfiniteScroll() {
        rvAnnouncements.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null
                        && layoutManager.findLastCompletelyVisibleItemPosition() == filteredAnnouncements.size() - 1) {
                    if (!isLoadingMore && !isLastItemReached) {
                        loadAnnouncements(false);
                    }
                }
            }
        });
    }

    private void setupSearchAndFilter() {
        etSearchNews.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            startDate = null;
            endDate = null;
            chipDateFilter.setVisibility(View.GONE);
            loadAnnouncements(true);
        });

        btnDatePicker.setOnClickListener(v -> showDateRangePicker());

        chipDateFilter.setOnCloseIconClickListener(v -> {
            startDate = null;
            endDate = null;
            chipDateFilter.setVisibility(View.GONE);
            filterList(etSearchNews.getText().toString());
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showMapPickerAfterPermission();
        }
    }

    private void showMapPickerAfterPermission() {
        // This is a helper to find the ViewHolder that was requesting the map
        // but since fragments can have multiple VHs, it's simpler to just let the user
        // click again
        // or we can just open a generic picker. For now, a toast is safe.
        Toast.makeText(getContext(), "Permission granted! Please tap the location icon again.", Toast.LENGTH_SHORT)
                .show();
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setSelection(new Pair<>(
                        MaterialDatePicker.todayInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = selection.first;
            endDate = selection.second;

            // Adjust endDate to include the full day (end of day)
            if (endDate != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(endDate);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                endDate = cal.getTimeInMillis();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String rangeStr = sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));
            chipDateFilter.setText("Range: " + rangeStr);
            chipDateFilter.setVisibility(View.VISIBLE);

            filterList(etSearchNews.getText().toString());
        });

        picker.show(getChildFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void filterList(String query) {
        filteredAnnouncements.clear();

        for (Announcement a : allAnnouncements) {
            boolean matchesSearch = a.title != null && a.title.toLowerCase().contains(query.toLowerCase());

            boolean matchesDate = true;
            if (startDate != null && endDate != null && a.timestamp != null) {
                long time = a.timestamp.toDate().getTime();
                matchesDate = (time >= startDate && time <= endDate);
            }

            if (matchesSearch && matchesDate) {
                filteredAnnouncements.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredAnnouncements.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadAnnouncements(boolean isInitial) {
        if (isInitial) {
            isLastItemReached = false;
            lastVisible = null;
            showLoading(true);
        } else {
            isLoadingMore = true;
        }

        Query query = db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    swipeRefresh.setRefreshing(false);
                    if (isInitial) {
                        showLoading(false);
                        allAnnouncements.clear();
                    }
                    isLoadingMore = false;

                    List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                    for (DocumentSnapshot doc : docs) {
                        Announcement a = new Announcement();
                        a.id = doc.getId();
                        a.title = doc.getString("title");
                        a.subtitle = doc.getString("subtitle");
                        a.category = doc.getString("category");
                        a.contact = doc.getString("contact");
                        a.date = doc.getString("date");
                        a.imageUrl = doc.getString("imageUrl");
                        a.status = doc.getString("status");
                        a.name = doc.getString("name");
                        a.incidentDate = doc.getString("incidentDate");
                        a.incidentTime = doc.getString("incidentTime");
                        a.locationAddress = doc.getString("locationAddress");
                        a.latitude = doc.getDouble("latitude");
                        a.longitude = doc.getDouble("longitude");
                        a.timestamp = doc.getTimestamp("timestamp");
                        allAnnouncements.add(a);
                    }

                    if (docs.size() > 0) {
                        lastVisible = docs.get(docs.size() - 1);
                        if (docs.size() < PAGE_SIZE) {
                            isLastItemReached = true;
                        }
                    } else {
                        isLastItemReached = true;
                    }

                    filterList(etSearchNews.getText().toString());
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    showLoading(false);
                    isLoadingMore = false;
                    Toast.makeText(requireContext(),
                            "Failed to load: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvAnnouncements.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showCreateAccountDialog(String message) {
        if (getContext() == null)
            return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Create Account")
                .setMessage(message)
                .setPositiveButton("Yes",
                        (dialog, which) -> startActivity(new Intent(requireActivity(), RegisterActivity.class)))
                .setNegativeButton("No", null)
                .show();
    }

    private void showLocationOnMap(double lat, double lng) {
        if (getContext() == null)
            return;
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_map_view);
        MapView map = dialog.findViewById(R.id.mapViewOnly);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        GeoPoint point = new GeoPoint(lat, lng);
        map.getController().setZoom(17.0);
        map.getController().setCenter(point);
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Sighting Location");
        map.getOverlays().add(marker);
        btnClose.setOnClickListener(va -> dialog.dismiss());
        dialog.show();
    }

    public static class Announcement {
        public String id, title, subtitle, category, contact, date, imageUrl, status, name;
        public String incidentDate, incidentTime, locationAddress;
        public Double latitude, longitude;
        public Timestamp timestamp;
    }

    public static class Comment {
        public String id, userId, userName, userAvatarUrl, text, locationAddress;
        public Double latitude, longitude;
        public Timestamp timestamp;
    }

    private class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementVH> {
        private final List<Announcement> items;

        AnnouncementAdapter(List<Announcement> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public AnnouncementVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.news_announcement, parent, false);
            return new AnnouncementVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AnnouncementVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class AnnouncementVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvDate, tvContact, tvToggleComments, tvCommentCount, tvNoComments;
            ImageView ivBanner;
            androidx.recyclerview.widget.RecyclerView rvComments;
            EditText etComment;
            ImageButton btnSendComment, btnCommentLocation;
            LinearLayout commentSection, containerAddComment;
            ProgressBar commentsProgress;
            TextView tvAttachedLocation, tvCategory, tvStatusBadge, tvIncidentDateTime, tvLocation;
            LinearLayout containerIncidentInfo;

            private Double attachedLat = null, attachedLng = null;
            private String attachedAddress = null;

            private final List<Comment> comments = new ArrayList<>();
            private CommentAdapter commentAdapter;
            private boolean commentsVisible = false;
            private boolean commentsLoaded = false;

            private String currentAnnouncementId;
            private String currentAnnouncementStatus;

            AnnouncementVH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvContact = itemView.findViewById(R.id.tvContact);
                tvDate = itemView.findViewById(R.id.tvDate);
                ivBanner = itemView.findViewById(R.id.ivBanner);
                rvComments = itemView.findViewById(R.id.rvComments);
                tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
                tvNoComments = itemView.findViewById(R.id.tvNoComments);
                etComment = itemView.findViewById(R.id.etComment);
                btnSendComment = itemView.findViewById(R.id.btnSendComment);
                btnCommentLocation = itemView.findViewById(R.id.btnCommentLocation);
                tvAttachedLocation = itemView.findViewById(R.id.tvAttachedLocation);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
                tvIncidentDateTime = itemView.findViewById(R.id.tvIncidentDateTime);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                containerIncidentInfo = itemView.findViewById(R.id.containerIncidentInfo);
                commentSection = itemView.findViewById(R.id.commentSection);
                containerAddComment = itemView.findViewById(R.id.containerAddComment);
                tvToggleComments = itemView.findViewById(R.id.tvToggleComments);
                commentsProgress = itemView.findViewById(R.id.commentsProgress);

                commentAdapter = new CommentAdapter(comments);
                rvComments.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                rvComments.setAdapter(commentAdapter);
                rvComments.setNestedScrollingEnabled(false);
            }

            void bind(Announcement announcement) {
                this.currentAnnouncementId = announcement.id;
                this.currentAnnouncementStatus = announcement.status;
                commentsLoaded = false;
                commentsVisible = false;
                comments.clear();
                commentAdapter.notifyDataSetChanged();
                commentSection.setVisibility(View.GONE);
                tvToggleComments.setText("💬 View Comments");

                tvTitle.setText(announcement.title != null ? announcement.title : "");

                tvSubtitle.setText(announcement.subtitle != null ? announcement.subtitle : "");
                tvDate.setText(announcement.date != null ? announcement.date : "");

                if (announcement.contact != null && !announcement.contact.isEmpty()) {
                    tvContact.setVisibility(View.VISIBLE);
                    tvContact.setText("📞 " + announcement.contact);
                } else {
                    tvContact.setVisibility(View.GONE);
                }

                tvCategory.setText(announcement.category != null ? announcement.category : "ANNOUNCEMENT");

                // Bind Incident Info
                boolean hasIncidentInfo = (announcement.incidentDate != null && !announcement.incidentDate.isEmpty())
                        || (announcement.locationAddress != null && !announcement.locationAddress.isEmpty());

                if (hasIncidentInfo) {
                    containerIncidentInfo.setVisibility(View.VISIBLE);
                    String dateTime = "";
                    if (announcement.incidentDate != null && !announcement.incidentDate.isEmpty())
                        dateTime += "📅 " + announcement.incidentDate;
                    if (announcement.incidentTime != null && !announcement.incidentTime.isEmpty())
                        dateTime += (dateTime.isEmpty() ? "" : " at ") + announcement.incidentTime;

                    tvIncidentDateTime.setVisibility(dateTime.isEmpty() ? View.GONE : View.VISIBLE);
                    tvIncidentDateTime.setText(dateTime);

                    if (announcement.locationAddress != null && !announcement.locationAddress.isEmpty()) {
                        tvLocation.setVisibility(View.VISIBLE);
                        tvLocation.setText("📍 " + announcement.locationAddress);

                        if (announcement.latitude != null && announcement.longitude != null) {
                            tvLocation.setOnClickListener(
                                    v -> showLocationOnMap(announcement.latitude, announcement.longitude));
                        } else {
                            tvLocation.setOnClickListener(null);
                        }
                    } else {
                        tvLocation.setVisibility(View.GONE);
                        tvLocation.setOnClickListener(null);
                    }
                } else {
                    containerIncidentInfo.setVisibility(View.GONE);
                }

                if (announcement.status != null && !announcement.status.isEmpty()) {
                    tvStatusBadge.setVisibility(View.VISIBLE);
                    tvStatusBadge.setText(announcement.status);
                } else {
                    // Default status if missing
                    tvStatusBadge.setVisibility(View.VISIBLE);
                    tvStatusBadge.setText("Verified by Police");
                }

                // Handle Case Closed - Disable new comments
                if ("Case Closed".equalsIgnoreCase(announcement.status)) {
                    containerAddComment.setVisibility(View.GONE);
                } else {
                    containerAddComment.setVisibility(View.VISIBLE);
                }

                if (announcement.imageUrl != null && !announcement.imageUrl.isEmpty()
                        && announcement.imageUrl.startsWith("http")) {
                    ivBanner.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(announcement.imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(ivBanner);
                    ivBanner.setOnClickListener(v -> showFullImageDialog(announcement.imageUrl));
                } else {
                    ivBanner.setImageResource(R.drawable.ic_image_placeholder);
                    ivBanner.setVisibility(View.VISIBLE); // Keep visible to show placeholder with badges
                    ivBanner.setOnClickListener(null);
                }

                tvToggleComments.setOnClickListener(v -> {
                    if (!commentsVisible) {
                        commentSection.setVisibility(View.VISIBLE);
                        tvToggleComments.setText("➖ Hide Comments");
                        commentsVisible = true;
                        if (!commentsLoaded)
                            loadComments(announcement.id);
                    } else {
                        commentSection.setVisibility(View.GONE);
                        tvToggleComments.setText("💬 View Comments");
                        commentsVisible = false;
                    }
                });

                btnSendComment.setOnClickListener(v -> {
                    if (isGuestMode) {
                        showCreateAccountDialog("Create account to comment on announcements?");
                        return;
                    }
                    String text = etComment.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        etComment.setError("Write something first");
                        return;
                    }

                    if (attachedLat == null || attachedLng == null) {
                        Toast.makeText(requireContext(),
                                        "📍 Please attach a location sighting to your comment.", Toast.LENGTH_LONG)
                                .show();
                        return;
                    }

                    postComment(announcement, text, etComment, btnSendComment);
                });

                btnCommentLocation.setOnClickListener(v -> {
                    if (isGuestMode) {
                        showCreateAccountDialog("Create account to attach location?");
                        return;
                    }
                    if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                                LOCATION_PERMISSION_REQUEST);
                    } else {
                        showMapPicker();
                    }
                });
            }

            private void showMapPicker() {
                Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.setContentView(R.layout.dialog_map_picker);

                MapView map = dialog.findViewById(R.id.mapViewPicker);
                ImageButton btnClose = dialog.findViewById(R.id.btnCloseMap);
                MaterialButton btnSelect = dialog.findViewById(R.id.btnSelectLocation);
                MaterialButton btnCurrent = dialog.findViewById(R.id.btnCurrentLocation);
                TextView tvStatus = dialog.findViewById(R.id.tvMapHint); // Reuse or find by id

                map.setTileSource(TileSourceFactory.MAPNIK);
                map.setMultiTouchControls(true);

                final Marker marker = new Marker(map);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(marker);

                final GeoPoint[] selectedPoint = { null };

                // Auto-center on current location
                FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(requireActivity());
                if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    client.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint p = new GeoPoint(location.getLatitude(), location.getLongitude());
                            selectedPoint[0] = p;
                            marker.setPosition(p);
                            map.getController().setZoom(17.0);
                            map.getController().setCenter(p);
                            btnSelect.setEnabled(true);
                        } else {
                            map.getController().setZoom(15.0);
                            map.getController().setCenter(new GeoPoint(14.6760, 121.0437));
                        }
                    });
                } else {
                    map.getController().setZoom(15.0);
                    map.getController().setCenter(new GeoPoint(14.6760, 121.0437));
                }

                map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint p) {
                        selectedPoint[0] = p;
                        marker.setPosition(p);
                        marker.setTitle("Pinned Sighting");
                        map.invalidate();
                        btnSelect.setEnabled(true);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint p) {
                        return false;
                    }
                }));

                btnCurrent.setOnClickListener(v -> {
                    client.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint p = new GeoPoint(location.getLatitude(), location.getLongitude());
                            selectedPoint[0] = p;
                            marker.setPosition(p);
                            map.getController().animateTo(p);
                            map.invalidate();
                            btnSelect.setEnabled(true);
                        }
                    });
                });

                btnSelect.setOnClickListener(v -> {
                    if (selectedPoint[0] != null) {
                        attachedLat = selectedPoint[0].getLatitude();
                        attachedLng = selectedPoint[0].getLongitude();

                        // Start reverse geocoding to get the address string
                        new Thread(() -> {
                            try {
                                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(),
                                        Locale.getDefault());
                                List<android.location.Address> addresses = geocoder.getFromLocation(attachedLat,
                                        attachedLng, 1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    android.location.Address addr = addresses.get(0);
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                                        sb.append(addr.getAddressLine(i));
                                        if (i < addr.getMaxAddressLineIndex())
                                            sb.append(", ");
                                    }
                                    attachedAddress = sb.toString();
                                } else {
                                    attachedAddress = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                            attachedLat, attachedLng);
                                }
                                requireActivity().runOnUiThread(() -> {
                                    tvAttachedLocation.setVisibility(View.VISIBLE);
                                    tvAttachedLocation.setText("📍 " + attachedAddress);
                                    btnCommentLocation.setColorFilter(
                                            ContextCompat.getColor(requireContext(), R.color.blue_primary));
                                });
                            } catch (Exception e) {
                                attachedAddress = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                                        attachedLat, attachedLng);
                                requireActivity().runOnUiThread(() -> {
                                    tvAttachedLocation.setVisibility(View.VISIBLE);
                                    tvAttachedLocation.setText("📍 Location pinned");
                                });
                            }
                        }).start();

                        dialog.dismiss();
                    }
                });

                btnClose.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }

            private void loadComments(String announcementId) {
                commentsProgress.setVisibility(View.VISIBLE);
                tvNoComments.setVisibility(View.GONE);
                db.collection("announcements")
                        .document(announcementId)
                        .collection("comments")
                        .orderBy("timestamp", Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            commentsLoaded = true;
                            commentsProgress.setVisibility(View.GONE);
                            comments.clear();
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Comment c = new Comment();
                                c.id = doc.getId();
                                c.userId = doc.getString("userId");
                                c.userName = doc.getString("userName");
                                c.userAvatarUrl = doc.getString("userAvatarUrl");
                                c.text = doc.getString("text");
                                c.latitude = doc.getDouble("latitude");
                                c.longitude = doc.getDouble("longitude");
                                c.locationAddress = doc.getString("locationAddress");
                                c.timestamp = doc.getTimestamp("timestamp");
                                comments.add(c);
                            }
                            commentAdapter.notifyDataSetChanged();
                            updateCommentCount(comments.size());
                            tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            commentsProgress.setVisibility(View.GONE);
                            Toast.makeText(itemView.getContext(),
                                    "Could not load comments", Toast.LENGTH_SHORT).show();
                        });
            }

            private void postComment(Announcement announcement, String text,
                                     EditText etComment, ImageButton btnSend) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(itemView.getContext(),
                            "Please log in to comment.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSend.setEnabled(false);
                db.collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String userName = userDoc.getString("fullName");
                            String userAvatarUrl = userDoc.getString("profileImageUrl");
                            if (userName == null || userName.isEmpty()) {
                                userName = userDoc.getString("username");
                            }
                            if (userName == null || userName.isEmpty()) {
                                userName = user.getDisplayName();
                            }
                            if (userName == null || userName.isEmpty()) {
                                userName = user.getEmail() != null ? user.getEmail() : "Resident";
                            }

                            Map<String, Object> commentData = new HashMap<>();
                            commentData.put("userId", user.getUid());
                            commentData.put("userName", userName);
                            commentData.put("userAvatarUrl", userAvatarUrl != null ? userAvatarUrl : "");
                            commentData.put("text", text);
                            commentData.put("timestamp", Timestamp.now());

                            // Location is guaranteed to be non-null here due to validation in listener
                            commentData.put("latitude", attachedLat);
                            commentData.put("longitude", attachedLng);
                            commentData.put("locationAddress", attachedAddress);

                            final String finalUserName = userName;
                            final String finalUserAvatarUrl = userAvatarUrl != null ? userAvatarUrl : "";

                            db.collection("announcements")
                                    .document(announcement.id)
                                    .collection("comments")
                                    .add(commentData)
                                    .addOnSuccessListener(docRef -> {
                                        btnSend.setEnabled(true);
                                        etComment.setText("");
                                        Comment newComment = new Comment();
                                        newComment.id = docRef.getId();
                                        newComment.userId = user.getUid();
                                        newComment.userName = finalUserName;
                                        newComment.userAvatarUrl = finalUserAvatarUrl;
                                        newComment.text = text;
                                        newComment.latitude = attachedLat;
                                        newComment.longitude = attachedLng;
                                        newComment.locationAddress = attachedAddress;
                                        newComment.timestamp = Timestamp.now();

                                        // Reset attached location after post
                                        attachedLat = null;
                                        attachedLng = null;
                                        attachedAddress = null;
                                        tvAttachedLocation.setVisibility(View.GONE);
                                        btnCommentLocation.setColorFilter(null);

                                        comments.add(newComment);
                                        commentAdapter.notifyItemInserted(comments.size() - 1);
                                        rvComments.scrollToPosition(comments.size() - 1);
                                        updateCommentCount(comments.size());
                                        tvNoComments.setVisibility(View.GONE);

                                        // Send notification to Admin
                                        Map<String, Object> notification = new HashMap<>();
                                        notification.put("title", "New Comment");
                                        notification.put("message",
                                                finalUserName + " commented on: " + announcement.title);
                                        notification.put("type", "new_comment");
                                        notification.put("referenceId", announcement.id);
                                        notification.put("createdAt",
                                                com.google.firebase.firestore.FieldValue.serverTimestamp());
                                        notification.put("isRead", false);

                                        db.collection("admin_notifications").add(notification);
                                    })
                                    .addOnFailureListener(e -> {
                                        btnSend.setEnabled(true);
                                        Toast.makeText(itemView.getContext(),
                                                "Failed to post comment", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(itemView.getContext(),
                                    "Could not fetch user info", Toast.LENGTH_SHORT).show();
                        });
            }

            private void updateCommentCount(int count) {
                tvCommentCount.setText(count + (count == 1 ? " Comment" : " Comments"));
            }

            private void showFullImageDialog(String imageUrl) {
                if (getContext() == null)
                    return;
                Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.setContentView(R.layout.dialog_fullscreen_image);

                ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
                ImageButton btnClose = dialog.findViewById(R.id.btnCloseImage);

                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .fitCenter()
                        .into(ivFullscreen);

                btnClose.setOnClickListener(v -> dialog.dismiss());
                ivFullscreen.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }

            private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentVH> {
                private final List<Comment> items;

                CommentAdapter(List<Comment> items) {
                    this.items = items;
                }

                @NonNull
                @Override
                public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_comment, parent, false);
                    return new CommentVH(v);
                }

                @Override
                public void onBindViewHolder(@NonNull CommentVH holder, int position) {
                    holder.bind(items.get(position));
                }

                @Override
                public int getItemCount() {
                    return items.size();
                }

                class CommentVH extends RecyclerView.ViewHolder {
                    ImageView ivAvatar;
                    TextView tvName, tvText, tvTime;
                    TextView btnViewLocation;
                    TextView btnEdit, btnDelete;

                    CommentVH(@NonNull View itemView) {
                        super(itemView);
                        ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
                        tvName = itemView.findViewById(R.id.tvCommentName);
                        tvText = itemView.findViewById(R.id.tvCommentText);
                        tvTime = itemView.findViewById(R.id.tvCommentTime);
                        btnViewLocation = itemView.findViewById(R.id.btnViewLocation);
                        btnEdit = itemView.findViewById(R.id.btnEditComment);
                        btnDelete = itemView.findViewById(R.id.btnDeleteComment);
                    }

                    void bind(Comment comment) {
                        tvName.setText(comment.userName != null ? comment.userName : "Resident");
                        tvText.setText(comment.text);

                        if (comment.timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
                            tvTime.setText(sdf.format(comment.timestamp.toDate()));
                        }

                        String currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
                        boolean isAuthor = comment.userId != null && comment.userId.equals(currentUid);
                        boolean isClosed = "Case Closed".equalsIgnoreCase(currentAnnouncementStatus);

                        if (isAuthor && !isClosed) {
                            btnEdit.setVisibility(View.VISIBLE);
                            btnDelete.setVisibility(View.VISIBLE);
                        } else {
                            btnEdit.setVisibility(View.GONE);
                            btnDelete.setVisibility(View.GONE);
                        }

                        btnDelete.setOnClickListener(v -> showDeleteConfirmation(comment));
                        btnEdit.setOnClickListener(v -> showEditDialog(comment));

                        if (comment.userAvatarUrl != null && !comment.userAvatarUrl.isEmpty()
                                && comment.userAvatarUrl.startsWith("http")) {
                            Glide.with(itemView.getContext())
                                    .load(comment.userAvatarUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .circleCrop()
                                    .into(ivAvatar);
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                        }

                        if (comment.latitude != null && comment.longitude != null) {
                            btnViewLocation.setVisibility(View.VISIBLE);
                            if (comment.locationAddress != null && !comment.locationAddress.isEmpty()) {
                                btnViewLocation.setText("📍 " + comment.locationAddress);
                            } else {
                                btnViewLocation.setText("📍 View Sighting Location");
                            }
                            btnViewLocation
                                    .setOnClickListener(v -> showLocationOnMap(comment.latitude, comment.longitude));
                        } else {
                            btnViewLocation.setVisibility(View.GONE);
                        }
                    }

                    private void showDeleteConfirmation(Comment comment) {
                        new android.app.AlertDialog.Builder(itemView.getContext())
                                .setTitle("Delete Sighting")
                                .setMessage("Are you sure you want to delete this sighting report?")
                                .setPositiveButton("Delete", (dialog, which) -> deleteComment(comment))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }

                    private void deleteComment(Comment comment) {
                        db.collection("announcements").document(currentAnnouncementId)
                                .collection("comments").document(comment.id)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    int pos = comments.indexOf(comment);
                                    if (pos != -1) {
                                        comments.remove(pos);
                                        commentAdapter.notifyItemRemoved(pos);
                                        updateCommentCount(comments.size());
                                        if (comments.isEmpty())
                                            tvNoComments.setVisibility(View.VISIBLE);
                                    }
                                });
                    }

                    private void showEditDialog(Comment comment) {
                        EditText etEdit = new EditText(itemView.getContext());
                        etEdit.setText(comment.text);
                        int padding = (int) (16 * itemView.getContext().getResources().getDisplayMetrics().density);
                        etEdit.setPadding(padding, padding, padding, padding);

                        new android.app.AlertDialog.Builder(itemView.getContext())
                                .setTitle("Edit Sighting Description")
                                .setView(etEdit)
                                .setPositiveButton("Update", (dialog, which) -> {
                                    String newText = etEdit.getText().toString().trim();
                                    if (!newText.isEmpty())
                                        updateComment(comment, newText);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }

                    private void updateComment(Comment comment, String newText) {
                        db.collection("announcements").document(currentAnnouncementId)
                                .collection("comments").document(comment.id)
                                .update("text", newText)
                                .addOnSuccessListener(aVoid -> {
                                    comment.text = newText;
                                    int pos = comments.indexOf(comment);
                                    if (pos != -1)
                                        commentAdapter.notifyItemChanged(pos);
                                });
                    }

                }
            }
        }
    }
}