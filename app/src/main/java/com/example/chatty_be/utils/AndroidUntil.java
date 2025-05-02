package com.example.chatty_be.utils;

import android.content.Context;
import android.widget.Toast;

public class AndroidUntil {
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
