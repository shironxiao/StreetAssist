package com.mobileapplication.streetassist.ui.resident.News;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewsFragment extends Fragment {

    // ── Firestore ─────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView rvAnnouncements;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    // ── Adapter ───────────────────────────────────────────────────────────────
    private AnnouncementAdapter adapter;
    private final List<Announcement> announcements = new ArrayList<>();

    // =========================================================================
    //  Fragment lifecycle
    // =========================================================================

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        rvAnnouncements = view.findViewById(R.id.rvAnnouncements);
        progressBar     = view.findViewById(R.id.progressBar);
        tvEmpty         = view.findViewById(R.id.tvEmpty);

        // RecyclerView setup
        adapter = new AnnouncementAdapter(announcements);
        rvAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAnnouncements.setAdapter(adapter);

        loadAnnouncements();
    }

    // =========================================================================
    //  Data loading
    // =========================================================================

    private void loadAnnouncements() {
        showLoading(true);

        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    showLoading(false);
                    announcements.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Announcement a = new Announcement();
                        a.id        = doc.getId();
                        a.title     = doc.getString("title");
                        a.subtitle  = doc.getString("subtitle");
                        a.category  = doc.getString("category");
                        a.contact   = doc.getString("contact");
                        a.date      = doc.getString("date");
                        a.imageUrl  = doc.getString("imageUrl");
                        a.timestamp = doc.getTimestamp("timestamp");
                        announcements.add(a);
                    }

                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(announcements.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to load announcements: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvAnnouncements.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    // =========================================================================
    //  Data model
    // =========================================================================

    public static class Announcement {
        public String    id;
        public String    title;
        public String    subtitle;
        public String    category;
        public String    contact;
        public String    date;
        public String    imageUrl;
        public Timestamp timestamp;
    }

    public static class Comment {
        public String    id;
        public String    userId;
        public String    userName;
        public String    userAvatarUrl;  // Cloudinary URL stored in Firestore users/{uid}.profileImageUrl
        public String    text;
        public Timestamp timestamp;
        public String    lastSeenAddress;
        public Double    lastSeenLatitude;
        public Double    lastSeenLongitude;
    }

    // =========================================================================
    //  RecyclerView Adapter
    // =========================================================================

    private class AnnouncementAdapter
            extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementVH> {

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
        public int getItemCount() { return items.size(); }

        // ── ViewHolder ────────────────────────────────────────────────────────

        class AnnouncementVH extends RecyclerView.ViewHolder {

            // Card views
            TextView  tvCategory, tvTitle, tvSubtitle, tvContact, tvDate;
            ImageView ivBanner;

            // Comments section
            RecyclerView      rvComments;
            TextView          tvCommentCount, tvNoComments;
            EditText          etComment;
            ImageButton       btnSendComment;
            LinearLayout      commentSection;
            TextView          tvToggleComments, btnPinLocation, tvPinnedLocation;
            ProgressBar       commentsProgress;

            // Comment state
            private final List<Comment>   comments = new ArrayList<>();
            private       CommentAdapter  commentAdapter;
            private       boolean         commentsVisible = false;
            private       boolean         commentsLoaded  = false;
            private       GeoPoint        selectedCommentPoint;
            private       String          selectedCommentAddress;

            AnnouncementVH(@NonNull View itemView) {
                super(itemView);

                tvCategory       = itemView.findViewById(R.id.tvCategory);
                tvTitle          = itemView.findViewById(R.id.tvTitle);
                tvSubtitle       = itemView.findViewById(R.id.tvSubtitle);
                tvContact        = itemView.findViewById(R.id.tvContact);
                tvDate           = itemView.findViewById(R.id.tvDate);
                ivBanner         = itemView.findViewById(R.id.ivBanner);

                rvComments       = itemView.findViewById(R.id.rvComments);
                tvCommentCount   = itemView.findViewById(R.id.tvCommentCount);
                tvNoComments     = itemView.findViewById(R.id.tvNoComments);
                etComment        = itemView.findViewById(R.id.etComment);
                btnSendComment   = itemView.findViewById(R.id.btnSendComment);
                commentSection   = itemView.findViewById(R.id.commentSection);
                tvToggleComments = itemView.findViewById(R.id.tvToggleComments);
                btnPinLocation   = itemView.findViewById(R.id.btnPinLocation);
                tvPinnedLocation = itemView.findViewById(R.id.tvPinnedLocation);
                commentsProgress = itemView.findViewById(R.id.commentsProgress);

                // Setup comments RecyclerView
                commentAdapter = new CommentAdapter(comments);
                rvComments.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                rvComments.setAdapter(commentAdapter);
                rvComments.setNestedScrollingEnabled(false);
            }

            void bind(Announcement announcement) {
                // Reset comment state on rebind (important for RecyclerView recycling)
                commentsLoaded  = false;
                commentsVisible = false;
                comments.clear();
                commentAdapter.notifyDataSetChanged();
                commentSection.setVisibility(View.GONE);
                tvToggleComments.setText("💬 View Comments");
                selectedCommentPoint = null;
                selectedCommentAddress = null;
                tvPinnedLocation.setText("No pinned location");
                tvPinnedLocation.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));

                // ── Populate card ──────────────────────────────────────────
                tvTitle.setText(announcement.title != null ? announcement.title : "");
                tvSubtitle.setText(announcement.subtitle != null ? announcement.subtitle : "");
                tvCategory.setText(announcement.category != null ? announcement.category : "General");
                tvDate.setText(announcement.date != null ? announcement.date : "");

                if (announcement.contact != null && !announcement.contact.isEmpty()) {
                    tvContact.setVisibility(View.VISIBLE);
                    tvContact.setText("📞 " + announcement.contact);
                } else {
                    tvContact.setVisibility(View.GONE);
                }

                // ── Banner image via Cloudinary URL ────────────────────────
                if (announcement.imageUrl != null && !announcement.imageUrl.isEmpty()
                        && announcement.imageUrl.startsWith("http")) {
                    ivBanner.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(announcement.imageUrl)
                            .placeholder(R.drawable.ic_image_placeholder) // add this drawable
                            .centerCrop()
                            .into(ivBanner);
                } else {
                    ivBanner.setVisibility(View.GONE);
                }

                // ── Toggle comments section ────────────────────────────────
                tvToggleComments.setOnClickListener(v -> {
                    if (!commentsVisible) {
                        commentSection.setVisibility(View.VISIBLE);
                        tvToggleComments.setText("🔼 Hide Comments");
                        commentsVisible = true;
                        if (!commentsLoaded) {
                            loadComments(announcement.id);
                        }
                    } else {
                        commentSection.setVisibility(View.GONE);
                        tvToggleComments.setText("💬 View Comments");
                        commentsVisible = false;
                    }
                });

                // ── Send comment ───────────────────────────────────────────
                btnSendComment.setOnClickListener(v -> {
                    String text = etComment.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        etComment.setError("Write something first");
                        return;
                    }
                    if (selectedCommentPoint == null || TextUtils.isEmpty(selectedCommentAddress)) {
                        Toast.makeText(itemView.getContext(),
                                "Please pin a last seen location first.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    postComment(announcement.id, text, etComment, btnSendComment);
                });

                btnPinLocation.setOnClickListener(v -> openLocationPickerDialog());
            }

            // ── Load comments from Firestore ──────────────────────────────────

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
                                c.id           = doc.getId();
                                c.userId       = doc.getString("userId");
                                c.userName     = doc.getString("userName");
                                c.userAvatarUrl = doc.getString("userAvatarUrl");
                                c.text         = doc.getString("text");
                                c.timestamp    = doc.getTimestamp("timestamp");
                                c.lastSeenAddress = doc.getString("lastSeenAddress");
                                c.lastSeenLatitude = doc.getDouble("lastSeenLatitude");
                                c.lastSeenLongitude = doc.getDouble("lastSeenLongitude");
                                comments.add(c);
                            }

                            commentAdapter.notifyDataSetChanged();
                            updateCommentCount(comments.size());
                            tvNoComments.setVisibility(
                                    comments.isEmpty() ? View.VISIBLE : View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            commentsProgress.setVisibility(View.GONE);
                            Toast.makeText(itemView.getContext(),
                                    "Could not load comments", Toast.LENGTH_SHORT).show();
                        });
            }

            // ── Post a new comment ────────────────────────────────────────────

            private void postComment(String announcementId, String text,
                                     EditText etComment, ImageButton btnSend) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(itemView.getContext(),
                            "Please log in to comment.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSend.setEnabled(false);

                // Fetch the current user's Firestore profile to get name + Cloudinary avatar
                db.collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String userName      = userDoc.getString("fullName");
                            String userAvatarUrl = userDoc.getString("profileImageUrl"); // Cloudinary URL

                            if (userName == null || userName.isEmpty()) {
                                userName = user.getEmail() != null ? user.getEmail() : "Resident";
                            }

                            Map<String, Object> commentData = new HashMap<>();
                            commentData.put("userId",       user.getUid());
                            commentData.put("userName",     userName);
                            commentData.put("userAvatarUrl", userAvatarUrl != null ? userAvatarUrl : "");
                            commentData.put("text",         text);
                            commentData.put("timestamp",    Timestamp.now());
                            commentData.put("lastSeenAddress", selectedCommentAddress);
                            commentData.put("lastSeenLatitude", selectedCommentPoint.getLatitude());
                            commentData.put("lastSeenLongitude", selectedCommentPoint.getLongitude());

                            final String finalUserName      = userName;
                            final String finalUserAvatarUrl = userAvatarUrl != null ? userAvatarUrl : "";

                            db.collection("announcements")
                                    .document(announcementId)
                                    .collection("comments")
                                    .add(commentData)
                                    .addOnSuccessListener(docRef -> {
                                        btnSend.setEnabled(true);
                                        etComment.setText("");

                                        // Add optimistically to local list
                                        Comment newComment = new Comment();
                                        newComment.id           = docRef.getId();
                                        newComment.userId       = user.getUid();
                                        newComment.userName     = finalUserName;
                                        newComment.userAvatarUrl = finalUserAvatarUrl;
                                        newComment.text         = text;
                                        newComment.timestamp    = Timestamp.now();
                                        newComment.lastSeenAddress = selectedCommentAddress;
                                        newComment.lastSeenLatitude = selectedCommentPoint.getLatitude();
                                        newComment.lastSeenLongitude = selectedCommentPoint.getLongitude();
                                        comments.add(newComment);
                                        commentAdapter.notifyItemInserted(comments.size() - 1);
                                        rvComments.scrollToPosition(comments.size() - 1);
                                        updateCommentCount(comments.size());
                                        tvNoComments.setVisibility(View.GONE);
                                        selectedCommentPoint = null;
                                        selectedCommentAddress = null;
                                        tvPinnedLocation.setText("No pinned location");
                                        tvPinnedLocation.setTextColor(
                                                ContextCompat.getColor(itemView.getContext(),
                                                        R.color.text_secondary));
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

            private void openLocationPickerDialog() {
                View dialogView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.dialog_pick_location, null, false);
                MapView mapView = dialogView.findViewById(R.id.mapViewPicker);
                EditText etSearch = dialogView.findViewById(R.id.etSearchLocation);
                TextView tvSelected = dialogView.findViewById(R.id.tvSelectedLocationPreview);
                TextView btnUseMyLocation = dialogView.findViewById(R.id.btnUseMyLocationPicker);
                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(requireActivity());

                Configuration.getInstance().load(
                        requireContext(),
                        PreferenceManager.getDefaultSharedPreferences(requireContext()));
                Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

                mapView.setTileSource(TileSourceFactory.MAPNIK);
                mapView.setMultiTouchControls(true);
                mapView.getController().setZoom(15.0);

                GeoPoint defaultPoint = selectedCommentPoint != null
                        ? selectedCommentPoint
                        : new GeoPoint(14.6760, 121.0437);
                mapView.getController().setCenter(defaultPoint);

                Marker[] markerHolder = new Marker[1];
                GeoPoint[] selectedPointHolder = new GeoPoint[]{selectedCommentPoint};
                String[] selectedAddressHolder = new String[]{
                        selectedCommentAddress != null ? selectedCommentAddress : ""
                };

                if (selectedCommentPoint != null) {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(selectedCommentPoint);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle("Pinned Location");
                    markerHolder[0] = marker;
                    mapView.getOverlays().add(marker);
                    if (!TextUtils.isEmpty(selectedCommentAddress)) {
                        tvSelected.setText(selectedCommentAddress);
                    }
                }

                mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint p) {
                        dropMapMarker(mapView, markerHolder, selectedPointHolder,
                                selectedAddressHolder, tvSelected, p);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint p) {
                        return false;
                    }
                }));

                etSearch.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                            || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        searchLocationAndPin(etSearch.getText().toString().trim(), mapView,
                                markerHolder, selectedPointHolder, selectedAddressHolder, tvSelected);
                        return true;
                    }
                    return false;
                });

                btnUseMyLocation.setOnClickListener(v -> {
                    if (ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1003);
                        Toast.makeText(requireContext(),
                                "Allow location permission, then tap again.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                            dropMapMarker(mapView, markerHolder, selectedPointHolder,
                                    selectedAddressHolder, tvSelected, point);
                            mapView.getController().setZoom(17.0);
                        } else {
                            Toast.makeText(requireContext(),
                                    "Could not get current location.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                        .setView(dialogView)
                        .setPositiveButton("Use Location", (d, which) -> {
                            if (selectedPointHolder[0] == null
                                    || TextUtils.isEmpty(selectedAddressHolder[0])) {
                                Toast.makeText(itemView.getContext(),
                                        "Please pin a location on the map first.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            selectedCommentPoint = selectedPointHolder[0];
                            selectedCommentAddress = selectedAddressHolder[0];
                            tvPinnedLocation.setText(selectedCommentAddress);
                            tvPinnedLocation.setTextColor(
                                    ContextCompat.getColor(itemView.getContext(), R.color.blue_primary));
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
            }

            private void dropMapMarker(MapView mapView,
                                       Marker[] markerHolder,
                                       GeoPoint[] selectedPointHolder,
                                       String[] selectedAddressHolder,
                                       TextView tvSelected,
                                       GeoPoint point) {
                if (markerHolder[0] != null) {
                    mapView.getOverlays().remove(markerHolder[0]);
                }
                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("Pinned Location");
                markerHolder[0] = marker;
                mapView.getOverlays().add(marker);
                mapView.getController().animateTo(point);
                mapView.invalidate();
                selectedPointHolder[0] = point;
                reverseGeocode(point, selectedAddressHolder, tvSelected);
            }

            private void reverseGeocode(GeoPoint point,
                                        String[] selectedAddressHolder,
                                        TextView tvSelected) {
                new Thread(() -> {
                    String fallback = String.format(Locale.getDefault(),
                            "Lat: %.5f, Lng: %.5f", point.getLatitude(), point.getLongitude());
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(
                                point.getLatitude(), point.getLongitude(), 1);
                        String addressText = fallback;
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                sb.append(address.getAddressLine(i));
                                if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                            }
                            if (sb.length() > 0) addressText = sb.toString();
                        }
                        final String finalAddressText = addressText;
                        selectedAddressHolder[0] = finalAddressText;
                        requireActivity().runOnUiThread(() -> tvSelected.setText(finalAddressText));
                    } catch (IOException e) {
                        selectedAddressHolder[0] = fallback;
                        requireActivity().runOnUiThread(() -> tvSelected.setText(fallback));
                    }
                }).start();
            }

            private void searchLocationAndPin(String query,
                                              MapView mapView,
                                              Marker[] markerHolder,
                                              GeoPoint[] selectedPointHolder,
                                              String[] selectedAddressHolder,
                                              TextView tvSelected) {
                if (TextUtils.isEmpty(query)) return;
                new Thread(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                        List<Address> results = geocoder.getFromLocationName(query, 1);
                        if (results != null && !results.isEmpty()) {
                            Address address = results.get(0);
                            GeoPoint point = new GeoPoint(address.getLatitude(), address.getLongitude());
                            requireActivity().runOnUiThread(() -> {
                                dropMapMarker(mapView, markerHolder, selectedPointHolder,
                                        selectedAddressHolder, tvSelected, point);
                                mapView.getController().setZoom(17.0);
                            });
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(),
                                            "Location not found.", Toast.LENGTH_SHORT).show());
                        }
                    } catch (IOException e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Search failed. Check your connection.",
                                        Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        }
    }

    // =========================================================================
    //  Comment Adapter
    // =========================================================================

    private static class CommentAdapter
            extends RecyclerView.Adapter<CommentAdapter.CommentVH> {

        private final List<Comment> items;

        CommentAdapter(List<Comment> items) { this.items = items; }

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
        public int getItemCount() { return items.size(); }

        static class CommentVH extends RecyclerView.ViewHolder {

            ImageView ivAvatar;
            TextView  tvName, tvText, tvTime, tvLocation;

            CommentVH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivCommentAvatar);
                tvName   = itemView.findViewById(R.id.tvCommentName);
                tvText   = itemView.findViewById(R.id.tvCommentText);
                tvTime   = itemView.findViewById(R.id.tvCommentTime);
                tvLocation = itemView.findViewById(R.id.tvCommentLocation);
            }

            void bind(Comment comment) {
                tvName.setText(comment.userName != null ? comment.userName : "Resident");
                tvText.setText(comment.text != null ? comment.text : "");

                // Format timestamp
                if (comment.timestamp != null) {
                    Date date = comment.timestamp.toDate();
                    String formatted = new SimpleDateFormat(
                            "MMM d, yyyy · h:mm a", Locale.getDefault()).format(date);
                    tvTime.setText(formatted);
                }

                if (!TextUtils.isEmpty(comment.lastSeenAddress)) {
                    tvLocation.setVisibility(View.VISIBLE);
                    tvLocation.setText("📍 Last seen: " + comment.lastSeenAddress);
                } else if (comment.lastSeenLatitude != null && comment.lastSeenLongitude != null) {
                    tvLocation.setVisibility(View.VISIBLE);
                    tvLocation.setText(String.format(Locale.getDefault(),
                            "📍 Last seen: %.5f, %.5f",
                            comment.lastSeenLatitude,
                            comment.lastSeenLongitude));
                } else {
                    tvLocation.setVisibility(View.GONE);
                }

                // Load Cloudinary avatar via Glide
                if (comment.userAvatarUrl != null && !comment.userAvatarUrl.isEmpty()
                        && comment.userAvatarUrl.startsWith("http")) {
                    Glide.with(itemView.getContext())
                            .load(comment.userAvatarUrl)
                            .placeholder(R.drawable.ic_default_avatar) // add this drawable
                            .circleCrop()
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }
        }
    }
}