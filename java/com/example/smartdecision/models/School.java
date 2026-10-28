package com.example.smartdecision.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.List;

@IgnoreExtraProperties
public class School implements Serializable {
    public String id;
    public String dbPath; // Stores the Firebase path (e.g., "Schools/id" or just "id")
    public String name;
    public String city;
    public String district;
    public String type;
    public String gender;
    public String fee_range;
    public String medium;
    public String merit_percentage;
    public String location_area;
    public String address;
    public String contact;
    public double rating;
    public int reviewCount;
    public List<String> facilities;

    // Class-wise fees
    public String fee_playgroup;
    public String fee_primary;
    public String fee_middle;
    public String fee_matric;
    public String fee_inter;
    
    // Default coordinates for Abbottabad if not in DB
    public double latitude = 34.1468;
    public double longitude = 73.2108;

    public String imageUrl; // Legacy, will keep for compatibility if needed
    public String logoUrl;
    public String coverUrl;
    public String status = "pending"; // pending, approved, rejected
    public String ownerUid;
    public String schoolLevel; // e.g., "Nursery to Inter"

    // Admission related fields
    public boolean admissionOpen = false;
    public String admissionStartDate;
    public String admissionEndDate;
    public int boysSeats;
    public int girlsSeats;
    public String admissionFee;
    public List<String> admissionLevels;

    public float distance = -1;
    public double score;

    public static String clean(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val).trim();
        if (s.equalsIgnoreCase("null") || s.equalsIgnoreCase("Unknown")) return "";
        
        // Robust cleaning for Map-style strings like {nursery=3500} or {FSC)=N/A}
        while (s.startsWith("{")) s = s.substring(1).trim();
        while (s.endsWith("}")) s = s.substring(0, s.length() - 1).trim();
        
        if (s.contains("=")) {
            s = s.substring(s.lastIndexOf("=") + 1).trim();
        }
        
        if (s.equalsIgnoreCase("null") || s.equalsIgnoreCase("Unknown")) return "";
        return s;
    }

    public void cleanAllFields() {
        this.name = clean(this.name);
        this.city = clean(this.city);
        this.district = clean(this.district);
        this.address = clean(this.address);
        this.contact = clean(this.contact);
        this.fee_playgroup = clean(this.fee_playgroup);
        this.fee_primary = clean(this.fee_primary);
        this.fee_middle = clean(this.fee_middle);
        this.fee_matric = clean(this.fee_matric);
        this.fee_inter = clean(this.fee_inter);
        this.fee_range = clean(this.fee_range);
        this.merit_percentage = clean(this.merit_percentage);
        this.type = clean(this.type);
        this.gender = clean(this.gender);
        this.medium = clean(this.medium);
        this.location_area = clean(this.location_area);
        this.status = clean(this.status);
        this.schoolLevel = clean(this.schoolLevel);
    }

    public School() {
        // Required for Firebase
    }

    // Getters for Adapter/Engine compatibility
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name != null ? name : "Unknown School"; }
    public String getCity() { return city; }
    public String getType() { return type != null ? type : "Private"; }
    public String getGender() { return gender != null ? gender : "Co-education"; }
    public String getFeeRange() { return fee_range != null ? fee_range : "0"; }
    public String getMeritPercentage() { 
        if (merit_percentage == null || merit_percentage.isEmpty() || merit_percentage.equals("0")) return "N/A";
        if (merit_percentage.contains("%")) return merit_percentage;
        return merit_percentage + "%"; 
    }
    public String getLocationArea() { return location_area != null ? location_area : "Abbottabad"; }
    public String getAddress() { return address != null ? address : "No address available"; }
    public String getContact() { return contact; }
    public double getRating() { return rating; }
    public List<String> getFacilities() { return facilities; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getDistance() { return distance; }
    public void setDistance(float d) { this.distance = d; }
    public double getScore() { return score; }
    public void setScore(double s) { this.score = s; }

    public int getNumericMerit() {
        if (merit_percentage == null || merit_percentage.isEmpty()) return 0;
        try {
            String clean = merit_percentage.split("-")[0].replaceAll("[^0-9]", "");
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getNumericFee() {
        // First try to find minimum fee among class-wise fees
        int minClassFee = Integer.MAX_VALUE;
        boolean foundClassFee = false;
        String[] classFees = {fee_playgroup, fee_primary, fee_middle, fee_matric, fee_inter};
        for (String f : classFees) {
            if (f != null && !f.equalsIgnoreCase("Unknown") && !f.equalsIgnoreCase("N/A")) {
                try {
                    int val = Integer.parseInt(f.replaceAll("[^0-9]", ""));
                    if (val > 0) {
                        if (val < minClassFee) minClassFee = val;
                        foundClassFee = true;
                    }
                } catch (Exception ignored) {}
            }
        }
        if (foundClassFee) return minClassFee;

        // Fallback to fee_range
        if (fee_range == null || fee_range.isEmpty() || fee_range.equalsIgnoreCase("Unknown")) return 0;
        try {
            String clean = fee_range.split("-")[0].replaceAll("[^0-9]", "");
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean hasAnyClassFees() {
        String[] fees = {fee_playgroup, fee_primary, fee_middle, fee_matric, fee_inter};
        for (String f : fees) {
            if (f != null && !f.equalsIgnoreCase("Unknown") && !f.equalsIgnoreCase("N/A") && !f.equals("0")) return true;
        }
        return false;
    }
    
    // Legacy support for older code
    public int getFee() { return getNumericFee(); }
    public String getMedium() { return medium != null ? medium : "English"; }

    private String formatFeeString(String fee) {
        if (fee == null || fee.isEmpty() || fee.equalsIgnoreCase("Unknown")) return "";
        // Remove everything except numbers
        String numeric = fee.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return fee;
        
        try {
            // Optional: Format with commas if needed, e.g. 5,000
            long val = Long.parseLong(numeric);
            return String.format("%,d", val);
        } catch (Exception e) {
            return numeric;
        }
    }

    public String getSpecificFee(String grade) {
        String lowerGrade = grade.toLowerCase();
        if (lowerGrade.contains("playgroup") || lowerGrade.contains("nursery")) return fee_playgroup;
        if (lowerGrade.contains("primary")) return fee_primary;
        if (lowerGrade.contains("middle")) return fee_middle;
        if (lowerGrade.contains("matric")) return fee_matric;
        if (lowerGrade.contains("inter")) return fee_inter;
        return null;
    }

    public int getNumericFeeForGrade(String grade) {
        String feeStr = getSpecificFee(grade);
        if (feeStr == null || feeStr.equalsIgnoreCase("Unknown") || feeStr.equalsIgnoreCase("N/A")) return 0;
        try {
            // Handle ranges like "5000-8000" by taking the lower bound
            String firstPart = feeStr.split("[-/]")[0].trim();
            return Integer.parseInt(firstPart.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public String getDynamicFeeDisplay() {
        return getDynamicFeeDisplay(null);
    }

    public String getDynamicFeeDisplay(String selectedGrade) {
        String[] labels = {"Nursery", "Primary", "Middle", "Matric", "Inter"};
        String[] values = {fee_playgroup, fee_primary, fee_middle, fee_matric, fee_inter};

        int firstIdx = -1;
        int lastIdx = -1;
        int minFee = Integer.MAX_VALUE;
        int maxFee = -1;

        for (int i = 0; i < values.length; i++) {
            String val = values[i];
            if (val != null && !val.equalsIgnoreCase("Unknown") && !val.equalsIgnoreCase("N/A") && !val.equals("0")) {
                try {
                    // Split in case the value itself is a range like "5000-8000"
                    String[] parts = val.split("[-/]");
                    for (String part : parts) {
                        String clean = part.replaceAll("[^0-9]", "");
                        if (!clean.isEmpty()) {
                            int feeVal = Integer.parseInt(clean);
                            if (feeVal > 0) {
                                if (firstIdx == -1) firstIdx = i;
                                lastIdx = i;
                                if (feeVal < minFee) minFee = feeVal;
                                if (feeVal > maxFee) maxFee = feeVal;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (firstIdx != -1) {
            String rangeLabel = "(" + labels[firstIdx] + " to " + labels[lastIdx] + ")";
            if (firstIdx == lastIdx) rangeLabel = "(" + labels[firstIdx] + ")";
            
            // If user selected a specific grade, show it as primary info
            if (selectedGrade != null && !selectedGrade.equalsIgnoreCase("Any") && !selectedGrade.isEmpty()) {
                String specFee = getSpecificFee(selectedGrade);
                if (specFee != null && !specFee.equalsIgnoreCase("Unknown") && !specFee.equalsIgnoreCase("N/A") && !specFee.equals("0")) {
                    // Check if specFee is a range like "4500-7000"
                    if (specFee.contains("-") || specFee.contains("/")) {
                        String[] pts = specFee.split("[-/]");
                        if (pts.length >= 2) {
                            return formatFeeString(pts[0]) + " to " + formatFeeString(pts[1]) + " (" + selectedGrade + ")";
                        }
                    }
                    return formatFeeString(specFee) + " (" + selectedGrade + ")";
                } else if (specFee != null && specFee.equalsIgnoreCase("N/A")) {
                    return "N/A";
                }
            }

            if (minFee == maxFee) {
                return formatFeeString(String.valueOf(minFee)) + " " + rangeLabel;
            } else {
                return formatFeeString(String.valueOf(minFee)) + " to " + formatFeeString(String.valueOf(maxFee)) + " " + rangeLabel;
            }
        }

        if (fee_range != null && !fee_range.equalsIgnoreCase("Unknown") && !fee_range.equals("0")) {
            return fee_range;
        }

        return "N/A";
    }

    public String inferSchoolLevel() {
        String[] labels = {"Nursery", "Primary", "Middle", "Matric", "Inter"};
        String[] values = {fee_playgroup, fee_primary, fee_middle, fee_matric, fee_inter};

        int start = -1;
        int end = -1;

        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && !v.isEmpty() && !v.equalsIgnoreCase("N/A") && !v.equalsIgnoreCase("0")) {
                if (start == -1) start = i;
                end = i;
            }
        }

        if (start != -1) {
            if (start == end) return labels[start] + " Only";
            return labels[start] + " to " + labels[end];
        }
        return "Not Specified";
    }
}
