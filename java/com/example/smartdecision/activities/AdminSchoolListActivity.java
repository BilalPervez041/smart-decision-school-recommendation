package com.example.smartdecision.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.SchoolAdapter;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import android.widget.Toast;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminSchoolListActivity extends AppCompatActivity {

    private RecyclerView rvSchools;
    private SchoolAdapter adapter;
    private List<School> schoolList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private String filterType;
    private FusedLocationProviderClient fusedLocationClient;
    private Location adminLocation;
    private com.google.firebase.database.ValueEventListener firebaseListener;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_school_list);

        filterType = getIntent().getStringExtra("filter");
        if (filterType == null) filterType = "All";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchAdminLocation();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(filterType + " Schools");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvSchools = findViewById(R.id.rvSchools);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvSchools.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SchoolAdapter(schoolList, school -> {
            Intent intent = new Intent(this, AdminSchoolDetailActivity.class);
            intent.putExtra("school", school);
            startActivity(intent);
        });
        rvSchools.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseHelper == null) firebaseHelper = new FirebaseHelper();
        if (firebaseListener == null) {
            loadSchools();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firebaseHelper != null && firebaseListener != null) {
            firebaseHelper.removeListener(firebaseListener);
            firebaseListener = null;
        }
    }

    private void fetchAdminLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                adminLocation = location;
                calculateDistances();
            }
        });
    }

    private void calculateDistances() {
        if (adminLocation == null || schoolList == null || schoolList.isEmpty()) return;
        for (School school : schoolList) {
            float[] results = new float[1];
            Location.distanceBetween(adminLocation.getLatitude(), adminLocation.getLongitude(),
                    school.getLatitude(), school.getLongitude(), results);
            float geodesicDistanceKm = results[0] / 1000;
            school.setDistance(geodesicDistanceKm * 1.32f); // Road estimation factor
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchAdminLocation();
        }
    }

    private void loadSchools() {
        progressBar.setVisibility(View.VISIBLE);
        // Using FirebaseHelper to ensure we see the same schools as the user role
        if (firebaseHelper == null) firebaseHelper = new FirebaseHelper();
        firebaseListener = firebaseHelper.getAllSchools(new FirebaseHelper.DataListener() {
            @Override
            public void onSuccess(List<School> schools) {
                schoolList.clear();
                for (School school : schools) {
                    String gender = school.gender != null ? school.gender : "";
                    
                    if (filterType.equals("All")) {
                        schoolList.add(school);
                    } else if (filterType.equalsIgnoreCase("Male")) {
                        if (gender.equalsIgnoreCase("Male") || gender.equalsIgnoreCase("Boys")) {
                            schoolList.add(school);
                        }
                    } else if (filterType.equalsIgnoreCase("Female")) {
                        if (gender.equalsIgnoreCase("Female") || gender.equalsIgnoreCase("Girls")) {
                            schoolList.add(school);
                        }
                    } else if (filterType.equalsIgnoreCase("Co-education")) {
                        if (gender.equalsIgnoreCase("Co-education") || gender.equalsIgnoreCase("Co-educational") || gender.equalsIgnoreCase("Co")) {
                            schoolList.add(school);
                        }
                    }
                }
                
                // Sort by Rating (Top to Bottom)
                Collections.sort(schoolList, (s1, s2) -> Double.compare(s2.getRating(), s1.getRating()));

                if (adminLocation != null) {
                    calculateDistances();
                }

                progressBar.setVisibility(View.GONE);
                if (schoolList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
                adapter.updateList(schoolList);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminSchoolListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
