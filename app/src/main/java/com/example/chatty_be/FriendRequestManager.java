package com.example.chatty_be;

import android.content.Context;
import android.util.Log;

import com.example.chatty_be.crypto.EncryptedStorePreference;
import com.example.chatty_be.utils.FirebaseUtil;

public class FriendRequestManager {
    private final Context context;

    public FriendRequestManager(Context context) {
        this.context = context;
    }

        public void onFriendshipAccept() {
            String friendUserId = "tzHamc8cTIc5uE0wwEaJCj2cUDF3";

            FirebaseUtil.getUserByUserId(friendUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String friendPublicKey = documentSnapshot.getString("publicKey");

                            try {
                                EncryptedStorePreference secureStorePreference = new EncryptedStorePreference(context);
                                secureStorePreference.put(friendUserId, friendPublicKey);
                                Log.d("SecurePrefs", "Stored mock public key for " + friendUserId);
                                Log.d("SecurePrefs", "Stored mock public key for " + friendPublicKey);
                            } catch (Exception e) {
                                Log.e("SecurePrefs", "Failed to securely store friend key", e);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Failed to fetch user", e);
                    });

        }
    public String extractFriendPublicKey(String userId) {
        EncryptedStorePreference securePrefs = new EncryptedStorePreference(context);
        String friendPublicKey = securePrefs.get(userId);

        if (friendPublicKey == null || friendPublicKey.isEmpty()) {
            Log.w("SecurePrefs", "No public key found for user: " + userId);
            return "";
        }

        Log.d("SecurePrefs", "Retrieved public key for user: " + userId);
        return friendPublicKey;
    }
    }
