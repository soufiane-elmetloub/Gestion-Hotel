package com.example.smarthotelapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.util.HashMap;

public class SessionManager {
    private static final String PREF_NAME = "SmartHotelPrefs";
    private static final String KEY_USER = "user";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_ASSIGNED_SECTION = "assignedSection";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Gson gson;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String userId, String username, String fullName, String phoneNumber, String assignedSection) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putString(KEY_PHONE_NUMBER, phoneNumber);
        editor.putString(KEY_ASSIGNED_SECTION, assignedSection);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    /**
     * Check login status
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get logged in user
     */
    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put("user_id", pref.getString(KEY_USER_ID, null));
        user.put("username", pref.getString(KEY_USERNAME, null));
        user.put("full_name", pref.getString(KEY_FULL_NAME, null));
        user.put("phone_number", pref.getString(KEY_PHONE_NUMBER, "N/A"));
        user.put("assigned_section", pref.getString(KEY_ASSIGNED_SECTION, null));
        return user;
    }

    /**
     * Logout user
     */
    public void logoutUser() {
        editor.clear();
        editor.commit();
        
        // Reset Volley singleton to prevent network errors on next login
        VolleySingleton.resetInstance();
    }

    /**
     * Get user name
     */
    public String getUserName() {
        return pref.getString(KEY_FULL_NAME, "User");
    }

    /**
     * Get assigned section
     */
    public String getAssignedSection() {
        return pref.getString(KEY_ASSIGNED_SECTION, "Not Assigned");
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, "-1");
    }
}
