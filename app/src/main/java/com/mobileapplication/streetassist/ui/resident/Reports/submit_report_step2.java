package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;

public class submit_report_step2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit_report_step2);

        MaterialButton btnNextStep2 = findViewById(R.id.btnNextStep2);
        btnNextStep2.setOnClickListener(v -> {
            Intent intent = new Intent(submit_report_step2.this, submit_report_step3.class);
            startActivity(intent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}