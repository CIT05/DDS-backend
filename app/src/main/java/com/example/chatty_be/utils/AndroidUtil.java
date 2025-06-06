package com.example.chatty_be.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.chatty_be.R;
import com.example.chatty_be.model.UserModel;

public class AndroidUtil {
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void passUserModelAsIntent(Intent intent, UserModel model){
        intent.putExtra("username", model.getUsername());
        intent.putExtra("phone", model.getPhone());
        intent.putExtra("userId", model.getUserId());
    }

    public  static UserModel getuserModelFromIntent(Intent intent ){
        UserModel userModel = new UserModel();
        userModel.setUsername(intent.getStringExtra("username"));
        userModel.setPhone(intent.getStringExtra("phone"));
        userModel.setUserId(intent.getStringExtra("userId"));

        return  userModel;
    }

    public static void setReviewPic(Context context, Uri imageUri, ImageView imageView) {
        Glide.with(context).load(imageUri).placeholder(R.drawable.plus_icon).error(R.drawable.plus_icon).into(imageView);

    }

}
