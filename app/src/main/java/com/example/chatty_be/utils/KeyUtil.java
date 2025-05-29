package com.example.chatty_be.utils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyUtil {
    public static KeyPair generateEphemeralKeyPair() throws Exception {
        KeyPairGenerator ephemeralKeys = KeyPairGenerator.getInstance("EC");
        ephemeralKeys.initialize(new ECGenParameterSpec("secp256r1"));
        return ephemeralKeys.generateKeyPair();
    }

    public static byte[] encodePublicKey(PublicKey key) {
        return key.getEncoded();
    }

    public static PublicKey decodePublicKey(byte[] encoded) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return kf.generatePublic(keySpec);
    }

    public static PrivateKey decodePrivateKey(byte[] encoded) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return kf.generatePrivate(keySpec);
    }
}
