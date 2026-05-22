package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.utils.SessionManager;

public class IntroductionUserLevel extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_user_level);

        Button btnResident = findViewById(R.id.btn_resident);
        Button btnAdmin = findViewById(R.id.btn_admin);

        if (btnResident != null) {
            btnResident.setOnClickListener(v -> {
                SessionManager sessionManager = new SessionManager(IntroductionUserLevel.this);
                if (!sessionManager.isIntroSeen()) {
                    Intent intent = new Intent(IntroductionUserLevel.this, AppIntroduction.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(IntroductionUserLevel.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(IntroductionUserLevel.this, AdminLoginActivity.class);
                startActivity(intent);
            });
        }
    }
}