package com.example.chatty_be.ui.friends;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.LoginUsernameActivity;
import com.example.chatty_be.MainActivity;
import com.example.chatty_be.R;
import com.example.chatty_be.adapter.SearchUserRecyclerAdapter;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.auth.User;

public class FriendsNearbyFragment extends Fragment {

    ProgressBar progressBar;

    LinearLayout friendsNearbyContentSuccess;

    LinearLayout friendsNearbyContentError;

    TextView loadingText;

    Button requestFriendsButton;
    FusedLocationProviderClient fusedLocationClient;

    LinearLayout locationPermissionLayout;

    //PLACEHOLDER
    RecyclerView recyclerView;
    SearchUserRecyclerAdapter adapter;

    private final ActivityResultLauncher<String> coarsePermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        simulateLoading(granted);
                    });

    public FriendsNearbyFragment() {
    }

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

        progressBar = view.findViewById(R.id.progressBar);
        friendsNearbyContentSuccess = view.findViewById(R.id.friendsNearbyContentSuccess);
        friendsNearbyContentError = view.findViewById(R.id.friendsNearbyContentError);
        loadingText = view.findViewById(R.id.friendsNearbyLoadingText);
        requestFriendsButton = view.findViewById(R.id.requestFriendsButton);
        locationPermissionLayout = view.findViewById(R.id.locationPermissionDenied);

        requestFriendsButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof FriendsFragment) {
                ((FriendsFragment) parent).showRequestFriendFragment();
            }
        });

        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        boolean hasPermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;


        if(!hasPermission) {
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }else {
            simulateLoading(true);
        }

        return view;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Toast.makeText(getContext(), "Location: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void simulateLoading(boolean isPermissionGranted) {
        progressBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        friendsNearbyContentSuccess.setVisibility(View.GONE);
        friendsNearbyContentError.setVisibility(View.GONE);
        locationPermissionLayout.setVisibility(View.GONE);

        if (!isPermissionGranted) {
            locationPermissionLayout.setVisibility(View.VISIBLE);
            return;
        }

        getLastLocation();
        progressBar.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);


        FirebaseUtil.allUsersCollectionReference().get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            if(task.isSuccessful()){
                Query query = FirebaseUtil.allUsersCollectionReference()
                        .whereGreaterThanOrEqualTo("username", "");

                FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                        .setQuery(query, UserModel.class).build();

                recyclerView = getView().findViewById(R.id.friendsNearbyListContainer);

                adapter = new SearchUserRecyclerAdapter(options, getContext());
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(adapter);
                adapter.startListening();
                friendsNearbyContentSuccess.setVisibility(View.VISIBLE);
            }
            else {
                friendsNearbyContentError.setVisibility(View.VISIBLE);
            }
        });
    }
}
