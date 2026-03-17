package com.mobileapplication.streetassist.ui.resident;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.mobileapplication.streetassist.R;
import com.mobileapplication.streetassist.databinding.ResidentMainNavBinding;

public class ResidentMainActivity extends AppCompatActivity {

    private ResidentMainNavBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ResidentMainNavBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the NavHostFragment safely
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // This line links the BottomNav IDs to the NavGraph IDs
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }
}