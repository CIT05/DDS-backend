package com.example.chatty_be;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chatty_be.model.ReviewModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class AddReviewActivity extends AppCompatActivity {
    ImageButton backButton;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
    Button postButton;
    ImageView reviewImage;

    EditText commentField;

    EditText nameField;

    Spinner locationType;

    private ActivityResultLauncher<Intent> pickLocationLauncher;
    @Nullable private GeoPoint selectedGeo;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();

                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            AndroidUtil.setReviewPic(this, selectedImageUri, reviewImage);
                        }
                    }
                }
        );

        setContentView(R.layout.activity_add_review);

        reviewImage = findViewById(R.id.btnAddImage);
        postButton    = findViewById(R.id.btnPost);
        backButton = findViewById(R.id.back_button);
        commentField = findViewById(R.id.etDescription);
        nameField = findViewById(R.id.etNameLocation);
        locationType = findViewById(R.id.spLocationType);

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());


        reviewImage.setOnClickListener(v -> {
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
                    });
        });

        pickLocationLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                double lat = result.getData().getDoubleExtra("lat", 0);
                                double lng = result.getData().getDoubleExtra("lng", 0);
                                selectedGeo = new GeoPoint(lat, lng);
                                Toast.makeText(this,
                                        String.format(Locale.US,
                                                "Location: %.5f, %.5f", lat, lng),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

        findViewById(R.id.btnPickLocation).setOnClickListener(v -> {
            Intent i = new Intent(this, MapPickerActivity.class);
            pickLocationLauncher.launch(i);
        });


        postButton.setOnClickListener(v -> uploadReview());

    }

    private void uploadReview() {
        if (!validateInput()) return;

        if (selectedGeo == null) {           // guard – user MUST pick a point
            Toast.makeText(this,
                    "Please pick a location first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = commentField.getText().toString().trim();
        String locName = nameField.getText().toString().trim();

        if (locName.isEmpty()) {
            Toast.makeText(this, "Please enter a name for the location", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("reviews").document();   // auto-ID
        String reviewId = doc.getId();
        String userUid  = FirebaseUtil.getCurrentUserId();

        ReviewModel review = new ReviewModel(
                reviewId,
                userUid,
                locName,
                locationType.getSelectedItem().toString(),
                comment,
                System.currentTimeMillis(),
                null,      // imageUrls will be added after upload
                selectedGeo
        );

//        If there’s a picture, upload it first ------------------------------
        if (selectedImageUri != null) {

            StorageReference picRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("review_pic")
                    .child(userUid)
                    .child(reviewId)
                    .child(UUID.randomUUID().toString() + ".jpg");

            picRef.putFile(selectedImageUri)
                    .continueWithTask(t -> picRef.getDownloadUrl())
                    .addOnSuccessListener(uri -> {
                        review.setImageUrls(Collections.singletonList(uri.toString()));
                        doc.set(review)           // write to Firestore
                                .addOnSuccessListener(v -> finishOk())
                                .addOnFailureListener(e -> toast(e.getMessage()));
                    })
                    .addOnFailureListener(e -> toast(e.getMessage()));

            // No picture – just write the doc ------------------------------------
        } else {
            doc.set(review)
                    .addOnSuccessListener(v -> finishOk())
                    .addOnFailureListener(e -> toast(e.getMessage()));
        }


    }
    private void finishOk() {
        Toast.makeText(this, "Review posted!", Toast.LENGTH_SHORT).show();
        finish();                                    // closes AddReviewActivity
    }

    private void toast(@Nullable String msg) {
        Toast.makeText(this,
                msg == null ? "Something went wrong" : msg,
                Toast.LENGTH_SHORT).show();
    }

    private boolean validateInput() {
        String locName  = nameField.getText().toString().trim();
        String comment  = commentField.getText().toString().trim();
        String type     = locationType.getSelectedItem().toString();

        if (locName.isEmpty()) {
            nameField.setError("Location name required");
            nameField.requestFocus();
            return false;
        }

        if (comment.isEmpty()) {
            commentField.setError("Please add a description");
            commentField.requestFocus();
            return false;
        }

        if (type.equals("Select type")) {          // use your real placeholder text
            Toast.makeText(this, "Choose a location type", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

}