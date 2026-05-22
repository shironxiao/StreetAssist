package com.mobileapplication.streetassist.ui.shared;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mobileapplication.streetassist.ui.auth.AppIntroduction;
import com.mobileapplication.streetassist.ui.auth.IntroductionUserLevel;
import com.mobileapplication.streetassist.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always go to level selection to allow the user to pick their user level
        startActivity(new Intent(this, IntroductionUserLevel.class));
        finish();
    }
}