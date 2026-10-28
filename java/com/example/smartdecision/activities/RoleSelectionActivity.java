package com.example.smartdecision.activities;

import android.content.Intent;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.smartdecision.R;
import com.google.android.material.card.MaterialCardView;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setContentView
        applyCurrentTheme();
        
        setContentView(R.layout.activity_role_selection);

        MaterialCardView cardUser = findViewById(R.id.cardUser);
        MaterialCardView cardOwner = findViewById(R.id.cardOwner);
        MaterialCardView cardAdmin = findViewById(R.id.cardAdmin);
        ImageButton btnToggleTheme = findViewById(R.id.btnToggleTheme);

        updateThemeIcon(btnToggleTheme);

        btnToggleTheme.setOnClickListener(v -> toggleTheme());

        cardUser.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "User");
            startActivity(intent);
        });

        cardOwner.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "Owner");
            startActivity(intent);
        });

        cardAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("role", "Admin");
            startActivity(intent);
        });
    }

    private void applyCurrentTheme() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleTheme() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = !prefs.getBoolean("DarkMode", false);
        prefs.edit().putBoolean("DarkMode", isDarkMode).apply();
        
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        
        recreate();
    }

    private void updateThemeIcon(ImageButton btn) {
        if (btn == null) return;
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        btn.setImageResource(isDarkMode ? android.R.drawable.ic_menu_day : android.R.drawable.ic_menu_recent_history);
        
        // Ensure color consistency for button tint
        btn.setColorFilter(Color.WHITE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
