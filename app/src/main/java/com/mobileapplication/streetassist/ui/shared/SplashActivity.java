package com.mobileapplication.streetassist.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.auth.AppIntroduction;
import com.mobileapplication.streetassist.ui.auth.IntroScreen1Fragment;
import com.mobileapplication.streetassist.ui.auth.LoginActivity;
import com.mobileapplication.streetassist.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);

        if (sessionManager.isIntroSeen()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Go to Intro Activity (NOT Fragment)
        startActivity(new Intent(this, AppIntroduction.class));
        finish();
    }
}