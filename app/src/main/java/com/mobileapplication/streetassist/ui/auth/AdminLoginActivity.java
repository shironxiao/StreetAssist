package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.admin.AdminDashboardActivity;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_login_activity);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.equals("augorio@gmail.com") && password.equals("1122")) {
                Toast.makeText(this, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tvBackToSelection).setOnClickListener(v -> {
            finish();
        });
    }
}