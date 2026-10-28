package com.example.smartdecision.models;

import java.io.Serializable;

public class Notification implements Serializable {
    public String id;
    public String title;
    public String message;
    public String status;
    public String type; // "user" or "owner"
    public String applicationId;
    public long timestamp;

    public Notification() {
    }

    public Notification(String id, String title, String message, String status, String type, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.status = status;
        this.type = type;
        this.timestamp = timestamp;
    }
}
