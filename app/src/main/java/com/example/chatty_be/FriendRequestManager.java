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

        public void onFriendshipAccept(String friendUserId) {

            FirebaseUtil.getUserByUserId(friendUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String friendPublicKey = documentSnapshot.getString("publicKey");

                            try {
                                EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);
                                encryptedStorePreference.put(friendUserId, friendPublicKey);
                            } catch (Exception e) {
                                Log.e("encryptedStorePreference", "Failed to securely store friend key", e);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Failed to fetch user", e);
                    });

        }
    public String extractFriendPublicKey(String userId) {
        EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);
        String friendPublicKey = encryptedStorePreference.get(userId);

        if (friendPublicKey == null || friendPublicKey.isEmpty()) {
            Log.w("EncryptedStorePreference", "No public key found for user: " + userId);
            return "";
        }

        return friendPublicKey;
    }
    }
