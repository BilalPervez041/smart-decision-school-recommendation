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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnSignup;
    private TextView tvGoToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    private String selectedRole = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        selectedRole = getIntent().getStringExtra("role");
        if (selectedRole == null) selectedRole = "User";

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBar = findViewById(R.id.progressBar);

        TextView tvRoleDisplay = findViewById(R.id.tvRoleDisplay);
        if (tvRoleDisplay != null) {
            tvRoleDisplay.setText("Signup as " + selectedRole);
        }

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(name, email, password);
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser(String name, String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(false);

        // First check if the email exists in Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean userExists = (task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty());
                
                if (userExists) {
                    // Email exists. To ensure "email authentication" for the second role, 
                    // we verify by signing in with the provided password.
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    // Password correct and email verified, now check and add the role
                                    checkAndAddRole(user.getUid(), selectedRole);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    btnSignup.setEnabled(true);
                                    Toast.makeText(this, "This email is registered but not verified. Please verify it first.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnSignup.setEnabled(true);
                            Toast.makeText(this, "Email is already registered. Enter correct password to add " + selectedRole + " role.", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Completely new user
                    createNewAccount(name, email, password);
                }
            } else {
                progressBar.setVisibility(View.GONE);
                btnSignup.setEnabled(true);
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndAddRole(String uid, String newRole) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String currentRoles = task.getResult().exists() ? task.getResult().getValue(String.class) : "";
                        if (currentRoles != null && currentRoles.contains(newRole)) {
                            progressBar.setVisibility(View.GONE);
                            btnSignup.setEnabled(true);
                            Toast.makeText(this, "You are already registered as a " + newRole + ".", Toast.LENGTH_LONG).show();
                        } else {
                            // Add new role to existing account
                            addRoleToExistingUser(uid, currentRoles, newRole);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnSignup.setEnabled(true);
                        Toast.makeText(this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addRoleToExistingUser(String uid, String currentRoles, String newRole) {
        String updatedRoles = (currentRoles == null || currentRoles.isEmpty()) ? newRole : currentRoles + "," + newRole;
        
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .setValue(updatedRoles)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignup.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Success! The " + newRole + " role has been added to your account.", Toast.LENGTH_LONG).show();
                        mAuth.signOut(); // User should login again
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to update roles: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createNewAccount(String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    saveUserToDatabase(user.getUid(), name, email, password, selectedRole);
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    btnSignup.setEnabled(true);
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnSignup.setEnabled(true);
                        Toast.makeText(this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String name, String email, String password, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("password", password); // Note: Storing plain passwords isn't ideal, but keeping as per your current logic
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());

        // Create a structured path: Users/uid/profile
        // And an empty placeholder for notifications
        Map<String, Object> finalUpdate = new HashMap<>();
        finalUpdate.put("Users/" + uid + "/profile", userData);
        finalUpdate.put("Users/" + uid + "/notifications/welcome", new com.example.smartdecision.models.Notification(
                "welcome",
                "Welcome to Smart Decision",
                "Hi " + name + ", your account has been created successfully!",
                "unread",
                "user",
                System.currentTimeMillis()
        ));

        FirebaseDatabase.getInstance().getReference().updateChildren(finalUpdate)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Signup Successful! Please check your email for verification.", Toast.LENGTH_LONG).show();
                        mAuth.signOut(); 
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
