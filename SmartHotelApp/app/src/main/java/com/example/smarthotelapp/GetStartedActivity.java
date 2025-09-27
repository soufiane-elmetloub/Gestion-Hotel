package com.example.smarthotelapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.view.View;

public class GetStartedActivity extends AppCompatActivity {
    private AnimatorSet animatorSet;
    private ImageView backgroundImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure window to handle transparent status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(true);

        setContentView(R.layout.activity_get_started);

        backgroundImage = findViewById(R.id.backgroundImage);
        Button btnGetStarted = findViewById(R.id.btnGetStarted);

        btnGetStarted.setOnClickListener(v -> {
            // إيقاف الأنيميشن قبل الانتقال
            if (animatorSet != null) {
                animatorSet.cancel();
            }

            Intent intent = new Intent(GetStartedActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        startBackgroundAnimation();
    }

    private void startBackgroundAnimation() {
        if (backgroundImage == null) return;

        // إنشاء تأثير تكبير للعرض
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(backgroundImage, View.SCALE_X, 1.0f, 1.15f);
        scaleXAnimator.setDuration(15000);
        scaleXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleXAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        // إنشاء تأثير تكبير للارتفاع
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(backgroundImage, View.SCALE_Y, 1.0f, 1.15f);
        scaleYAnimator.setDuration(15000);
        scaleYAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    @Override
    protected void onDestroy() {
        if (animatorSet != null) {
            animatorSet.cancel();
            animatorSet = null;
        }
        super.onDestroy();
    }
}
