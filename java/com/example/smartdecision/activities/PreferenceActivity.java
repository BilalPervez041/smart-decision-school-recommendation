package com.example.smartdecision.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import android.provider.MediaStore;

public class PreferenceActivity extends AppCompatActivity {

    private Spinner spinnerGrade, spinnerGender;
    private SeekBar seekBarBudget;
    private EditText editBudget;
    private Button btnFindSchools, btnShowAll;
    private LinearLayout layoutPercentage;
    private EditText editPercentage;
    private RadioGroup radioGroupType, radioGroupMedium;
    private View btnOpenDrawer;
    private ShapeableImageView imgUserProfile;
    private TextView tvProfileInitial, tvNotificationBadge;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference notifRef;
    private ValueEventListener notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setContentView
        applyCurrentTheme();
        
        setContentView(R.layout.activity_preference);

        spinnerGrade = findViewById(R.id.spinnerGrade);
        spinnerGender = findViewById(R.id.spinnerGender);
        seekBarBudget = findViewById(R.id.seekBarBudget);
        editBudget = findViewById(R.id.editBudget);
        btnFindSchools = findViewById(R.id.btnFindSchools);
        btnShowAll = findViewById(R.id.btnShowAll);
        layoutPercentage = findViewById(R.id.layoutPercentage);
        editPercentage = findViewById(R.id.editPercentage);
        radioGroupType = findViewById(R.id.radioGroupType);
        radioGroupMedium = findViewById(R.id.radioGroupMedium);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        imgUserProfile = findViewById(R.id.imgUserProfile);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        View btnNotifications = findViewById(R.id.btnNotifications);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        if (mAuth.getUid() != null) {
            notifRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getUid()).child("notifications");
        }

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(PreferenceActivity.this, NotificationsActivity.class));
        });

        // Load User Profile Data
        loadUserProfile();
        loadSavedPreferences();

        // Drawer Logic
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                showProfileDialog();
            } else if (id == R.id.nav_change_password) {
                showChangePasswordDialog();
            } else if (id == R.id.nav_logout) {
                showLogoutConfirmation();
            } else if (id == R.id.nav_delete) {
                showDeleteConfirmation();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Profile Click Logic
        imgUserProfile.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Logic 1: Show/Hide Percentage based on Grade
        spinnerGrade.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGrade = parent.getItemAtPosition(position).toString();
                if (selectedGrade.toLowerCase().contains("playgroup") || selectedGrade.toLowerCase().contains("nursery")) {
                    layoutPercentage.setVisibility(View.GONE);
                } else {
                    layoutPercentage.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        seekBarBudget.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    editBudget.setText(String.valueOf(progress));
                    editBudget.setSelection(editBudget.getText().length());
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        editBudget.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int val = Integer.parseInt(s.toString());
                        if (val > 100000) { // Limit max to 100k for stability
                            val = 100000;
                            editBudget.setText("100000");
                            editBudget.setSelection(6);
                        }
                        if (val > seekBarBudget.getMax()) {
                            seekBarBudget.setMax(val);
                        }
                        seekBarBudget.setProgress(val);
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnFindSchools.setOnClickListener(v -> {
            if (validateFilters()) {
                savePreferences(false);
                startActivity(new Intent(PreferenceActivity.this, MainActivity.class));
            }
        });

        // Logic 2: View All Button
        btnShowAll.setOnClickListener(v -> {
            savePreferences(true);
            startActivity(new Intent(PreferenceActivity.this, MainActivity.class));
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    private void observeNotifications() {
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (count > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(count));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        notifRef.addValueEventListener(notifListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getUid() != null) {
            observeNotifications();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String email = user.getEmail();

                        // Update Header
                        View headerView = navigationView.getHeaderView(0);
                        TextView navName = headerView.findViewById(R.id.navHeaderName);
                        TextView navEmail = headerView.findViewById(R.id.navHeaderEmail);
                        ShapeableImageView navImg = headerView.findViewById(R.id.navHeaderImage);

                        navName.setText(name);
                        navEmail.setText(email);
                        navImg.setImageResource(android.R.drawable.ic_menu_myplaces);

                        // Profile icon on main screen
                        imgUserProfile.setImageResource(android.R.drawable.ic_menu_myplaces);
                        imgUserProfile.setVisibility(View.VISIBLE);
                        tvProfileInitial.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void showLogoutConfirmation() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    // Clear lastRole to allow selecting a new role upon next login
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().remove("lastRole").apply();
                    Intent intent = new Intent(PreferenceActivity.this, RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showDeleteConfirmation() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account permanently? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (d, w) -> {
                    String uid = user.getUid();
                    user.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mDatabase.child("Users").child(uid).removeValue();
                            Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PreferenceActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showProfileDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_profile_settings, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputLayout tilName = view.findViewById(R.id.tilUpdateName);
        com.google.android.material.textfield.TextInputLayout tilEmail = view.findViewById(R.id.tilUpdateEmail);
        EditText etName = view.findViewById(R.id.etUpdateName);
        EditText etEmail = view.findViewById(R.id.etUpdateEmail);
        
        Button btnUpdate = view.findViewById(R.id.btnUpdateProfile);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Constrain width for a professional look
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etEmail.setText(user.getEmail());
            mDatabase.child("Users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        etName.setText(snapshot.child("name").getValue(String.class));
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        btnUpdate.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();

            tilName.setError(null);
            tilEmail.setError(null);

            if (user != null) {
                if (newName.isEmpty()) {
                    tilName.setError("Username cannot be empty");
                    return;
                }

                // Update Name in DB
                mDatabase.child("Users").child(user.getUid()).child("name").setValue(newName);

                // Professional Email Update (Verification link style)
                if (!newEmail.isEmpty() && !newEmail.equals(user.getEmail())) {
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Verification email sent. Please check " + newEmail, Toast.LENGTH_LONG).show();
                            mDatabase.child("Users").child(user.getUid()).child("email").setValue(newEmail);
                        } else {
                            tilEmail.setError("Failed to update email: " + task.getException().getMessage());
                        }
                    });
                }

                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                loadUserProfile();
                dialog.dismiss();
            }
        });
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputLayout tilOld = view.findViewById(R.id.tilOldPassword);
        com.google.android.material.textfield.TextInputLayout tilNew = view.findViewById(R.id.tilNewPassword);
        com.google.android.material.textfield.TextInputLayout tilConfirm = view.findViewById(R.id.tilConfirmPassword);

        EditText etOldPassword = view.findViewById(R.id.etOldPassword);
        EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        Button btnUpdatePassword = view.findViewById(R.id.btnUpdatePasswordFinal);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Constrain width for a professional look
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        FirebaseUser user = mAuth.getCurrentUser();

        btnUpdatePassword.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString();
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            // Clear previous errors
            tilOld.setError(null);
            tilNew.setError(null);
            tilConfirm.setError(null);

            if (oldPass.isEmpty()) {
                tilOld.setError("Old password required");
                return;
            }
            if (newPass.isEmpty()) {
                tilNew.setError("New password required");
                return;
            }
            if (newPass.length() < 6) {
                tilNew.setError("Must be at least 6 characters");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                tilConfirm.setError("Passwords do not match");
                return;
            }

            if (user != null && user.getEmail() != null) {
                com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(user.getEmail(), oldPass);

                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                mDatabase.child("Users").child(user.getUid()).child("password").setValue(newPass);
                                Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(this, "Update Failed: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        tilOld.setError("Incorrect current password");
                    }
                });
            }
        });
    }

    private boolean validateFilters() {
        String selectedGrade = spinnerGrade.getSelectedItem().toString().toLowerCase();
        boolean isNurseryOrPlaygroup = selectedGrade.contains("playgroup") || selectedGrade.contains("nursery");

        if (!isNurseryOrPlaygroup) {
            String percentageStr = editPercentage.getText().toString().trim();
            if (percentageStr.isEmpty()) {
                editPercentage.setError("Please enter your previous percentage");
                editPercentage.requestFocus();
                return false;
            }
            try {
                int p = Integer.parseInt(percentageStr);
                if (p < 0 || p > 100) {
                    editPercentage.setError("Enter a valid percentage (0-100)");
                    return false;
                }
            } catch (NumberFormatException e) {
                editPercentage.setError("Invalid number");
                return false;
            }
        }
        return true;
    }

    private void applyCurrentTheme() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("DarkMode", false);
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void savePreferences(boolean showAll) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (showAll) {
            editor.putBoolean("showAll", true);
        } else {
            editor.putBoolean("showAll", false);
            editor.putString("grade", spinnerGrade.getSelectedItem().toString());
            editor.putInt("budget", seekBarBudget.getProgress());
            editor.putString("gender", spinnerGender.getSelectedItem().toString());
            editor.putString("percentage", editPercentage.getText().toString());
            
            // Save Type
            int selectedTypeId = radioGroupType.getCheckedRadioButtonId();
            String type = "Any";
            if (selectedTypeId == R.id.radioPrivate) type = "Private";
            else if (selectedTypeId == R.id.radioPublic) type = "Public";
            editor.putString("type", type);

            // Save Medium
            int selectedMediumId = radioGroupMedium.getCheckedRadioButtonId();
            String medium = "Any";
            if (selectedMediumId == R.id.radioEnglish) medium = "English";
            else if (selectedMediumId == R.id.radioUrdu) medium = "Urdu";
            editor.putString("medium", medium);
        }
        editor.apply();
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (prefs.contains("grade")) {
            String grade = prefs.getString("grade", "Any");
            for (int i = 0; i < spinnerGrade.getCount(); i++) {
                if (spinnerGrade.getItemAtPosition(i).toString().equals(grade)) {
                    spinnerGrade.setSelection(i);
                    break;
                }
            }
        }
        
        if (prefs.contains("budget")) {
            int budget = prefs.getInt("budget", 5000);
            seekBarBudget.setProgress(budget);
            editBudget.setText(String.valueOf(budget));
        }

        if (prefs.contains("gender")) {
            String gender = prefs.getString("gender", "Any");
            for (int i = 0; i < spinnerGender.getCount(); i++) {
                if (spinnerGender.getItemAtPosition(i).toString().equals(gender)) {
                    spinnerGender.setSelection(i);
                    break;
                }
            }
        }

        if (prefs.contains("percentage")) {
            editPercentage.setText(prefs.getString("percentage", ""));
        }

        if (prefs.contains("type")) {
            String type = prefs.getString("type", "Private");
            if (type.equalsIgnoreCase("Private")) radioGroupType.check(R.id.radioPrivate);
            else if (type.equalsIgnoreCase("Public")) radioGroupType.check(R.id.radioPublic);
        }

        if (prefs.contains("medium")) {
            String medium = prefs.getString("medium", "English");
            if (medium.equalsIgnoreCase("English")) radioGroupMedium.check(R.id.radioEnglish);
            else if (medium.equalsIgnoreCase("Urdu")) radioGroupMedium.check(R.id.radioUrdu);
        }
    }
}
