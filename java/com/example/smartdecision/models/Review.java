package com.example.smartdecision.models;

import java.io.Serializable;

public class Review implements Serializable {
    public String id;
    public String userId;
    public String userName;
    public String schoolId;
    public String comment;
    public float rating;
    public long timestamp;

    public Review() {
    }

    public Review(String id, String userId, String userName, String schoolId, String comment, float rating, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.schoolId = schoolId;
        this.comment = comment;
        this.rating = rating;
        this.timestamp = timestamp;
    }
}
