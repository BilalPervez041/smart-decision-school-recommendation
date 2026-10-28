package com.example.smartdecision.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.ReviewAdapter;
import com.example.smartdecision.models.Review;
import com.example.smartdecision.models.School;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<Review> reviewList;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmit;
    private android.view.View layoutSubmitReview;
    private String schoolId;
    private String dbPath;
    private String currentUserId;
    private String userRole = "User";
    private String currentUserName = "Anonymous";
    private DatabaseReference reviewsRef;
    private ValueEventListener reviewsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        schoolId = getIntent().getStringExtra("schoolId");
        dbPath = getIntent().getStringExtra("dbPath");
        
        if (schoolId == null) {
            finish();
            return;
        }

        rvReviews = findViewById(R.id.rvReviews);
        layoutSubmitReview = findViewById(R.id.cardAddReview); // Updated to match XML ID
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etReviewComment);
        btnSubmit = findViewById(R.id.btnSubmitReview);

        currentUserId = FirebaseAuth.getInstance().getUid();

        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);

        // Initialize path finding
        initializeReviewsRef();

        fetchUserName();
        checkUserRole();

        btnSubmit.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(this, "Please login to write a review", Toast.LENGTH_SHORT).show();
                return;
            }
            if (reviewsRef == null) {
                Toast.makeText(this, "Initializing system...", Toast.LENGTH_SHORT).show();
                return;
            }
            float rating = ratingBar.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            if (comment.isEmpty()) {
                Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
                return;
            }

            submitReview(rating, comment);
        });
    }

    private void initializeReviewsRef() {
        String rootPath = (dbPath != null && !dbPath.isEmpty()) ? dbPath : "Schools/" + schoolId;
        
        // Comprehensive list of potential paths for reviews
        String[] potentialPaths = {
            rootPath + "/reviews",
            rootPath + "/Reviews",
            "Reviews/" + schoolId,
            "reviews/" + schoolId,
            "Schools/" + schoolId + "/reviews",
            "Schools/" + schoolId + "/Reviews"
        };

        final boolean[] found = {false};
        
        // Default fallback to ensure we have a reference for new submissions
        reviewsRef = FirebaseDatabase.getInstance().getReference(rootPath).child("reviews");

        for (String path : potentialPaths) {
            DatabaseReference tempRef = FirebaseDatabase.getInstance().getReference(path);
            tempRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!found[0] && snapshot.exists() && snapshot.hasChildren()) {
                        found[0] = true;
                        reviewsRef = tempRef;
                        loadReviews(); // Re-load from the correct path
                        loadUserReview();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // Trigger initial load with default path
        loadReviews();
        loadUserReview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reviewsRef != null) {
            loadReviews();
            loadUserReview();
        }
    }

    private void loadUserReview() {
        if (currentUserId == null || reviewsRef == null) return;
        reviewsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Review existing = snapshot.getValue(Review.class);
                if (existing != null) {
                    ratingBar.setRating(existing.rating);
                    etComment.setText(existing.comment);
                    btnSubmit.setText("Update Review");
                } else {
                    btnSubmit.setText("Submit Review");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (reviewsRef != null && reviewsListener != null) {
            reviewsRef.removeEventListener(reviewsListener);
        }
    }

    private void checkUserRole() {
        // Set initial config for regular user
        adapter.setConfig(false, currentUserId, this::showDeleteDialog);

        if (currentUserId == null) return;
        
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userRole = snapshot.child("role").getValue(String.class);
                        if (userRole == null) {
                            userRole = snapshot.child("profile").child("role").getValue(String.class);
                        }
                        if (userRole == null) userRole = "User";

                        boolean isAdmin = "Admin".equalsIgnoreCase(userRole) || 
                                         (FirebaseAuth.getInstance().getCurrentUser() != null && 
                                          "admin@gmail.com".equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()));
                        
                        adapter.setConfig(isAdmin, currentUserId, ReviewsActivity.this::showDeleteDialog);

                        // If user is Owner, hide the review submission UI
                        if (userRole.toLowerCase().contains("owner")) {
                            if (layoutSubmitReview != null) {
                                layoutSubmitReview.setVisibility(android.view.View.GONE);
                            } else {
                                // Fallback: hide individual components if no layout container
                                if (ratingBar != null) ratingBar.setVisibility(android.view.View.GONE);
                                if (etComment != null) etComment.setVisibility(android.view.View.GONE);
                                if (btnSubmit != null) btnSubmit.setVisibility(android.view.View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showDeleteDialog(Review review) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReview(review))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReview(Review review) {
        // 1. Remove the review
        reviewsRef.child(review.id).removeValue().addOnSuccessListener(aVoid -> {
            // 2. Update School Rating (Transaction)
            String rootPath = (dbPath != null && !dbPath.isEmpty()) ? dbPath : "Schools/" + schoolId;
            DatabaseReference schoolRef = FirebaseDatabase.getInstance().getReference(rootPath);
            schoolRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {
                    if (mutableData.getValue() == null) return com.google.firebase.database.Transaction.success(mutableData);

                    Integer countObj = mutableData.child("reviewCount").getValue(Integer.class);
                    Double ratingObj = mutableData.child("rating").getValue(Double.class);
                    
                    int oldCount = countObj != null ? countObj : 0;
                    double oldRating = ratingObj != null ? ratingObj : 0.0;
                    
                    if (oldCount <= 1) {
                        mutableData.child("reviewCount").setValue(0);
                        mutableData.child("rating").setValue(0.0);
                    } else {
                        // Reverse the average calculation: (TotalSum - reviewRating) / (oldCount - 1)
                        double totalSum = oldRating * oldCount;
                        double newRating = (totalSum - review.rating) / (oldCount - 1);
                        mutableData.child("reviewCount").setValue(oldCount - 1);
                        mutableData.child("rating").setValue(newRating);
                    }

                    return com.google.firebase.database.Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (committed) {
                        Toast.makeText(ReviewsActivity.this, "Review deleted and rating updated", Toast.LENGTH_SHORT).show();
                        if (review.id.equals(currentUserId)) {
                            loadUserReview(); // Reset the UI form
                        }
                    }
                }
            });
        });
    }

    private void fetchUserName() {
        if (currentUserId != null) {
            // Updated to use the new professional profile path
            FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("profile").child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                currentUserName = snapshot.getValue(String.class);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void loadReviews() {
        if (reviewsRef == null) return;
        if (reviewsListener != null) {
            reviewsRef.removeEventListener(reviewsListener);
        }
        reviewsListener = reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                double sumOfRatings = 0;
                int count = 0;
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Review review = parseReviewManually(ds);
                    if (review != null) {
                        reviewList.add(0, review);
                        sumOfRatings += review.rating;
                        count++;
                    }
                }
                adapter.notifyDataSetChanged();
                
                // Professional Rating Formula: (Sum of all ratings) / (Total count)
                if (count > 0) {
                    double professionalAvg = sumOfRatings / (double) count;
                    updateSchoolRatingInDB(professionalAvg, count);
                } else if (snapshot.exists()) {
                    updateSchoolRatingInDB(0.0, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private Review parseReviewManually(DataSnapshot ds) {
        try {
            // First try default Firebase parsing
            Review r = ds.getValue(Review.class);
            
            // If essential fields are missing, try manual flexible parsing
            if (r == null || r.rating == 0) {
                r = new Review();
                r.id = ds.getKey();
                r.userId = getFlexibleString(ds, "userId", "user_id", "uid");
                r.userName = getFlexibleString(ds, "userName", "user_name", "name", "reviewerName");
                r.comment = getFlexibleString(ds, "comment", "text", "review", "message");
                r.schoolId = getFlexibleString(ds, "schoolId", "school_id", "id");
                r.rating = (float) getFlexibleDouble(ds, "rating", "score", "stars", "ratingValue");
                r.timestamp = getFlexibleLong(ds, "timestamp", "time", "date");
            }
            
            if (r.rating > 0) return r;
        } catch (Exception e) {
            android.util.Log.e("ReviewsActivity", "Error parsing review: " + ds.getKey(), e);
        }
        return null;
    }

    private String getFlexibleString(DataSnapshot ds, String... keys) {
        for (String k : keys) {
            Object val = ds.child(k).getValue();
            if (val != null && !String.valueOf(val).isEmpty()) return String.valueOf(val);
        }
        return "";
    }

    private double getFlexibleDouble(DataSnapshot ds, String... keys) {
        for (String k : keys) {
            Object val = ds.child(k).getValue();
            if (val != null) {
                try { return Double.parseDouble(String.valueOf(val)); } catch (Exception ignored) {}
            }
        }
        return 0.0;
    }

    private long getFlexibleLong(DataSnapshot ds, String... keys) {
        for (String k : keys) {
            Object val = ds.child(k).getValue();
            if (val != null) {
                try { return Long.parseLong(String.valueOf(val)); } catch (Exception ignored) {}
            }
        }
        return System.currentTimeMillis();
    }

    private void updateSchoolRatingInDB(double avgRating, int count) {
        String rootPath = (dbPath != null && !dbPath.isEmpty()) ? dbPath : "Schools/" + schoolId;
        DatabaseReference schoolRef = FirebaseDatabase.getInstance().getReference(rootPath);
        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", avgRating);
        updates.put("reviewCount", count);
        schoolRef.updateChildren(updates);
    }

    private void submitReview(float rating, String comment) {
        if (currentUserId == null) return;

        // Use currentUserId as review ID to ensure one review per user per school
        String id = currentUserId;

        reviewsRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Review oldReview = snapshot.getValue(Review.class);
                boolean isUpdate = oldReview != null;
                float oldRatingValue = isUpdate ? oldReview.rating : 0;

                Review newReview = new Review(id, currentUserId, currentUserName, schoolId, comment, rating, System.currentTimeMillis());

                String rootPath = (dbPath != null && !dbPath.isEmpty()) ? dbPath : "Schools/" + schoolId;
                DatabaseReference schoolRef = FirebaseDatabase.getInstance().getReference(rootPath);
                schoolRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                    @NonNull
                    @Override
                    public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData mutableData) {
                        if (mutableData.getValue() == null) return com.google.firebase.database.Transaction.success(mutableData);

                        Integer countObj = mutableData.child("reviewCount").getValue(Integer.class);
                        Double ratingObj = mutableData.child("rating").getValue(Double.class);
                        
                        int count = countObj != null ? countObj : 0;
                        double currentRating = ratingObj != null ? ratingObj : 0.0;
                        double totalSum = currentRating * count;

                        if (isUpdate) {
                            // Update existing: subtract old, add new, count stays same
                            totalSum = totalSum - oldRatingValue + rating;
                            double newRating = count > 0 ? totalSum / count : rating;
                            mutableData.child("rating").setValue(newRating);
                        } else {
                            // New review: add new, increment count
                            totalSum += rating;
                            int newCount = count + 1;
                            double newRating = totalSum / newCount;
                            mutableData.child("reviewCount").setValue(newCount);
                            mutableData.child("rating").setValue(newRating);
                        }

                        return com.google.firebase.database.Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                        if (committed) {
                            reviewsRef.child(id).setValue(newReview);
                            Toast.makeText(ReviewsActivity.this, isUpdate ? "Review updated!" : "Review submitted!", Toast.LENGTH_SHORT).show();
                            loadUserReview(); // Refresh button state
                        } else {
                            Toast.makeText(ReviewsActivity.this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
