package com.example.chatty_be.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class ReviewModel {

    private String id;              // reviewId
    private String userUid;         // author
    private String locationName;    // nameField
    private String locationType;    // spinner value
    private String comment;         // commentField
    private long   createdAt;       // System.currentTimeMillis()
    private List<String> imageUrls; // 0–n download URLs
    @Nullable
    private GeoPoint geo;

    /** Empty constructor needed by Firebase */
    public ReviewModel() {}

    public ReviewModel(String id,
                       String userUid,
                       String locationName,
                       String locationType,
                       String comment,
                       long createdAt,
                       List<String> imageUrls,
                       @Nullable GeoPoint geo
                       ) {
        this.id           = id;
        this.userUid      = userUid;
        this.locationName = locationName;
        this.locationType = locationType;
        this.comment      = comment;
        this.createdAt    = createdAt;
        this.imageUrls    = imageUrls;
        this.geo = geo;
    }

    public ReviewModel(String reviewId, String userUid, String locName, String string, String comment, long l, Object o, GeoPoint selectedGeo, Object o1) {
    }

    public String getId()                     { return id; }
    public void   setId(String id)            { this.id = id; }

    public String getUserUid()                { return userUid; }
    public void   setUserUid(String userUid)  { this.userUid = userUid; }

    public String getLocationName()                   { return locationName; }
    public void   setLocationName(String name)        { this.locationName = name; }

    public String getLocationType()                   { return locationType; }
    public void   setLocationType(String type)        { this.locationType = type; }

    public String getComment()                        { return comment; }
    public void   setComment(String comment)          { this.comment = comment; }

    public long   getCreatedAt()                      { return createdAt; }
    public void   setCreatedAt(long createdAt)        { this.createdAt = createdAt; }

    public List<String> getImageUrls()                { return imageUrls; }
    public void         setImageUrls(List<String> u)  { this.imageUrls = u; }

    public void setGeo(@Nullable GeoPoint geo) { this.geo = geo; }
    public @Nullable GeoPoint getGeo()        { return geo;      }
}
