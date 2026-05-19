package com.mobileapplication.streetassist.admin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdminProfileActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminProfile";

    // Views
    private DrawerLayout drawerLayout;
    private CircleImageView ivProfilePhoto;
    private TextView tvProfileName, tvProfileSub;
    private TextView tvValFullName, tvValEmail, tvValContact;
    private FloatingActionButton fabEditPhoto;
    private MaterialButton btnLogout;
    private View rowFullName, rowContact, rowCreateAdmin;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Cloudinary Settings (matching ProfileFragment.java)
    private static final String CLOUD_NAME = "durqaiei1";
    private static final String UPLOAD_PRESET = "streetassist_unsigned";
    private static final String API_KEY = "938268411726485";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            logout();
            return;
        }

        // Verify Admin Role
        db.collection("users").document(currentUser.getUid()).get()
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

        // Setup Image Picker Launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadToCloudinary(imageUri);
                        }
                    }
                });

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_profile);

        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileSub = findViewById(R.id.tvProfileSub);
        tvValFullName = findViewById(R.id.tvValFullName);
        tvValEmail = findViewById(R.id.tvValEmail);
        tvValContact = findViewById(R.id.tvValContact);
        fabEditPhoto = findViewById(R.id.fabEditPhoto);
        btnLogout = findViewById(R.id.btnLogout);

        rowFullName = findViewById(R.id.rowFullName);
        rowContact = findViewById(R.id.rowContact);
        rowCreateAdmin = findViewById(R.id.rowCreateAdmin);

        // Click Listeners
        fabEditPhoto.setOnClickListener(v -> openImagePicker());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        rowCreateAdmin.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminRegisterActivity.class));
        });

        rowFullName.setOnClickListener(v -> showEditDialog("Edit Full Name", "fullName", tvValFullName.getText().toString(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS));
        rowContact.setOnClickListener(v -> showEditDialog("Edit Contact Number", "contactNumber", tvValContact.getText().toString(), InputType.TYPE_CLASS_PHONE));

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    startActivity(new Intent(AdminProfileActivity.this, AdminDashboardActivity.class));
                    finish();
                }
            }
        });

        // Load profile data
        loadAdminProfile();
    }

    private void loadAdminProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::populateUI)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists()) return;

        String fullName = doc.getString("fullName");
        String email = doc.getString("email");
        String role = doc.getString("role");
        String photoUrl = doc.getString("profilePhotoUrl");
        String contact = doc.getString("contactNumber");

        tvProfileName.setText(fullName != null ? fullName : "—");
        tvProfileSub.setText(role != null ? role.toUpperCase() : "ADMINISTRATOR");

        tvValFullName.setText(fullName != null ? fullName : "—");
        tvValEmail.setText(email != null ? email : "—");
        tvValContact.setText(contact != null ? contact : "—");

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivProfilePhoto);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_default_avatar)
                    .transform(new CircleCrop())
                    .into(ivProfilePhoto);
        }
    }

    private void showEditDialog(String title, String fieldName, String currentValue, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(inputType);
        if (!"—".equals(currentValue)) {
            input.setText(currentValue);
            input.setSelection(currentValue.length());
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        lp.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(lp);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (newValue.isEmpty()) {
                Toast.makeText(this, "Value cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfileField(fieldName, newValue);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateProfileField(String fieldName, String newValue) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put(fieldName, newValue);
        update.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(user.getUid())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    loadAdminProfile();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private void uploadToCloudinary(Uri imageUri) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
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
                dos.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n");
                dos.writeBytes("streetassist/profile_photos\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int status = conn.getResponseCode();
                InputStream responseStream = status == 200 ? conn.getInputStream() : conn.getErrorStream();
                byte[] responseBytes = readAllBytes(responseStream);
                responseStream.close();
                conn.disconnect();

                JSONObject json = new JSONObject(new String(responseBytes));

                if (status == 200 && json.has("secure_url")) {
                    String newPhotoUrl = json.getString("secure_url");
                    savePhotoUrlToFirestore(newPhotoUrl);
                } else {
                    String error = json.optString("error", "Upload failed");
                    runOnUiThread(() -> Toast.makeText(this, "Upload error: " + error, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void savePhotoUrlToFirestore(String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("profilePhotoUrl", photoUrl);
        update.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(user.getUid())
                .update(update)
                .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                    Glide.with(this)
                            .load(photoUrl)
                            .transform(new CircleCrop())
                            .error(R.drawable.ic_default_avatar)
                            .into(ivProfilePhoto);
                    tvProfileName.setText(tvProfileName.getText()); // trigger repaint
                    Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(this, "Failed to save photo: " + e.getMessage(), Toast.LENGTH_LONG).show()));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                    .update("fcmToken", FieldValue.delete());
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, IntroductionUserLevel.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        } else if (id == R.id.nav_all_reports) {
            startActivity(new Intent(this, AdminReportsActivity.class));
            finish();
        } else if (id == R.id.nav_announcements) {
            startActivity(new Intent(this, AdminAnnouncementsActivity.class));
            finish();
        } else if (id == R.id.nav_trash) {
            startActivity(new Intent(this, AdminTrashActivity.class));
            finish();
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, AdminNotificationActivity.class));
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
        }

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
