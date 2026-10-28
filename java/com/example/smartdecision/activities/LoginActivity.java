package com.example.smartdecision.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartdecision.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToSignup, tvForgotPassword;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    private String selectedRole = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        selectedRole = getIntent().getStringExtra("role");
        if (selectedRole == null) selectedRole = "User";

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        TextView tvRoleDisplay = findViewById(R.id.tvRoleDisplay);
        if (tvRoleDisplay != null) {
            tvRoleDisplay.setText("Login as " + selectedRole);
        }

        // Check if user is already logged in AND verified
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            checkUserRoleAndRedirect(currentUser.getUid());
            return;
        }

        // Hide signup for Admin
        if ("Admin".equals(selectedRole)) {
            tvGoToSignup.setVisibility(View.GONE);
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Special Admin check as requested
            if ("Admin".equals(selectedRole)) {
                if (email.equals("admin@gmail.com") && password.equals("admin12")) {
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                    finish();
                    return;
                } else {
                    Toast.makeText(this, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            loginUser(email, password);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        tvGoToSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            intent.putExtra("role", selectedRole);
            startActivity(intent);
        });
    }

    private void checkUserRoleAndRedirect(String uid) {
        if ("Admin".equals(selectedRole)) {
            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        // Check BOTH possible paths for the role to ensure compatibility
        FirebaseDatabase.getInstance().getReference("Users").child(uid).get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult().exists()) {
                com.google.firebase.database.DataSnapshot snapshot = task.getResult();
                
                // Try to get role from root or from profile node
                String role = snapshot.child("role").getValue(String.class);
                if (role == null) {
                    role = snapshot.child("profile").child("role").getValue(String.class);
                }
                
                if (role == null) {
                    // Default fallback if no role found
                    startActivity(new Intent(LoginActivity.this, PreferenceActivity.class));
                    finish();
                    return;
                }

                // Verification: check if actual role matches selected role
                if (!role.toLowerCase().contains(selectedRole.toLowerCase())) {
                     String message = "This account is registered as " + role + ". Please use the correct role to login.";
                     Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                     mAuth.signOut();
                     return;
                }

                // Save the selected role for auto-login persistence
                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                        .putString("lastRole", selectedRole)
                        .apply();

                if (selectedRole.equalsIgnoreCase("Owner")) {
                    startActivity(new Intent(LoginActivity.this, OwnerDashboardActivity.class));
                } else if (selectedRole.equalsIgnoreCase("Admin")) {
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, PreferenceActivity.class));
                }
                finish();
            } else {
                startActivity(new Intent(LoginActivity.this, PreferenceActivity.class));
                finish();
            }
        });
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        
        final EditText input = new EditText(this);
        input.setHint("Enter your registered email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Add padding to EditText
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton("Send Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void loginUser(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            checkUserRoleAndRedirect(user.getUid());
                        } else {
                            Toast.makeText(this, "Please verify your email to access your account.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(LoginActivity.this, "This email is not registered. Please sign up first.", Toast.LENGTH_LONG).show();
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(LoginActivity.this, "Invalid credentials. Please check your email and password.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + (exception != null ? exception.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
