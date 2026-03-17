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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.ui.resident.ResidentMainActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        MaterialButton btnGetStarted = findViewById(R.id.btnGetStarted);
        TextView tvSignUpPrompt = findViewById(R.id.tvSignUpPrompt);
        String fullText = "Don't have an Account ? Sign up";
        SpannableString ss = new SpannableString(fullText);

        // Create the click and style behavior
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Navigate to RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4169E1")); // Royal Blue
                ds.setUnderlineText(false); // Removes the default underline
                ds.setFakeBoldText(true);    // Makes it bold
            }
        };
        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ResidentMainActivity.class);
                startActivity(intent);
            });
        }
        // "Sign up" starts at index 24 and ends at 31
        ss.setSpan(clickableSpan, 24, 31, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvSignUpPrompt.setText(ss);
        tvSignUpPrompt.setMovementMethod(LinkMovementMethod.getInstance());
        tvSignUpPrompt.setHighlightColor(Color.TRANSPARENT); // Removes gray highlight on click
    }
}