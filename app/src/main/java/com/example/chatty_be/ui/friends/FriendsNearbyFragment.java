package com.example.chatty_be.ui.friends;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.R;
import com.example.chatty_be.adapter.UserLocationRecyclerAdapter;
import com.example.chatty_be.model.UserLocationModel;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.example.chatty_be.utils.LocationUtil;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsNearbyFragment extends Fragment {

    ProgressBar progressBar;
    LinearLayout friendsNearbyContentSuccess;
    LinearLayout friendsNearbyContentError;
    TextView loadingText;
    TextView errorText;
    Button requestFriendsButton;
    FusedLocationProviderClient fusedLocationClient;
    LinearLayout locationPermissionLayout;
    UserLocationModel userLocationModel;
    RecyclerView recyclerView;
    UserLocationRecyclerAdapter adapter;

    private final ActivityResultLauncher<String> coarsePermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        handleLocationPermissionResult(granted);
                    });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends_nearby, container, false);

        initializeViews(view);
        resetViews();
        setupRequestFriendsButton();


        if (!hasLocationPermission()) {
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            handleLocationPermissionResult(true);
        }

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.friendsNearbyListContainer);
        progressBar = view.findViewById(R.id.progressBar);
        friendsNearbyContentSuccess = view.findViewById(R.id.friendsNearbyContentSuccess);
        friendsNearbyContentError = view.findViewById(R.id.friendsNearbyContentError);
        loadingText = view.findViewById(R.id.friendsNearbyLoadingText);
        errorText = view.findViewById(R.id.friendsNearbyErrorText);
        requestFriendsButton = view.findViewById(R.id.requestFriendsButton);
        locationPermissionLayout = view.findViewById(R.id.locationPermissionDenied);
    }

    private void setupRequestFriendsButton() {
        requestFriendsButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof FriendsFragment) {
                ((FriendsFragment) parent).showRequestFriendFragment();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleLocationPermissionResult(boolean isPermissionGranted) {

        resetViews();

        if (!isPermissionGranted) {
            locationPermissionLayout.setVisibility(View.VISIBLE);
            return;
        }

        getLastLocation();
    }

    private void resetViews() {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        friendsNearbyContentSuccess.setVisibility(View.GONE);
        friendsNearbyContentError.setVisibility(View.GONE);
        locationPermissionLayout.setVisibility(View.GONE);
        requestFriendsButton.setVisibility(View.GONE);
    }

    private void displayError(String message) {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        friendsNearbyContentError.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void displayNearbyUsersDetails(Map<String, UserLocationModel> userLocations) {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        if (userLocations.isEmpty()) {
            displayError("No nearby users found.");
            requestFriendsButton.setVisibility(View.VISIBLE);
            return;
        }


        List<String> userIds = List.copyOf(userLocations.keySet());


        adapter = new UserLocationRecyclerAdapter(new ArrayList<>(), requireContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        for (int i = 0; i < userIds.size(); i += 10) {
            int end = Math.min(i + 10, userIds.size());
            List<String> chunk = userIds.subList(i, end);

            FirebaseUtil.allUsersCollectionReference()
                    .whereIn("userId", chunk)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<UserModel> users = task.getResult().toObjects(UserModel.class);

                            adapter.addUsers(users);
                            friendsNearbyContentSuccess.setVisibility(View.VISIBLE);
                        } else {
                            Log.w("FriendsNearbyFragment", "Error getting users", task.getException());
                            displayError("Failed to fetch user details. Please try again later.");
                        }
                    });
        }

    }

    private void fetchNearbyUsers(String myCell,
                                  double radiusMeters,
                                  String currentUserId,
                                  Timestamp now) {

        FirebaseUtil.allUserLocationReference()
                // only exact same cell
                .whereEqualTo("geoHash", myCell)
                .whereGreaterThan("expireAt", now)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, UserLocationModel> hits = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        UserLocationModel ul = doc.toObject(UserLocationModel.class);
                        if (ul.getUserId().equals(currentUserId)) continue;
                        hits.put(ul.getUserId(), ul);
                    }
                    displayNearbyUsersDetails(hits);
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsNearby", "Error fetching nearby cells", e);
                    // show the real exception message to help you debug
                    displayError("Failed to load nearby cells: " + e.getMessage());
                });
    }




    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        progressBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);

        fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                null
                                )
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveUserLocation(location);
                    } else {
                        Log.w("FriendsNearbyFragment", "No location found");
                        displayError("Failed to get your location. Please ensure location services are enabled.");
//
                    }
                });
    }


    @SuppressLint("MissingPermission")
    private void saveUserLocation(Location location) {
        String currentUserId = FirebaseUtil.getCurrentUserId();

        // 1) pick your blur-size (5 chars ≃4.9×4.9 km)
        String cell = LocationUtil.encodeGeohash(
                location.getLatitude(),
                location.getLongitude(),
                /*precision=*/5
        );

        // 2) expire after 30 min
        Timestamp expireAt = new Timestamp(
                Timestamp.now().getSeconds() + 30 * 60,
                0
        );

        // 3) build the model
        UserLocationModel locModel = new UserLocationModel(
                location.getLatitude(),
                location.getLongitude(),
                currentUserId,
                expireAt,
                cell
        );

        // 4) write under user_locations/{uid}
        FirebaseUtil.getUserLocationReference(currentUserId)
                .set(locModel)
                .addOnSuccessListener(unused -> {
                    Log.d("FriendsNearby", "Saved geohash=" + cell);
                    // kick off the nearby-friends lookup
                    fetchNearbyUsers(cell,
                            /*radiusMeters=*/5000,
                            currentUserId,
                            Timestamp.now());
                })
                .addOnFailureListener(e -> {
                    Log.w("FriendsNearby", "Could not save blurred location", e);
                    displayError("Could not save your location. Try again.");
                });
    }




}
