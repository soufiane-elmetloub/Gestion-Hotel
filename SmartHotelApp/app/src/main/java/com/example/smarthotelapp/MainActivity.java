package com.example.smarthotelapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    
    private SessionManager sessionManager;
    private TextView usernameText;
    private ImageView logoImage;
    private ImageView notificationIcon;
    private TextView notificationBadge;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure window to handle transparent status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_main);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // Redirect to login if not logged in
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        // Initialize views
        usernameText = findViewById(R.id.usernameText);
        logoImage = findViewById(R.id.logoImage);
        notificationIcon = findViewById(R.id.notificationIcon);
        notificationBadge = findViewById(R.id.notificationBadge);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Get user data from session
        String userName = sessionManager.getUserName();
        String assignedSection = sessionManager.getAssignedSection();
        // Set username and assigned section in header
        if (usernameText != null) {
            usernameText.setText(userName + " - " + assignedSection);
        }

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up notification icon click listener
        notificationIcon.setOnClickListener(v -> {
            // Reset badge visually on tap
            if (notificationBadge != null) {
                notificationBadge.setText("");
                notificationBadge.setVisibility(android.view.View.GONE);
            }
            Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Initial badge update
        updateNotificationBadge();

        // Select initial tab (default: home). If launched with open_tab=bookings, open Bookings tab.
        String openTab = getIntent() != null ? getIntent().getStringExtra("open_tab") : null;
        int initialItemId = (openTab != null && openTab.equals("bookings")) ? R.id.nav_bookings : R.id.nav_home;
        bottomNavigationView.setSelectedItemId(initialItemId);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh badge when returning to main screen
        updateNotificationBadge();
    }

    private void updateNotificationBadge() {
        String userIdStr = sessionManager != null ? sessionManager.getUserId() : null;
        int employeeId = -1;
        try { employeeId = Integer.parseInt(userIdStr != null ? userIdStr : "-1"); } catch (Exception ignored) {}
        if (employeeId <= 0) {
            if (notificationBadge != null) {
                notificationBadge.setText("");
                notificationBadge.setVisibility(android.view.View.GONE);
            }
            return;
        }

        String url = NetworkConfig.getNotificationsUrl(employeeId, true, 500); // only_unread
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean success = response.optBoolean("success", false);
                        int unread = 0;
                        if (success) {
                            JSONArray arr = response.optJSONArray("data");
                            unread = (arr != null) ? arr.length() : 0;
                        }
                        applyBadge(unread);
                    } catch (Exception ignored) { applyBadge(0); }
                },
                error -> applyBadge(0)
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void applyBadge(int count){
        if (notificationBadge == null) return;
        if (count > 0){
            notificationBadge.setText(String.valueOf(count));
            notificationBadge.setVisibility(android.view.View.VISIBLE);
        } else {
            notificationBadge.setText("");
            notificationBadge.setVisibility(android.view.View.GONE);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_bookings) {
                selectedFragment = new BookingsFragment();
            } else if (itemId == R.id.nav_rooms) {
                // Use the dynamic RoomsFragment implementation under ui.rooms
                selectedFragment = new com.example.smarthotelapp.ui.rooms.RoomsFragment();
            } else if (itemId == R.id.nav_clients) {
                selectedFragment = new ClientsFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        // Handle back button press
        super.onBackPressed();
    }
}
