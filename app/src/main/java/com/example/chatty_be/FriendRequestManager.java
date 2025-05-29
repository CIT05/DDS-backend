package com.example.chatty_be;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.chatty_be.crypto.EncryptedStorePreference;
import com.example.chatty_be.model.ChatRoomModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.google.firebase.Timestamp;

import java.util.Arrays;


public class FriendRequestManager {

    private final Context context;
    private final String currentUserId;

    public FriendRequestManager(Context context) {
        this.context = context;
        this.currentUserId = FirebaseUtil.getCurrentUserId();
    }

    public void sendFriendRequest(String targetUserId, FriendRequestCallback callback) {
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        FirebaseUtil.getFriendRequestDocumentRef(currentUserId, targetUserId)
                .get()
                .addOnSuccessListener(outgoingReqDoc -> {
                    FirebaseUtil.getFriendRequestDocumentRef(targetUserId, currentUserId)
                            .get()
                            .addOnSuccessListener(incomingReqDoc -> {
                                boolean alreadyRequested = (outgoingReqDoc.exists() && "pending".equals(outgoingReqDoc.getString("status"))) ||
                                        (incomingReqDoc.exists() && "pending".equals(incomingReqDoc.getString("status")));

                                if (alreadyRequested) {
                                    Toast.makeText(context, "Friend request already pending with " + targetUserId, Toast.LENGTH_SHORT).show();
                                    callback.onResult(false);
                                    return;
                                }

                                // Check if they are already friends
                                FirebaseUtil.getFriendDocumentRef(currentUserId, targetUserId)
                                        .get()
                                        .addOnSuccessListener(friendDoc -> {
                                            if (friendDoc.exists()) {
                                                Toast.makeText(context, "You are already friends with " + targetUserId, Toast.LENGTH_SHORT).show();
                                                callback.onResult(false);
                                                return;
                                            }

                                            // No pending request or existing friendship, send new one
                                            // Create incoming request for target user
                                            FirebaseUtil.getFriendRequestDocumentRef(targetUserId, currentUserId)
                                                    .set(new FriendRequestData("pending", Timestamp.now(), "incoming"))
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Create outgoing request for current user
                                                        FirebaseUtil.getFriendRequestDocumentRef(currentUserId, targetUserId)
                                                                .set(new FriendRequestData("pending", Timestamp.now(), "outgoing"))
                                                                .addOnSuccessListener(aVoid1 -> {
                                                                    Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                                                    callback.onResult(true);
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("FriendRequestManager", "Failed to set outgoing request: " + e.getMessage());
                                                                    Toast.makeText(context, "Failed to send request.", Toast.LENGTH_SHORT).show();
                                                                    callback.onResult(false);
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("FriendRequestManager", "Failed to set incoming request: " + e.getMessage());
                                                        Toast.makeText(context, "Failed to send request.", Toast.LENGTH_SHORT).show();
                                                        callback.onResult(false);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FriendRequestManager", "Error checking existing friendship: " + e.getMessage());
                                            Toast.makeText(context, "Error checking friendship status.", Toast.LENGTH_SHORT).show();
                                            callback.onResult(false);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendRequestManager", "Error checking incoming request status: " + e.getMessage());
                                Toast.makeText(context, "Error checking request status.", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendRequestManager", "Error checking outgoing request status: " + e.getMessage());
                    Toast.makeText(context, "Error checking request status.", Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                });
    }

    public void acceptFriendRequest(String initiatingUserId, FriendRequestCallback callback) {
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            return;
        }

        // 1. Fetch and store friend's public key
        FirebaseUtil.getUserByUserId(initiatingUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String friendPublicKey = documentSnapshot.getString("publicKey");

                        if (friendPublicKey != null && !friendPublicKey.isEmpty()) {
                            try {
                                EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);
                                encryptedStorePreference.put(initiatingUserId, friendPublicKey);
                                Log.d("FriendRequestManager", "Friend public key stored for: " + initiatingUserId);
                            } catch (Exception e) {
                                Log.e("FriendRequestManager", "Failed to securely store friend key", e);
                                Toast.makeText(context, "Failed to store public key.", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                                return;
                            }
                        } else {
                            Log.w("FriendRequestManager", "Public key not found for initiating user: " + initiatingUserId);
                            Toast.makeText(context, "Public key not found for friend. Proceeding without key storage.", Toast.LENGTH_LONG).show();

                        }
                        finalizeFriendshipAcceptance(initiatingUserId, callback);

                    } else {
                        Log.e("FriendRequestManager", "Initiating user document not found: " + initiatingUserId);
                        Toast.makeText(context, "Initiating user not found.", Toast.LENGTH_SHORT).show();
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendRequestManager", "Failed to fetch initiating user for public key: " + e.getMessage());
                    Toast.makeText(context, "Error fetching friend details.", Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                });
    }

    private void finalizeFriendshipAcceptance(String initiatingUserId, FriendRequestCallback callback) {
        FirebaseUtil.getFriendRequestDocumentRef(currentUserId, initiatingUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {

                    FirebaseUtil.getFriendRequestDocumentRef(initiatingUserId, currentUserId)
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {

                                FirebaseUtil.getFriendDocumentRef(initiatingUserId, currentUserId)
                                        .set(new FriendData(Timestamp.now()))
                                        .addOnSuccessListener(aVoid2 -> {

                                            FirebaseUtil.getFriendDocumentRef(currentUserId, initiatingUserId)
                                                    .set(new FriendData(Timestamp.now()))
                                                    .addOnSuccessListener(aVoid3 -> {

                                                        String chatRoomId = FirebaseUtil.getChatRoomId(currentUserId, initiatingUserId);
                                                        FirebaseUtil.getChatRoomReference(chatRoomId)
                                                                .get()
                                                                .addOnSuccessListener(chatRoomDoc -> {
                                                                    if (!chatRoomDoc.exists()) {
                                                                        ChatRoomModel newChatroom = new ChatRoomModel(
                                                                                chatRoomId,
                                                                                Arrays.asList(currentUserId, initiatingUserId),
                                                                                Timestamp.now(),
                                                                                ""
                                                                        );
                                                                        FirebaseUtil.getChatRoomReference(chatRoomId)
                                                                                .set(newChatroom)
                                                                                .addOnSuccessListener(aVoid4 -> {
                                                                                    Toast.makeText(context, "Friend request accepted and chatroom created!", Toast.LENGTH_SHORT).show();
                                                                                    callback.onResult(true);
                                                                                })
                                                                                .addOnFailureListener(e -> {
                                                                                    Log.e("FriendRequestManager", "Failed to create chatroom: " + e.getMessage());
                                                                                    Toast.makeText(context, "Accepted, but failed to create chatroom.", Toast.LENGTH_SHORT).show();
                                                                                    callback.onResult(false);
                                                                                });
                                                                    } else {
                                                                        Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                                                                        callback.onResult(true);
                                                                    }
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("FriendRequestManager", "Error checking chatroom: " + e.getMessage());
                                                                    Toast.makeText(context, "Accepted, but error checking chatroom.", Toast.LENGTH_SHORT).show();
                                                                    callback.onResult(false);
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("FriendRequestManager", "Failed to add friend to current user: " + e.getMessage());
                                                        Toast.makeText(context, "Failed to accept request.", Toast.LENGTH_SHORT).show();
                                                        callback.onResult(false);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FriendRequestManager", "Failed to add friend to initiating user: " + e.getMessage());
                                            Toast.makeText(context, "Failed to accept request.", Toast.LENGTH_SHORT).show();
                                            callback.onResult(false);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FriendRequestManager", "Failed to delete outgoing request: " + e.getMessage());
                                Toast.makeText(context, "Failed to accept request.", Toast.LENGTH_SHORT).show();
                                callback.onResult(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendRequestManager", "Failed to delete incoming request: " + e.getMessage());
                    Toast.makeText(context, "Failed to accept request.", Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
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

    public interface FriendRequestCallback {
        void onResult(boolean success);
    }

    private static class FriendRequestData {
        public String status;
        public Timestamp timestamp;
        public String type;

        public FriendRequestData() {}

        public FriendRequestData(String status, Timestamp timestamp, String type) {
            this.status = status;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    private static class FriendData {
        public Timestamp friendsSince;

        public FriendData() {} // No-argument constructor required for Firestore

        public FriendData(Timestamp friendsSince) {
            this.friendsSince = friendsSince;
        }
    }
}
