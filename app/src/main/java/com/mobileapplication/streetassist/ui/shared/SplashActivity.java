package com.mobileapplication.streetassist.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;
import com.mobileapplication.streetassist.ui.auth.AppIntroduction;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;
import com.mobileapplication.streetassist.ui.resident.ResidentMainActivity;
import com.mobileapplication.streetassist.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);

        if (!sessionManager.isIntroSeen()) {
            startActivity(new Intent(this, AppIntroduction.class));
            finish();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            navigateByRole(currentUser.getUid());
        } else {
            startActivity(new Intent(this, IntroductionUserLevel.class));
            finish();
        }
    }

    private void navigateByRole(String uid) {
        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        Intent intent;
                        if ("admin".equalsIgnoreCase(role)) {
                            intent = new Intent(this, AdminDashboardActivity.class);
                        } else {
                            intent = new Intent(this, ResidentMainActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, IntroductionUserLevel.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, IntroductionUserLevel.class));
                    finish();
                });
    }
}