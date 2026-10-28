package com.example.smartdecision.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.smartdecision.R;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private ProgressBar progressBar;
    private TextView textSchoolCount;
    private ImageButton btnBack;
    private FloatingActionButton btnRecenter;
    
    private boolean isSingleMode = false;
    private String selectedGrade = "Any";
    private static List<School> cachedSchools = null;
    private final GeoPoint ABBOTTABAD_CENTER = new GeoPoint(34.1689, 73.2215);
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint userGeoPoint = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load OSMDroid configuration
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_map);

        // Initialize UI components
        progressBar = findViewById(R.id.mapProgressBar);
        map = findViewById(R.id.map);
        textSchoolCount = findViewById(R.id.textSchoolCount);
        btnBack = findViewById(R.id.btnBack);
        btnRecenter = findViewById(R.id.btnRecenter);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        setupListeners();
        showUserLocation();

        isSingleMode = getIntent().getBooleanExtra("single_school_mode", false);
        selectedGrade = getIntent().getStringExtra("selectedGrade");
        if (selectedGrade == null) selectedGrade = "Any";

        ArrayList<School> intentSchools = (ArrayList<School>) getIntent().getSerializableExtra("schools");

        if (intentSchools != null && !intentSchools.isEmpty()) {
            renderMarkers(intentSchools, isSingleMode);
        } else {
            if (cachedSchools != null) {
                renderMarkers(cachedSchools, false);
            } else {
                loadSchoolsFromFirebase();
            }
        }
    }

    private void setupMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.0);
        map.getController().setCenter(ABBOTTABAD_CENTER);

        // Professional Dark Mode for Map Tiles
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            float[] darkFilter = {
                    -0.7f, 0, 0, 0, 230, // Red
                    0, -0.7f, 0, 0, 230, // Green
                    0, 0, -0.7f, 0, 240, // Blue
                    0, 0, 0, 1.0f, 0     // Alpha
            };
            map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(darkFilter));
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnRecenter.setOnClickListener(v -> {
            if (userGeoPoint != null) {
                map.getController().animateTo(userGeoPoint, 16.0, 1000L);
            } else {
                map.getController().animateTo(ABBOTTABAD_CENTER, 13.0, 1000L);
                Toast.makeText(this, "Location not found, showing city center", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                Marker userMarker = new Marker(map);
                userMarker.setPosition(userGeoPoint);
                userMarker.setTitle("Your Location");
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                
                // Professional Blue Marker for User Location
                Drawable userIcon = ContextCompat.getDrawable(this, R.drawable.ic_user_location);
                if (userIcon != null) {
                    // Force blue color just in case the drawable isn't blue
                    userIcon.setTint(Color.parseColor("#2196F3")); 
                    userMarker.setIcon(userIcon);
                }
                
                map.getOverlays().add(userMarker);
                map.invalidate();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showUserLocation();
        }
    }

    private void loadSchoolsFromFirebase() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        new FirebaseHelper().getAllSchools(new FirebaseHelper.DataListener() {
            @Override
            public void onSuccess(List<School> schools) {
                cachedSchools = schools;
                renderMarkers(schools, false);
            }

            @Override
            public void onFailure(Exception e) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(MapActivity.this, "Network Error: Unable to fetch schools", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderMarkers(List<School> schools, boolean focusOnSingle) {
        if (map == null || schools == null) return;

        runOnUiThread(() -> {
            map.getOverlays().clear();
            showUserLocation(); // Re-add user pin

            // Update UI Header
            if (textSchoolCount != null) {
                textSchoolCount.setText("Showing " + schools.size() + " schools near you");
            }

            GeoPoint singleFocus = null;
            Drawable redIcon = ContextCompat.getDrawable(this, R.drawable.ic_marker_red);
            Drawable greenIcon = ContextCompat.getDrawable(this, R.drawable.ic_marker_green);

            for (School s : schools) {
                double lat = s.getLatitude();
                double lng = s.getLongitude();

                if (Math.abs(lat) < 0.1) {
                    lat = 34.1689 + (Math.random() - 0.5) * 0.02;
                    lng = 73.2215 + (Math.random() - 0.5) * 0.02;
                }

                GeoPoint pos = new GeoPoint(lat, lng);
                if (singleFocus == null) singleFocus = pos;

                Marker marker = new Marker(map);
                marker.setPosition(pos);
                marker.setTitle(s.getName());
                marker.setSnippet(s.getType() + "\nClick to view details");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // Use robust check matching RecommendationEngine for Public/Government schools
                String schoolType = (s.getType() != null) ? s.getType().toLowerCase() : "private";
                boolean isPublic = schoolType.contains("public") || schoolType.contains("government");
                
                // Professional Marker logic: Red for Govt, Green for Private
                marker.setIcon(isPublic ? redIcon : greenIcon);
                
                marker.setOnMarkerClickListener((m, mapView) -> {
                    Intent intent = new Intent(MapActivity.this, SchoolDetailActivity.class);
                    intent.putExtra("school", s);
                    intent.putExtra("selectedGrade", selectedGrade);
                    startActivity(intent);
                    return true; 
                });

                map.getOverlays().add(marker);
            }

            if (focusOnSingle && singleFocus != null) {
                map.getController().animateTo(singleFocus, 17.0, 1000L);
            } else {
                map.getController().animateTo(ABBOTTABAD_CENTER, 13.0, 1000L);
            }

            if (progressBar != null) progressBar.setVisibility(View.GONE);
            map.invalidate();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
