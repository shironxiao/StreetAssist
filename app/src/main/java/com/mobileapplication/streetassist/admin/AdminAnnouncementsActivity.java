package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.Dialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.Calendar;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;
import com.mobileapplication.streetassist.ui.auth.AdminLoginActivity;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminAnnouncementsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminAnnouncements";
    private DrawerLayout drawerLayout;
    private AdminNotificationBadgeHelper badgeHelper;
    private androidx.recyclerview.widget.RecyclerView rvAnnouncements;
    private com.mobileapplication.streetassist.admin.AnnouncementAdapter adapter;
    private List<Map<String, Object>> announcementList = new ArrayList<>();
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> proofImagePickerLauncher;
    private Uri selectedImageUri;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private TextView tvUploadStatus;
    private android.widget.LinearLayout currentDialogImagesLayout;
    private android.widget.HorizontalScrollView currentDialogImagesScroll;
    private String uploadingProofAnnouncementId;
    private boolean shouldAutoSetCaseClosed = false;
    private List<String> editExistingUrls = new ArrayList<>();
    private boolean isEditingAnnouncement = false;

    // Cloudinary Config
    private static final String CLOUD_NAME = "durqaiei1";
    private static final String UPLOAD_PRESET = "streetassist_unsigned";
    private static final String API_KEY = "938268411726485";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String selectedDate = "";
    private String selectedTime = "";
    private double selectedLat = 0, selectedLng = 0;
    private String selectedAddress = "";

    private android.view.View layoutSelectionActions;
    private com.google.android.material.button.MaterialButton btnCancelSelection;
    private com.google.android.material.button.MaterialButton btnDeleteSelected;
    private android.view.View layoutHeaderSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_announcements);

        drawerLayout = findViewById(R.id.drawer_layout);
        badgeHelper = new AdminNotificationBadgeHelper(this);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_announcements);

        // Fix Sidebar Obscuration
        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        // Verify Admin Role
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (!"admin".equals(role)) {
                        Toast.makeText(this, "Access Denied: Admin role required", Toast.LENGTH_LONG).show();
                        logout();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Role verification failed", e);
                    Toast.makeText(this, "Error verifying role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Preserve existing selections; do not clear the list here
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                if (uri != null && !selectedImageUris.contains(uri)) {
                                    selectedImageUris.add(uri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null && !selectedImageUris.contains(uri)) {
                                selectedImageUris.add(uri);
                            }
                        }
                        // Remove any accidental duplicates while preserving order
                        selectedImageUris = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(selectedImageUris));
                        selectedImageUri = selectedImageUris.isEmpty() ? null : selectedImageUris.get(0);

                        if (isEditingAnnouncement) {
                            updateEditImagesPreview();
                        } else {
                            updateDialogSelectedImagesText();
                            updateDialogSelectedImagesPreview();
                        }

                        Toast.makeText(this, selectedImageUris.size() + " image(s) selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        proofImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        List<Uri> uris = new ArrayList<>();
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                if (uri != null) {
                                    uris.add(uri);
                                }
                            }
                        } else if (result.getData().getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                uris.add(uri);
                            }
                        }

                        if (!uris.isEmpty() && uploadingProofAnnouncementId != null) {
                            // Add newly selected proof images to the existing list
                            selectedImageUris.addAll(uris);
                            // Optionally update UI preview for proof images (reuse existing method)
                            updateEditImagesPreview();
                            // Upload the selected proof images
                            uploadMultipleProofs(uploadingProofAnnouncementId, uris);
                        }
                    }
                }
        );

        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        rvAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new com.mobileapplication.streetassist.admin.AnnouncementAdapter(this, announcementList);
        rvAnnouncements.setAdapter(adapter);

        layoutSelectionActions = findViewById(R.id.layoutSelectionActions);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        layoutHeaderSection = findViewById(R.id.layoutHeaderSection);

        adapter.setSelectionListener(new com.mobileapplication.streetassist.admin.AnnouncementAdapter.OnSelectionListener() {
            @Override
            public void onSelectionChanged(int count) {
                if (btnDeleteSelected != null) {
                    btnDeleteSelected.setText("Delete (" + count + ")");
                }
            }

            @Override
            public void onSelectionModeStarted() {
                if (layoutSelectionActions != null) {
                    layoutSelectionActions.setVisibility(android.view.View.VISIBLE);
                }
                if (layoutHeaderSection != null) {
                    layoutHeaderSection.setVisibility(android.view.View.GONE);
                }
            }

            @Override
            public void onSelectionModeEnded() {
                if (layoutSelectionActions != null) {
                    layoutSelectionActions.setVisibility(android.view.View.GONE);
                }
                if (layoutHeaderSection != null) {
                    layoutHeaderSection.setVisibility(android.view.View.VISIBLE);
                }
            }
        });

        if (btnCancelSelection != null) {
            btnCancelSelection.setOnClickListener(v -> adapter.clearSelection());
        }

        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> deleteMultipleAnnouncements(adapter.getSelectedAnnouncementIds()));
        }

        MaterialButton btnAdd = findViewById(R.id.btnAddAnnouncement);
        btnAdd.setOnClickListener(v -> showAddAnnouncementDialog());

        // Check if opened from notification
        String targetAnnouncementId = getIntent().getStringExtra("announcementId");
        if (targetAnnouncementId != null) {
            getIntent().removeExtra("announcementId");
            showCommentsDialog(targetAnnouncementId);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                } else if (adapter.getSelectedAnnouncementIds().size() > 0) {
                    adapter.clearSelection();
                } else {
                    startActivity(new Intent(AdminAnnouncementsActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }
        });

        fetchAnnouncements();

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            Log.d(TAG, "Menu button clicked");
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                Log.e(TAG, "DrawerLayout is null!");
            }
        });
    }

    private void updateDialogSelectedImagesText() {
        if (tvUploadStatus != null) {
            if (selectedImageUris.size() > 1) {
                tvUploadStatus.setText(selectedImageUris.size() + " images selected!");
                tvUploadStatus.setTextColor(getResources().getColor(R.color.green_primary, getTheme()));
            } else if (selectedImageUris.size() == 1) {
                tvUploadStatus.setText("1 image selected!");
                tvUploadStatus.setTextColor(getResources().getColor(R.color.green_primary, getTheme()));
            } else {
                tvUploadStatus.setText("Upload Images");
                tvUploadStatus.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
        }
    }

    private void updateDialogSelectedImagesPreview() {
        if (currentDialogImagesLayout == null) return;
        currentDialogImagesLayout.removeAllViews();

        if (selectedImageUris.isEmpty()) {
            if (currentDialogImagesScroll != null) {
                currentDialogImagesScroll.setVisibility(android.view.View.GONE);
            }
            return;
        }

        if (currentDialogImagesScroll != null) {
            currentDialogImagesScroll.setVisibility(android.view.View.VISIBLE);
        }

        float density = getResources().getDisplayMetrics().density;
        int size = (int) (80 * density);
        int margin = (int) (8 * density);
        int corner = (int) (12 * density);

        for (int i = 0; i < selectedImageUris.size(); i++) {
            final Uri uri = selectedImageUris.get(i);

            // Container frame layout to allow delete icon overlay
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                    new android.widget.LinearLayout.LayoutParams(size + (int)(10 * density), size + (int)(10 * density));
            containerParams.setMargins(0, 0, margin, 0);
            container.setLayoutParams(containerParams);

            // Card view for rounded image
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            android.widget.FrameLayout.LayoutParams cardParams = 
                    new android.widget.FrameLayout.LayoutParams(size, size);
            cardParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
            card.setLayoutParams(cardParams);
            card.setRadius(corner);
            card.setCardElevation(1 * density);
            card.setStrokeWidth(0);

            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            
            com.bumptech.glide.Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(iv);

            card.addView(iv);
            container.addView(card);

            // Delete / Remove button overlay
            android.widget.ImageView btnDelete = new android.widget.ImageView(this);
            android.widget.FrameLayout.LayoutParams deleteParams = 
                    new android.widget.FrameLayout.LayoutParams((int)(20 * density), (int)(20 * density));
            deleteParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackgroundResource(R.drawable.bg_status_badge);
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444)); // red
            btnDelete.setColorFilter(android.graphics.Color.WHITE);
            btnDelete.setPadding((int)(3 * density), (int)(3 * density), (int)(3 * density), (int)(3 * density));
            btnDelete.setElevation(4 * density);
            btnDelete.setClickable(true);
            btnDelete.setFocusable(true);

            final int index = i;
            btnDelete.setOnClickListener(v -> {
                selectedImageUris.remove(index);
                if (selectedImageUris.isEmpty()) {
                    selectedImageUri = null;
                } else {
                    selectedImageUri = selectedImageUris.get(0);
                }
                updateDialogSelectedImagesText();
                updateDialogSelectedImagesPreview();
            });

            container.addView(btnDelete);
            currentDialogImagesLayout.addView(container);
        }
    }

    private void updateEditImagesPreview() {
        if (currentDialogImagesLayout == null) return;
        currentDialogImagesLayout.removeAllViews();

        int totalCount = editExistingUrls.size() + selectedImageUris.size();

        if (totalCount == 0) {
            if (currentDialogImagesScroll != null) {
                currentDialogImagesScroll.setVisibility(android.view.View.GONE);
            }
            if (tvUploadStatus != null) {
                tvUploadStatus.setText("Add Image");
                tvUploadStatus.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            return;
        }

        if (currentDialogImagesScroll != null) {
            currentDialogImagesScroll.setVisibility(android.view.View.VISIBLE);
        }

        if (tvUploadStatus != null) {
            tvUploadStatus.setText(totalCount + " image(s) loaded/selected");
            tvUploadStatus.setTextColor(getResources().getColor(R.color.green_primary, getTheme()));
        }

        float density = getResources().getDisplayMetrics().density;
        int size = (int) (80 * density);
        int margin = (int) (8 * density);
        int corner = (int) (12 * density);

        // 1. Show remaining existing URLs
        for (int i = 0; i < editExistingUrls.size(); i++) {
            final String url = editExistingUrls.get(i);

            // Container frame layout to allow delete icon overlay
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                    new android.widget.LinearLayout.LayoutParams(size + (int)(10 * density), size + (int)(10 * density));
            containerParams.setMargins(0, 0, margin, 0);
            container.setLayoutParams(containerParams);

            // Card view for rounded image
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            android.widget.FrameLayout.LayoutParams cardParams = 
                    new android.widget.FrameLayout.LayoutParams(size, size);
            cardParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
            card.setLayoutParams(cardParams);
            card.setRadius(corner);
            card.setCardElevation(1 * density);
            card.setStrokeWidth(0);

            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            
            com.bumptech.glide.Glide.with(this)
                    .load(url)
                    .centerCrop()
                    .into(iv);

            card.addView(iv);
            container.addView(card);

            // Delete button overlay
            android.widget.ImageView btnDelete = new android.widget.ImageView(this);
            android.widget.FrameLayout.LayoutParams deleteParams = 
                    new android.widget.FrameLayout.LayoutParams((int)(20 * density), (int)(20 * density));
            deleteParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackgroundResource(R.drawable.bg_status_badge);
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444)); // red
            btnDelete.setColorFilter(android.graphics.Color.WHITE);
            btnDelete.setPadding((int)(3 * density), (int)(3 * density), (int)(3 * density), (int)(3 * density));
            btnDelete.setElevation(4 * density);
            btnDelete.setClickable(true);
            btnDelete.setFocusable(true);

            final int index = i;
            btnDelete.setOnClickListener(v -> {
                editExistingUrls.remove(index);
                updateEditImagesPreview();
            });

            container.addView(btnDelete);
            currentDialogImagesLayout.addView(container);
        }

        // 2. Show new selected Uris
        for (int i = 0; i < selectedImageUris.size(); i++) {
            final Uri uri = selectedImageUris.get(i);

            // Container frame layout to allow delete icon overlay
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.LinearLayout.LayoutParams containerParams = 
                    new android.widget.LinearLayout.LayoutParams(size + (int)(10 * density), size + (int)(10 * density));
            containerParams.setMargins(0, 0, margin, 0);
            container.setLayoutParams(containerParams);

            // Card view for rounded image
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            android.widget.FrameLayout.LayoutParams cardParams = 
                    new android.widget.FrameLayout.LayoutParams(size, size);
            cardParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
            card.setLayoutParams(cardParams);
            card.setRadius(corner);
            card.setCardElevation(1 * density);
            card.setStrokeWidth(0);

            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            
            com.bumptech.glide.Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(iv);

            card.addView(iv);
            container.addView(card);

            // Delete button overlay
            android.widget.ImageView btnDelete = new android.widget.ImageView(this);
            android.widget.FrameLayout.LayoutParams deleteParams = 
                    new android.widget.FrameLayout.LayoutParams((int)(20 * density), (int)(20 * density));
            deleteParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setBackgroundResource(R.drawable.bg_status_badge);
            btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444)); // red
            btnDelete.setColorFilter(android.graphics.Color.WHITE);
            btnDelete.setPadding((int)(3 * density), (int)(3 * density), (int)(3 * density), (int)(3 * density));
            btnDelete.setElevation(4 * density);
            btnDelete.setClickable(true);
            btnDelete.setFocusable(true);

            final int index = i;
            btnDelete.setOnClickListener(v -> {
                selectedImageUris.remove(index);
                if (selectedImageUris.isEmpty()) {
                    selectedImageUri = null;
                } else {
                    selectedImageUri = selectedImageUris.get(0);
                }
                updateEditImagesPreview();
            });

            container.addView(btnDelete);
            currentDialogImagesLayout.addView(container);
        }
    }

    private boolean currentDialogScrollShowOrHide(boolean show) {
        if (currentDialogImagesScroll != null) {
            currentDialogImagesScroll.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        return !show;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (badgeHelper != null) {
            badgeHelper.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (badgeHelper != null) {
            badgeHelper.stopListening();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (id == R.id.nav_all_reports) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminReportsActivity.class));
            finish();
        } else if (id == R.id.nav_announcements) {
            // Already here
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminProfileActivity.class));
        } else if (id == R.id.nav_trash) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminTrashActivity.class));
            finish();
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminNotificationActivity.class));
            finish();
        } else if (id == R.id.nav_logout) {
            logout();
        } else {
            Toast.makeText(this, "Unknown navigation item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        }

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void logout() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                    .update("fcmToken", com.google.firebase.firestore.FieldValue.delete());
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAddAnnouncementDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);
        android.widget.EditText etName = dialogView.findViewById(R.id.etName);
        android.widget.EditText etAge = dialogView.findViewById(R.id.etAge);
        android.widget.AutoCompleteTextView etSex = dialogView.findViewById(R.id.etSex);
        android.widget.EditText etSubtitle = dialogView.findViewById(R.id.etSubtitle);
        android.widget.EditText etContact = dialogView.findViewById(R.id.etContact);
        android.widget.EditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.EditText etTime = dialogView.findViewById(R.id.etTime);
        android.widget.EditText etLocation = dialogView.findViewById(R.id.etLocation);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String contactNumber = documentSnapshot.getString("contactNumber");
                        if (contactNumber != null && !contactNumber.isEmpty()) {
                            etContact.setText(contactNumber);
                        }
                    });
        }

        // Set up Sex Dropdown
        String[] sexOptions = {"Male", "Female"};
        android.widget.ArrayAdapter<String> adapterSex = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sexOptions);
        etSex.setAdapter(adapterSex);
        etSex.setText("Male", false); // Default
        android.view.View containerUpload = dialogView.findViewById(R.id.containerUpload);
        tvUploadStatus = dialogView.findViewById(R.id.tvUploadStatus);
        currentDialogImagesLayout = dialogView.findViewById(R.id.layoutSelectedImages);
        currentDialogImagesScroll = dialogView.findViewById(R.id.scrollSelectedImages);
        android.view.View btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnPost = dialogView.findViewById(R.id.btnPost);

        isEditingAnnouncement = false;
        selectedImageUri = null;
        selectedImageUris.clear();
        editExistingUrls.clear();
        updateDialogSelectedImagesPreview();
        selectedDate = "";
        selectedTime = "";
        selectedLat = 0;
        selectedLng = 0;
        selectedAddress = "";



        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_StreetAssist)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Pickers
        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etTime.setOnClickListener(v -> showTimePicker(etTime));
        etLocation.setOnClickListener(v -> showMapPicker(etLocation));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        containerUpload.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.setAction(android.provider.MediaStore.ACTION_PICK_IMAGES);
                intent.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 10);
            } else {
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            try {
                imagePickerLauncher.launch(intent);
            } catch (Exception e) {
                try {
                    Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                    fallback.setType("image/*");
                    fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    imagePickerLauncher.launch(Intent.createChooser(fallback, "Select Images"));
                } catch (Exception ex) {
                    Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnPost.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String age = etAge.getText().toString();
            String sex = etSex.getText().toString();
            String title = name; // Use name as title
            String category = "MISSING PERSON";
            String subtitle = etSubtitle.getText().toString();
            String contact = etContact.getText().toString();

            if (!name.isEmpty()) {
                btnPost.setEnabled(false);
                btnPost.setText("Posting...");
                if (!selectedImageUris.isEmpty()) {
                    uploadMultipleImagesToCloudinary(selectedImageUris, title, name, age, sex, category, subtitle, contact, dialog);
                } else {
                    postToFirestoreWithImages(title, name, age, sex, category, subtitle, contact, "", new ArrayList<>(), dialog);
                }
            } else {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int dialogWidth = (int) (dm.widthPixels * 0.94f);
            // Allow height to wrap content but with max constraint
            dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showDatePicker(EditText et) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month + 1, dayOfMonth, year);
            et.setText(selectedDate);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(EditText et) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String amPm = hourOfDay < 12 ? "AM" : "PM";
            int hour = hourOfDay % 12;
            if (hour == 0) hour = 12;
            selectedTime = String.format(Locale.getDefault(), "%d:%02d %s", hour, minute, amPm);
            et.setText(selectedTime);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void showMapPicker(EditText et) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_map_picker);

        MapView map = dialog.findViewById(R.id.mapViewPicker);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseMap);
        com.google.android.material.button.MaterialButton btnSelect = dialog.findViewById(R.id.btnSelectLocation);
        com.google.android.material.button.MaterialButton btnCurrent = dialog.findViewById(R.id.btnCurrentLocation);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        final Marker marker = new Marker(map);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(marker);

        final GeoPoint[] selectedPoint = {null};

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                selectedPoint[0] = p;
                marker.setPosition(p);
                map.invalidate();
                btnSelect.setEnabled(true);
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        }));

        btnCurrent.setOnClickListener(v -> {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
            }
        });

        btnSelect.setOnClickListener(v -> {
            if (selectedPoint[0] != null) {
                selectedLat = selectedPoint[0].getLatitude();
                selectedLng = selectedPoint[0].getLongitude();
                
                new Thread(() -> {
                    try {
                        android.location.Geocoder geocoder = new android.location.Geocoder(this, Locale.getDefault());
                        List<android.location.Address> addresses = geocoder.getFromLocation(selectedLat, selectedLng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            selectedAddress = addresses.get(0).getAddressLine(0);
                        } else {
                            selectedAddress = String.format(Locale.getDefault(), "%.5f, %.5f", selectedLat, selectedLng);
                        }
                        runOnUiThread(() -> et.setText(selectedAddress));
                    } catch (Exception e) {
                        selectedAddress = String.format(Locale.getDefault(), "%.5f, %.5f", selectedLat, selectedLng);
                        runOnUiThread(() -> et.setText(selectedAddress));
                    }
                }).start();
                dialog.dismiss();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void uploadToCloudinary(Uri imageUri, String title, String name, String age, String sex, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to open image", Toast.LENGTH_SHORT).show());
                    return;
                }
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                String boundary = "----FormBoundary" + System.currentTimeMillis();
                String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                dos.writeBytes(API_KEY + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"announcement.jpg\"\r\n");
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

                JSONObject json = new JSONObject(new String(responseBytes));
                if (status == 200 && json.has("secure_url")) {
                    String imageUrl = json.getString("secure_url");
                    runOnUiThread(() -> postToFirestore(title, name, age, sex, category, subtitle, contact, imageUrl, dialog));
                } else {
                    String error = json.optString("error", "Upload failed");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Upload error: " + error, Toast.LENGTH_LONG).show();
                        android.widget.Button btnPost = dialog.findViewById(R.id.btnPost);
                        if (btnPost != null) {
                            btnPost.setEnabled(true);
                            btnPost.setText("Post Announcement");
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.widget.Button btnPost = dialog.findViewById(R.id.btnPost);
                    if (btnPost != null) {
                        btnPost.setEnabled(true);
                        btnPost.setText("Post Announcement");
                    }
                });
            }
        });
    }

    private void postToFirestore(String title, String name, String age, String sex, String category, String subtitle, String contact, String imageUrl, androidx.appcompat.app.AlertDialog dialog) {
        java.util.Map<String, Object> post = new java.util.HashMap<>();
        post.put("title", title);
        post.put("name", name);
        post.put("age", age);
        post.put("sex", sex);
        post.put("category", category);
        post.put("subtitle", subtitle);
        post.put("contact", contact);
        post.put("imageUrl", imageUrl);
        post.put("status", "Verified by Police"); // Default status
        post.put("date", new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        post.put("timestamp", com.google.firebase.Timestamp.now());

        // New fields from pickers
        post.put("incidentDate", selectedDate);
        post.put("incidentTime", selectedTime);
        post.put("locationAddress", selectedAddress);
        post.put("latitude", selectedLat);
        post.put("longitude", selectedLng);

        db.collection("announcements").add(post)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to post announcement", e);
                    Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.widget.Button btnPost = dialog.findViewById(R.id.btnPost);
                    if (btnPost != null) {
                        btnPost.setEnabled(true);
                        btnPost.setText("Post Announcement");
                    }
                });
    }

    private void uploadMultipleImagesToCloudinary(List<Uri> uris, String title, String name, String age, String sex, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
        if (uris == null || uris.isEmpty()) {
            postToFirestoreWithImages(title, name, age, sex, category, subtitle, contact, "", new ArrayList<>(), dialog);
            return;
        }

        Toast.makeText(this, "Uploading 1 of " + uris.size() + " images...", Toast.LENGTH_SHORT).show();
        List<String> uploadedUrls = new java.util.ArrayList<>();
        uploadNextImage(0, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
    }

    private void uploadNextImage(int index, List<Uri> uris, List<String> uploadedUrls, String title, String name, String age, String sex, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
        if (index >= uris.size()) {
            String primaryUrl = uploadedUrls.isEmpty() ? "" : uploadedUrls.get(0);
            runOnUiThread(() -> postToFirestoreWithImages(title, name, age, sex, category, subtitle, contact, primaryUrl, uploadedUrls, dialog));
            return;
        }

        Uri imageUri = uris.get(index);
        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to read image at index " + index, Toast.LENGTH_SHORT).show();
                        uploadNextImage(index + 1, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                    });
                    return;
                }
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                String boundary = "----FormBoundary" + System.currentTimeMillis();
                String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                dos.writeBytes(API_KEY + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"announcement_" + index + ".jpg\"\r\n");
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

                JSONObject json = new JSONObject(new String(responseBytes));
                if (status == 200 && json.has("secure_url")) {
                    String imageUrl = json.getString("secure_url");
                    uploadedUrls.add(imageUrl);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Uploaded " + (index + 1) + " of " + uris.size() + " images", Toast.LENGTH_SHORT).show();
                    uploadNextImage(index + 1, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to upload image " + index + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    uploadNextImage(index + 1, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                });
            }
        });
    }

    private void postToFirestoreWithImages(String title, String name, String age, String sex, String category, String subtitle, String contact, String imageUrl, List<String> imageUrls, androidx.appcompat.app.AlertDialog dialog) {
        java.util.Map<String, Object> post = new java.util.HashMap<>();
        post.put("title", title);
        post.put("name", name);
        post.put("age", age);
        post.put("sex", sex);
        post.put("category", category);
        post.put("subtitle", subtitle);
        post.put("contact", contact);
        post.put("imageUrl", imageUrl);
        post.put("announcementImages", imageUrls);
        post.put("status", "Verified by Police"); // Default status
        post.put("date", new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        post.put("timestamp", com.google.firebase.Timestamp.now());

        // New fields from pickers
        post.put("incidentDate", selectedDate);
        post.put("incidentTime", selectedTime);
        post.put("locationAddress", selectedAddress);
        post.put("latitude", selectedLat);
        post.put("longitude", selectedLng);

        db.collection("announcements").add(post)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to post announcement", e);
                    Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.widget.Button btnPost = dialog.findViewById(R.id.btnPost);
                    if (btnPost != null) {
                        btnPost.setEnabled(true);
                        btnPost.setText("Post Announcement");
                    }
                });
    }

    private Double parseDoubleSafely(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public void showEditAnnouncementDialog(String id, Map<String, Object> announcement) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);

        android.widget.TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        if (tvDialogTitle != null) {
            tvDialogTitle.setText("Edit Announcement");
        }

        android.widget.EditText etName = dialogView.findViewById(R.id.etName);
        android.widget.EditText etAge = dialogView.findViewById(R.id.etAge);
        android.widget.AutoCompleteTextView etSex = dialogView.findViewById(R.id.etSex);
        android.widget.EditText etSubtitle = dialogView.findViewById(R.id.etSubtitle);
        android.widget.EditText etContact = dialogView.findViewById(R.id.etContact);
        android.widget.EditText etDate = dialogView.findViewById(R.id.etDate);
        android.widget.EditText etTime = dialogView.findViewById(R.id.etTime);
        android.widget.EditText etLocation = dialogView.findViewById(R.id.etLocation);

        // Prepopulate
        if (etName != null) etName.setText((String) announcement.get("name"));
        if (etAge != null) etAge.setText((String) announcement.get("age"));
        if (etSex != null) {
            String[] sexOptions = {"Male", "Female"};
            android.widget.ArrayAdapter<String> adapterSex = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sexOptions);
            etSex.setAdapter(adapterSex);
            String sexVal = (String) announcement.get("sex");
            etSex.setText(sexVal != null ? sexVal : "Male", false);
        }
        if (etSubtitle != null) etSubtitle.setText((String) announcement.get("subtitle"));
        if (etContact != null) etContact.setText((String) announcement.get("contact"));
        if (etDate != null) etDate.setText((String) announcement.get("incidentDate"));
        if (etTime != null) etTime.setText((String) announcement.get("incidentTime"));
        if (etLocation != null) etLocation.setText((String) announcement.get("locationAddress"));

        // Setup Pickers using existing state
        selectedDate = (String) announcement.get("incidentDate");
        if (selectedDate == null) selectedDate = "";
        selectedTime = (String) announcement.get("incidentTime");
        if (selectedTime == null) selectedTime = "";
        selectedAddress = (String) announcement.get("locationAddress");
        if (selectedAddress == null) selectedAddress = "";

        Double latObj = parseDoubleSafely(announcement.get("latitude"));
        selectedLat = latObj != null ? latObj : 0.0;
        Double lngObj = parseDoubleSafely(announcement.get("longitude"));
        selectedLng = lngObj != null ? lngObj : 0.0;

        android.view.View containerUpload = dialogView.findViewById(R.id.containerUpload);
        tvUploadStatus = dialogView.findViewById(R.id.tvUploadStatus);
        currentDialogImagesLayout = dialogView.findViewById(R.id.layoutSelectedImages);
        currentDialogImagesScroll = dialogView.findViewById(R.id.scrollSelectedImages);

        isEditingAnnouncement = true;
        selectedImageUri = null;
        selectedImageUris.clear();
        editExistingUrls.clear();

        // Show current image upload state
        Object imgs = announcement.get("announcementImages");
        if (imgs instanceof List && !((List) imgs).isEmpty()) {
            for (Object o : (List<?>) imgs) {
                if (o instanceof String) {
                    editExistingUrls.add((String) o);
                }
            }
        } else {
            String singleImg = (String) announcement.get("imageUrl");
            if (singleImg != null && !singleImg.isEmpty()) {
                editExistingUrls.add(singleImg);
            }
        }
        updateEditImagesPreview();

        android.view.View btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnPost = dialogView.findViewById(R.id.btnPost);
        if (btnPost != null) {
            btnPost.setText("Save Changes");
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_StreetAssist)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Pickers
        if (etDate != null) etDate.setOnClickListener(v -> showDatePicker(etDate));
        if (etTime != null) etTime.setOnClickListener(v -> showTimePicker(etTime));
        if (etLocation != null) etLocation.setOnClickListener(v -> showMapPicker(etLocation));

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (containerUpload != null) {
            containerUpload.setOnClickListener(v -> {
                Intent intent = new Intent();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.setAction(android.provider.MediaStore.ACTION_PICK_IMAGES);
                    intent.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 10);
                } else {
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                try {
                    imagePickerLauncher.launch(intent);
                } catch (Exception e) {
                    try {
                        Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                        fallback.setType("image/*");
                        fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        imagePickerLauncher.launch(Intent.createChooser(fallback, "Select Images"));
                    } catch (Exception ex) {
                        Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> {
                String name = etName != null ? etName.getText().toString() : "";
                String age = etAge != null ? etAge.getText().toString() : "";
                String sex = etSex != null ? etSex.getText().toString() : "Male";
                String title = name;
                String category = "MISSING PERSON";
                String subtitle = etSubtitle != null ? etSubtitle.getText().toString() : "";
                String contact = etContact != null ? etContact.getText().toString() : "";

                if (!name.isEmpty()) {
                    btnPost.setEnabled(false);
                    btnPost.setText("Saving...");
                    if (!selectedImageUris.isEmpty()) {
                        // Upload new images to Cloudinary, then update Firestore
                        uploadMultipleImagesForEdit(id, selectedImageUris, title, name, age, sex, category, subtitle, contact, dialog);
                    } else {
                        // Keep remaining existing images, update other fields
                        String existingPrimary = editExistingUrls.isEmpty() ? "" : editExistingUrls.get(0);
                        updateFirestoreAnnouncement(id, title, name, age, sex, category, subtitle, contact, existingPrimary, editExistingUrls, dialog);
                    }
                } else {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int dialogWidth = (int) (dm.widthPixels * 0.94f);
            dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void uploadMultipleImagesForEdit(String id, List<Uri> uris, String title, String name, String age, String sex, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
        Toast.makeText(this, "Uploading 1 of " + uris.size() + " images...", Toast.LENGTH_SHORT).show();
        List<String> uploadedUrls = new java.util.ArrayList<>();
        uploadNextImageForEdit(0, id, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
    }

    private void uploadNextImageForEdit(int index, String id, List<Uri> uris, List<String> uploadedUrls, String title, String name, String age, String sex, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
        if (index >= uris.size()) {
            List<String> finalUrls = new ArrayList<>(editExistingUrls);
            finalUrls.addAll(uploadedUrls);
            String primaryUrl = finalUrls.isEmpty() ? "" : finalUrls.get(0);
            runOnUiThread(() -> updateFirestoreAnnouncement(id, title, name, age, sex, category, subtitle, contact, primaryUrl, finalUrls, dialog));
            return;
        }

        Uri imageUri = uris.get(index);
        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to read image at index " + index, Toast.LENGTH_SHORT).show();
                        uploadNextImageForEdit(index + 1, id, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                    });
                    return;
                }
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                String boundary = "----FormBoundary" + System.currentTimeMillis();
                String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                dos.writeBytes(API_KEY + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"announcement_edit_" + index + ".jpg\"\r\n");
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

                JSONObject json = new JSONObject(new String(responseBytes));
                if (status == 200 && json.has("secure_url")) {
                    String imageUrl = json.getString("secure_url");
                    uploadedUrls.add(imageUrl);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Uploaded " + (index + 1) + " of " + uris.size() + " images", Toast.LENGTH_SHORT).show();
                    uploadNextImageForEdit(index + 1, id, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to upload image " + index + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    uploadNextImageForEdit(index + 1, id, uris, uploadedUrls, title, name, age, sex, category, subtitle, contact, dialog);
                });
            }
        });
    }

    private void updateFirestoreAnnouncement(String id, String title, String name, String age, String sex, String category, String subtitle, String contact, String imageUrl, List<String> imageUrls, androidx.appcompat.app.AlertDialog dialog) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("title", title);
        updates.put("name", name);
        updates.put("age", age);
        updates.put("sex", sex);
        updates.put("category", category);
        updates.put("subtitle", subtitle);
        updates.put("contact", contact);

        // Only update image fields if new images were uploaded/supplied
        if (imageUrl != null && !imageUrl.isEmpty()) {
            updates.put("imageUrl", imageUrl);
        }
        if (imageUrls != null && !imageUrls.isEmpty()) {
            updates.put("announcementImages", imageUrls);
        }

        // New fields from pickers
        updates.put("incidentDate", selectedDate);
        updates.put("incidentTime", selectedTime);
        updates.put("locationAddress", selectedAddress);
        updates.put("latitude", selectedLat);
        updates.put("longitude", selectedLng);

        db.collection("announcements").document(id).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Announcement updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update announcement", e);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.widget.Button btnPost = dialog.findViewById(R.id.btnPost);
                    if (btnPost != null) {
                        btnPost.setEnabled(true);
                        btnPost.setText("Save Changes");
                    }
                });
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private void fetchAnnouncements() {
        db.collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }
                    if (value != null) {
                        announcementList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("id", doc.getId());
                                announcementList.add(data);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    public void showCommentsDialog(String announcementId) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_admin_announcement_comments, null);
        androidx.recyclerview.widget.RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        android.widget.TextView tvNoComments = dialogView.findViewById(R.id.tvNoComments);
        android.widget.ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
        com.google.android.material.chip.ChipGroup cgSort = dialogView.findViewById(R.id.cgSort);

        rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        List<Map<String, Object>> commentList = new ArrayList<>();
        
        // Simple inner adapter for comments
        androidx.recyclerview.widget.RecyclerView.Adapter commentAdapter = new androidx.recyclerview.widget.RecyclerView.Adapter() {
            @NonNull
            @Override
            public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                android.view.View v = getLayoutInflater().inflate(R.layout.item_comment, parent, false);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
                Map<String, Object> comment = commentList.get(position);
                android.widget.TextView tvName = holder.itemView.findViewById(R.id.tvCommentName);
                android.widget.TextView tvText = holder.itemView.findViewById(R.id.tvCommentText);
                android.widget.TextView tvTime = holder.itemView.findViewById(R.id.tvCommentTime);
                android.widget.TextView tvLocation = holder.itemView.findViewById(R.id.tvCommentLocation);
                android.widget.ImageView ivAvatar = holder.itemView.findViewById(R.id.ivCommentAvatar);

                tvName.setText(String.valueOf(comment.get("userName")));
                tvText.setText(String.valueOf(comment.get("text")));
                
                com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) comment.get("timestamp");
                if (ts != null) {
                    tvTime.setText(new java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault()).format(ts.toDate()));
                }

                String address = (String) comment.get("locationAddress");
                Double lat = parseDoubleSafely(comment.get("latitude"));
                Double lng = parseDoubleSafely(comment.get("longitude"));

                if (address != null && !address.isEmpty()) {
                    tvLocation.setVisibility(android.view.View.VISIBLE);
                    tvLocation.setText("Sighting: " + address);
                    if (lat != null && lng != null) {
                        Double finalLat = lat;
                        Double finalLng = lng;
                        tvLocation.setOnClickListener(v -> launchGoogleMaps(finalLat, finalLng, address));
                        tvLocation.setTextColor(getResources().getColor(R.color.blue_primary, getTheme()));
                    }
                } else {
                    tvLocation.setVisibility(android.view.View.GONE);
                }

                String avatarUrl = (String) comment.get("userAvatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    com.bumptech.glide.Glide.with(AdminAnnouncementsActivity.this).load(avatarUrl).circleCrop().into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }

            @Override
            public int getItemCount() { return commentList.size(); }
        };
        rvComments.setAdapter(commentAdapter);

        cgSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipNewest) {
                commentList.sort((c1, c2) -> {
                    com.google.firebase.Timestamp t1 = (com.google.firebase.Timestamp) c1.get("timestamp");
                    com.google.firebase.Timestamp t2 = (com.google.firebase.Timestamp) c2.get("timestamp");
                    if (t1 == null || t2 == null) return 0;
                    return t2.compareTo(t1); // Descending
                });
            } else if (checkedId == R.id.chipOldest) {
                commentList.sort((c1, c2) -> {
                    com.google.firebase.Timestamp t1 = (com.google.firebase.Timestamp) c1.get("timestamp");
                    com.google.firebase.Timestamp t2 = (com.google.firebase.Timestamp) c2.get("timestamp");
                    if (t1 == null || t2 == null) return 0;
                    return t1.compareTo(t2); // Ascending
                });
            }
            commentAdapter.notifyDataSetChanged();
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        progressBar.setVisibility(android.view.View.VISIBLE);
        final com.google.firebase.firestore.ListenerRegistration commentsListener = db.collection("announcements")
                .document(announcementId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        commentList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            commentList.add(doc.getData());
                        }
                        
                        // Sort by current selected chip
                        int checkedId = cgSort.getCheckedChipId();
                        if (checkedId == R.id.chipOldest) {
                            commentList.sort((c1, c2) -> {
                                com.google.firebase.Timestamp t1 = (com.google.firebase.Timestamp) c1.get("timestamp");
                                com.google.firebase.Timestamp t2 = (com.google.firebase.Timestamp) c2.get("timestamp");
                                if (t1 == null || t2 == null) return 0;
                                return t1.compareTo(t2); // Ascending
                            });
                        } else {
                            commentList.sort((c1, c2) -> {
                                com.google.firebase.Timestamp t1 = (com.google.firebase.Timestamp) c1.get("timestamp");
                                com.google.firebase.Timestamp t2 = (com.google.firebase.Timestamp) c2.get("timestamp");
                                if (t1 == null || t2 == null) return 0;
                                return t2.compareTo(t1); // Descending
                            });
                        }
                        
                        commentAdapter.notifyDataSetChanged();
                        tvNoComments.setVisibility(commentList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                    }
                });

        dialog.setOnDismissListener(dialogInterface -> {
            if (commentsListener != null) {
                commentsListener.remove();
            }
        });

        dialog.show();
    }

    public void deleteAnnouncement(String id) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("announcements").document(id).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void deleteMultipleAnnouncements(java.util.Set<String> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) return;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Announcements")
                .setMessage("Are you sure you want to delete the " + selectedIds.size() + " selected announcements?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (String id : selectedIds) {
                        batch.delete(db.collection("announcements").document(id));
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Announcements deleted successfully", Toast.LENGTH_SHORT).show();
                                adapter.clearSelection();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete announcements: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showUpdateStatusDialog(String id, String currentStatus) {
        String[] statuses = {"Verified by Police", "Search Ongoing", "Located Safely / Resolved", "Case Closed"};
        int checkedItem = -1;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(currentStatus)) {
                checkedItem = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setSingleChoiceItems(statuses, checkedItem, (dialog, which) -> {
                    String newStatus = statuses[which];
                    if ("Case Closed".equalsIgnoreCase(newStatus)) {
                        db.collection("announcements").document(id).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        List<String> proofImages = (List<String>) documentSnapshot.get("caseClosedImages");
                                        if (proofImages != null && !proofImages.isEmpty()) {
                                            updateStatusDirectly(id, newStatus, dialog);
                                        } else {
                                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                                    .setTitle("Proof Required")
                                                    .setMessage("You must upload a proof image (e.g. proof of safety or resolution) to close this case. Would you like to pick and upload a proof image now?")
                                                    .setPositiveButton("Upload Proof", (uploadDialog, uploadWhich) -> {
                                                        dialog.dismiss();
                                                        startProofImagePickerAndSetCaseClosed(id);
                                                    })
                                                    .setNegativeButton("Cancel", null)
                                                    .show();
                                        }
                                    } else {
                                        updateStatusDirectly(id, newStatus, dialog);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    updateStatusDirectly(id, newStatus, dialog);
                                });
                    } else {
                        updateStatusDirectly(id, newStatus, dialog);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatusDirectly(String id, String newStatus, android.content.DialogInterface dialog) {
        db.collection("announcements").document(id)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update status", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void startProofImagePickerAndSetCaseClosed(String announcementId) {
        this.shouldAutoSetCaseClosed = true;
        startProofImagePicker(announcementId);
    }

    private void showLocationOnMap(double lat, double lng) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void launchGoogleMaps(double lat, double lng, String label) {
        try {
            String uri = String.format(java.util.Locale.US, "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng, android.net.Uri.encode(label));
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng));
                startActivity(fallbackIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching Google Maps", e);
            Toast.makeText(this, "Could not open Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    public void startProofImagePicker(String announcementId) {
        this.uploadingProofAnnouncementId = announcementId;
        Intent intent = new Intent();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.setAction(android.provider.MediaStore.ACTION_PICK_IMAGES);
            intent.putExtra(android.provider.MediaStore.EXTRA_PICK_IMAGES_MAX, 10);
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        try {
            proofImagePickerLauncher.launch(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
                fallback.setType("image/*");
                fallback.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                proofImagePickerLauncher.launch(Intent.createChooser(fallback, "Select Proof Images"));
            } catch (Exception ex) {
                Toast.makeText(this, "No image picker available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadMultipleProofs(String id, List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        Toast.makeText(this, "Uploading 1 of " + uris.size() + " proof images...", Toast.LENGTH_SHORT).show();
        List<String> uploadedUrls = new java.util.ArrayList<>();
        uploadNextProofImage(0, id, uris, uploadedUrls);
    }

    private void uploadNextProofImage(int index, String id, List<Uri> uris, List<String> uploadedUrls) {
        if (index >= uris.size()) {
            runOnUiThread(() -> {
                Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("caseClosedImages", com.google.firebase.firestore.FieldValue.arrayUnion(uploadedUrls.toArray()));
                if (shouldAutoSetCaseClosed) {
                    updates.put("status", "Case Closed");
                }

                db.collection("announcements").document(id)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "All proof images uploaded successfully!", Toast.LENGTH_SHORT).show();
                            if (shouldAutoSetCaseClosed) {
                                Toast.makeText(this, "Status updated to: Case Closed", Toast.LENGTH_SHORT).show();
                            }
                            shouldAutoSetCaseClosed = false;
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            shouldAutoSetCaseClosed = false;
                        });
            });
            return;
        }

        Uri imageUri = uris.get(index);
        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to open image at index " + index, Toast.LENGTH_SHORT).show();
                        uploadNextProofImage(index + 1, id, uris, uploadedUrls);
                    });
                    return;
                }
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                String boundary = "----FormBoundary" + System.currentTimeMillis();
                String uploadUrl = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                dos.writeBytes(API_KEY + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"proof_" + index + ".jpg\"\r\n");
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

                JSONObject json = new JSONObject(new String(responseBytes));
                if (status == 200 && json.has("secure_url")) {
                    String imageUrl = json.getString("secure_url");
                    uploadedUrls.add(imageUrl);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Uploaded " + (index + 1) + " of " + uris.size() + " proofs", Toast.LENGTH_SHORT).show();
                    uploadNextProofImage(index + 1, id, uris, uploadedUrls);
                });
            } catch (Exception e) {
                Log.e(TAG, "Cloudinary upload failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Upload failed at index " + index + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    uploadNextProofImage(index + 1, id, uris, uploadedUrls);
                });
            }
        });
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
}
