package com.example.smarthotelapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_LANGUAGE = "language"; // ar, en, fr
    private static final String KEY_DARK_MODE = "dark_mode"; // boolean
    private static final String KEY_SERVER_HOST = "server_host"; // e.g., 192.168.1.8 or mypc.local

    private static SettingsManager instance;
    private final SharedPreferences prefs;

    private SettingsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "ar");
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    // Server host (manual override) management
    public void setServerHost(String host) {
        if (host == null) host = "";
        host = host.trim();
        // Normalize input: remove protocol if provided
        if (host.startsWith("http://")) host = host.substring(7);
        if (host.startsWith("https://")) host = host.substring(8);
        // Remove trailing slash if any
        if (host.endsWith("/")) host = host.substring(0, host.length() - 1);
        prefs.edit().putString(KEY_SERVER_HOST, host).apply();
    }

    public String getServerHost() {
        return prefs.getString(KEY_SERVER_HOST, "");
    }
}
