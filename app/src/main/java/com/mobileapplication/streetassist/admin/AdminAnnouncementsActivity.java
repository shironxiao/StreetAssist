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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;

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
    private androidx.recyclerview.widget.RecyclerView rvAnnouncements;
    private com.mobileapplication.streetassist.admin.AnnouncementAdapter adapter;
    private List<Map<String, Object>> announcementList = new ArrayList<>();
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private TextView tvUploadStatus;

    // Cloudinary Config
    private static final String CLOUD_NAME = "durqaiei1";
    private static final String UPLOAD_PRESET = "streetassist_unsigned";
    private static final String API_KEY = "938268411726485";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_announcements);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_announcements);

        // Fix Sidebar Obscuration
        navigationView.bringToFront();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        db = FirebaseFirestore.getInstance();

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (tvUploadStatus != null) {
                            tvUploadStatus.setText("Image selected!");
                            tvUploadStatus.setTextColor(getResources().getColor(R.color.green_primary, getTheme()));
                        }
                        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        rvAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new com.mobileapplication.streetassist.admin.AnnouncementAdapter(this, announcementList);
        rvAnnouncements.setAdapter(adapter);

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
        } else if (id == R.id.nav_trash) {
            startActivity(new Intent(this, com.mobileapplication.streetassist.admin.AdminTrashActivity.class));
            finish();
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, AdminNotificationActivity.class));
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
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, IntroductionUserLevel.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAddAnnouncementDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);
        android.widget.EditText etTitle = dialogView.findViewById(R.id.etTitle);
        android.widget.EditText etCategory = dialogView.findViewById(R.id.etCategory);
        android.widget.EditText etSubtitle = dialogView.findViewById(R.id.etSubtitle);
        android.widget.EditText etContact = dialogView.findViewById(R.id.etContact);
        android.view.View containerUpload = dialogView.findViewById(R.id.containerUpload);
        tvUploadStatus = dialogView.findViewById(R.id.tvUploadStatus);
        android.view.View btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnPost = dialogView.findViewById(R.id.btnPost);

        selectedImageUri = null; // Reset for new dialog

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_StreetAssist)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Make background transparent to show card corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked");
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        containerUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            imagePickerLauncher.launch(intent);
        });

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String category = etCategory.getText().toString();
            String subtitle = etSubtitle.getText().toString();
            String contact = etContact.getText().toString();

            if (!title.isEmpty() && !category.isEmpty()) {
                if (selectedImageUri != null) {
                    uploadToCloudinary(selectedImageUri, title, category, subtitle, contact, dialog);
                } else {
                    postToFirestore(title, category, subtitle, contact, "", dialog);
                }
            } else {
                Toast.makeText(this, "Title and Category are required", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            int dialogWidth = (int) (dm.widthPixels * 0.94f);
            int dialogHeight = (int) (dm.heightPixels * 0.88f);
            dialog.getWindow().setLayout(
                    dialogWidth,
                    dialogHeight
            );
        }
    }

    private void uploadToCloudinary(Uri imageUri, String title, String category, String subtitle, String contact, androidx.appcompat.app.AlertDialog dialog) {
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
                    runOnUiThread(() -> postToFirestore(title, category, subtitle, contact, imageUrl, dialog));
                } else {
                    String error = json.optString("error", "Upload failed");
                    runOnUiThread(() -> Toast.makeText(this, "Upload error: " + error, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void postToFirestore(String title, String category, String subtitle, String contact, String imageUrl, androidx.appcompat.app.AlertDialog dialog) {
        java.util.Map<String, Object> post = new java.util.HashMap<>();
        post.put("title", title);
        post.put("category", category);
        post.put("subtitle", subtitle);
        post.put("contact", contact);
        post.put("imageUrl", imageUrl);
        post.put("date", new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        post.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("announcements").add(post)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
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

                String address = (String) comment.get("lastSeenAddress");
                if (address != null && !address.isEmpty()) {
                    tvLocation.setVisibility(android.view.View.VISIBLE);
                    tvLocation.setText("📍 Last seen: " + address);
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

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        progressBar.setVisibility(android.view.View.VISIBLE);
        db.collection("announcements")
                .document(announcementId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    commentList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        commentList.add(doc.getData());
                    }
                    commentAdapter.notifyDataSetChanged();
                    tvNoComments.setVisibility(commentList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                });

        dialog.show();
    }


}