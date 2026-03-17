package com.mobileapplication.streetassist.ui.auth;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;

public class AppIntroScreen2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_intro_screen2);

        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                Intent intent = new Intent(AppIntroScreen2.this, AppIntroScreen3.class);
                startActivity(intent);
            });
        }

    }
}
