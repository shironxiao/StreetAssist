package com.mobileapplication.streetassist.ui.resident.Profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private TextView tvValFullName, tvValEmail, tvValAddress, tvValContact;
    private TextView tvInitials;
    private FloatingActionButton fabEditPhoto;
    private MaterialButton btnLogout;
    private View rowFullName, rowEmail, rowAddress, rowPassword, rowContact;

    private MaterialButton btnEditProfileHeader;

    // ── Firebase ─────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Cloudinary ───────────────────────────────────────────────────────────
    private static final String CLOUD_NAME    = "durqaiei1";
    private static final String UPLOAD_PRESET = "streetassist_unsigned";
    private static final String API_KEY       = "938268411726485";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── Image picker / profile edit launchers ────────────────────────────────
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> profileEditLauncher;

    // ── Cached user data ──────────────────────────────────────────────────────
    private String currentPhotoUrl       = null;
    private String currentContactNumber  = "";
    private String currentEmail          = "";
    private String currentAddress        = "";

    // ─────────────────────────────────────────────────────────────────────────

    public ProfileFragment() {}

    // =========================================================================
    //  Lifecycle
    // =========================================================================

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
                        if (imageUri != null) uploadToCloudinary(imageUri);
                    }
                });

        // Register profile-edit launcher — refresh profile on return
        profileEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadUserProfile();
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

        // ── Bind views ───────────────────────────────────────
        tvProfileName        = view.findViewById(R.id.tvProfileName);
        tvProfileSub         = view.findViewById(R.id.tvProfileSub);
        tvValFullName        = view.findViewById(R.id.tvValFullName);
        tvValEmail           = view.findViewById(R.id.tvValEmail);
        tvValAddress         = view.findViewById(R.id.tvValAddress);
        tvValContact         = view.findViewById(R.id.tvValContact);
        ivProfilePhoto       = view.findViewById(R.id.ivProfilePhoto);
        tvInitials           = view.findViewById(R.id.tvInitials);
        fabEditPhoto         = view.findViewById(R.id.fabEditPhoto);
        btnLogout            = view.findViewById(R.id.btnLogout);
        btnEditProfileHeader = view.findViewById(R.id.btnEditProfileHeader);

        rowFullName  = view.findViewById(R.id.rowFullName);
        rowEmail     = view.findViewById(R.id.rowEmail);
        rowAddress   = view.findViewById(R.id.rowAddress);
        rowPassword  = view.findViewById(R.id.rowPassword);
        rowContact   = view.findViewById(R.id.rowContact);

        // ── Load data ────────────────────────────────────────
        loadUserProfile();

        // ── Click listeners ──────────────────────────────────
        fabEditPhoto.setOnClickListener(v -> openImagePicker());

        btnEditProfileHeader.setOnClickListener(v -> openProfileEdit());

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        rowEmail.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Email cannot be changed.", Toast.LENGTH_SHORT).show());

        rowAddress.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Address cannot be changed here.", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // =========================================================================
    //  Firestore – load & populate profile
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

    @SuppressWarnings("unchecked")
    private void populateUI(DocumentSnapshot doc) {
        if (!doc.exists() || getContext() == null) return;

        String fullName = doc.getString("fullName");
        String email    = doc.getString("email");
        String role     = doc.getString("role");
        String photoUrl = doc.getString("profilePhotoUrl");
        String contact  = doc.getString("contactNumber");
        currentPhotoUrl = photoUrl;

        // Address fields
        String barangay = "", city = "", province = "";
        if (doc.contains("address")) {
            Map<String, Object> address = (Map<String, Object>) doc.get("address");
            if (address != null) {
                barangay = address.get("barangay") != null ? (String) address.get("barangay") : "";
                city     = address.get("city")     != null ? (String) address.get("city")     : "";
                province = address.get("province") != null ? (String) address.get("province") : "";
            }
        }

        // Header
        tvProfileName.setText(fullName != null ? fullName : "—");
        tvProfileSub.setText(
                (role != null ? role.toUpperCase() : "RESIDENT")
                        + " · " + city.toUpperCase());

        // Info rows
        tvValFullName.setText(fullName != null ? fullName : "—");
        tvValEmail.setText(email != null ? email : "—");

        String addressDisplay = barangay + ", " + city + ", " + province;
        tvValAddress.setText(addressDisplay);

        currentContactNumber = contact        != null ? contact        : "";
        currentEmail         = email          != null ? email          : "";
        currentAddress       = addressDisplay;

        tvValContact.setText(!currentContactNumber.isEmpty() ? currentContactNumber : "—");

        // ── Profile photo logic ──────────────────────────────────────────────
        if (photoUrl != null && !photoUrl.isEmpty()) {
            // User has a real photo — load it, hide initials
            tvInitials.setVisibility(View.GONE);
            ivProfilePhoto.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivProfilePhoto);
        } else {
            // No photo — show default avatar image, hide initials TextView
            tvInitials.setVisibility(View.GONE);
            ivProfilePhoto.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(R.drawable.ic_default_avatar)
                    .transform(new CircleCrop())
                    .into(ivProfilePhoto);
        }
    }

    // =========================================================================
    //  Navigate to ProfileEdit
    // =========================================================================

    private void openProfileEdit() {
        Intent intent = new Intent(getActivity(), ProfileEdit.class);
        intent.putExtra(ProfileEdit.EXTRA_FULL_NAME, tvValFullName.getText().toString());
        intent.putExtra(ProfileEdit.EXTRA_CONTACT,   currentContactNumber);
        intent.putExtra(ProfileEdit.EXTRA_EMAIL,     currentEmail);
        intent.putExtra(ProfileEdit.EXTRA_ADDRESS,   currentAddress);
        profileEditLauncher.launch(intent);
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
                InputStream inputStream =
                        requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = readAllBytes(inputStream);
                inputStream.close();

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
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; "
                        + "filename=\"profile.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageBytes);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int status = conn.getResponseCode();
                InputStream responseStream = status == 200
                        ? conn.getInputStream() : conn.getErrorStream();
                byte[] responseBytes = readAllBytes(responseStream);
                responseStream.close();
                conn.disconnect();

                JSONObject json = new JSONObject(new String(responseBytes));

                if (status == 200 && json.has("secure_url")) {
                    String newPhotoUrl = json.getString("secure_url");
                    savePhotoUrlToFirestore(newPhotoUrl);
                } else {
                    String error = json.optString("error", "Upload failed");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Upload error: " + error, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
                    ivProfilePhoto.setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(photoUrl)
                            .transform(new CircleCrop())
                            .error(R.drawable.ic_default_avatar)
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
}