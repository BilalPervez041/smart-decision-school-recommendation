package com.example.smartdecision.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.example.smartdecision.R;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminSchoolDetailActivity extends AppCompatActivity {

    private School school;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_school_detail);

        school = (School) getIntent().getSerializableExtra("school");
        if (school == null) {
            finish();
            return;
        }
        school.cleanAllFields(); // Clean intent data immediately

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        initView();
        setupActions();
        listenForRealtimeUpdates();
    }

    private void listenForRealtimeUpdates() {
        String path = (school.dbPath != null && !school.dbPath.isEmpty()) ? school.dbPath : "Schools/" + school.id;
        FirebaseDatabase.getInstance().getReference(path)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            FirebaseHelper.populateSchool(snapshot, school);
                            initView(); 
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String robustClean(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val).trim();
        
        // Use the centralized cleaning logic from School model
        return School.clean(s);
    }

    private void initView() {
        TextView name = findViewById(R.id.adminDetailName);
        TextView level = findViewById(R.id.adminDetailLevel);
        TextView address = findViewById(R.id.adminDetailAddress);
        TextView status = findViewById(R.id.adminDetailStatus);
        TextView type = findViewById(R.id.adminDetailType);
        TextView gender = findViewById(R.id.adminDetailGender);
        TextView medium = findViewById(R.id.adminDetailMedium);
        com.google.android.material.chip.ChipGroup chipGroupFacilities = findViewById(R.id.adminChipGroupFacilities);
        TextView contact = findViewById(R.id.adminDetailContact);
        TextView merit = findViewById(R.id.adminDetailMerit);
        
        android.widget.RatingBar ratingBar = findViewById(R.id.adminDetailRatingBar);
        TextView ratingText = findViewById(R.id.adminDetailRatingText);

        name.setText(school.getName());
        if (level != null) {
            String displayLevel = school.schoolLevel;
            if (displayLevel == null || displayLevel.isEmpty() || displayLevel.equalsIgnoreCase("Not Specified")) {
                displayLevel = school.inferSchoolLevel();
            }
            level.setText(displayLevel);
        }
        address.setText(school.getAddress() + ", " + school.getCity());
        if (school.status != null && school.status.equalsIgnoreCase("approved")) {
            status.setText("✓ " + school.status.toUpperCase());
            status.setTextColor(0xFF10B981); // Emerald Green
        } else {
            status.setText("⚠ " + (school.status != null ? school.status.toUpperCase() : "PENDING"));
            status.setTextColor(0xFFEF4444); // Rose Red
        }
        
        type.setText(school.getType());
        gender.setText(school.getGender());
        medium.setText(school.getMedium());
        
        if (school.getFacilities() != null && !school.getFacilities().isEmpty()) {
            chipGroupFacilities.removeAllViews();
            for (String facility : school.getFacilities()) {
                if (facility == null || facility.trim().isEmpty()) continue;
                
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText(facility.trim());
                chip.setChipIconResource(android.R.drawable.btn_star_big_on); // Star icon
                chip.setChipIconSize(35f);
                chip.setChipBackgroundColorResource(android.R.color.white);
                chip.setChipStrokeWidth(2f);
                chip.setChipStrokeColorResource(R.color.accent_blue);
                chip.setTextColor(getResources().getColor(android.R.color.black));
                chip.setTextSize(13f);
                chipGroupFacilities.addView(chip);
            }
        } else {
            chipGroupFacilities.removeAllViews();
            TextView noFac = new TextView(this);
            noFac.setText("N/A");
            chipGroupFacilities.addView(noFac);
        }
        contact.setText(school.getContact() != null ? school.getContact() : "N/A");
        merit.setText(school.getMeritPercentage());

        ratingBar.setRating((float) school.getRating());
        ratingText.setText(String.format(java.util.Locale.US, "(%.1f)", school.getRating()));

        TextView textAdmissionStatus = findViewById(R.id.textAdminAdmissionStatus);
        if (textAdmissionStatus != null) {
            textAdmissionStatus.setText(school.admissionOpen ? "● Open" : "○ Closed");
            textAdmissionStatus.setTextColor(school.admissionOpen ? 
                    0xFF10B981 : // Emerald
                    0xFFEF4444); // Rose
        }

        ImageView coverImage = findViewById(R.id.adminDetailCoverImage);
        ImageView detailLogo = findViewById(R.id.adminDetailLogo);

        String coverUrl = (school.coverUrl != null && !school.coverUrl.isEmpty()) ? school.coverUrl : school.imageUrl;
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("Unknown")) {
            Glide.with(this).load(coverUrl).into(coverImage);
        }

        if (school.logoUrl != null && !school.logoUrl.isEmpty() && !school.logoUrl.equals("Unknown")) {
            Glide.with(this).load(school.logoUrl).into(detailLogo);
        } else {
            detailLogo.setImageResource(R.drawable.spinner_background);
        }

        setupFees();
        setupAdmissionCard();
        setupMarquee();
    }

    private void setupMarquee() {
        TextView tvMarquee = findViewById(R.id.tvMarqueeStatus);
        if (tvMarquee == null) return;

        if (school.admissionOpen) {
            tvMarquee.setVisibility(View.VISIBLE);
            tvMarquee.setText("★ ADMISSIONS OPEN: " + (school.admissionLevels != null ? String.join(", ", school.admissionLevels) : "All Levels") + 
                             " ★ Last Date: " + school.admissionEndDate + " ★ Apply Now! ★");
            tvMarquee.setSelected(true); // Required for marquee to start
        } else {
            tvMarquee.setVisibility(View.GONE);
        }
    }

    private void setupAdmissionCard() {
        View admissionCard = findViewById(R.id.cardAdmissionInfo);
        if (admissionCard == null) return;

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
            if (school.boysSeats > 0) seatInfo.append("Boys: ").append(school.boysSeats).append("  ");
            if (school.girlsSeats > 0) seatInfo.append("Girls: ").append(school.girlsSeats);
            if (tvSeats != null) tvSeats.setText("Seats Available - " + seatInfo.toString().trim());
            
            if (tvFee != null) tvFee.setText("Admission Fee: " + school.admissionFee + " PKR");
            
            if (tvLevels != null && school.admissionLevels != null) {
                tvLevels.setText("Levels: " + String.join(", ", school.admissionLevels));
            }
        } else {
            admissionCard.setVisibility(View.GONE);
        }
    }

    private void setupFees() {
        LinearLayout container = findViewById(R.id.adminContainerFees);
        container.removeAllViews();

        String[] labels = {"Nursery/Playgroup", "Primary", "Middle", "Matric", "Inter"};
        String[] values = {
            school.fee_playgroup,
            school.fee_primary,
            school.fee_middle,
            school.fee_matric,
            school.fee_inter
        };

        boolean hasFees = false;
        for (int i = 0; i < labels.length; i++) {
            String val = values[i];
            if (val != null && !val.isEmpty() && !val.equalsIgnoreCase("N/A") && !val.equalsIgnoreCase("Unknown") && !val.equals("0")) {
                hasFees = true;
                addFeeRow(container, labels[i], val);
            }
        }
        
        if (!hasFees) {
            TextView tv = new TextView(this);
            tv.setText("No fee data available");
            tv.setPadding(16, 16, 16, 16);
            tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
            container.addView(tv);
        }
    }

    private void addFeeRow(LinearLayout container, String label, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_fee_row, container, false);
        TextView t1 = row.findViewById(R.id.tvFeeLabel);
        TextView t2 = row.findViewById(R.id.tvFeeValue);
        
        t1.setText(label);
        t2.setText(value + " PKR");

        container.addView(row);
    }

    private void setupActions() {
        findViewById(R.id.btnAdminEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddSchoolActivity.class);
            intent.putExtra("edit_school", school);
            intent.putExtra("is_admin", true);
            startActivity(intent);
        });

        findViewById(R.id.btnAdminContact).setOnClickListener(v -> fetchOwnerEmailAndSend());

        findViewById(R.id.btnAdminReviews).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsActivity.class);
            intent.putExtra("schoolId", school.id);
            intent.putExtra("dbPath", school.dbPath);
            startActivity(intent);
        });

        findViewById(R.id.btnAdminDelete).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete School")
                    .setMessage("Are you sure you want to permanently remove this school from the system?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String path = (school.dbPath != null && !school.dbPath.isEmpty()) ? school.dbPath : "Schools/" + school.id;
                        FirebaseDatabase.getInstance().getReference(path).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "School deleted successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void fetchOwnerEmailAndSend() {
        if (school.ownerUid == null || school.ownerUid.isEmpty()) {
            sendEmail("dummy@gmail.com");
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users").child(school.ownerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String email = snapshot.child("email").getValue(String.class);
                        sendEmail(email != null ? email : "dummy@gmail.com");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        sendEmail("dummy@gmail.com");
                    }
                });
    }

    private void sendEmail(String email) {
        String subject = "Regarding School Profile: " + school.getName();
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        try {
            startActivity(Intent.createChooser(intent, "Contact Owner"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }
}
