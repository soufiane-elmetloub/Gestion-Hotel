package com.example.smarthotelapp;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class SmartHotelApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved theme and language at startup
        SettingsManager sm = SettingsManager.getInstance(this);

        // Theme
        AppCompatDelegate.setDefaultNightMode(
                sm.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Language (AppCompat 1.6+ application locales)
        String lang = sm.getLanguage();
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocale);

        // Initialize network configuration: auto-detect working IP on real devices
        NetworkConfig.initialize(this);
    }
}
