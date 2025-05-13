package com.example.chatty_be;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chatty_be.utils.AndroidUtil;
import com.github.dhaval2404.imagepicker.ImagePicker;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class AddReviewActivity extends AppCompatActivity {
    ImageButton backButton;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;
//    FrameLayout addImageBox;

    ImageView reviewImage;

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

        backButton = findViewById(R.id.back_button);
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

    }


}