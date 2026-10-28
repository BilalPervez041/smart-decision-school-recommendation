package com.example.smartdecision;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class SmartDecisionApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Persist theme preference across app launches
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
