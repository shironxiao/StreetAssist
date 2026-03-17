package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.utils.SessionManager;

public class AppIntroScreen3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_intro_screen3);

        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                SessionManager sessionManager = new SessionManager(this);
                sessionManager.setIntroSeen();

                Intent intent = new Intent(AppIntroScreen3.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

    }
}
