package com.example.chatty_be.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class KdfUtils {

    public static byte[] hmacSha256(byte[] key, String info)
            throws GeneralSecurityException {

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(info.getBytes(StandardCharsets.UTF_8));
    }

    private KdfUtils() {}

}
