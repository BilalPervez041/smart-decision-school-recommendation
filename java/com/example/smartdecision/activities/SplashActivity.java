package com.example.smartdecision.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartdecision.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.isEmailVerified()) {
                String lastRole = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("lastRole", "");
                if (!lastRole.isEmpty()) {
                    redirectBasedOnRole(user.getUid(), lastRole);
                } else {
                    checkUserRoleAndRedirect(user.getUid());
                }
            } else {
                startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                finish();
            }
        }, 1500);
    }

    private void redirectBasedOnRole(String uid, String role) {
        Intent intent;
        if ("Owner".equals(role)) {
            intent = new Intent(SplashActivity.this, OwnerDashboardActivity.class);
        } else if ("Admin".equals(role)) {
            intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, PreferenceActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void checkUserRoleAndRedirect(String uid) {
        // Special case for hardcoded Admin
        if ("admin@gmail.com".equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
            finish();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String role = task.getResult().getValue(String.class);
                        if ("Owner".equals(role)) {
                            startActivity(new Intent(SplashActivity.this, OwnerDashboardActivity.class));
                        } else if ("Admin".equals(role)) {
                            startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, PreferenceActivity.class));
                        }
                    } else {
                        startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                    }
                    finish();
                }).addOnFailureListener(e -> {
                    startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                    finish();
                });
    }
}
