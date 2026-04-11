package com.mobileapplication.streetassist.ui.resident.Reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobileapplication.streetassist.R;

public class SubmitReportStep2Fragment extends Fragment {

    public SubmitReportStep2Fragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_submit_report_step2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etAssistanceDescription = view.findViewById(R.id.etAssistanceDescription);
        TextInputEditText etContactNumber         = view.findViewById(R.id.etContactNumber);

        // ── Retrieve data from Step 1 via Bundle ──────────────────────────────
        Bundle args = getArguments();
        double latitude     = args != null ? args.getDouble("latitude", 0)     : 0;
        double longitude    = args != null ? args.getDouble("longitude", 0)    : 0;
        String locationText = args != null ? args.getString("locationText")    : null;
        String age          = args != null ? args.getString("age")             : null;
        String sex          = args != null ? args.getString("sex")             : null;
        String description  = args != null ? args.getString("description")     : null;
        long   seenAt       = args != null ? args.getLong("seenAt", 0)         : 0;

        // ── Auto-fill contact number from Firestore ───────────────────────────
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String savedContact = doc.getString("contactNumber");
                            if (savedContact != null && !savedContact.isEmpty()) {
                                etContactNumber.setText(savedContact);
                            }
                        }
                    });
        }

        // ── Next button ───────────────────────────────────────────────────────
        MaterialButton btnNext = view.findViewById(R.id.btnNextStep2);
        btnNext.setOnClickListener(v -> {
            String assistanceDesc = etAssistanceDescription.getText() != null
                    ? etAssistanceDescription.getText().toString().trim() : "";

            if (assistanceDesc.isEmpty()) {
                etAssistanceDescription.setError("Please describe the assistance needed.");
                etAssistanceDescription.requestFocus();
                return;
            }

            String contact = etContactNumber.getText() != null
                    ? etContactNumber.getText().toString().trim() : "";

            // ── Bundle all data and navigate to Step 3 ────────────────────────
            Bundle nextArgs = new Bundle();
            nextArgs.putDouble("latitude",              latitude);
            nextArgs.putDouble("longitude",             longitude);
            nextArgs.putString("locationText",          locationText);
            nextArgs.putString("age",                   age);
            nextArgs.putString("sex",                   sex);
            nextArgs.putString("description",           description);
            nextArgs.putString("assistanceDescription", assistanceDesc);
            nextArgs.putString("contact",               contact);
            nextArgs.putLong("seenAt",                  seenAt);

            Navigation.findNavController(requireView())
                    .navigate(R.id.submitReportStep3Fragment, nextArgs);
        });


        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }
}