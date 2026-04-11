package com.mobileapplication.streetassist.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.utils.SessionManager;

public class IntroScreen3Fragment extends Fragment {

    public IntroScreen3Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate your fragment layout
        View view = inflater.inflate(R.layout.fragment_intro_screen3, container, false);

        // Initialize button
        MaterialButton btnGetStarted = view.findViewById(R.id.btnGetStarted);

        new SessionManager(requireContext()).setIntroSeen();

        // Navigate to Login, clear back stack
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();

        return view;
    }

}