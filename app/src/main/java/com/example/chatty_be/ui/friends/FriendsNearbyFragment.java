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

    private void fetchNearbyUsers(double latitude, double longitude, double radius, String currentUserId, Timestamp currentTimestamp) {
        LocationUtil.MinMaxCoordinates minMaxCoordinates = LocationUtil.getMinMaxCoordinatesBox(latitude, longitude, radius);


        FirebaseUtil.getFriendsRef(currentUserId).get().addOnCompleteListener(friendTask -> {
            if (friendTask.isSuccessful()) {
                List<String> friendIds = new ArrayList<>();
                for (QueryDocumentSnapshot document : friendTask.getResult())
                    friendIds.add(document.getId());

                FirebaseUtil.allUserLocationReference().
                        whereNotEqualTo("userId", currentUserId)
                        .whereIn("userId", friendIds)
                        .whereGreaterThan("expireAt", currentTimestamp)
                        .whereGreaterThanOrEqualTo("latitude", minMaxCoordinates.minLat)
                        .whereLessThanOrEqualTo("latitude", minMaxCoordinates.maxLat)
                        .whereGreaterThanOrEqualTo("longitude", minMaxCoordinates.minLon)
                        .whereLessThanOrEqualTo("longitude", minMaxCoordinates.maxLat)
                        .get()
                        .addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {

                                Map<String, UserLocationModel> userLocations = new HashMap<String, UserLocationModel>();


                                for (QueryDocumentSnapshot document : task1.getResult()) {

                                    UserLocationModel userLocation = document.toObject(UserLocationModel.class);



                                    boolean isUserInsideTheCircle = LocationUtil.isCoordinatesInCircle(
                                            userLocation.getLatitude(),
                                            userLocation.getLongitude(),
                                            latitude,
                                            longitude,
                                            radius);

                                    if (isUserInsideTheCircle) {
                                        userLocations.put(userLocation.getUserId(), userLocation);
                                    }
                                }

                                displayNearbyUsersDetails(userLocations);

                                if (task1.getResult().isEmpty()) {
                                    displayError("No nearby users found.");
                                    requestFriendsButton.setVisibility(View.VISIBLE);
                                } else {
                                    loadingText.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    friendsNearbyContentSuccess.setVisibility(View.VISIBLE);
                                }
                            } else {
                                displayError("No nearby users found.");
                                requestFriendsButton.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                Log.w("FriendsNearbyFragment", "Error getting friends", friendTask.getException());
                displayError("Failed to fetch friends. Please try again later.");
            }
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

    private void saveUserLocation(Location location) {
        double latitude = LocationUtil.roundLocation(location.getLatitude(), 100);
        double longitude = LocationUtil.roundLocation(location.getLongitude(), 100);
        double radius = 5000;

        Timestamp expireAt = new Timestamp(Timestamp.now().getSeconds() + 30 * 60, 0);

        String currentUserId = FirebaseUtil.getCurrentUserId();
        Timestamp currentTimestamp = Timestamp.now();

        userLocationModel = new UserLocationModel(
                latitude, longitude, currentUserId, expireAt);

        Log.d("FriendsNearbyFragment", "Saving location: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());


        FirebaseUtil.getUserLocationReference(FirebaseUtil.getCurrentUserId()).set(userLocationModel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("FriendsNearbyFragment", "User location set successfully");
                fetchNearbyUsers(latitude, longitude, radius, currentUserId, currentTimestamp);
            } else {
                Log.w("FriendsNearbyFragment", "Can not save user location");
            }
        });
    }
}
