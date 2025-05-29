package com.example.chatty_be.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {

    public static String getCurrentUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }
    public static DocumentReference currentUserDetails() {
        String currentUserId = getCurrentUserId();
        return FirebaseFirestore.getInstance().collection("users").document(currentUserId);
    }

    public static CollectionReference allUsersCollectionReference() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static CollectionReference allUserLocationReference(){
        return FirebaseFirestore.getInstance().collection("user_locations");
    }

    public static DocumentReference getUserLocationReference(String userId){
        return FirebaseFirestore.getInstance().collection("user_locations").document(userId);
    }


    public static DocumentReference getChatRoomReference(String chatRoomId) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatRoomId);
    }

    public static CollectionReference getChatRoomMessageReference(String chatroomId) {
        return getChatRoomReference(chatroomId).collection("chats");
    }

    public static String getChatRoomId(String userId1, String userId2) {
        // Ensure consistent chatRoomId regardless of order
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static DocumentReference getUserByUserId(String userId) {
        return FirebaseFirestore.getInstance().collection("users").document(userId);
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        if (userIds.get(0).equals(FirebaseUtil.getCurrentUserId())) {
            return allUsersCollectionReference().document(userIds.get(1));
        } else {
            return allUsersCollectionReference().document(userIds.get(0));
        }
    }

    public static String timestampToString(Timestamp timestamp) {
        // Corrected minutes format from "MM" to "mm"
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }


    public static CollectionReference getFriendRequestsRef(String userId) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("friendRequests");
    }

    public static DocumentReference getFriendRequestDocumentRef(String targetUserId, String sourceUserId) {
        return getFriendRequestsRef(targetUserId).document(sourceUserId);
    }

    public static CollectionReference getFriendsRef(String userId) {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("friends");
    }

    public static DocumentReference getFriendDocumentRef(String userId, String friendId) {
        return getFriendsRef(userId).document(friendId);
    }

    public static DocumentReference getChatRoomBetween(String user1Id, String user2Id) {
        String chatRoomId = user1Id.compareTo(user2Id) < 0
                ? user1Id + "_" + user2Id
                : user2Id + "_" + user1Id;

        return FirebaseFirestore.getInstance()
                .collection("chatrooms")
                .document(chatRoomId);
    }

    public static StorageReference getCurrentReviewPicStorageRef(){
        return FirebaseStorage.getInstance().getReference().child("review_pic")
                .child(FirebaseUtil.getCurrentUserId());
    }
}
