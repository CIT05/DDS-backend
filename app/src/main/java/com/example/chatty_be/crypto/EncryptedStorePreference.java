package com.example.chatty_be.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.example.chatty_be.crypto.CryptoManager;
import com.example.chatty_be.utils.EncryptionUtil;

import javax.crypto.SecretKey;

public class EncryptedStorePreference {

    private static final String PREF_NAME = "secure_prefs_native";
    private final SharedPreferences preferences;

    public EncryptedStorePreference(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            CryptoManager.generateAESKey();
        } catch (Exception e) {
            Log.e("EncryptedStore", "Failed to create Keystore key", e);
        }
    }

    public void put(String key, String value) {
        try {
            SecretKey secretKey = CryptoManager.getSecretKey();
            byte[] iv = new byte[12];
            byte[] encrypted = EncryptionUtil.encrypt(value, secretKey, iv);

            preferences.edit()
                    .putString(key + "_data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(key + "_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .apply();
        } catch (Exception e) {
            Log.e("EncryptedStore", "Encryption failed", e);
        }
    }

    public String get(String key) {
        try {
            String encryptedBase64 = preferences.getString(key + "_data", null);
            String ivBase64 = preferences.getString(key + "_iv", null);

            if (encryptedBase64 == null || ivBase64 == null) return null;

            byte[] encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);

            SecretKey secretKey = CryptoManager.getSecretKey();
            return EncryptionUtil.decrypt(encrypted, secretKey, iv);

        } catch (Exception e) {
            Log.e("EncryptedStore", "Decryption failed", e);
            return null;
        }
    }
}