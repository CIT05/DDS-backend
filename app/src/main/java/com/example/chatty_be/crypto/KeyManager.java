package com.example.chatty_be.crypto;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.chatty_be.utils.KeyUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

public class KeyManager {

    private static final String PRIVATE_KEY_PREF = "identity_private_key";
    private static final String PUBLIC_KEY_PREF = "identity_public_key";

    private static final int BASE64_FLAGS = Base64.NO_WRAP;

    public static void generateIdentityKeyPair(Context context) throws Exception {
        EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);

        if (encryptedStorePreference.get(PRIVATE_KEY_PREF) != null && encryptedStorePreference.get(PUBLIC_KEY_PREF) != null) {
            return;
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String privateKeyBase64 = Base64.encodeToString(keyPair.getPrivate().getEncoded(), BASE64_FLAGS);
        String publicKeyBase64 = Base64.encodeToString(keyPair.getPublic().getEncoded(), BASE64_FLAGS);

        encryptedStorePreference.put(PRIVATE_KEY_PREF, privateKeyBase64);
        encryptedStorePreference.put(PUBLIC_KEY_PREF, publicKeyBase64);

    }

    public static PublicKey getPublicKey(Context context) throws Exception {
        EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);
        String base64 = encryptedStorePreference.get(PUBLIC_KEY_PREF);

        if (base64 == null) throw new Exception("Public key not found");

        byte[] raw = Base64.decode(base64, BASE64_FLAGS);
        return KeyUtil.decodePublicKey(raw);
    }

    public static PrivateKey getPrivateKey(Context context) throws Exception {
        EncryptedStorePreference encryptedStorePreference = new EncryptedStorePreference(context);
        String base64 = encryptedStorePreference.get(PRIVATE_KEY_PREF);

        if (base64 == null) throw new Exception("Private key not found");

        byte[] raw = Base64.decode(base64, BASE64_FLAGS);
        return KeyUtil.decodePrivateKey(raw);
    }

    public static void initIdentityKeys(Context context) {
        try {
            generateIdentityKeyPair(context);
            CryptoManager.generateAESKey();
            Log.d("KeyManager", "Identity keys initialized.");
        } catch (Exception e) {
            Log.e("KeyManager", "Failed to initialize identity keys", e);
        }
    }


}
