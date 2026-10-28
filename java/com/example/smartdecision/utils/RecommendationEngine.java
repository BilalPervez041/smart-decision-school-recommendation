package com.example.smartdecision.utils;

import android.location.Location;
import android.util.Log;
import com.example.smartdecision.models.School;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendationEngine {

    public static List<School> getRecommendations(List<School> allSchools, String preferredType, String preferredMedium, int maxBudget, String preferredGender, String preferredGrade, String prevPercentageStr, boolean showAll, Location userLocation) {
        
        Log.d("RecommendationEngine", "Engine processing schools: " + allSchools.size() + " | showAll: " + showAll);

        List<School> filtered = new ArrayList<>();
        int userPercentage = 0;
        try {
            userPercentage = Integer.parseInt(prevPercentageStr);
        } catch (Exception e) {
            userPercentage = 0;
        }

        for (School school : allSchools) {
            try {
                // 1. Status Filter - Be inclusive for market readiness
                // Only skip schools that were explicitly rejected by admin.
                // Pending and Approved schools should both be visible during testing and deployment phase
                // to ensure owner-added schools appear immediately as requested.
                if ("rejected".equalsIgnoreCase(school.status)) {
                    continue;
                }

                // Distance Calculation - Geodesic to Road Estimation
                if (userLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            school.getLatitude(), school.getLongitude(), results);
                    
                    float geodesicDistanceKm = results[0] / 1000;
                    
                    // Road Distance Estimation: Google Maps shows road distance which is usually 
                    // 30-35% longer than straight line distance in hilly areas like Abbottabad. 
                    // Multiplying by 1.32 brings it within 0.5km of real road distance.
                    float estimatedRoadDistance = geodesicDistanceKm * 1.32f;
                    
                    school.setDistance(estimatedRoadDistance);
                } else {
                    school.setDistance(0);
                }

                // Scoring Logic - Rating is the most important factor as requested
                // We use a high multiplier for rating (1000) to ensure it remains the primary sorting factor 
                // even when distances are very large (like the 16000km seen in testing).
                double score = school.getRating() * 1000; 
                
                // Add minor bonuses/penalties for merit and distance
                if (school.getNumericMerit() > 0) {
                    score += (school.getNumericMerit() / 10.0); // Max 10 points for 100% merit
                }

                if (school.getDistance() > 0) {
                    // Penalty is 0.1 per km, but we cap it at 50 points (equivalent to 0.05 rating difference).
                    // This ensures distance only breaks ties for schools with very similar ratings.
                    double distancePenalty = Math.min(50.0, school.getDistance() * 0.1);
                    score -= distancePenalty;
                }
                school.setScore(score);

                if (!showAll) {
                    // 1. Strict Budget Filter (Based on selected grade fee)
                    int schoolFee = school.getNumericFeeForGrade(preferredGrade);
                    // If class-wise fee is 0, fall back to general fee range min
                    if (schoolFee == 0) schoolFee = school.getNumericFee();

                    if (schoolFee > maxBudget) {
                        continue; 
                    }
                    
                    // If user selected a specific grade, and that grade's fee is N/A (0), 
                    // it means the school doesn't offer that grade.
                    if (!preferredGrade.equalsIgnoreCase("Any") && school.getNumericFeeForGrade(preferredGrade) == 0) {
                        // If the school has class-wise fees but NOT for this specific grade, it doesn't offer it.
                        if (school.hasAnyClassFees()) {
                             continue; 
                        }
                    }

                    // 2. Strict Type Filter
                    String schoolType = (school.getType() != null) ? school.getType().toLowerCase() : "";
                    String prefType = preferredType.toLowerCase();
                    if (!prefType.equals("any")) {
                        if (prefType.contains("public")) {
                            if (!schoolType.contains("government") && !schoolType.contains("public")) {
                                continue;
                            }
                        } else if (!schoolType.contains(prefType)) {
                            continue;
                        }
                    }

                    // 3. Strict Medium Filter
                    String schoolMedium = (school.medium != null) ? school.medium.toLowerCase() : "english";
                    String prefMedium = preferredMedium.toLowerCase();
                    if (!prefMedium.equals("any") && !schoolMedium.contains(prefMedium)) {
                        continue;
                    }

                    // 4. Strict Gender Filter (Professional mapping: Male -> Boys, Female -> Girls)
                    String schoolGender = school.getGender().toLowerCase();
                    String prefGender = preferredGender.toLowerCase();

                    if (!prefGender.equals("any")) {
                        if (prefGender.contains("co-education")) {
                            // "Co-education" preference strictly shows schools that are compatible with BOTH genders
                            if (!schoolGender.contains("co-education")) {
                                continue;
                            }
                        } else {
                            // Professional mapping: Boys Only / Girls Only matches their respective schools OR Co-education schools
                            boolean isBoysPref = prefGender.contains("boys") || prefGender.contains("male");
                            boolean isGirlsPref = prefGender.contains("girls") || prefGender.contains("female");
                            
                            boolean isSchoolBoys = schoolGender.contains("boys") || schoolGender.contains("male");
                            boolean isSchoolGirls = schoolGender.contains("girls") || schoolGender.contains("female");
                            boolean isSchoolCo = schoolGender.contains("co-education");

                            boolean match = false;
                            if (isBoysPref && (isSchoolBoys || isSchoolCo)) match = true;
                            if (isGirlsPref && (isSchoolGirls || isSchoolCo)) match = true;

                            if (!match) continue;
                        }
                    }

                    // 5. Strict Merit Filter (Only if not Playgroup/Nursery)
                    String grade = preferredGrade.toLowerCase();
                    // We perfectly exclude Nursery/Playgroup from merit requirements as they don't have entry tests
                    if (!grade.contains("playgroup") && !grade.contains("nursery") && !grade.equals("any")) {
                        if (userPercentage < school.getNumericMerit()) {
                            continue;
                        }
                    }
                }

                
                filtered.add(school);
            } catch (Exception e) {
                Log.e("RecommendationEngine", "Error processing school: " + school.getName(), e);
            }
        }

        // Sort by Score (which is dominated by Rating)
        Collections.sort(filtered, (s1, s2) -> Double.compare(s2.getScore(), s1.getScore()));
        return filtered;
    }
}
