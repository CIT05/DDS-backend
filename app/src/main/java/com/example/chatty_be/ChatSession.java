package com.example.chatty_be;

import com.example.chatty_be.utils.ECDHUtil;
import com.example.chatty_be.utils.HMACUtil;
import com.example.chatty_be.utils.KeyUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ChatSession {

    private final PrivateKey ephemeralPrivateKey;
    private final PublicKey ephemeralPublicKey;
    private final PublicKey recipientPublicKey;
    //private byte[] chainKey;
    private byte[] sharedSecret;

    public ChatSession(PublicKey recipientPublicKey) throws Exception {
        this.recipientPublicKey = recipientPublicKey;

        // Generate ephemeral keys
        KeyPair ephemeralKeys = KeyUtil.generateEphemeralKeyPair();
        this.ephemeralPrivateKey = ephemeralKeys.getPrivate();
        this.ephemeralPublicKey = ephemeralKeys.getPublic();

        // Compute shared secret using ECDH
        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(ephemeralPrivateKey, recipientPublicKey);

        // Initialize chain key
        //this.chainKey = HMACUtil.hmacSHA256(sharedSecret, "init".getBytes(StandardCharsets.UTF_8));
        this.sharedSecret = ECDHUtil.deriveSharedSecret(ephemeralPrivateKey, recipientPublicKey);
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
