package com.example.smartdecision.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.CityStatAdapter;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalSchools, tvPendingApprovals, tvBoysSchools, tvGirlsSchools, tvCoEduSchools, tvNotificationBadge;
    private View btnNotifications;
    private View cardTotal, cardPending, cardBoys, cardGirls, cardCoEdu;
    private RecyclerView rvCityStats;
    private CityStatAdapter cityStatAdapter;
    private DatabaseReference mDatabase;
    private int pendingCount = 0;

    private ValueEventListener statsListener;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // Handled by custom layout in XML
        }

        tvTotalSchools = findViewById(R.id.tvTotalSchools);
        tvPendingApprovals = findViewById(R.id.tvPendingApprovals);
        tvBoysSchools = findViewById(R.id.tvBoysSchools);
        tvGirlsSchools = findViewById(R.id.tvGirlsSchools);
        tvCoEduSchools = findViewById(R.id.tvCoEduSchools);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnNotifications = findViewById(R.id.btnNotifications);
        rvCityStats = findViewById(R.id.rvCityStats);

        cardTotal = findViewById(R.id.cardTotalSchools);
        cardPending = findViewById(R.id.cardPending);
        cardBoys = findViewById(R.id.cardBoys);
        cardGirls = findViewById(R.id.cardGirls);
        cardCoEdu = findViewById(R.id.cardCoEdu);

        rvCityStats.setLayoutManager(new LinearLayoutManager(this));
        cityStatAdapter = new CityStatAdapter(new HashMap<>());
        rvCityStats.setAdapter(cityStatAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("Schools");

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminApprovalActivity.class));
        });

        setupClickListeners();
        loadStats();
    }

    private void setupClickListeners() {
        cardTotal.setOnClickListener(v -> openSchoolList("All"));
        cardPending.setOnClickListener(v -> startActivity(new Intent(this, AdminApprovalActivity.class)));
        cardBoys.setOnClickListener(v -> openSchoolList("Male"));
        cardGirls.setOnClickListener(v -> openSchoolList("Female"));
        cardCoEdu.setOnClickListener(v -> openSchoolList("Co-education"));
    }

    private void openSchoolList(String filter) {
        Intent intent = new Intent(this, AdminSchoolListActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Logout").setIcon(android.R.drawable.ic_lock_power_off).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firebaseHelper != null && statsListener != null) {
            firebaseHelper.removeListener(statsListener);
        }
    }

    private void loadStats() {
        // Professional approach: Using FirebaseHelper to ensure we match 'View All Schools' logic
        if (firebaseHelper == null) firebaseHelper = new FirebaseHelper();
        statsListener = firebaseHelper.getAllSchools(new FirebaseHelper.DataListener() {
            @Override
            public void onSuccess(List<School> schools) {
                int totalCount = 0;
                int pendingCountLocal = 0;
                int boysCount = 0;
                int girlsCount = 0;
                int coeduCount = 0;
                Map<String, Integer> cityCounts = new HashMap<>();

                if (schools != null && !schools.isEmpty()) {
                    for (School school : schools) {
                        // 1. All schools in the database contribute to Total
                        totalCount++;

                        // 2. Count by Gender (Smart matching)
                        String gender = school.gender;
                        if (gender != null) {
                            if (gender.equalsIgnoreCase("Male") || gender.equalsIgnoreCase("Boys")) {
                                boysCount++;
                            } else if (gender.equalsIgnoreCase("Female") || gender.equalsIgnoreCase("Girls")) {
                                girlsCount++;
                            } else if (gender.equalsIgnoreCase("Co-education") || gender.equalsIgnoreCase("Co-educational") || gender.equalsIgnoreCase("Co")) {
                                coeduCount++;
                            }
                        }

                        // 3. Count Pending Approvals
                        if (school.status != null && school.status.equalsIgnoreCase("pending")) {
                            pendingCountLocal++;
                        }

                        // 4. City-wise Distribution
                        String city = (school.city != null && !school.city.equalsIgnoreCase("Unknown")) ? school.city : "Other";
                        cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
                    }
                }

                // Update Dashboard Cards
                tvTotalSchools.setText(String.valueOf(totalCount));
                tvPendingApprovals.setText(String.valueOf(pendingCountLocal));
                tvBoysSchools.setText(String.valueOf(boysCount));
                tvGirlsSchools.setText(String.valueOf(girlsCount));
                tvCoEduSchools.setText(String.valueOf(coeduCount));

                // Update notification and city stats
                cityStatAdapter.updateData(cityCounts);
                pendingCount = pendingCountLocal;
                if (pendingCount > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(pendingCount));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("AdminDashboard", "Sync error: " + e.getMessage());
            }
        });
    }
}
