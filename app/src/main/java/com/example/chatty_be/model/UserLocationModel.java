package com.example.chatty_be.model;

import com.google.firebase.Timestamp;

public class UserLocationModel {

    private double latitude;
    private double longitude;
    private String userId;
    private Timestamp expireAt;


    public UserLocationModel () {
    }

    public UserLocationModel(double latitude, double longitude, String userId, Timestamp expireAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
        this.expireAt = expireAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Timestamp expireAt) {
        this.expireAt = expireAt;
    }
}
