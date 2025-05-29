package com.example.chatty_be.model;

public class FriendRequestItem {
    private final String uid;
    private final String displayName;
    private final boolean isFriend;
    private final String status;
    private final boolean incoming;

    public FriendRequestItem(String uid, String displayName, boolean isFriend, String status, boolean incoming) {
        this.uid = uid;
        this.displayName = displayName;
        this.isFriend = isFriend;
        this.status = status;
        this.incoming = incoming;
    }

    public String getUid() { return uid; }
    public String getDisplayName() { return displayName; }
    public boolean isFriend() { return isFriend; }
    public String getStatus() { return status; }
    public boolean isIncoming() { return incoming; }
}
