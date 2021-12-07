package com.example.minharte;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.os.Bundle;
import android.view.View;

import com.example.minharte.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        BottomNavigationView bottomNavView = binding.bottomNavView;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();

        NavGraph navGraph = navController.getGraph();
        if (mUser == null) {
            navGraph.setStartDestination(R.id.auth_navigation);
            navController.setGraph(navGraph);
        }

        NavigationUI.setupWithNavController(bottomNavView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.home_fragment ||
                    destination.getId() == R.id.search_fragment ||
                    destination.getId() == R.id.profile_fragment
            ) {
                bottomNavView.setVisibility(View.VISIBLE);
            } else {
                bottomNavView.setVisibility(View.GONE);
            }
        });
    }
}