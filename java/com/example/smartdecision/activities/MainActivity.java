package com.example.smartdecision.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.SchoolAdapter;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import com.example.smartdecision.utils.RecommendationEngine;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SchoolAdapter.OnSchoolClickListener {

    private RecyclerView recyclerView;
    private SchoolAdapter adapter;
    private List<School> allSchools = new ArrayList<>();
    private List<School> filteredSchools = new ArrayList<>();
    private EditText editSearch;
    private FirebaseHelper firebaseHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private static final String PREFS_NAME = "LocationPrefs";
    private com.google.firebase.database.ValueEventListener firebaseListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load last saved location from preferences
        loadSavedLocation();

        recyclerView = findViewById(R.id.recyclerViewSchools);
        editSearch = findViewById(R.id.editSearch);
        ImageButton btnFilter = findViewById(R.id.btnFilter);
        ImageButton btnMapIcon = findViewById(R.id.btnMapIcon);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SchoolAdapter(filteredSchools, this);
        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getUserLocation();

        firebaseHelper = new FirebaseHelper();
        loadSchools();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PreferenceActivity.class));
        });

        btnMapIcon.setOnClickListener(v -> {
            if (filteredSchools.isEmpty()) {
                Toast.makeText(this, "No schools match your current filter", Toast.LENGTH_SHORT).show();
                return;
            }
            
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            boolean showAll = prefs.getBoolean("showAll", true);
            String grade = prefs.getString("grade", "Any");

            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("schools", new ArrayList<>(filteredSchools));
            intent.putExtra("selectedGrade", showAll ? "Any" : grade);
            intent.putExtra("single_school_mode", false);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseListener == null) {
            loadSchools();
        } else if (!allSchools.isEmpty()) {
            applyRecommendations();
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

    private void loadSchools() {
        Log.d("MainActivity", "Loading from Firebase...");
        firebaseListener = firebaseHelper.getAllSchools(new FirebaseHelper.DataListener() {
            @Override
            public void onSuccess(List<School> schools) {
                Log.d("MainActivity", "Success! Received: " + schools.size());
                allSchools.clear();
                allSchools.addAll(schools);
                
                // If we're filtering by name in the UI, we should maintain that
                String currentSearch = editSearch.getText().toString();
                if (!currentSearch.isEmpty()) {
                    filter(currentSearch);
                } else {
                    applyRecommendations();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MainActivity", "Firebase Error: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        // Try to get fresh location for better accuracy (High Accuracy)
        fusedLocationClient.getCurrentLocation(100, null) // PRIORITY_HIGH_ACCURACY is usually 100
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userLocation = location;
                        saveLocation(location);
                        Log.d("MainActivity", "Fresh Location: " + location.getLatitude() + ", " + location.getLongitude());
                        if (!allSchools.isEmpty()) {
                            applyRecommendations();
                        }
                    } else {
                        // If fresh fails, try last known
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                            if (lastLoc != null) {
                                userLocation = lastLoc;
                                saveLocation(lastLoc);
                                applyRecommendations();
                            }
                        });
                    }
                });
    }

    private void saveLocation(Location loc) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putFloat("lat", (float) loc.getLatitude());
        editor.putFloat("lng", (float) loc.getLongitude());
        editor.apply();
    }

    private void loadSavedLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float lat = prefs.getFloat("lat", 0);
        float lng = prefs.getFloat("lng", 0);
        if (lat != 0 && lng != 0) {
            userLocation = new Location("");
            userLocation.setLatitude(lat);
            userLocation.setLongitude(lng);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        }
    }

    private boolean isMessageShown = false;

    private void applyRecommendations() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean showAll = prefs.getBoolean("showAll", true); 
        
        String type = prefs.getString("type", "Any");
        String medium = prefs.getString("medium", "Any");
        String gender = prefs.getString("gender", "Any");
        int budget = prefs.getInt("budget", 100000);
        String grade = prefs.getString("grade", "Any");
        String percentage = prefs.getString("percentage", "0");

        Location loc = userLocation;
        if (loc == null) {
            // Default to Abbottabad center if GPS fails
            loc = new Location("");
            loc.setLatitude(34.146); loc.setLongitude(73.211);
        }

        filteredSchools.clear();
        // Always use RecommendationEngine to ensure distance is calculated and sorting is applied
        filteredSchools.addAll(RecommendationEngine.getRecommendations(allSchools, type, medium, budget, gender, grade, percentage, showAll, loc));
        
        adapter.setSelectedGrade(showAll ? "Any" : grade);
        adapter.updateList(filteredSchools);

        if (filteredSchools.isEmpty()) {
            if (!isMessageShown) {
                Toast.makeText(this, "No schools match your filter.", Toast.LENGTH_LONG).show();
                isMessageShown = true;
            }
        } else {
            isMessageShown = false;
        }
    }

    private void filter(String text) {
        List<School> searchList = new ArrayList<>();
        String query = text.toLowerCase().trim();
        for (School school : allSchools) {
            if (school.getName().toLowerCase().contains(query)) {
                searchList.add(school);
            }
        }
        // Sort search results by score/rating to match recommendations
        Collections.sort(searchList, (s1, s2) -> Double.compare(s2.getScore(), s1.getScore()));
        adapter.updateList(searchList);
    }

    @Override
    public void onSchoolClick(School school) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean showAll = prefs.getBoolean("showAll", true);
        String grade = prefs.getString("grade", "Any");

        Intent intent = new Intent(this, SchoolDetailActivity.class);
        intent.putExtra("school", school);
        intent.putExtra("selectedGrade", showAll ? "Any" : grade);
        startActivity(intent);
    }
}
