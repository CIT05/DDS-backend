package com.example.chatty_be.model;

import com.google.firebase.Timestamp;

public class UserLocationModel {
    private String userId;
    private Timestamp expireAt;

    private String geoHash;

    public UserLocationModel () {
    }

    public UserLocationModel(String userId, Timestamp expireAt, String geoHash) {
        this.userId = userId;
        this.expireAt = expireAt;
        this.geoHash = geoHash;
    }

    public String getGeoHash()  { return geoHash; }
    public void setGeoHash(String geoHash)       { this.geoHash = geoHash; }

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
