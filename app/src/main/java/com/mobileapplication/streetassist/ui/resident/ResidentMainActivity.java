package com.mobileapplication.streetassist.ui.resident;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.databinding.ResidentMainNavBinding;

public class ResidentMainActivity extends AppCompatActivity {

    private ResidentMainNavBinding binding;
    private boolean isGuestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ResidentMainNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        isGuestMode = getIntent().getBooleanExtra("is_guest", false);

        // Get the NavHostFragment safely
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // This line links the BottomNav IDs to the NavGraph IDs
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            if (isGuestMode) {
                configureGuestNavigation(navController);
            }
        }
    }

    private void configureGuestNavigation(NavController navController) {
        if (binding.bottomNavigation.getMenu().findItem(R.id.home) != null) {
            binding.bottomNavigation.getMenu().findItem(R.id.home).setVisible(false);
        }
        if (binding.bottomNavigation.getMenu().findItem(R.id.report) != null) {
            binding.bottomNavigation.getMenu().findItem(R.id.report).setVisible(false);
        }

        navController.navigate(R.id.news);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.news || itemId == R.id.profile) {
                return NavigationUI.onNavDestinationSelected(item, navController);
            }
            Toast.makeText(this,
                    "Guest mode supports News and Profile only.",
                    Toast.LENGTH_SHORT).show();
            return false;
        });
    }
}