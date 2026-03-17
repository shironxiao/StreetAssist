package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mobileapplication.streetassist.R;

public class RegisterActivity extends AppCompatActivity {

    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvTermsLink;
    private TextView tvLoginPrompt;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // Initialize Views
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        tvTermsLink = findViewById(R.id.tvTermsLink);
        tvLoginPrompt = findViewById(R.id.tvLoginPrompt);
        btnBack = findViewById(R.id.btnBack);

        // Back button (prevent crash if not in XML)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Spannables
        setupTermsSpannable();
        setupLoginSpannable();

        // Register button logic
        btnRegister.setOnClickListener(v -> {
            if (!cbTerms.isChecked()) {
                Toast.makeText(this,
                        "You must accept the terms and privacy policy to continue",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                        "Account Created Successfully!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLoginSpannable() {
        String loginText = "Already have an account? Log in";
        SpannableString ssLogin = new SpannableString(loginText);

        ClickableSpan loginClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                finish(); // or start LoginActivity
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ssLogin.setSpan(loginClick, 25, loginText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvLoginPrompt.setText(ssLogin);
        tvLoginPrompt.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupTermsSpannable() {
        String text = "I accept the terms and privacy policy";
        SpannableString ss = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermsDialog();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1"));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        ss.setSpan(clickableSpan, 13, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTermsLink.setText(ss);
        tvTermsLink.setMovementMethod(LinkMovementMethod.getInstance());
        tvTermsLink.setHighlightColor(Color.TRANSPARENT);
    }

    private void showTermsDialog() {
        String termsContent = "Terms of Service – StreetAssist\n" +
                "By using the StreetAssist mobile application, you agree to the following terms:\n\n" +
                "1. Purpose of the Application\n" +
                "StreetAssist is designed to allow residents to report concerns related to homelessness in their community.\n\n" +
                "2. User Responsibility\n" +
                "Users agree to provide accurate information. Misuse or false reports are not permitted.\n\n" +
                "3. Appropriate Use\n" +
                "The application should only be used for its intended purpose. Users must not harass others.\n\n" +
                "4. Service Availability\n" +
                "This is a student project and may be modified or discontinued at any time.\n\n" +
                "5. Acceptance of Terms\n" +
                "By registering, you confirm you have read and agreed to these Terms.\n\n" +
                "----------------------------------\n\n" +
                "Privacy Policy – StreetAssist\n" +
                "1. Information Collected\n" +
                "We collect Name, Email, Location (for reports), and submitted reports.\n\n" +
                "2. Use of Information\n" +
                "Data is used to identify users and provide location-based reporting.\n\n" +
                "3. Data Protection\n" +
                "Personal information will not be shared with third parties.\n\n" +
                "4. Location Data\n" +
                "Used only when submitting a report to identify the area needing assistance.\n\n" +
                "5. Contact\n" +
                "Questions? Contact: streetassist.support@example.com";

        new AlertDialog.Builder(this)
                .setTitle("Terms & Privacy Policy")
                .setMessage(termsContent)
                .setPositiveButton("I Understand", (dialog, which) -> dialog.dismiss())
                .show();
    }
}