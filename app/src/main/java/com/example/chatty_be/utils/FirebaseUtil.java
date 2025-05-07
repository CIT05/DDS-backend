package com.example.chatty_be.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public  static  DocumentReference getChatRoomReference(String chatRoomId){
        return  FirebaseFirestore.getInstance().collection("chatrooms").document(chatRoomId);
    }

    public  static CollectionReference getChatRoomMessageReference(String chatroomId){
        return  getChatRoomReference(chatroomId).collection("chats");
    }

    public  static String getChatRoomId(String userId1,String userId2 ){
        if(userId1.hashCode()<userId2.hashCode()){
            return userId1 +"_" + userId2;
        }else{
            return userId2 +"_" + userId1;
        }
    }
}
