package com.example.chatty_be.utils;

import android.util.Base64;
import android.util.Log;

import com.example.chatty_be.ChatSession;
import com.example.chatty_be.EncryptedMessage;

import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MessagesUtil {

    public static final String TAG = "MessageCryptoUtil";

    public static EncryptedMessage encryptMessage(String plaintext, ChatSession session) {
        try {
            byte[] aesKey = session.getNextSendAESKey();
            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");

            byte[] iv = new byte[12];
            byte[] ciphertext = EncryptionUtil.encrypt(plaintext, secretKey, iv);
            Log.d(TAG, "Sending Ephemeral Public Key: " + Base64.encodeToString(session.getMyEphemeralPublicKeyEncoded(), Base64.NO_WRAP));

            return new EncryptedMessage(
                    Base64.encodeToString(ciphertext, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP),
                    Base64.encodeToString(session.getMyEphemeralPublicKeyEncoded(), Base64.NO_WRAP)
            );
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    public static String decryptMessage(String base64Ciphertext, String base64IV, ChatSession session) {
        try {
            byte[] aesKey = session.getNextReceiveAESKey();
            Log.d(TAG, "AES Key used for decryption: " + Base64.encodeToString(aesKey, Base64.NO_WRAP));

            SecretKey secretKey = new SecretKeySpec(aesKey, "AES");

            byte[] ciphertext = Base64.decode(base64Ciphertext,Base64.NO_WRAP);

            byte[] iv = Base64.decode(base64IV, Base64.NO_WRAP);
            Log.d(TAG, "Decoded IV: " + Arrays.toString(iv));
            Log.d(TAG, "Decoded Ciphertext: " + Arrays.toString(ciphertext));

            return EncryptionUtil.decrypt(ciphertext, secretKey, iv);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }
}

