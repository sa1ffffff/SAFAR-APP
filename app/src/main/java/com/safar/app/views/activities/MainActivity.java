package com.safar.app.views.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.safar.app.R;
import com.safar.app.views.fragments.InboxFragment;
import com.safar.app.views.fragments.ListFragment;
import com.safar.app.views.fragments.MapFragment;

public class MainActivity extends AppCompatActivity {

    private MainStateHolder stateHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        stateHolder = new MainStateHolder();
        ComposeView bottomNavCompose = findViewById(R.id.bottomNavCompose);
        MainBridgeKt.setupComposeUI(this, bottomNavCompose, stateHolder);

        // Default: load map
        if (savedInstanceState == null) {
            loadFragment(new MapFragment());
        }

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
            onTabSelected(1);
        }
    }

    public void onTabSelected(int index) {
        stateHolder.setSelectedTab(index);
        switch (index) {
            case 0: loadFragment(new MapFragment());   break;
            case 1: loadFragment(new ListFragment());  break;
            case 2: loadFragment(new InboxFragment()); break;
            case 3:
                startActivity(new Intent(this, ProfileActivity.class));
                break;
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
