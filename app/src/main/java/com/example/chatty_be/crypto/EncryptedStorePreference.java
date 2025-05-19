package com.example.chatty_be.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptedStorePreference {

    private static final String PREF_NAME = "encrypted_secure_store";

    private static final int BASE64_FLAGS = Base64.NO_WRAP;
    private final SharedPreferences preferences;


    public EncryptedStorePreference(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            CryptoManager.generateAESKey();
        } catch (Exception e) {
            Log.e("EncryptedStorePreference", "Failed to create Keystore key", e);
        }
    }

    public void put(String key, String value) {
        try {
            SecretKey secretKey = CryptoManager.getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(value.getBytes());

            preferences.edit()
                    .putString(key + "_data", Base64.encodeToString(encrypted, BASE64_FLAGS))
                    .putString(key + "_iv", Base64.encodeToString(iv, BASE64_FLAGS))
                    .apply();
        } catch (Exception e) {
            Log.e("EncryptedStorePreference", "Encryption failed", e);
        }
    }

    public String get(String key) {
        try {
            String encryptedBase64 = preferences.getString(key + "_data", null);
            String ivBase64 = preferences.getString(key + "_iv", null);

            if (encryptedBase64 == null || ivBase64 == null) return null;

            byte[] encrypted = Base64.decode(encryptedBase64, BASE64_FLAGS);
            byte[] iv = Base64.decode(ivBase64, BASE64_FLAGS);

            SecretKey secretKey = CryptoManager.getSecretKey();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes);
        } catch (Exception e) {
            Log.e("EncryptedStorePreference", "Decryption failed", e);
            return null;
        }
    }
}