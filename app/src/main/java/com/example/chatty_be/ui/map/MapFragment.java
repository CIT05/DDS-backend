package com.example.chatty_be.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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

import com.example.chatty_be.AddReviewActivity;
import com.example.chatty_be.ChatActivity;
import com.example.chatty_be.databinding.FragmentMapBinding;
import com.example.chatty_be.utils.AndroidUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private FusedLocationProviderClient fusedClient;

    /*──── modern permission launcher ────*/
    private final ActivityResultLauncher<String> coarsePermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            zoomToUserArea();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Location permission denied – default view will be shown",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

    /*──────────────── onCreateView ────────────────*/
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);

        binding.addReviewBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddReviewActivity.class);
            startActivity(intent);
        });

        binding.mapView.getMapboxMap().loadStyle(Style.SATELLITE);

        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            zoomToUserArea();

        } else {
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void zoomToUserArea() {
        fusedClient.getLastLocation().addOnSuccessListener(this::moveCamera);
    }

    private void moveCamera(Location loc) {
        if (loc == null) return;
        binding.mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(Point.fromLngLat(loc.getLongitude(), loc.getLatitude()))
                        .zoom(10.0)
                        .build());
    }

    @Override
    public void onDestroyView() {
        binding.mapView.onDestroy();
        binding = null;
        super.onDestroyView();
    }
}
