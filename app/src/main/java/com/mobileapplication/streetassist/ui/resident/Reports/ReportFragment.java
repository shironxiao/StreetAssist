package com.mobileapplication.streetassist.ui.resident.Reports;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.mobileapplication.streetassist.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {

    private static final String TAG = "ReportFragment";

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private String currentUserId;

    // UI
    private RecyclerView recyclerReports;
    private View emptyState;
    private ReportAdapter adapter;

    // Data
    private final List<Map<String, Object>> allReports = new ArrayList<>();
    private String currentFilter = "All";

    public ReportFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        // ── Get current logged-in user ────────────────────────────────────────
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No logged-in user found.");
            Toast.makeText(getContext(), "Please log in to view reports.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserId = user.getUid();
        Log.d(TAG, "Logged-in userId: " + currentUserId);

        db = FirebaseFirestore.getInstance();

        // ── Bind views ────────────────────────────────────────────────────────
        recyclerReports = root.findViewById(R.id.recyclerReports);
        emptyState      = root.findViewById(R.id.emptyState);

        setupRecyclerView();
        setupChipFilters(root);
        setupButtons(root);
        loadReports();
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new ReportAdapter(requireContext(), new ArrayList<>());
        recyclerReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerReports.setAdapter(adapter);
    }

    // ── Firestore Fetch ────────────────────────────────────────────────────────

    private void loadReports() {
        Log.d(TAG, "Fetching reports for userId: " + currentUserId);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        // Simple query — only filter by userId, NO orderBy
        // This avoids requiring a Firestore composite index.
        // Sorting is done manually below.
        listenerRegistration = db.collection("reports")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Firestore error: " + error.getMessage(), error);
                        Toast.makeText(getContext(),
                                "Failed to load reports: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        showEmptyState();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No documents found for userId: " + currentUserId);
                        showEmptyState();
                        return;
                    }

                    Log.d(TAG, "Documents found: " + snapshots.size());

                    allReports.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            // Fallback: use Firestore document ID if reportId field is missing
                            if (!data.containsKey("reportId") || data.get("reportId") == null) {
                                data.put("reportId", doc.getId());
                            }
                            allReports.add(data);
                            Log.d(TAG, "Report loaded → id: " + doc.getId()
                                    + " | status: " + data.get("status")
                                    + " | userId: " + data.get("userId"));
                        }
                    }

                    // Sort newest first manually
                    allReports.sort((a, b) -> {
                        Object tsA = a.get("timestamp");
                        Object tsB = b.get("timestamp");
                        if (tsA instanceof com.google.firebase.Timestamp
                                && tsB instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) tsB)
                                    .compareTo((com.google.firebase.Timestamp) tsA);
                        }
                        return 0;
                    });

                    applyFilter();
                });
    }

    // ── Chip Filters ───────────────────────────────────────────────────────────

    private void setupChipFilters(View root) {
        ChipGroup chipGroup = root.findViewById(R.id.chipGroupFilters);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);
            if      (id == R.id.chipAll)        currentFilter = "All";
            else if (id == R.id.chipPending)    currentFilter = "Pending";
            else if (id == R.id.chipVerified)   currentFilter = "Verified";
            else if (id == R.id.chipInProgress) currentFilter = "In Progress";
            else if (id == R.id.chipResolved)   currentFilter = "Resolved";

            Log.d(TAG, "Filter: " + currentFilter);
            applyFilter();
        });
    }

    // ── Filter Logic ───────────────────────────────────────────────────────────

    private void applyFilter() {
        List<Map<String, Object>> filtered = new ArrayList<>();

        for (Map<String, Object> report : allReports) {
            String status = report.get("status") != null
                    ? report.get("status").toString() : "";

            if (currentFilter.equals("All") || currentFilter.equals(status)) {
                filtered.add(report);
            }
        }

        Log.d(TAG, "Filtered results: " + filtered.size());

        if (filtered.isEmpty()) {
            showEmptyState();
        } else {
            showReportList(filtered);
        }
    }

    // ── Buttons ────────────────────────────────────────────────────────────────

    private void setupButtons(View root) {
        MaterialButton btnAddNewReport = root.findViewById(R.id.btnAddNewReport);
        btnAddNewReport.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), submit_report_step1.class)));

        MaterialButton btnSubmitReport = root.findViewById(R.id.btnSubmitReport);
        if (btnSubmitReport != null) {
            btnSubmitReport.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), submit_report_step1.class)));
        }
    }

    // ── Show / Hide ────────────────────────────────────────────────────────────

    private void showEmptyState() {
        if (getView() == null) return;
        emptyState.setVisibility(View.VISIBLE);
        recyclerReports.setVisibility(View.GONE);
    }

    private void showReportList(List<Map<String, Object>> list) {
        if (getView() == null) return;
        emptyState.setVisibility(View.GONE);
        recyclerReports.setVisibility(View.VISIBLE);
        adapter.updateList(list);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}