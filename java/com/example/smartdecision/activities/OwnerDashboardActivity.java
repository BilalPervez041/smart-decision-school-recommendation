package com.example.smartdecision.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.OwnerSchoolAdapter;
import com.example.smartdecision.models.School;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity {

    private RecyclerView rvOwnerSchools;
    private OwnerSchoolAdapter adapter;
    private List<School> schoolList;
    private FloatingActionButton fabAddSchool;
    private View tvEmptyState;
    private TextView tvSchoolCount, tvNotificationBadge;
    private DatabaseReference mDatabase;
    private DatabaseReference notifRef;
    private ValueEventListener notifListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        currentUserId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Schools");

        rvOwnerSchools = findViewById(R.id.rvOwnerSchools);
        fabAddSchool = findViewById(R.id.fabAddSchool);
        tvEmptyState = findViewById(R.id.tvEmptyState) != null ? findViewById(R.id.tvEmptyState) : null;
        tvSchoolCount = findViewById(R.id.tvSchoolCount);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        View btnNotifications = findViewById(R.id.btnNotifications);
        View btnStudentRecords = findViewById(R.id.btnStudentRecords);

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(OwnerDashboardActivity.this, NotificationsActivity.class));
        });

        btnStudentRecords.setOnClickListener(v -> {
            startActivity(new Intent(OwnerDashboardActivity.this, ApprovedStudentsActivity.class));
        });

        if (currentUserId != null) {
            notifRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("notifications");
        }

        rvOwnerSchools.setLayoutManager(new LinearLayoutManager(this));
        schoolList = new ArrayList<>();
        adapter = new OwnerSchoolAdapter(this, schoolList);
        rvOwnerSchools.setAdapter(adapter);

        fabAddSchool.setOnClickListener(v -> {
            startActivity(new Intent(OwnerDashboardActivity.this, AddSchoolActivity.class));
        });

        loadOwnerSchools();
        loadOwnerProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            observeNotifications();
            loadOwnerSchools();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // Close app on back press from Dashboard
    }

    private void loadOwnerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("name").getValue(String.class);
                                String email = snapshot.child("email").getValue(String.class);
                                
                                TextView tvName = findViewById(R.id.tvOwnerName);
                                TextView tvEmail = findViewById(R.id.tvOwnerEmail);
                                
                                if (tvName != null) tvName.setText(name);
                                if (tvEmail != null) tvEmail.setText(email);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void loadOwnerSchools() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                schoolList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    School school = data.getValue(School.class);
                    if (school != null && currentUserId != null && currentUserId.equals(school.ownerUid)) {
                        school.id = data.getKey();
                        schoolList.add(school);
                    }
                }
                
                if (schoolList.isEmpty()) {
                    if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
                    rvOwnerSchools.setVisibility(View.GONE);
                } else {
                    if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    rvOwnerSchools.setVisibility(View.VISIBLE);
                }
                if (tvSchoolCount != null) tvSchoolCount.setText(String.valueOf(schoolList.size()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(OwnerDashboardActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Logout").setIcon(android.R.drawable.ic_lock_power_off).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 2, 0, "Delete Account").setIcon(android.R.drawable.ic_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().remove("lastRole").apply();
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finishAffinity();
            return true;
        } else if (item.getItemId() == 2) {
            showDeleteConfirmation();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account permanently? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (d, w) -> {
                    String uid = user.getUid();
                    // First delete all schools owned by this user
                    FirebaseDatabase.getInstance().getReference("Schools").orderByChild("ownerUid").equalTo(uid)
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                    for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                                        ds.getRef().removeValue();
                                    }
                                    
                                    // Then delete the user account and user data
                                    user.delete().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseDatabase.getInstance().getReference("Users").child(uid).removeValue();
                                            Toast.makeText(OwnerDashboardActivity.this, "Account and all associated schools deleted", Toast.LENGTH_SHORT).show();
                                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().remove("lastRole").apply();
                                            Intent intent = new Intent(OwnerDashboardActivity.this, RoleSelectionActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(OwnerDashboardActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                                    Toast.makeText(OwnerDashboardActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
