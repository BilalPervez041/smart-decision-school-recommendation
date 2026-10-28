package com.example.smartdecision.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.example.smartdecision.models.School;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import com.example.smartdecision.utils.FirebaseHelper;

public class SchoolDetailActivity extends AppCompatActivity {
    private DatabaseReference schoolRef;
    private ValueEventListener schoolListener;
    private String currentUserId;
    private String userRole = "User";
    private School currentSchool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_detail);

        currentSchool = (School) getIntent().getSerializableExtra("school");
        if (currentSchool == null) return;

        // Use the dbPath from the school object, fallback to legacy "Schools/" path if missing
        String path = currentSchool.dbPath != null ? currentSchool.dbPath : "Schools/" + currentSchool.id;
        schoolRef = FirebaseDatabase.getInstance().getReference(path);

        currentUserId = FirebaseAuth.getInstance().getUid();
        checkUserRole();

        setupUI(currentSchool);
        observeSchoolChanges();
    }

    private void observeSchoolChanges() {
        schoolListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    School updatedSchool = new School();
                    updatedSchool.id = snapshot.getKey();
                    updatedSchool.dbPath = currentSchool.dbPath; // Preserve the correct path
                    
                    FirebaseHelper.populateSchool(snapshot, updatedSchool);

                    setupUI(updatedSchool);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        schoolRef.addValueEventListener(schoolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (schoolRef != null && schoolListener != null) {
            schoolRef.removeEventListener(schoolListener);
        }
    }

    private void checkUserRole() {
        if (currentUserId == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();
                userRole = snapshot.child("role").getValue(String.class);
                if (userRole == null) {
                    userRole = snapshot.child("profile").child("role").getValue(String.class);
                }
                if (userRole == null) userRole = "User";
                
                // Refresh UI to hide/show buttons based on role
                setupUI(currentSchool);
            }
        });
    }

    private void setupUI(School school) {
        TextView name = findViewById(R.id.detailName);
        TextView address = findViewById(R.id.detailAddress);
        TextView fee = findViewById(R.id.detailFee);
        TextView type = findViewById(R.id.detailType);
        TextView gender = findViewById(R.id.detailGender);
        TextView medium = findViewById(R.id.detailMedium);
        TextView merit = findViewById(R.id.detailMerit);
        TextView contact = findViewById(R.id.detailContact);
        TextView district = findViewById(R.id.detailDistrict);
        TextView level = findViewById(R.id.detailLevel);
        
        // Rating and Reviews Summary
        android.widget.RatingBar ratingBar = findViewById(R.id.detailRatingBar);
        TextView tvRating = findViewById(R.id.tvDetailRating);
        if (ratingBar != null) ratingBar.setRating((float) school.rating);
        if (tvRating != null) tvRating.setText(String.format("%.1f (%d reviews)", school.rating, school.reviewCount));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        name.setText(school.getName());
        address.setText(school.getAddress() + ", " + school.getCity());

        if (level != null) {
            String displayLevel = school.schoolLevel;
            if (displayLevel == null || displayLevel.isEmpty() || displayLevel.equalsIgnoreCase("Not Specified") || displayLevel.equalsIgnoreCase("Unknown")) {
                displayLevel = school.inferSchoolLevel();
            }
            level.setText(displayLevel);
        }

        String selectedGrade = getIntent().getStringExtra("selectedGrade");
        String feeRange = school.getDynamicFeeDisplay(selectedGrade);
        
        if (!feeRange.equals("N/A")) {
            fee.setText(feeRange + " PKR");
        } else {
            fee.setText("N/A");
        }
        type.setText(school.getType());
        gender.setText(school.getGender());
        medium.setText(school.getMedium());
        merit.setText(school.getMeritPercentage());
        contact.setText(school.getContact());
        if (district != null) {
            district.setText(school.district != null ? school.district : "N/A");
        }

        ShapeableImageView detailImage = findViewById(R.id.detailImage);
        ImageView detailLogo = findViewById(R.id.detailLogo);

        // Load Cover Image
        String coverUrl = (school.coverUrl != null && !school.coverUrl.isEmpty() && !school.coverUrl.equals("Unknown")) 
                ? school.coverUrl : school.imageUrl;
        
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("Unknown")) {
            Glide.with(this).load(coverUrl).into(detailImage);
        }

        // Load Logo
        if (school.logoUrl != null && !school.logoUrl.isEmpty() && !school.logoUrl.equals("Unknown")) {
            Glide.with(this).load(school.logoUrl).into(detailLogo);
        } else {
            detailLogo.setImageResource(R.drawable.spinner_background); // Fallback
        }
        
        com.google.android.material.chip.ChipGroup chipGroupFacilities = findViewById(R.id.chipGroupFacilities);
        
        if (school.getFacilities() != null && !school.getFacilities().isEmpty()) {
            chipGroupFacilities.removeAllViews();
            for (String facility : school.getFacilities()) {
                if (facility == null || facility.trim().isEmpty()) continue;
                
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(facility.trim());
                chip.setChipIconResource(android.R.drawable.btn_star_big_on); // Star icon
                chip.setChipIconSize(35f);
                chip.setChipBackgroundColorResource(R.color.white);
                chip.setChipStrokeWidth(2f);
                chip.setChipStrokeColorResource(R.color.accent_blue);
                chip.setTextColor(getResources().getColor(android.R.color.black));
                chip.setTextSize(13f);
                chipGroupFacilities.addView(chip);
            }
        } else {
            chipGroupFacilities.removeAllViews();
            TextView noFac = new TextView(this);
            noFac.setText("No facilities listed");
            chipGroupFacilities.addView(noFac);
        }

        // Admission Section in Detail
        View admissionCard = findViewById(R.id.cardAdmissionInfo);
        if (admissionCard != null) {
            if (school.admissionOpen) {
                admissionCard.setVisibility(View.VISIBLE);
                TextView tvStatus = findViewById(R.id.detailAdmissionStatus);
                TextView tvDates = findViewById(R.id.detailAdmissionDates);
                TextView tvSeats = findViewById(R.id.detailAdmissionSeats);
                TextView tvFee = findViewById(R.id.detailAdmissionFee);
                TextView tvLevels = findViewById(R.id.detailAdmissionLevels);

                if (tvStatus != null) tvStatus.setText("Admissions are currently OPEN");
                if (tvDates != null) tvDates.setText("From " + school.admissionStartDate + " to " + school.admissionEndDate);
                
                StringBuilder seatInfo = new StringBuilder();
                if (school.boysSeats > 0) seatInfo.append("Boys Seats: ").append(school.boysSeats).append("  ");
                if (school.girlsSeats > 0) seatInfo.append("Girls Seats: ").append(school.girlsSeats);
                if (tvSeats != null) tvSeats.setText(seatInfo.toString().trim());
                
                if (tvFee != null) tvFee.setText("Admission Fee: " + school.admissionFee + " PKR");
                
                if (tvLevels != null && school.admissionLevels != null) {
                    tvLevels.setText("Levels: " + String.join(", ", school.admissionLevels));
                }
            } else {
                admissionCard.setVisibility(View.GONE);
            }
        }

        setupGradeWiseFees(school, selectedGrade);

        // Setup Admission Marquee
        setupAdmissionMarquee(school);

        // Setup Navigation
        setupNavigation(school);

        MaterialButton btnApply = findViewById(R.id.btnApplyAdmission);
        if (userRole != null && userRole.toLowerCase().contains("owner")) {
            btnApply.setVisibility(View.GONE);
        } else {
            btnApply.setVisibility(View.VISIBLE);
            btnApply.setOnClickListener(v -> showAdmissionDialog(school));
        }

        findViewById(R.id.btnReviews).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsActivity.class);
            intent.putExtra("schoolId", school.id);
            intent.putExtra("dbPath", school.dbPath); // Pass the correct path
            startActivity(intent);
        });
    }

    private void showAdmissionDialog(School school) {
        if (!school.admissionOpen) {
            Toast.makeText(this, "Admissions are currently closed for this school.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_admission_form, null);
        
        EditText etName = view.findViewById(R.id.etStudentName);
        EditText etFName = view.findViewById(R.id.etFatherName);
        EditText etContact = view.findViewById(R.id.etContact);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPercentage = view.findViewById(R.id.etPercentage);
        Spinner spLevel = view.findViewById(R.id.spAdmissionLevel);
        LinearLayout layoutPercentage = view.findViewById(R.id.layoutPercentage);

        com.google.android.material.textfield.TextInputLayout tilName = view.findViewById(R.id.tilStudentName);
        com.google.android.material.textfield.TextInputLayout tilFName = view.findViewById(R.id.tilFatherName);
        com.google.android.material.textfield.TextInputLayout tilContact = view.findViewById(R.id.tilContact);
        com.google.android.material.textfield.TextInputLayout tilEmail = view.findViewById(R.id.tilEmail);
        com.google.android.material.textfield.TextInputLayout tilPercentage = view.findViewById(R.id.tilPercentage);

        if (school.admissionLevels != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, school.admissionLevels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spLevel.setAdapter(adapter);
        }

        spLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = spLevel.getSelectedItem().toString();
                if (selected.equalsIgnoreCase("Nursery") || selected.equalsIgnoreCase("Playgroup")) {
                    layoutPercentage.setVisibility(View.GONE);
                } else {
                    layoutPercentage.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(view)
                .setTitle("Admission Form")
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Add TextWatchers to clear errors
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilName.setError(null);
                tilFName.setError(null);
                tilContact.setError(null);
                tilEmail.setError(null);
                tilPercentage.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(watcher);
        etFName.addTextChangedListener(watcher);
        etContact.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);
        etPercentage.addTextChangedListener(watcher);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String fName = etFName.getText().toString().trim();
            String contact = etContact.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String level = spLevel.getSelectedItem() != null ? spLevel.getSelectedItem().toString() : "";
            String percentageStr = etPercentage.getText().toString().trim();

            boolean hasError = false;

            if (name.isEmpty()) {
                tilName.setError("Student name is required");
                hasError = true;
            } else tilName.setError(null);

            if (fName.isEmpty()) {
                tilFName.setError("Father name is required");
                hasError = true;
            } else tilFName.setError(null);

            if (contact.isEmpty()) {
                tilContact.setError("Contact number is required");
                hasError = true;
            } else tilContact.setError(null);

            if (email.isEmpty()) {
                tilEmail.setError("Email address is required");
                hasError = true;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Invalid email format");
                hasError = true;
            } else tilEmail.setError(null);

            if (layoutPercentage.getVisibility() == View.VISIBLE) {
                if (percentageStr.isEmpty()) {
                    tilPercentage.setError("Please enter your previous percentage");
                    hasError = true;
                } else {
                    try {
                        double userPercent = Double.parseDouble(percentageStr);
                        double schoolPercent = 0;
                        try {
                            schoolPercent = Double.parseDouble(school.merit_percentage.replace("%", "").trim());
                        } catch (Exception e) {}

                        if (userPercent < schoolPercent) {
                            tilPercentage.setError("Percentage below merit (" + schoolPercent + "%)");
                            hasError = true;
                        } else {
                            tilPercentage.setError(null);
                        }
                    } catch (NumberFormatException e) {
                        tilPercentage.setError("Invalid percentage format");
                        hasError = true;
                    }
                }
            }

            if (hasError) return;

            submitAdmission(name, fName, contact, email, level, percentageStr, school);
            dialog.dismiss();
        });
    }

    private void submitAdmission(String name, String fName, String contact, String email, String level, String percentage, School school) {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Admissions").push();
        String id = ref.getKey();

        Map<String, Object> application = new HashMap<>();
        application.put("id", id);
        application.put("studentName", name);
        application.put("fatherName", fName);
        application.put("contact", contact);
        application.put("email", email);
        application.put("level", level);
        application.put("percentage", percentage);
        application.put("schoolId", school.id);
        application.put("dbPath", school.dbPath); // Store the path for accurate seat decrementing
        application.put("schoolName", school.name);
        application.put("ownerId", school.ownerUid != null ? school.ownerUid : "unknown");
        application.put("userId", currentUserId);
        application.put("status", "pending");
        application.put("timestamp", System.currentTimeMillis());

        ref.setValue(application).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Add to user notifications
                DatabaseReference userNotifRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("notifications").push();
                Map<String, Object> userNotif = new HashMap<>();
                userNotif.put("id", userNotifRef.getKey());
                userNotif.put("title", "Application Pending");
                userNotif.put("message", "Your application for " + school.name + " is currently pending.");
                userNotif.put("status", "pending");
                userNotif.put("type", "user");
                userNotif.put("timestamp", System.currentTimeMillis());
                userNotifRef.setValue(userNotif);

                // Add to owner notifications
                if (school.ownerUid != null && !school.ownerUid.equals("null") && !school.ownerUid.isEmpty()) {
                    DatabaseReference ownerNotifRef = FirebaseDatabase.getInstance().getReference("Users").child(school.ownerUid).child("notifications").push();
                    Map<String, Object> ownerNotif = new HashMap<>();
                    ownerNotif.put("id", ownerNotifRef.getKey());
                    ownerNotif.put("title", "New Admission Request");
                    ownerNotif.put("message", name + " has applied for " + level + " admission.");
                    ownerNotif.put("applicationId", id);
                    ownerNotif.put("type", "owner");
                    ownerNotif.put("timestamp", System.currentTimeMillis());
                    ownerNotifRef.setValue(ownerNotif);
                }

                Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to submit application", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdmissionMarquee(School school) {
        TextView tvMarquee = findViewById(R.id.tvMarqueeStatus);
        if (tvMarquee == null) return;

        tvMarquee.setVisibility(View.VISIBLE);
        tvMarquee.setSelected(true); // Required for marquee to start

        if (school.admissionOpen) {
            tvMarquee.setBackgroundColor(0xFF2E7D32); // Professional Green
            String levels = (school.admissionLevels != null && !school.admissionLevels.isEmpty()) 
                    ? String.join(", ", school.admissionLevels) : "All Levels";
            String marqueeText = "★ ADMISSIONS ARE NOW OPEN! Available for: " + levels + 
                    ". Last date to apply: " + (school.admissionEndDate != null ? school.admissionEndDate : "Soon") + 
                    ". Secure your child's future today! ★   ";
            tvMarquee.setText(marqueeText + marqueeText); // Repeat for smoother scroll
        } else {
            tvMarquee.setBackgroundColor(0xFFC62828); // Professional Red
            String marqueeText = "● Admissions are currently closed. New registrations will start soon. Stay tuned for updates! ●   ";
            tvMarquee.setText(marqueeText + marqueeText);
        }
    }

    private void setupNavigation(School school) {
        View cardLocation = findViewById(R.id.cardLocation);
        if (cardLocation == null) return;

        cardLocation.setOnClickListener(v -> {
            if (school.latitude == 0 && school.longitude == 0) {
                Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Open Google Maps navigation
            String uri = "google.navigation:q=" + school.latitude + "," + school.longitude;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback to browser if Google Maps app is not installed
                String webUri = "https://www.google.com/maps/dir/?api=1&destination=" + school.latitude + "," + school.longitude;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            }
        });
    }

    private void setupGradeWiseFees(School school, String selectedGrade) {
        LinearLayout container = findViewById(R.id.containerGradeFees);
        View divider = findViewById(R.id.feeDivider);
        TextView label = findViewById(R.id.labelGradeFees);

        String[] labels = {"Nursery/Playgroup", "Primary", "Middle", "Matric", "Inter"};
        String[] internalKeys = {"Nursery", "Primary", "Middle", "Matric", "Inter"};
        String[] values = {school.fee_playgroup, school.fee_primary, school.fee_middle, school.fee_matric, school.fee_inter};
        
        boolean hasAny = false;
        container.removeAllViews();

        for (int i = 0; i < labels.length; i++) {
            if (values[i] != null && !values[i].isEmpty() && !values[i].equalsIgnoreCase("Unknown") && !values[i].equalsIgnoreCase("N/A") && !values[i].equals("0")) {
                hasAny = true;
                boolean isSelected = (selectedGrade != null && selectedGrade.equalsIgnoreCase(internalKeys[i]));
                addFeeRow(container, labels[i], values[i], isSelected);
            }
        }

        if (!hasAny) {
            divider.setVisibility(View.GONE);
            label.setVisibility(View.GONE);
            container.setVisibility(View.GONE);
        }
    }

    private void addFeeRow(LinearLayout container, String label, String value, boolean isSelected) {
        View row = getLayoutInflater().inflate(R.layout.item_fee_row, container, false);
        TextView t1 = row.findViewById(R.id.tvFeeLabel);
        TextView t2 = row.findViewById(R.id.tvFeeValue);
        com.google.android.material.card.MaterialCardView card = row.findViewById(R.id.cardFeeRow);
        ImageView icon = row.findViewById(R.id.ivFeeIcon);
        
        t1.setText(isSelected ? label + " (Match)" : label);
        t2.setText(value + " PKR");

        if (isSelected) {
            // Highlight selected row
            card.setStrokeColor(getResources().getColor(R.color.colorPrimary));
            card.setStrokeWidth(4);
            t2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green for match
            icon.setAlpha(1.0f);
        }

        container.addView(row);
    }
}
