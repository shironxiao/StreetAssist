package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.shared.SplashActivity;

public class IntroductionUserLevel extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_user_level);

        Button btnResident = findViewById(R.id.btn_resident);
        Button btnAdmin = findViewById(R.id.btn_admin);

        if (btnResident != null) {
            btnResident.setOnClickListener(v -> {
                Intent intent = new Intent(IntroductionUserLevel.this, SplashActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> {
                // Add your admin navigation here later
                // Intent intent = new Intent(IntroductionUserLevel.this, AdminActivity.class);
                // startActivity(intent);
            });
        }
    }
}