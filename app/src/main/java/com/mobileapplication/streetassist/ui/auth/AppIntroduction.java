package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;

public class AppIntroduction extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_introduction);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new IntroScreen0Fragment())
                .commit();
    }
}
