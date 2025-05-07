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

}
