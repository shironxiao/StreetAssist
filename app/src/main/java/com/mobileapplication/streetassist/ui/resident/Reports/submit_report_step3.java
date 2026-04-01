package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
// Import your MainActivity class here
// import com.mobileapplication.streetassist.MainActivity;

public class submit_report_step3 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_report_step3);

        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            // This clears the activity stack so the user can't "Go Back" into the forms
            Intent intent = new Intent(submit_report_step3.this, com.mobileapplication.streetassist.ui.resident.Home.HomeFragment.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}