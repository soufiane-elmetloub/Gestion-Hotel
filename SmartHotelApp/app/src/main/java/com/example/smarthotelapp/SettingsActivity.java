package com.example.smarthotelapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager settingsManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsManager = SettingsManager.getInstance(this);

        // Init views
        RadioGroup languageGroup = findViewById(R.id.language_group);
        Switch themeSwitch = findViewById(R.id.switch_dark_mode);
        EditText inputServer = findViewById(R.id.input_server_host);
        Button btnSaveServer = findViewById(R.id.btn_save_server);
        Button btnTestServer = findViewById(R.id.btn_test_server);

        // Set current state
        String currentLang = settingsManager.getLanguage();
        if ("ar".equals(currentLang)) {
            languageGroup.check(R.id.radio_ar);
        } else if ("fr".equals(currentLang)) {
            languageGroup.check(R.id.radio_fr);
        } else {
            languageGroup.check(R.id.radio_en);
        }

        themeSwitch.setChecked(settingsManager.isDarkMode());

        // Initialize server host field from settings
        String savedHost = settingsManager.getServerHost();
        if (inputServer != null) {
            inputServer.setText(savedHost);
        }

        // Listeners
        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = "en";
            if (checkedId == R.id.radio_ar) lang = "ar";
            else if (checkedId == R.id.radio_fr) lang = "fr";

            applyLanguage(lang);
        });

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                applyTheme(isChecked);
            }
        });

        if (btnSaveServer != null) {
            btnSaveServer.setOnClickListener(v -> {
                String host = inputServer != null ? inputServer.getText().toString() : "";
                if (TextUtils.isEmpty(host)) {
                    Toast.makeText(this, "يرجى إدخال عنوان الخادم", Toast.LENGTH_SHORT).show();
                    return;
                }
                settingsManager.setServerHost(host);
                // Reinitialize base URL by calling initialize (stores context) and quick test
                NetworkConfig.initialize(this.getApplicationContext());
                Toast.makeText(this, "تم حفظ العنوان: " + settingsManager.getServerHost(), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnTestServer != null) {
            btnTestServer.setOnClickListener(v -> {
                String host = inputServer != null ? inputServer.getText().toString() : "";
                if (TextUtils.isEmpty(host)) {
                    Toast.makeText(this, "يرجى إدخال عنوان الخادم للاختبار", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(() -> {
                    boolean ok = NetworkConfig.testHostReachable(host, 2000);
                    runOnUiThread(() -> {
                        if (ok) {
                            Toast.makeText(this, "الاتصال ناجح بالخادم", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "تعذر الاتصال. تحقق من الشبكة أو جدار الحماية أو العنوان", Toast.LENGTH_LONG).show();
                        }
                    });
                }).start();
            });
        }

        // Optional: back navigation
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
    }

    private void applyLanguage(String lang) {
        settingsManager.setLanguage(lang);
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocale);
        recreate();
    }

    private void applyTheme(boolean dark) {
        settingsManager.setDarkMode(dark);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        recreate();
    }
}
