package com.example.smarthotelapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Modern approach for full-screen display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_welcome);

        // العثور على العناصر في الواجهة
        final ImageView logoImageView = findViewById(R.id.logoImageView);
        final TextView appNameTextView = findViewById(R.id.appNameTextView);

        // التأكد من أن العناصر مرئية مع شفافية كاملة في البداية
        logoImageView.setVisibility(View.VISIBLE);
        appNameTextView.setVisibility(View.VISIBLE);
        logoImageView.setAlpha(0f);
        appNameTextView.setAlpha(0f);

        // إظهار الشعار تدريجياً
        logoImageView.animate()
                .alpha(1f)
                .setDuration(2000)
                .withEndAction(() -> {
                    // بعد اكتمال ظهور الشعار، نبدأ في إظهار النص
                    appNameTextView.animate()
                            .alpha(1f)
                            .setDuration(2000)
                            .withEndAction(() -> {
                                // الانتقال إلى شاشة Get Started بعد اكتمال كل التأثيرات
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    Intent intent = new Intent(WelcomeActivity.this, GetStartedActivity.class);
                                    startActivity(intent);
                                    finish();
                                }, 1500); // انتظار 1.5 ثانية إضافية قبل الانتقال
                            })
                            .start();
                })
                .start();
    }
}
