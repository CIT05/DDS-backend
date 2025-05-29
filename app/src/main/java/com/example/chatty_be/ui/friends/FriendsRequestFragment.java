package com.example.chatty_be.ui.friends;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.R;
import com.example.chatty_be.adapter.FriendRequestAdapter;
import com.example.chatty_be.model.FriendRequestItem;
import com.example.chatty_be.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private FriendRequestAdapter adapter;
    private final List<FriendRequestItem> requestItems = new ArrayList<>();
    private final Map<String, FriendRequestItem> userItemMap = new HashMap<>();
    private ListenerRegistration friendsListener;
    private ListenerRegistration incomingRequestListener;
    private ListenerRegistration outgoingRequestListener;

    private final ActivityResultLauncher<String> contactPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d("FriendsFragment", "Contacts permission granted: " + isGranted);
                if (isGranted) {
                    loadMatchedContacts();
                } else {
                    Toast.makeText(requireContext(), "Contacts permission is required to find friends", Toast.LENGTH_SHORT).show();
                    updateRecyclerView();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_request, container, false);
        recyclerView = view.findViewById(R.id.friend_requests_recycler);

        adapter = new FriendRequestAdapter(requestItems, requireContext(), this::refreshData);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshDataWithListeners();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (friendsListener != null) {
            friendsListener.remove();
            friendsListener = null;
        }
        if (incomingRequestListener != null) {
            incomingRequestListener.remove();
            incomingRequestListener = null;
        }
        if (outgoingRequestListener != null) {
            outgoingRequestListener.remove();
            outgoingRequestListener = null;
        }
    }


    private void refreshDataWithListeners() {
        Log.d("FriendsFragment", "refreshDataWithListeners() called.");
        requestItems.clear();
        userItemMap.clear();
        adapter.notifyDataSetChanged();

        String currentUserId = FirebaseUtil.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            Log.e("FriendsFragment", "Current user ID is null. Cannot refresh data.");
            return;
        }
        Log.d("FriendsFragment", "Current User ID: " + currentUserId);


        Log.d("FriendsFragment", "Listening for existing friends...");
        friendsListener = FirebaseUtil.getFriendsRef(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FriendsFragment", "Friends listener failed.", error);
                        return;
                    }
                    if (value != null) {
                        Log.d("FriendsFragment", "Friends data changed.");
                        Set<String> currentFriends = new HashSet<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String friendId = doc.getId();
                            currentFriends.add(friendId);
                            FirebaseUtil.getUserByUserId(friendId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String name = getDisplayName(userDoc, friendId);
                                        FriendRequestItem item = new FriendRequestItem(friendId, name, true, null, false);
                                        userItemMap.put(friendId, item);
                                        updateRecyclerView();
                                    })
                                    .addOnFailureListener(e -> Log.e("FriendsFragment", "Error getting friend user details: " + e.getMessage()));
                        }

                        userItemMap.entrySet().removeIf(entry -> entry.getValue().isFriend() && !currentFriends.contains(entry.getKey()));
                        updateRecyclerView();
                    }
                });


        Log.d("FriendsFragment", "Listening for incoming friend requests...");
        incomingRequestListener = FirebaseUtil.getFriendRequestsRef(currentUserId)
                .whereEqualTo("type", "incoming")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FriendsFragment", "Incoming requests listener failed.", error);
                        return;
                    }
                    if (value != null) {
                        Log.d("FriendsFragment", "Incoming requests data changed.");
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String fromUserId = doc.getId();
                            FirebaseUtil.getUserByUserId(fromUserId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String name = getDisplayName(userDoc, fromUserId);
                                        FriendRequestItem item = new FriendRequestItem(fromUserId, "Request from: " + name, false, "pending", true);
                                        userItemMap.put(fromUserId, item);
                                        updateRecyclerView();
                                    })
                                    .addOnFailureListener(e -> Log.e("FriendsFragment", "Error getting incoming request user details: " + e.getMessage()));
                        }
                        // Remove items that are no longer pending incoming requests
                        userItemMap.entrySet().removeIf(entry -> entry.getValue().isIncoming() && ("pending".equals(entry.getValue().getStatus())));
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            userItemMap.put(doc.getId(), new FriendRequestItem(doc.getId(), "...", false, "pending", true)); // Add placeholders to ensure they are considered
                        }
                        // Re-fetch details to update or add correctly
                        FirebaseUtil.getFriendRequestsRef(currentUserId)
                                .whereEqualTo("type", "incoming")
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot) {
                                        String fromUserId = doc.getId();
                                        FirebaseUtil.getUserByUserId(fromUserId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {
                                                    String name = getDisplayName(userDoc, fromUserId);
                                                    FriendRequestItem item = new FriendRequestItem(fromUserId, "Request from: " + name, false, "pending", true);
                                                    userItemMap.put(fromUserId, item);
                                                    updateRecyclerView();
                                                });
                                    }
                                });
                        updateRecyclerView();
                    }
                });

        Log.d("FriendsFragment", "Listening for outgoing friend requests...");
        outgoingRequestListener = FirebaseUtil.getFriendRequestsRef(currentUserId)
                .whereEqualTo("type", "outgoing")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FriendsFragment", "Outgoing requests listener failed.", error);
                        return;
                    }
                    if (value != null) {
                        Log.d("FriendsFragment", "Outgoing requests data changed.");
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String toUserId = doc.getId();
                            FirebaseUtil.getUserByUserId(toUserId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String name = getDisplayName(userDoc, toUserId);
                                        FriendRequestItem item = new FriendRequestItem(toUserId, name, false, "pending", false);
                                        userItemMap.put(toUserId, item);
                                        updateRecyclerView();
                                    })
                                    .addOnFailureListener(e -> Log.e("FriendsFragment", "Error getting outgoing request user details: " + e.getMessage()));
                        }

                        userItemMap.entrySet().removeIf(entry -> !entry.getValue().isIncoming() && ("pending".equals(entry.getValue().getStatus())));
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            userItemMap.put(doc.getId(), new FriendRequestItem(doc.getId(), "...", false, "pending", false)); // Add placeholders
                        }

                        FirebaseUtil.getFriendRequestsRef(currentUserId)
                                .whereEqualTo("type", "outgoing")
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot) {
                                        String toUserId = doc.getId();
                                        FirebaseUtil.getUserByUserId(toUserId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {
                                                    String name = getDisplayName(userDoc, toUserId);
                                                    FriendRequestItem item = new FriendRequestItem(toUserId, name, false, "pending", false);
                                                    userItemMap.put(toUserId, item);
                                                    updateRecyclerView();
                                                });
                                    }
                                });
                        updateRecyclerView();
                    }
                });

        checkAndLoadContacts();
    }

    private void updateRecyclerView() {
        requestItems.clear();
        requestItems.addAll(userItemMap.values());
        Collections.sort(requestItems, (o1, o2) -> {
            if (o1.isFriend() && !o2.isFriend()) return -1;
            if (!o1.isFriend() && o2.isFriend()) return 1;

            boolean o1IncomingPending = "pending".equals(o1.getStatus()) && o1.isIncoming();
            boolean o2IncomingPending = "pending".equals(o2.getStatus()) && o2.isIncoming();
            if (o1IncomingPending && !o2IncomingPending) return -1;
            if (!o1IncomingPending && o2IncomingPending) return 1;

            boolean o1OutgoingPending = "pending".equals(o1.getStatus()) && !o1.isIncoming();
            boolean o2OutgoingPending = "pending".equals(o2.getStatus()) && !o2.isIncoming();
            if (o1OutgoingPending && !o2OutgoingPending) return -1;
            if (!o1OutgoingPending && o2OutgoingPending) return 1;

            return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
        });
        requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        Log.d("FriendsFragment", "RecyclerView updated. Total items: " + requestItems.size());
    }


    private void checkAndLoadContacts() {
        Log.d("FriendsFragment", "checkAndLoadContacts() called.");
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d("FriendsFragment", "Contacts permission already granted. Loading matched contacts.");
            loadMatchedContacts();
        } else {
            Log.d("FriendsFragment", "Requesting contacts permission.");
            contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS);
        }
    }

    private void loadMatchedContacts() {
        Log.d("FriendsFragment", "loadMatchedContacts() called.");
        String currentUserId = FirebaseUtil.getCurrentUserId();
        if (currentUserId == null) {
            Log.e("FriendsFragment", "Current user ID is null in loadMatchedContacts.");
            updateRecyclerView();
            return;
        }

        List<String> localPhoneNumbers = getLocalPhoneNumbers();
        Log.d("FriendsFragment", "Local phone numbers retrieved: " + localPhoneNumbers.size() + " numbers.");
        if (localPhoneNumbers.isEmpty()) {
            Toast.makeText(requireContext(), "No phone numbers found in your contacts.", Toast.LENGTH_LONG).show();
            Log.w("FriendsFragment", "No local phone numbers found.");
            updateRecyclerView();
            return;
        }

        FirebaseUtil.allUsersCollectionReference().get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("FriendsFragment", "Fetched " + snapshot.size() + " users from Firebase for contact matching.");
                    for (DocumentSnapshot userDoc : snapshot.getDocuments()) {
                        String userId = userDoc.getId();
                        if (userId.equals(currentUserId)) continue;

                        String phone = userDoc.getString("phone");
                        if (phone != null) {
                            String normalizedFirebasePhone = phone.replaceAll("[^\\d+]", "");
                            if (localPhoneNumbers.contains(normalizedFirebasePhone)) {
                                if (!userItemMap.containsKey(userId)) {
                                    String displayName = getDisplayName(userDoc, userId);
                                    FriendRequestItem item = new FriendRequestItem(userId, displayName, false, null, false);
                                    userItemMap.put(userId, item);
                                    Log.d("FriendsFragment", "Added potential friend from contacts: " + displayName + " (" + userId + ")");
                                }
                            }
                        }
                    }
                    updateRecyclerView();
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsFragment", "Error loading all users for contact matching: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading contacts.", Toast.LENGTH_SHORT).show();
                    updateRecyclerView();
                });
    }

    private List<String> getLocalPhoneNumbers() {
        List<String> numbers = new ArrayList<>();
        Log.d("FriendsFragment", "getLocalPhoneNumbers() called.");
        Cursor cursor = null;
        try {
            cursor = requireActivity().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") String number = cursor.getString(
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    );
                    if (number != null) {
                        String normalizedNumber = number.replaceAll("[^\\d+]", "");
                        Log.d("Contacts", "Raw contact number: " + number + ", Normalized: " + normalizedNumber);
                        numbers.add(normalizedNumber);
                    }
                }
            } else {
                Log.e("Contacts", "Cursor was null in getLocalPhoneNumbers");
            }
        } catch (SecurityException e) {
            Log.e("Contacts", "SecurityException: Contacts permission denied. " + e.getMessage());
            Toast.makeText(requireContext(), "Permission denied to read contacts.", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return numbers;
    }

    private String getDisplayName(DocumentSnapshot doc, String fallback) {
        if (doc.contains("username") && doc.getString("username") != null && !doc.getString("username").isEmpty()) {
            return doc.getString("username");
        }
        if (doc.contains("phoneNumber") && doc.getString("phoneNumber") != null && !doc.getString("phoneNumber").isEmpty()) {
            return doc.getString("phoneNumber");
        }
        return fallback;
    }

    public void refreshData() {
        onStop();
        refreshDataWithListeners();
    }
}