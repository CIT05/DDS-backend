package com.example.chatty_be.crypto;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

public class KeyManager {

        private static final String KEY_ALIAS = "identity_key";

        public static void generateIdentityKeyPair() throws Exception {

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

                keyPairGenerator.initialize(
                        new KeyGenParameterSpec.Builder(KEY_ALIAS,
                                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setUserAuthenticationRequired(false)
                                .build());

                keyPairGenerator.generateKeyPair();
            }
        }

        public static PublicKey getPublicKey() throws Exception {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);

            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new Exception("Not an instance of a PrivateKeyEntry");
            }

            return ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
        }

}
