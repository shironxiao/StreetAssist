package com.mobileapplication.streetassist.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.AppIntroScreen1;
import com.mobileapplication.streetassist.ui.auth.LoginActivity;
import com.mobileapplication.streetassist.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);

        // ✅ CHECK FIRST BEFORE SHOWING UI
        if (sessionManager.isIntroSeen()) {
            // Skip everything → go directly to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; // VERY IMPORTANT
        }

        // ✅ Only show this if first time
        setContentView(R.layout.app_introduction);

        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, AppIntroScreen1.class));
            finish();
        });
    }
}