package com.example.smartdecision.utils;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.smartdecision.models.School;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper_Final";
    private final DatabaseReference dbRef;

    public interface DataListener {
        void onSuccess(List<School> schools);
        void onFailure(Exception e);
    }

    public FirebaseHelper() {
        // Online-only mode for real-time market readiness
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(false);
        } catch (Exception e) {
            // Already initialized
        }
        this.dbRef = FirebaseDatabase.getInstance().getReference();
        this.dbRef.keepSynced(true);
    }

    public com.google.firebase.database.ValueEventListener getAllSchools(DataListener listener) {
        Log.d(TAG, "Requesting data from Firebase Root...");
        
        com.google.firebase.database.ValueEventListener listener1 = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<School> schools = new ArrayList<>();
                if (!snapshot.exists()) {
                    listener.onSuccess(schools);
                    return;
                }

                // 1. New System: Check "Schools" node (Flat or Owner-nested)
                DataSnapshot schoolsNode = snapshot.child("Schools");
                if (schoolsNode.exists()) {
                    for (DataSnapshot child : schoolsNode.getChildren()) {
                        if (populateSchool(child, new School())) {
                            parseSchool(child, schools, "Schools/" + child.getKey());
                        } else {
                            // Try nested structure: Schools/ownerUid/schoolId
                            for (DataSnapshot nested : child.getChildren()) {
                                parseSchool(nested, schools, "Schools/" + child.getKey() + "/" + nested.getKey());
                            }
                        }
                    }
                }

                // 2. Legacy/Manual: Check Root (skipping internal system nodes)
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    if (key == null || isInternalNode(key)) continue;

                    if (populateSchool(child, new School())) {
                        parseSchool(child, schools, key);
                    } else {
                        // Explore manual container nodes
                        for (DataSnapshot nested : child.getChildren()) {
                            parseSchool(nested, schools, key + "/" + nested.getKey());
                        }
                    }
                }
                
                Log.d(TAG, "Total professional schools aggregated: " + schools.size());
                listener.onSuccess(schools);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database Error: " + error.getMessage());
                listener.onFailure(error.toException());
            }
        };
        dbRef.addValueEventListener(listener1);
        return listener1;
    }

    private boolean isInternalNode(String key) {
        String k = key.toLowerCase();
        return k.equalsIgnoreCase("Users") || 
               k.equalsIgnoreCase("Admin") || k.equalsIgnoreCase("Admins") ||
               k.equalsIgnoreCase("Schools") || k.equalsIgnoreCase("Ratings") ||
               k.equalsIgnoreCase("Reviews") || k.equalsIgnoreCase("Notifications") ||
               k.equalsIgnoreCase("Admissions") || k.equalsIgnoreCase("Admission") ||
               k.equalsIgnoreCase("Messages") || k.equalsIgnoreCase("Profile") ||
               k.equalsIgnoreCase("Chats") || k.equalsIgnoreCase("Feedback") ||
               k.equalsIgnoreCase("app_settings") || k.equalsIgnoreCase("stats") ||
               k.equalsIgnoreCase("tokens") || k.equalsIgnoreCase("logs") ||
               k.equalsIgnoreCase("requests") || k.equalsIgnoreCase("queries");
    }

    public void removeListener(com.google.firebase.database.ValueEventListener listener) {
        if (listener != null) {
            dbRef.removeEventListener(listener);
        }
    }

    public static void parseSchool(DataSnapshot child, List<School> schools, String dbPath) {
        String key = child.getKey();
        if (key == null) return;

        // Professional Duplicate Prevention
        for (School s : schools) {
            if (s.id != null && s.id.equals(key)) return;
        }

        Log.d(TAG, "Parsing school: " + key + " from " + dbPath);
        try {
            School school = new School();
            school.id = key;
            school.dbPath = dbPath;
            
            boolean isValid = populateSchool(child, school);

            if (isValid) {
                schools.add(school);
                Log.d(TAG, "Added: " + school.name + " [ID: " + key + " Path: " + school.dbPath + "]");
            } else {
                Log.d(TAG, "Skipping invalid school node: " + key);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing node " + child.getKey(), e);
        }
    }

    public static boolean populateSchool(DataSnapshot child, School school) {
        // Basic Info - Using flexible matching
        school.name = getFlexibleString(child, "name", "SchoolName", "schoolName", "Name", "title");
        
        // Validation: If name is missing, check if it has location info. 
        // If it's just a junk node or a different system node, skip it.
        if (school.name.isEmpty() || school.name.equalsIgnoreCase("Unknown") || school.name.equalsIgnoreCase("null")) {
            String city = getFlexibleString(child, "city", "City", "location", "location_city", "town");
            String address = getFlexibleString(child, "address", "Address", "full_address", "location");
            
            if (city.isEmpty() && address.isEmpty()) {
                return false; // Not a school
            }
            school.name = "School: " + child.getKey();
        }

        school.city = getFlexibleString(child, "city", "City", "location", "location_city", "town");
        school.type = getFlexibleString(child, "type", "Type", "school_type", "SchoolType", "Ownership");
        school.gender = getFlexibleString(child, "gender", "Gender", "sex", "Gender_Type");
        school.location_area = getFlexibleString(child, "location_area", "area", "LocationArea");
        school.address = getFlexibleString(child, "address", "Address", "full_address", "location");
        school.district = getFlexibleString(child, "district", "District", "dist", "tehsil", "DistrictName");
        school.contact = getFlexibleString(child, "contact", "phone", "Contact", "PhoneNo", "Mobile");
        school.medium = getFlexibleString(child, "medium", "Medium", "instruction_medium", "MediumOfInstruction", "Language");
        school.merit_percentage = getFlexibleString(child, "merit_percentage", "merit", "Merit", "MeritPercentage", "LastYearMerit");
        school.fee_range = getFlexibleString(child, "fee_range", "fees", "FeeRange", "MonthlyFee", "FeeStructure");
        
        // Specific Fees - Try exact keys first
        school.fee_playgroup = getFlexibleString(child, "fee_playgroup", "fee_nursery", "nursery_fee", "playgroup_fee", "fee_nursery_playgroup", "Nursery_Fee");
        school.fee_primary = getFlexibleString(child, "fee_primary", "primary_fee", "fee_primary_level", "PrimaryFee", "Primary_Fee");
        school.fee_middle = getFlexibleString(child, "fee_middle", "middle_fee", "fee_middle_level", "MiddleFee", "Middle_Fee");
        school.fee_matric = getFlexibleString(child, "fee_matric", "matric_fee", "high_fee", "fee_matric_level", "MatricFee", "High_Fee");
        school.fee_inter = getFlexibleString(child, "fee_inter", "inter_fee", "college_fee", "fee_inter_level", "InterFee", "CollegeFee");

        // Advanced/Dynamic Fee Discovery
        extractFees(child, school);

        // New Fields
        school.imageUrl = getFlexibleString(child, "imageUrl", "image", "photo", "Image", "Photo", "coverUrl", "cover");
        school.logoUrl = getFlexibleString(child, "logoUrl", "logo", "Logo", "logo_url", "school_logo", "logoURL", "LogoUrl");
        school.coverUrl = getFlexibleString(child, "coverUrl", "cover", "Cover", "cover_url", "background", "header_image", "CoverUrl", "image", "Photo");
        
        school.status = getFlexibleString(child, "status", "Status", "approval_status");
        // If status is missing (legacy), we assume it's approved for backward compatibility
        if (school.status.isEmpty()) {
            school.status = "approved";
        }
        school.ownerUid = getFlexibleString(child, "ownerUid", "owner", "uid", "OwnerUid", "OwnerId");
        school.schoolLevel = getFlexibleString(child, "schoolLevel", "level", "GradeLevel", "school_level", "grade_level", "Classes", "Category");

        // Facilities Extraction
        extractRobustFacilities(child, school);

        // Admission related extraction
        school.admissionOpen = Boolean.TRUE.equals(child.child("admissionOpen").getValue(Boolean.class)) ||
                               Boolean.TRUE.equals(child.child("AdmissionOpen").getValue(Boolean.class));
        school.admissionStartDate = getFlexibleString(child, "admissionStartDate", "AdmissionStart");
        school.admissionEndDate = getFlexibleString(child, "admissionEndDate", "AdmissionEnd", "LastDate");
        school.boysSeats = getFlexibleInt(child, "boysSeats", "BoysSeats");
        school.girlsSeats = getFlexibleInt(child, "girlsSeats", "GirlsSeats");
        school.admissionFee = getFlexibleString(child, "admissionFee", "Admission_Fee");
        
        DataSnapshot levelsSnap = child.child("admissionLevels");
        if (!levelsSnap.exists()) levelsSnap = child.child("AdmissionLevels");
        if (levelsSnap.exists()) {
            school.admissionLevels = new ArrayList<>();
            for (DataSnapshot l : levelsSnap.getChildren()) {
                school.admissionLevels.add(School.clean(l.getValue()));
            }
        }

        // Numeric fields with safety - check common variations
        school.rating = getFlexibleDouble(child, "rating", "Rating", "score", "AvgRating");
        school.latitude = getFlexibleDouble(child, "latitude", "Lat", "Latitude");
        school.longitude = getFlexibleDouble(child, "longitude", "Lng", "Longitude");
        school.reviewCount = getFlexibleInt(child, "reviewCount", "ReviewCount", "TotalReviews");

        return true; // Successfully populated
    }

    public static int getFlexibleInt(DataSnapshot snap, String... keys) {
        for (String key : keys) {
            Object val = snap.child(key).getValue();
            if (val != null) {
                try {
                    return Integer.parseInt(String.valueOf(val));
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    public static void extractFees(DataSnapshot snap, School school) {
        for (DataSnapshot sc : snap.getChildren()) {
            String childKey = sc.getKey() != null ? sc.getKey().toLowerCase() : "";
            
            if (childKey.equals("fees") || childKey.equals("fee") || childKey.equals("monthlyfee") || childKey.equals("feestructure")) {
                if (sc.hasChildren()) {
                    extractFees(sc, school); // Recursive for nested fee objects
                } else {
                    String val = School.clean(sc.getValue());
                    if (school.fee_range.isEmpty()) school.fee_range = val;
                }
                continue;
            }

            Object valObj = sc.getValue();
            if (valObj == null || sc.hasChildren()) continue;
            String val = School.clean(valObj);
            if (val.isEmpty() || val.equals("0")) continue;

            if (childKey.contains("nursery") || childKey.contains("playgroup") || childKey.contains("play_group")) {
                if (school.fee_playgroup.isEmpty()) school.fee_playgroup = val;
            } else if (childKey.contains("primary")) {
                if (school.fee_primary.isEmpty()) school.fee_primary = val;
            } else if (childKey.contains("middle")) {
                if (school.fee_middle.isEmpty()) school.fee_middle = val;
            } else if (childKey.contains("matric") || childKey.contains("high") || childKey.contains("9th") || childKey.contains("10th")) {
                if (school.fee_matric.isEmpty()) school.fee_matric = val;
            } else if (childKey.contains("inter") || childKey.contains("college") || childKey.contains("secondary") || childKey.contains("11th") || childKey.contains("12th")) {
                if (school.fee_inter.isEmpty()) school.fee_inter = val;
            }
        }
    }

    public static String getFlexibleString(DataSnapshot snap, String... keys) {
        for (String key : keys) {
            Object val = snap.child(key).getValue();
            if (val != null && !String.valueOf(val).isEmpty() && !String.valueOf(val).equalsIgnoreCase("Unknown") && !String.valueOf(val).equalsIgnoreCase("null")) {
                return School.clean(val);
            }
        }
        return "";
    }

    public static void extractRobustFacilities(DataSnapshot snapshot, School school) {
        List<String> found = extractFacilities(snapshot.child("facilities"));
        if (found.isEmpty()) found = extractFacilities(snapshot.child("Facilities"));
        if (found.isEmpty()) found = extractFacilities(snapshot.child("Services"));
        if (found.isEmpty()) found = extractFacilities(snapshot.child("facility"));
        
        // Root level scanning for common facility boolean/string flags
        if (found.isEmpty()) {
            String[] commonFacilities = {"Library", "Lab", "Computer", "Transport", "Hostel", "Playground", "Security", "Sports", "Gym", "Canteen", "Hall", "Electricity", "Water"};
            for (DataSnapshot child : snapshot.getChildren()) {
                String key = child.getKey() != null ? child.getKey().toLowerCase() : "";
                Object val = child.getValue();
                if (val == null) continue;
                String sVal = String.valueOf(val).toLowerCase();
                
                for (String f : commonFacilities) {
                    if (key.contains(f.toLowerCase())) {
                        if (sVal.equals("yes") || sVal.equals("true") || sVal.equals("1") || sVal.equals("available")) {
                            if (!found.contains(f)) found.add(f);
                        }
                    }
                }
            }
        }
        school.facilities = found;
    }

    public static List<String> extractFacilities(DataSnapshot snap) {
        List<String> list = new ArrayList<>();
        if (!snap.exists()) return list;
        Object val = snap.getValue();
        if (val == null) return list;

        if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (item != null) {
                    String s = School.clean(item);
                    if (!s.isEmpty()) list.add(s);
                }
            }
        } else if (val instanceof java.util.Map) {
            for (Object key : ((java.util.Map<?, ?>) val).keySet()) {
                String s = School.clean(key);
                if (!s.isEmpty()) list.add(s);
            }
        } else if (val instanceof String) {
            String sVal = (String) val;
            if (sVal.startsWith("{") && sVal.endsWith("}")) {
                sVal = sVal.substring(1, sVal.length() - 1);
            }
            String[] parts = sVal.split(",");
            for (String p : parts) {
                String item = p.trim();
                // For facilities in a string-map, we want the KEY (before =), not the value
                if (item.contains("=")) {
                    item = item.split("=")[0].trim();
                }
                String cleaned = School.clean(item);
                if (!cleaned.isEmpty()) list.add(cleaned);
            }
        }
        return list;
    }

    public static double getFlexibleDouble(DataSnapshot snap, String... keys) {
        for (String key : keys) {
            Object val = snap.child(key).getValue();
            if (val != null) {
                try {
                    return Double.parseDouble(String.valueOf(val));
                } catch (Exception ignored) {}
            }
        }
        return 0.0;
    }
}
