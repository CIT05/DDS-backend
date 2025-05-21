package com.example.chatty_be;

import android.util.Base64;

import com.example.chatty_be.utils.ECDHUtil;
import com.example.chatty_be.utils.HMACUtil;
import com.example.chatty_be.utils.KeyUtil;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ChatSession {
    private KeyPair myEphemeralKeyPair;
    private PublicKey lastPeerEphemeralKey;
    private byte[] sendingChainKey;
    private byte[] receivingChainKey;

    private boolean mustUseNewEphemeralForNextSend = false;

    public ChatSession(PublicKey recipientPublicKey) throws Exception {
        this.myEphemeralKeyPair = KeyUtil.generateEphemeralKeyPair();
        this.lastPeerEphemeralKey = recipientPublicKey;

        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myEphemeralKeyPair.getPrivate(), recipientPublicKey);

        sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
        receivingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-recv".getBytes(StandardCharsets.UTF_8));
    }

    public static ChatSession forReceiving(PrivateKey myPrivateKey, String base64PeerEphemeralKey) throws Exception {
        byte[] raw = Base64.decode(base64PeerEphemeralKey, Base64.NO_WRAP);
        PublicKey receivedPeerKey = KeyUtil.decodePublicKey(raw);

        ChatSession session = new ChatSession();
        session.lastPeerEphemeralKey = receivedPeerKey;

        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myPrivateKey, receivedPeerKey);

        session.receivingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
        session.sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-recv".getBytes(StandardCharsets.UTF_8));

        session.mustUseNewEphemeralForNextSend = true;
        return session;
    }

    private ChatSession() {}

    public byte[] getMyEphemeralPublicKeyEncoded() {
        return KeyUtil.encodePublicKey(myEphemeralKeyPair.getPublic());
    }

    public void applyPeerEphemeralKeyIfChanged(PublicKey newPeerKey, PrivateKey myPrivateKey) throws Exception {
        if (!newPeerKey.equals(lastPeerEphemeralKey)) {
            byte[] newSharedSecret = ECDHUtil.deriveSharedSecret(myPrivateKey, newPeerKey);
            receivingChainKey = HMACUtil.hmacSHA256(newSharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
            lastPeerEphemeralKey = newPeerKey;

            mustUseNewEphemeralForNextSend = true;
        }
    }

    public void rotateMyEphemeralKeyIfNeeded(PublicKey peerKey) throws Exception {
        if (mustUseNewEphemeralForNextSend) {
            this.myEphemeralKeyPair = KeyUtil.generateEphemeralKeyPair();
            byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myEphemeralKeyPair.getPrivate(), peerKey);
            sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
            mustUseNewEphemeralForNextSend = false;
        }
    }

    public byte[] getNextSendAESKey() throws Exception {
        sendingChainKey = HMACUtil.hmacSHA256(sendingChainKey, "ratchet".getBytes(StandardCharsets.UTF_8));
        return HMACUtil.hmacSHA256(sendingChainKey, "message".getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getNextReceiveAESKey() throws Exception {
        receivingChainKey = HMACUtil.hmacSHA256(receivingChainKey, "ratchet".getBytes(StandardCharsets.UTF_8));
        return HMACUtil.hmacSHA256(receivingChainKey, "message".getBytes(StandardCharsets.UTF_8));
    }
}
