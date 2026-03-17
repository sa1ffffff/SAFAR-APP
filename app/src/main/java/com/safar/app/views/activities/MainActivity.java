package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.safar.app.R;
import com.safar.app.databinding.ActivityMainBinding;
import com.safar.app.views.fragments.InboxFragment;
import com.safar.app.views.fragments.ListFragment;
import com.safar.app.views.fragments.MapFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // CRITICAL FIX: Enable edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Default: load map
        if (savedInstanceState == null) {
            loadFragment(new MapFragment());
            binding.bottomNav.setSelectedItemId(R.id.nav_map);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                loadFragment(new MapFragment());
            } else if (id == R.id.nav_list) {
                loadFragment(new ListFragment());
            } else if (id == R.id.nav_inbox) {
                loadFragment(new InboxFragment());
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false; // Don't highlight profile in bottom nav
            }
            return true;
        });

        // Handle intent from PostAdActivity: open Rides tab
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "rides".equals(intent.getStringExtra("openTab"))) {
            binding.bottomNav.setSelectedItemId(R.id.nav_list);
            loadFragment(new ListFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
