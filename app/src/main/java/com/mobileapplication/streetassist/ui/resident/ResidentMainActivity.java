package com.mobileapplication.streetassist.ui.resident;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.databinding.ResidentMainNavBinding; // Correct binding class

public class ResidentMainActivity extends AppCompatActivity {

    private ResidentMainNavBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ResidentMainNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Link Bottom Navigation with NavController
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }
}