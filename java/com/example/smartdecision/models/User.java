package com.example.smartdecision.models;

public class User {
    public String uid;
    public String name;
    public String email;
    public String password;
    public String role; // "User", "Owner", "Admin"

    public User() {
    }

    public User(String uid, String name, String email, String password, String role) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }
}
