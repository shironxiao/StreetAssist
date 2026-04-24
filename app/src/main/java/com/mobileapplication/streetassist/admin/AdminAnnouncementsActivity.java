package com.mobileapplication.streetassist.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminAnnouncementsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminAnnouncements";
    private DrawerLayout drawerLayout;
    private androidx.recyclerview.widget.RecyclerView rvAnnouncements;
    private com.mobileapplication.streetassist.admin.AnnouncementAdapter adapter;
    private List<Map<String, Object>> announcementList = new ArrayList<>();
    private FirebaseFirestore db;

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

        rvAnnouncements = findViewById(R.id.rvAnnouncements);
        rvAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new com.mobileapplication.streetassist.admin.AnnouncementAdapter(this, announcementList);
        rvAnnouncements.setAdapter(adapter);

        MaterialButton btnAdd = findViewById(R.id.btnAddAnnouncement);
        btnAdd.setOnClickListener(v -> showAddAnnouncementDialog());

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
        com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        com.google.android.material.textfield.TextInputEditText etCategory = dialogView.findViewById(R.id.etCategory);
        com.google.android.material.textfield.TextInputEditText etSubtitle = dialogView.findViewById(R.id.etSubtitle);
        com.google.android.material.textfield.TextInputEditText etContact = dialogView.findViewById(R.id.etContact);
        com.google.android.material.textfield.TextInputEditText etImageUrl = dialogView.findViewById(R.id.etImageUrl);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Post", (dialog, which) -> {
                    String title = etTitle.getText().toString();
                    String category = etCategory.getText().toString();
                    String subtitle = etSubtitle.getText().toString();
                    String contact = etContact.getText().toString();
                    String imageUrl = etImageUrl.getText().toString();

                    if (!title.isEmpty() && !category.isEmpty()) {
                        java.util.Map<String, Object> post = new java.util.HashMap<>();
                        post.put("title", title);
                        post.put("category", category);
                        post.put("subtitle", subtitle);
                        post.put("contact", contact);
                        post.put("imageUrl", imageUrl);
                        post.put("date", new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(new java.util.Date()));
                        post.put("timestamp", com.google.firebase.Timestamp.now());

                        db.collection("announcements").add(post)
                                .addOnSuccessListener(doc -> Toast.makeText(this, "Posted successfully!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Title and Category are required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                                announcementList.add(data);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}