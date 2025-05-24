package com.example.chatty_be.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {


    public static String getCurrentUserId(){
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn(){
        return getCurrentUserId() != null;
    }
    public static DocumentReference currentUserDetails(){
        String currentUserId = getCurrentUserId();
        return FirebaseFirestore.getInstance().collection("users").document(currentUserId);
    }

    public static CollectionReference allUsersCollectionReference(){
        return  FirebaseFirestore.getInstance().collection("users");
    }

    public static CollectionReference allUserLocationReference(){
        return FirebaseFirestore.getInstance().collection("user_locations");
    }

    public static DocumentReference getUserLocationReference(String userId){
        return FirebaseFirestore.getInstance().collection("user_locations").document(userId);
    }

    public  static  DocumentReference getChatRoomReference(String chatRoomId){
        return  FirebaseFirestore.getInstance().collection("chatrooms").document(chatRoomId);
    }

    public  static CollectionReference getChatRoomMessageReference(String chatroomId){
        return  getChatRoomReference(chatroomId).collection("chats");
    }

    public static String getChatRoomId(String userId1,String userId2 ){
        if(userId1.hashCode()<userId2.hashCode()){
            return userId1 +"_" + userId2;
        }else{
            return userId2 +"_" + userId1;
        }
    }

    public static CollectionReference allChatroomCollectionReference(){
        return  FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static DocumentReference getUserByUserId(String userId){
        return FirebaseFirestore.getInstance().collection("users").document(userId);
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(FirebaseUtil.getCurrentUserId())){
            return allUsersCollectionReference().document(userIds.get(1));
        }else{
            return allUsersCollectionReference().document(userIds.get(0));
        }
    }

    public static String timestampToString(Timestamp timestamp){
        return  new SimpleDateFormat("HH:MM").format(timestamp.toDate());
    }
}
