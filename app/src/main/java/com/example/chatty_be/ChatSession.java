package com.example.chatty_be;

import android.util.Base64;
import android.util.Log;

import com.example.chatty_be.utils.ECDHUtil;
import com.example.chatty_be.utils.HMACUtil;
import com.example.chatty_be.utils.KeyUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ChatSession {

    private PrivateKey ephemeralPrivateKey;
    private PublicKey ephemeralPublicKey;

    //private byte[] chainKey;
    private byte[] sharedSecret;

    //sending
    public ChatSession(PublicKey recipientPublicKey) throws Exception {
        KeyPair ephemeralKeys = KeyUtil.generateEphemeralKeyPair();
        this.ephemeralPrivateKey = ephemeralKeys.getPrivate();
        this.ephemeralPublicKey = ephemeralKeys.getPublic();
        this.sharedSecret = ECDHUtil.deriveSharedSecret(ephemeralPrivateKey, recipientPublicKey);
    }

    //receiving
    private ChatSession(byte[] sharedSecret, PublicKey senderEphemeralPublicKey) {
        this.sharedSecret = sharedSecret;
        this.ephemeralPrivateKey = null;
        this.ephemeralPublicKey = senderEphemeralPublicKey;
    }

    public static ChatSession forReceiving(PrivateKey receiverPrivateKey, String base64EphemeralSenderKey) throws Exception {
        byte[] raw = Base64.decode(base64EphemeralSenderKey, Base64.NO_WRAP);
        PublicKey senderEphemeralKey = KeyUtil.decodePublicKey(raw);
        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(receiverPrivateKey, senderEphemeralKey);

        return new ChatSession(sharedSecret, senderEphemeralKey);
    }


    public byte[] getEphemeralPublicKeyEncoded() {
        return KeyUtil.encodePublicKey(ephemeralPublicKey);
    }

    public byte[] getNextAESKey() throws Exception {
        // chainKey = HMACUtil.hmacSHA256(chainKey, "ratchet".getBytes(StandardCharsets.UTF_8));
        // return HMACUtil.hmacSHA256(chainKey, "message".getBytes(StandardCharsets.UTF_8));
        return HMACUtil.hmacSHA256(sharedSecret, "aes-key".getBytes(StandardCharsets.UTF_8));
    }


}
