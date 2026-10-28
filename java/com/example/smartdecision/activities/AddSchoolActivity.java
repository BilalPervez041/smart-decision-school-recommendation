package com.example.smartdecision.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartdecision.R;
import com.example.smartdecision.models.School;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddSchoolActivity extends AppCompatActivity {

    private ImageView ivCoverPhoto, ivLogoPhoto;
    private View btnRemoveCover, btnRemoveLogo;
    private EditText etName, etCity, etDistrict, etLocationArea, etAddress, etLatitude, etLongitude, etContact, etFacilities, etMerit;
    private EditText etFeeNursery, etFeePrimary, etFeeMiddle, etFeeMatric, etFeeInter;
    private EditText etStartDate, etEndDate, etBoysSeats, etGirlsSeats, etAdmissionFee;
    private AutoCompleteTextView spinnerGender, spinnerType, spinnerMedium, spinnerLevel;
    private SwitchMaterial switchAdmission;
    private ChipGroup chipGroupLevels;
    private View layoutAdmissionFields, cardAdmissionDetails;
    private Button btnSubmit;
    private ProgressBar progressBar;

    private Uri coverUri, logoUri;
    private static final int PICK_COVER_REQUEST = 1;
    private static final int PICK_LOGO_REQUEST = 2;
    private TextInputLayout tilName, tilCity, tilDistrict, tilGender, tilType, tilMedium, tilLevel, tilContact, tilArea, tilAddress, tilLat, tilLng, tilMerit;
    private TextInputLayout tilBoysSeats, tilGirlsSeats, tilStartDate, tilEndDate, tilAdmissionFee;
    private School existingSchool;
    private boolean isEditMode = false;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_school);

        isAdminMode = getIntent().getBooleanExtra("is_admin", false);

        initViews();
        setupSpinners();

        if (getIntent().hasExtra("edit_school")) {
            existingSchool = (School) getIntent().getSerializableExtra("edit_school");
            isEditMode = true;
            prefillData();
            btnSubmit.setText("Update School Information");
        }

        if (isAdminMode) {
            if (cardAdmissionDetails != null) cardAdmissionDetails.setVisibility(View.GONE);
        }

        findViewById(R.id.frameSelectCover).setOnClickListener(v -> openGallery(PICK_COVER_REQUEST));
        findViewById(R.id.frameSelectLogo).setOnClickListener(v -> openGallery(PICK_LOGO_REQUEST));
        findViewById(R.id.frameSelectCover).setOnClickListener(v -> openGallery(PICK_COVER_REQUEST));
        findViewById(R.id.frameSelectLogo).setOnClickListener(v -> openGallery(PICK_LOGO_REQUEST));

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void prefillData() {
        if (existingSchool == null) return;
        
        etName.setText(existingSchool.name);
        etCity.setText(existingSchool.city);
        etDistrict.setText(existingSchool.district);
        etLocationArea.setText(existingSchool.location_area);
        etAddress.setText(existingSchool.address);
        etLatitude.setText(String.valueOf(existingSchool.latitude));
        etLongitude.setText(String.valueOf(existingSchool.longitude));
        etContact.setText(existingSchool.contact);
        etMerit.setText(existingSchool.merit_percentage);
        
        if (existingSchool.facilities != null) {
            etFacilities.setText(String.join(",", existingSchool.facilities));
        }

        etFeeNursery.setText(existingSchool.fee_playgroup);
        etFeePrimary.setText(existingSchool.fee_primary);
        etFeeMiddle.setText(existingSchool.fee_middle);
        etFeeMatric.setText(existingSchool.fee_matric);
        etFeeInter.setText(existingSchool.fee_inter);

        spinnerGender.setText(existingSchool.gender, false);
        spinnerType.setText(existingSchool.type, false);
        spinnerMedium.setText(existingSchool.medium, false);
        spinnerLevel.setText(existingSchool.schoolLevel, false);
        
        updateFeeFieldsVisibility(existingSchool.schoolLevel);
        updateSeatFieldsVisibility(existingSchool.gender);

        switchAdmission.setChecked(existingSchool.admissionOpen);
        layoutAdmissionFields.setVisibility(existingSchool.admissionOpen ? View.VISIBLE : View.GONE);
        etStartDate.setText(existingSchool.admissionStartDate);
        etEndDate.setText(existingSchool.admissionEndDate);
        etBoysSeats.setText(String.valueOf(existingSchool.boysSeats));
        etGirlsSeats.setText(String.valueOf(existingSchool.girlsSeats));
        etAdmissionFee.setText(existingSchool.admissionFee);
        
        if (existingSchool.admissionLevels != null) {
            for (String level : existingSchool.admissionLevels) {
                if (level.equalsIgnoreCase("Nursery")) ((Chip)findViewById(R.id.chipNursery)).setChecked(true);
                if (level.equalsIgnoreCase("Primary")) ((Chip)findViewById(R.id.chipPrimary)).setChecked(true);
                if (level.equalsIgnoreCase("Middle")) ((Chip)findViewById(R.id.chipMiddle)).setChecked(true);
                if (level.equalsIgnoreCase("Matric")) ((Chip)findViewById(R.id.chipMatric)).setChecked(true);
                if (level.equalsIgnoreCase("Inter")) ((Chip)findViewById(R.id.chipInter)).setChecked(true);
            }
        }

        if (existingSchool.coverUrl != null && !existingSchool.coverUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(existingSchool.coverUrl).into(ivCoverPhoto);
            ivCoverPhoto.setPadding(0,0,0,0);
            if (btnRemoveCover != null) btnRemoveCover.setVisibility(View.VISIBLE);
        } else if (existingSchool.imageUrl != null && !existingSchool.imageUrl.isEmpty()) {
            // Backward compatibility
            com.bumptech.glide.Glide.with(this).load(existingSchool.imageUrl).into(ivCoverPhoto);
            ivCoverPhoto.setPadding(0,0,0,0);
            if (btnRemoveCover != null) btnRemoveCover.setVisibility(View.VISIBLE);
        }

        if (existingSchool.logoUrl != null && !existingSchool.logoUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(existingSchool.logoUrl).into(ivLogoPhoto);
            ivLogoPhoto.setPadding(0,0,0,0);
            if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        ivCoverPhoto = findViewById(R.id.ivCoverPhoto);
        ivLogoPhoto = findViewById(R.id.ivLogoPhoto);
        btnRemoveCover = findViewById(R.id.btnRemoveCover);
        btnRemoveLogo = findViewById(R.id.btnRemoveLogo);

        if (btnRemoveCover != null) {
            btnRemoveCover.setOnClickListener(v -> {
                coverUri = null;
                if (existingSchool != null) existingSchool.coverUrl = "";
                ivCoverPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                ivCoverPhoto.setPadding(100, 100, 100, 100);
                btnRemoveCover.setVisibility(View.GONE);
            });
        }

        if (btnRemoveLogo != null) {
            btnRemoveLogo.setOnClickListener(v -> {
                logoUri = null;
                if (existingSchool != null) existingSchool.logoUrl = "";
                ivLogoPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                ivLogoPhoto.setPadding(70, 70, 70, 70);
                btnRemoveLogo.setVisibility(View.GONE);
            });
        }

        etName = findViewById(R.id.etSchoolName);
        etCity = findViewById(R.id.etCity);
        etDistrict = findViewById(R.id.etDistrict);
        etLocationArea = findViewById(R.id.etLocationArea);
        etAddress = findViewById(R.id.etAddress);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etContact = findViewById(R.id.etContact);
        etFacilities = findViewById(R.id.etFacilities);
        etMerit = findViewById(R.id.etMerit);

        tilName = findViewById(R.id.tilName);
        tilCity = findViewById(R.id.tilCity);
        tilDistrict = findViewById(R.id.tilDistrict);
        tilGender = findViewById(R.id.tilGender);
        tilType = findViewById(R.id.tilType);
        tilMedium = findViewById(R.id.tilMedium);
        tilLevel = findViewById(R.id.tilLevel);
        tilContact = findViewById(R.id.tilContact);
        tilArea = findViewById(R.id.tilArea);
        tilAddress = findViewById(R.id.tilAddress);
        tilLat = findViewById(R.id.tilLat);
        tilLng = findViewById(R.id.tilLng);
        tilMerit = findViewById(R.id.tilMerit);

        // Admission Views
        switchAdmission = findViewById(R.id.switchAdmission);
        layoutAdmissionFields = findViewById(R.id.layoutAdmissionFields);
        cardAdmissionDetails = findViewById(R.id.cardAdmissionDetails);
        chipGroupLevels = findViewById(R.id.chipGroupLevels);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etBoysSeats = findViewById(R.id.etBoysSeats);
        etGirlsSeats = findViewById(R.id.etGirlsSeats);
        etAdmissionFee = findViewById(R.id.etAdmissionFee);
        
        tilBoysSeats = findViewById(R.id.tilBoysSeats);
        tilGirlsSeats = findViewById(R.id.tilGirlsSeats);
        tilStartDate = findViewById(R.id.tilStartDate);
        tilEndDate = findViewById(R.id.tilEndDate);
        tilAdmissionFee = findViewById(R.id.tilAdmissionFee);

        switchAdmission.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutAdmissionFields.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        
        etFeeNursery = findViewById(R.id.etFeeNursery);
        etFeePrimary = findViewById(R.id.etFeePrimary);
        etFeeMiddle = findViewById(R.id.etFeeMiddle);
        etFeeMatric = findViewById(R.id.etFeeMatric);
        etFeeInter = findViewById(R.id.etFeeInter);

        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerMedium = findViewById(R.id.spinnerMedium);
        spinnerLevel = findViewById(R.id.spinnerLevel);

        btnSubmit = findViewById(R.id.btnSubmitSchool);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        String[] genders = {"Male", "Female", "Co-education"};
        String[] types = {"Public", "Private"};
        String[] mediums = {"Urdu", "English"};
        String[] levels = {"Nursery to Middle", "Nursery to Matric", "Nursery to Inter", "Primary to Middle", "Primary to Matric", "Primary to Inter", "Middle to Matric", "Middle to Inter"};

        spinnerGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders));
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types));
        spinnerMedium.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mediums));
        spinnerLevel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, levels));

        spinnerGender.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGender = (String) parent.getItemAtPosition(position);
            updateSeatFieldsVisibility(selectedGender);
        });

        spinnerLevel.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLevel = (String) parent.getItemAtPosition(position);
            updateFeeFieldsVisibility(selectedLevel);
        });
    }

    private void updateSeatFieldsVisibility(String gender) {
        if (gender == null) return;
        if (gender.equalsIgnoreCase("Male")) {
            tilBoysSeats.setVisibility(View.VISIBLE);
            tilGirlsSeats.setVisibility(View.GONE);
        } else if (gender.equalsIgnoreCase("Female")) {
            tilBoysSeats.setVisibility(View.GONE);
            tilGirlsSeats.setVisibility(View.VISIBLE);
        } else if (gender.equalsIgnoreCase("Co-education")) {
            tilBoysSeats.setVisibility(View.VISIBLE);
            tilGirlsSeats.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker(EditText editText) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            editText.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateFeeFieldsVisibility(String level) {
        View tilNursery = findViewById(R.id.tilFeeNursery);
        View tilPrimary = findViewById(R.id.tilFeePrimary);
        View tilMiddle = findViewById(R.id.tilFeeMiddle);
        View tilMatric = findViewById(R.id.tilFeeMatric);
        View tilInter = findViewById(R.id.tilFeeInter);

        View chNursery = findViewById(R.id.chipNursery);
        View chPrimary = findViewById(R.id.chipPrimary);
        View chMiddle = findViewById(R.id.chipMiddle);
        View chMatric = findViewById(R.id.chipMatric);
        View chInter = findViewById(R.id.chipInter);

        String lvl = (level == null) ? "" : level.toLowerCase();
        
        int start = 0, end = 4;
        if (lvl.startsWith("nursery")) start = 0;
        else if (lvl.startsWith("primary")) start = 1;
        else if (lvl.startsWith("middle")) start = 2;

        if (lvl.endsWith("middle")) end = 2;
        else if (lvl.endsWith("matric")) end = 3;
        else if (lvl.endsWith("inter")) end = 4;

        boolean showNursery = (start <= 0 && end >= 0);
        boolean showPrimary = (start <= 1 && end >= 1);
        boolean showMiddle = (start <= 2 && end >= 2);
        boolean showMatric = (start <= 3 && end >= 3);
        boolean showInter = (start <= 4 && end >= 4);

        if (tilNursery != null) {
            tilNursery.setVisibility(showNursery ? View.VISIBLE : View.GONE);
            if (chNursery != null) chNursery.setVisibility(showNursery ? View.VISIBLE : View.GONE);
            if (!showNursery) {
                if (etFeeNursery != null) etFeeNursery.setText("");
                if (chNursery instanceof com.google.android.material.chip.Chip) ((com.google.android.material.chip.Chip)chNursery).setChecked(false);
            }
        }
        if (tilPrimary != null) {
            tilPrimary.setVisibility(showPrimary ? View.VISIBLE : View.GONE);
            if (chPrimary != null) chPrimary.setVisibility(showPrimary ? View.VISIBLE : View.GONE);
            if (!showPrimary) {
                if (etFeePrimary != null) etFeePrimary.setText("");
                if (chPrimary instanceof com.google.android.material.chip.Chip) ((com.google.android.material.chip.Chip)chPrimary).setChecked(false);
            }
        }
        if (tilMiddle != null) {
            tilMiddle.setVisibility(showMiddle ? View.VISIBLE : View.GONE);
            if (chMiddle != null) chMiddle.setVisibility(showMiddle ? View.VISIBLE : View.GONE);
            if (!showMiddle) {
                if (etFeeMiddle != null) etFeeMiddle.setText("");
                if (chMiddle instanceof com.google.android.material.chip.Chip) ((com.google.android.material.chip.Chip)chMiddle).setChecked(false);
            }
        }
        if (tilMatric != null) {
            tilMatric.setVisibility(showMatric ? View.VISIBLE : View.GONE);
            if (chMatric != null) chMatric.setVisibility(showMatric ? View.VISIBLE : View.GONE);
            if (!showMatric) {
                if (etFeeMatric != null) etFeeMatric.setText("");
                if (chMatric instanceof com.google.android.material.chip.Chip) ((com.google.android.material.chip.Chip)chMatric).setChecked(false);
            }
        }
        if (tilInter != null) {
            tilInter.setVisibility(showInter ? View.VISIBLE : View.GONE);
            if (chInter != null) chInter.setVisibility(showInter ? View.VISIBLE : View.GONE);
            if (!showInter) {
                if (etFeeInter != null) etFeeInter.setText("");
                if (chInter instanceof com.google.android.material.chip.Chip) ((com.google.android.material.chip.Chip)chInter).setChecked(false);
            }
        }
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_COVER_REQUEST) {
                coverUri = data.getData();
                ivCoverPhoto.setImageURI(coverUri);
                ivCoverPhoto.setPadding(0, 0, 0, 0);
                if (btnRemoveCover != null) btnRemoveCover.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_LOGO_REQUEST) {
                logoUri = data.getData();
                ivLogoPhoto.setImageURI(logoUri);
                ivLogoPhoto.setPadding(0, 0, 0, 0);
                if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.VISIBLE);
            }
        }
    }

    private void validateAndSubmit() {
        // Clear previous errors
        tilName.setError(null);
        tilCity.setError(null);
        tilDistrict.setError(null);
        tilGender.setError(null);
        tilType.setError(null);
        tilMedium.setError(null);
        tilLevel.setError(null);
        tilContact.setError(null);
        tilArea.setError(null);
        tilAddress.setError(null);
        tilLat.setError(null);
        tilLng.setError(null);
        tilMerit.setError(null);

        String name = etName.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String district = etDistrict.getText().toString().trim();
        String gender = spinnerGender.getText().toString();
        String type = spinnerType.getText().toString();
        String medium = spinnerMedium.getText().toString();
        String level = spinnerLevel.getText().toString();
        String locArea = etLocationArea.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String lat = etLatitude.getText().toString().trim();
        String lng = etLongitude.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String facilities = etFacilities.getText().toString().trim();
        String merit = etMerit.getText().toString().trim();
        String selectedLevel = spinnerLevel.getText().toString();

        boolean isValid = true;

        if (name.isEmpty()) {
            tilName.setError("School name is required");
            isValid = false;
        }
        if (city.isEmpty()) {
            tilCity.setError("City is required");
            isValid = false;
        }
        if (district.isEmpty()) {
            tilDistrict.setError("District is required");
            isValid = false;
        }
        if (gender.isEmpty()) {
            tilGender.setError("Select gender");
            isValid = false;
        }
        if (type.isEmpty()) {
            tilType.setError("Select school type");
            isValid = false;
        }
        if (medium.isEmpty()) {
            tilMedium.setError("Select medium");
            isValid = false;
        }
        if (level.isEmpty()) {
            tilLevel.setError("Select school level");
            isValid = false;
        }
        if (contact.isEmpty()) {
            tilContact.setError("Contact number is required");
            isValid = false;
        }
        if (locArea.isEmpty()) {
            tilArea.setError("Area is required");
            isValid = false;
        }
        if (address.isEmpty()) {
            tilAddress.setError("Address is required");
            isValid = false;
        }
        if (lat.isEmpty()) {
            tilLat.setError("Latitude required");
            isValid = false;
        }
        if (lng.isEmpty()) {
            tilLng.setError("Longitude required");
            isValid = false;
        }

        // Facilities count validation (Max 15)
        if (!facilities.isEmpty()) {
            String[] parts = facilities.split(",");
            int count = 0;
            for (String p : parts) if (!p.trim().isEmpty()) count++;
            
            if (count > 15) {
                Toast.makeText(this, "Maximum 15 facilities allowed (You entered: " + count + ")", Toast.LENGTH_LONG).show();
                isValid = false;
            }
        }

        // Percentage validation: Allow owner/admin to set any percentage
        if (!selectedLevel.toLowerCase().contains("nursery")) {
            if (merit.isEmpty()) {
                tilMerit.setError("Merit percentage is required");
                isValid = false;
            } else {
                try {
                    int m = Integer.parseInt(merit);
                    if (m < 0 || m > 100) {
                        tilMerit.setError("Percentage must be between 0 and 100");
                        isValid = false;
                    }
                } catch (Exception e) {
                    tilMerit.setError("Invalid percentage");
                    isValid = false;
                }
            }
        }

        if (!isValid) {
            Toast.makeText(this, "Please fix errors in the form", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic updated: logo and cover are optional as requested
        startSubmission(name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
    }

    private void startSubmission(String name, String city, String district, String gender, String type, String medium, String level,
                                         String locArea, String address, String lat, String lng, String contact, String facilities, String merit) {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (isEditMode) {
            handleImageUploads(name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
        } else {
            // Duplicate check
            FirebaseDatabase.getInstance().getReference("Schools")
                .orderByChild("name")
                .equalTo(name)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        boolean isDuplicate = false;
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            School s = ds.getValue(School.class);
                            if (s != null && level.equals(s.schoolLevel)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(AddSchoolActivity.this, "This school level is already registered.", Toast.LENGTH_SHORT).show();
                        } else {
                            handleImageUploads(name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
                        }
                    }

                    @Override
                    public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                    }
                });
        }
    }

    private void handleImageUploads(String name, String city, String district, String gender, String type, String medium, String level,
                                    String locArea, String address, String lat, String lng, String contact, String facilities, String merit) {
        
        final String[] urls = new String[2]; // 0: cover, 1: logo
        if (isEditMode) {
            urls[0] = existingSchool.coverUrl != null ? existingSchool.coverUrl : existingSchool.imageUrl;
            urls[1] = existingSchool.logoUrl;
        } else {
            urls[0] = "";
            urls[1] = "";
        }

        if (coverUri != null) {
            uploadToCloudinary(coverUri, new CloudinaryCallback() {
                @Override
                public void onComplete(String url) {
                    urls[0] = url;
                    checkLogoAndSave(urls, name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
                }
                @Override
                public void onError(String msg) {
                    runOnUiThread(() -> { progressBar.setVisibility(View.GONE); btnSubmit.setEnabled(true); Toast.makeText(AddSchoolActivity.this, "Cover upload failed: " + msg, Toast.LENGTH_SHORT).show(); });
                }
            });
        } else {
            checkLogoAndSave(urls, name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
        }
    }

    private void checkLogoAndSave(String[] urls, String name, String city, String district, String gender, String type, String medium, String level,
                                  String locArea, String address, String lat, String lng, String contact, String facilities, String merit) {
        if (logoUri != null) {
            uploadToCloudinary(logoUri, new CloudinaryCallback() {
                @Override
                public void onComplete(String url) {
                    urls[1] = url;
                    saveSchoolToDb(urls[0], urls[1], name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
                }
                @Override
                public void onError(String msg) {
                    runOnUiThread(() -> { progressBar.setVisibility(View.GONE); btnSubmit.setEnabled(true); Toast.makeText(AddSchoolActivity.this, "Logo upload failed: " + msg, Toast.LENGTH_SHORT).show(); });
                }
            });
        } else {
            saveSchoolToDb(urls[0], urls[1], name, city, district, gender, type, medium, level, locArea, address, lat, lng, contact, facilities, merit);
        }
    }

    private interface CloudinaryCallback {
        void onComplete(String url);
        void onError(String msg);
    }

    private void uploadToCloudinary(Uri uri, CloudinaryCallback callback) {
        File file = getFileFromUri(uri);
        if (file == null) {
            callback.onError("File access error");
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).build();
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "image/jpeg";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse(mimeType), file))
                .addFormDataPart("upload_preset", "smart_decision") 
                .build();

        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dpd4b0ua5/image/upload")
                .post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    if (r.isSuccessful() && r.body() != null) {
                        JSONObject json = new JSONObject(r.body().string());
                        callback.onComplete(json.getString("secure_url"));
                    } else { callback.onError("Server error"); }
                } catch (Exception e) { callback.onError(e.getMessage()); }
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File file = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, len);
            outputStream.flush(); outputStream.close(); inputStream.close();
            return file;
        } catch (IOException e) { return null; }
    }

    private void saveSchoolToDb(String coverUrl, String logoUrl, String name, String city, String district, String gender, String type, String medium, 
                               String level, String locArea, String address, String lat, String lng, String contact, String facilities, String merit) {
        
        School school = isEditMode ? existingSchool : new School();
        school.name = name;
        school.city = city;
        school.district = district;
        school.gender = gender;
        school.type = type;
        school.medium = medium;
        school.schoolLevel = level;
        school.location_area = locArea;
        school.address = address;
        school.merit_percentage = merit;
        try {
            school.latitude = Double.parseDouble(lat);
            school.longitude = Double.parseDouble(lng);
        } catch (Exception ignored) {}
        school.contact = contact;
        school.coverUrl = coverUrl;
        school.logoUrl = logoUrl;
        school.imageUrl = coverUrl; // Maintain legacy field
        
        if (!isEditMode) {
            school.ownerUid = FirebaseAuth.getInstance().getUid();
            school.status = "pending";
            school.rating = 0.0;
        }
        
        if (facilities != null && !facilities.isEmpty()) {
            school.facilities = Arrays.asList(facilities.split(","));
        }

        school.fee_playgroup = etFeeNursery.getText().toString().trim();
        school.fee_primary = etFeePrimary.getText().toString().trim();
        school.fee_middle = etFeeMiddle.getText().toString().trim();
        school.fee_matric = etFeeMatric.getText().toString().trim();
        school.fee_inter = etFeeInter.getText().toString().trim();
        
        // Clean fields before saving to avoid bracketed values
        school.fee_playgroup = School.clean(school.fee_playgroup);
        school.fee_primary = School.clean(school.fee_primary);
        school.fee_middle = School.clean(school.fee_middle);
        school.fee_matric = School.clean(school.fee_matric);
        school.fee_inter = School.clean(school.fee_inter);

        if (!isAdminMode) {
            school.admissionOpen = switchAdmission.isChecked();
            if (school.admissionOpen) {
                school.admissionStartDate = etStartDate.getText().toString();
                school.admissionEndDate = etEndDate.getText().toString();
                try {
                    school.boysSeats = Integer.parseInt(etBoysSeats.getText().toString().isEmpty() ? "0" : etBoysSeats.getText().toString());
                    school.girlsSeats = Integer.parseInt(etGirlsSeats.getText().toString().isEmpty() ? "0" : etGirlsSeats.getText().toString());
                } catch (Exception ignored) {}
                school.admissionFee = etAdmissionFee.getText().toString();
                
                List<String> selectedLevels = new ArrayList<>();
                if (((Chip)findViewById(R.id.chipNursery)).isChecked()) selectedLevels.add("Nursery");
                if (((Chip)findViewById(R.id.chipPrimary)).isChecked()) selectedLevels.add("Primary");
                if (((Chip)findViewById(R.id.chipMiddle)).isChecked()) selectedLevels.add("Middle");
                if (((Chip)findViewById(R.id.chipMatric)).isChecked()) selectedLevels.add("Matric");
                if (((Chip)findViewById(R.id.chipInter)).isChecked()) selectedLevels.add("Inter");
                school.admissionLevels = selectedLevels;
            }
        }

        school.fee_range = school.getDynamicFeeDisplay();
        
        // Final cleanup for "N/A" or empty strings
        if (school.fee_playgroup.equalsIgnoreCase("N/A") || school.fee_playgroup.isEmpty()) school.fee_playgroup = "";
        if (school.fee_primary.equalsIgnoreCase("N/A") || school.fee_primary.isEmpty()) school.fee_primary = "";
        if (school.fee_middle.equalsIgnoreCase("N/A") || school.fee_middle.isEmpty()) school.fee_middle = "";
        if (school.fee_matric.equalsIgnoreCase("N/A") || school.fee_matric.isEmpty()) school.fee_matric = "";
        if (school.fee_inter.equalsIgnoreCase("N/A") || school.fee_inter.isEmpty()) school.fee_inter = "";

        String key = isEditMode ? school.id : FirebaseDatabase.getInstance().getReference("Schools").push().getKey();
        if (key == null) return;

        String path = (isEditMode && school.dbPath != null && !school.dbPath.isEmpty()) ? school.dbPath : "Schools/" + key;
        school.id = key;
        if (isEditMode) school.dbPath = path; // Ensure dbPath is preserved/set on edit
        
        FirebaseDatabase.getInstance().getReference(path).setValue(school)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, isEditMode ? "School updated!" : "Submitted successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
