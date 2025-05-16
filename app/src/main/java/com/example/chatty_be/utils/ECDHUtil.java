package com.example.chatty_be.utils;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;

public class ECDHUtil {

    public static byte[] deriveSharedSecret(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(privateKey);
        ka.doPhase(publicKey, true); //true as there are only 2 people involved, no multi keys
        return ka.generateSecret();
    }
}