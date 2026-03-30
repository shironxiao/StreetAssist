package com.mobileapplication.streetassist.ui.resident.Profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.LoginActivity;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    // ── Views ────────────────────────────────────────────────────────────────
    private CircleImageView ivProfilePhoto;
    private TextView tvProfileName, tvProfileSub;
    private TextView tvValFullName, tvValEmail, tvValAddress;
    private TextView tvInitials;
    private FloatingActionButton fabEditPhoto;
    private MaterialButton btnLogout;
    private View rowFullName, rowEmail, rowAddress, rowPassword;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Cloudinary ───────────────────────────────────────────────────────────
    private static final String CLOUD_NAME  = "durqaiei1"; // replace with yours
    private static final String UPLOAD_PRESET = "streetassist_unsigned"; // create this in Cloudinary dashboard
    private static final String API_KEY     = "938268411726485";
    // NOTE: Never embed API_SECRET in client apps — use unsigned upload presets instead
    // Create an unsigned upload preset named "streetassist_unsigned" in:
    // Cloudinary Dashboard → Settings → Upload → Upload Presets → Add preset → Unsigned

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── Image picker ─────────────────────────────────────────────────────────
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // ── User data ─────────────────────────────────────────────────────────────
    private String currentPhotoUrl = null;

    // ─────────────────────────────────────────────────────────────────────────

    public ProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Register image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadToCloudinary(imageUri);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvProfileName  = view.findViewById(R.id.tvProfileName);
        tvProfileSub   = view.findViewById(R.id.tvProfileSub);
        tvValFullName  = view.findViewById(R.id.tvValFullName);
        tvValEmail     = view.findViewById(R.id.tvValEmail);
        tvValAddress   = view.findViewById(R.id.tvValAddress);
        fabEditPhoto   = view.findViewById(R.id.fabEditPhoto);
        btnLogout      = view.findViewById(R.id.btnLogout);
        rowFullName    = view.findViewById(R.id.rowFullName);
        rowEmail       = view.findViewById(R.id.rowEmail);
        rowAddress     = view.findViewById(R.id.rowAddress);
        rowPassword    = view.findViewById(R.id.rowPassword);

        // NOTE: Replace the MaterialCardView avatar TextView in your XML with
        // a CircleImageView (id: ivProfilePhoto) and keep the TextView (id: tvInitials)
        // behind it as a fallback. See XML note at the bottom of this file.
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        tvInitials     = view.findViewById(R.id.tvInitials);

        // Load user data from Firestore
        loadUserProfile();

        // FAB – pick photo
        fabEditPhoto.setOnClickListener(v -> openImagePicker());

        // Row clicks
        rowFullName.setOnClickListener(v -> showEditNameDialog());
        rowEmail.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Email cannot be changed here.", Toast.LENGTH_SHORT).show());
        rowAddress.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Address editing coming soon.", Toast.LENGTH_SHORT).show());
        rowPassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // =========================================================================
    //  Firestore – load profile
    // =========================================================================

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(this::populateUI)
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists() || getContext() == null) return;

        String fullName = doc.getString("fullName");
        String email    = doc.getString("email");
        String role     = doc.getString("role");
        String photoUrl = doc.getString("profilePhotoUrl");
        currentPhotoUrl = photoUrl;

        // Address
        String barangay = "";
        String city     = "";
        String province = "";
        String region   = "";

        if (doc.contains("address")) {
            Map<String, Object> address =
                    (Map<String, Object>) doc.get("address");
            if (address != null) {
                barangay = address.get("barangay") != null
                        ? (String) address.get("barangay") : "";
                city     = address.get("city") != null
                        ? (String) address.get("city") : "";
                province = address.get("province") != null
                        ? (String) address.get("province") : "";
                region   = address.get("region") != null
                        ? (String) address.get("region") : "";
            }
        }

        // Header
        tvProfileName.setText(fullName != null ? fullName : "—");
        tvProfileSub.setText(
                (role != null ? role.toUpperCase() : "RESIDENT")
                        + " · "
                        + city.toUpperCase());

        // Rows
        tvValFullName.setText(fullName != null ? fullName : "—");
        tvValEmail.setText(email != null ? email : "—");

        String addressDisplay = barangay + ", " + city + ", " + province;
        tvValAddress.setText(addressDisplay);

        // Initials fallback avatar
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts    = fullName.trim().split("\\s+");
            String initials   = parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0))
                    + String.valueOf(parts[parts.length - 1].charAt(0))
                    : String.valueOf(parts[0].charAt(0));
            tvInitials.setText(initials.toUpperCase());
        }

        // Profile photo
        if (photoUrl != null && !photoUrl.isEmpty()) {
            tvInitials.setVisibility(View.GONE);
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.circle_bg_light_blue)
                    .into(ivProfilePhoto);
        } else {
            tvInitials.setVisibility(View.VISIBLE);
        }
    }

    // =========================================================================
    //  Image picker → Cloudinary upload
    // =========================================================================

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
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
    private void uploadToCloudinary(Uri imageUri) {
        Toast.makeText(getContext(), "Uploading photo…", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                // Read image bytes
                InputStream inputStream =
                        requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

                // Build multipart body
                String boundary  = "----FormBoundary" + System.currentTimeMillis();
                String uploadUrl = "https://api.cloudinary.com/v1_1/"
                        + CLOUD_NAME + "/image/upload";

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                // upload_preset field (unsigned)
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");

                // api_key field
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
                dos.writeBytes(API_KEY + "\r\n");

                // folder field (organise uploads neatly)
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n");
                dos.writeBytes("streetassist/profile_photos\r\n");

                // image file field
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; "
                        + "filename=\"profile.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                // Read response
                int status = conn.getResponseCode();
                InputStream responseStream = status == 200
                        ? conn.getInputStream()
                        : conn.getErrorStream();
                byte[] responseBytes = readAllBytes(responseStream);
                responseStream.close();
                conn.disconnect();

                String responseStr = new String(responseBytes);
                JSONObject json    = new JSONObject(responseStr);

                if (status == 200 && json.has("secure_url")) {
                    String photoUrl = json.getString("secure_url");
                    savePhotoUrlToFirestore(photoUrl);
                } else {
                    String error = json.optString("error", "Upload failed");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Upload error: " + error,
                                    Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
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
                .addOnSuccessListener(aVoid -> requireActivity().runOnUiThread(() -> {
                    currentPhotoUrl = photoUrl;
                    tvInitials.setVisibility(View.GONE);
                    Glide.with(this)
                            .load(photoUrl)
                            .transform(new CircleCrop())
                            .into(ivProfilePhoto);
                    Toast.makeText(getContext(),
                            "Profile photo updated!", Toast.LENGTH_SHORT).show();
                }))
                .addOnFailureListener(e -> requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Failed to save photo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()));
    }

    // =========================================================================
    //  Edit Name Dialog
    // =========================================================================

    private void showEditNameDialog() {
        if (getContext() == null) return;

        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setText(tvValFullName.getText());
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setPadding(48, 32, 48, 16);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Full Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateFullName(newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFullName(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("fullName",  newName);
        update.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("users")
                .document(user.getUid())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    tvValFullName.setText(newName);
                    tvProfileName.setText(newName);

                    // Refresh initials
                    String[] parts  = newName.trim().split("\\s+");
                    String initials = parts.length >= 2
                            ? String.valueOf(parts[0].charAt(0))
                            + String.valueOf(parts[parts.length - 1].charAt(0))
                            : String.valueOf(parts[0].charAt(0));
                    if (currentPhotoUrl == null || currentPhotoUrl.isEmpty()) {
                        tvInitials.setText(initials.toUpperCase());
                    }

                    Toast.makeText(getContext(),
                            "Name updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to update name: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // =========================================================================
    //  Change Password Dialog
    // =========================================================================

    private void showChangePasswordDialog() {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext())
                .inflate(android.R.layout.simple_list_item_2, null);

        // Build inputs manually for simplicity
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        android.widget.EditText etCurrent = new android.widget.EditText(getContext());
        etCurrent.setHint("Current password");
        etCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.EditText etNew = new android.widget.EditText(getContext());
        etNew.setHint("New password");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.EditText etConfirm = new android.widget.EditText(getContext());
        etConfirm.setHint("Confirm new password");
        etConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        layout.addView(etCurrent);
        layout.addView(etNew);
        layout.addView(etConfirm);

        new AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String current = etCurrent.getText().toString();
                    String newPass  = etNew.getText().toString();
                    String confirm  = etConfirm.getText().toString();

                    if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                        Toast.makeText(getContext(),
                                "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(getContext(),
                                "New password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(getContext(),
                                "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    changePassword(current, newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // Re-authenticate first (required by Firebase for sensitive operations)
        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid2 ->
                                        Toast.makeText(getContext(),
                                                "Password updated successfully!",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Failed to update password: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Current password is incorrect",
                                Toast.LENGTH_LONG).show());
    }

    // =========================================================================
    //  Logout
    // =========================================================================

    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =========================================================================
    //  Lifecycle
    // =========================================================================

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}