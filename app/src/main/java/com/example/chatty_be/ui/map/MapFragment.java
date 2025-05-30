package com.example.chatty_be.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.chatty_be.AddReviewActivity;
import com.example.chatty_be.R;
import com.example.chatty_be.databinding.FragmentMapBinding;
import com.example.chatty_be.ui.friends.FriendsNearbyFragment;
import com.example.chatty_be.ui.friends.FriendsRequestFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private FusedLocationProviderClient fusedClient;
    private PointAnnotationManager pointAnnotationManager;
    private final Map<PointAnnotation, DocumentSnapshot> annotationMap = new HashMap<>();
    BottomNavigationView bottomNav;


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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentMapBinding.inflate(inflater, container, false);
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());

        binding.addReviewBtn.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddReviewActivity.class));
        });

        binding.mapView.getMapboxMap().loadStyle(Style.SATELLITE, style -> {
            // Initialize annotation manager
            AnnotationPlugin annoPlugin = (AnnotationPlugin)
                    binding.mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);
            pointAnnotationManager = (PointAnnotationManager)
                    annoPlugin.createAnnotationManager(
                            AnnotationType.PointAnnotation,
                            new AnnotationConfig()
                    );

            // Real-time listener for user's reviews
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("reviews")
                    .whereEqualTo("userUid", uid)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Toast.makeText(requireContext(),
                                    "Error loading reviews: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Clear old pins
                        pointAnnotationManager.deleteAll();
                        annotationMap.clear();

                        // Draw new pins
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            GeoPoint gp = doc.getGeoPoint("geo");
                            if (gp == null) continue;
                            Point pt = Point.fromLngLat(gp.getLongitude(), gp.getLatitude());

                            PointAnnotationOptions opts = new PointAnnotationOptions()
                                    .withPoint(pt)
                                    .withIconImage(bitmapFromDrawable(R.drawable.red_marker))
                                    .withIconSize(3.0f);
                            PointAnnotation ann = pointAnnotationManager.create(opts);
                            annotationMap.put(ann, doc);
                        }

                        // Set click listener
                        pointAnnotationManager.addClickListener(annotation -> {
                            DocumentSnapshot clicked = annotationMap.get(annotation);
                            if (clicked != null) showReviewPopup(clicked);
                            return true;
                        });
                    });
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);

        bottomNav = binding.bottomNavigation;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            zoomToUserArea();
        } else {
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (item.getItemId() == R.id.menu_my_pins) {
                FirebaseFirestore.getInstance()
                        .collection("reviews")
                        .whereEqualTo("userUid", currentUserId)
                        .get()
                        .addOnSuccessListener(this::renderReviewAnnotations)
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Failed to load your pins", Toast.LENGTH_SHORT).show()
                        );
            } else if (item.getItemId() == R.id.menu_my_friends_pins) {
                loadFriendUidsAndDisplayPins();
            }

            return true;
        });

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

    private void showReviewPopup(DocumentSnapshot doc) {
        View popup = LayoutInflater.from(requireContext())
                .inflate(R.layout.popup_review, null);
        ImageView img = popup.findViewById(R.id.review_image);
        TextView tvName = popup.findViewById(R.id.review_name);
        TextView tvDesc = popup.findViewById(R.id.review_desc);
        TextView tvType = popup.findViewById(R.id.review_type);

        tvName.setText(doc.getString("locationName"));
        tvType.setText(doc.getString("locationType"));
        tvDesc.setText(doc.getString("comment"));

        List<String> urls = (List<String>) doc.get("imageUrls");
        if (urls != null && !urls.isEmpty()) {
            img.setVisibility(View.VISIBLE);
            Glide.with(this).load(urls.get(0)).into(img);
        } else {
            img.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(requireContext())
                .setView(popup)
                .setPositiveButton("OK", null)
                .show();
    }

    private Bitmap bitmapFromDrawable(@DrawableRes int resId) {
        Drawable d = ResourcesCompat.getDrawable(
                requireContext().getResources(), resId, null);
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable) d).getBitmap();
        }
        Bitmap bmp = Bitmap.createBitmap(
                d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, c.getWidth(), c.getHeight());
        d.draw(c);
        return bmp;
    }

    private void loadFriendUidsAndDisplayPins() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Classic loop instead of stream().toList()
                    List<String> friendUids = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        friendUids.add(doc.getId());
                    }

                    if (friendUids.isEmpty()) {
                        pointAnnotationManager.deleteAll();
                        Toast.makeText(getContext(), "No friends found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Now query reviews for those UIDs
                    FirebaseFirestore.getInstance()
                            .collection("reviews")
                            .whereIn("userUid", friendUids)
                            .get()
                            .addOnSuccessListener(this::renderReviewAnnotations)
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to load friend pins", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load friends list", Toast.LENGTH_SHORT).show()
                );
    }


    private void renderReviewAnnotations(QuerySnapshot snapshots) {
        pointAnnotationManager.deleteAll();
        annotationMap.clear();

        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            GeoPoint gp = doc.getGeoPoint("geo");
            if (gp == null) continue;

            Point pt = Point.fromLngLat(gp.getLongitude(), gp.getLatitude());
            PointAnnotationOptions opts = new PointAnnotationOptions()
                    .withPoint(pt)
                    .withIconImage(bitmapFromDrawable(R.drawable.red_marker))
                    .withIconSize(3.0f);

            PointAnnotation ann = pointAnnotationManager.create(opts);
            annotationMap.put(ann, doc);
        }

        pointAnnotationManager.addClickListener(annotation -> {
            DocumentSnapshot clicked = annotationMap.get(annotation);
            if (clicked != null) showReviewPopup(clicked);
            return true;
        });
    }


}
