package com.mobileapplication.streetassist.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;

public class IntroScreen2Fragment extends Fragment {

    public IntroScreen2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate your fragment layout
        View view = inflater.inflate(R.layout.fragment_intro_screen2, container, false);

        // Initialize button
        MaterialButton btnGetStarted = view.findViewById(R.id.btnGetStarted);

        // Handle button click
        btnGetStarted.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new IntroScreen3Fragment())
                    .commit();
        });

        return view;
    }
}