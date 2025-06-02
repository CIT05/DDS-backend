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
    private KeyPair myEphemeralKeyPair;
    private PublicKey lastPeerEphemeralKey;
    private byte[] sendingChainKey;
    private byte[] receivingChainKey;

    public static final String TAG = "ChatSession";
    
    private boolean mustUseNewEphemeralForNextSend = false;

    public ChatSession(PublicKey recipientPublicKey) throws Exception {
        this.myEphemeralKeyPair = KeyUtil.generateEphemeralKeyPair();
        this.lastPeerEphemeralKey = recipientPublicKey;

        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myEphemeralKeyPair.getPrivate(), recipientPublicKey);
        Log.d(TAG, "Cryptography - Constructor - My Ephemeral Public Key: " + Base64.encodeToString(myEphemeralKeyPair.getPublic().getEncoded(), Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - Constructor - Recipient Public Key (used for DH): " + Base64.encodeToString(recipientPublicKey.getEncoded(), Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - Constructor - Derived Shared Secret: " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));

        sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
        receivingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-recv".getBytes(StandardCharsets.UTF_8));
        Log.d(TAG, "Cryptography - Constructor - Initial sendingChainKey: " + Base64.encodeToString(sendingChainKey, Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - Constructor - Initial receivingChainKey: " + Base64.encodeToString(receivingChainKey, Base64.NO_WRAP));
    }

    public static ChatSession forReceiving(PrivateKey myPrivateKey, String base64PeerEphemeralKey) throws Exception {
        byte[] raw = Base64.decode(base64PeerEphemeralKey, Base64.NO_WRAP);
        PublicKey receivedPeerKey = KeyUtil.decodePublicKey(raw);

        Log.d(TAG, "Cryptography - forReceiving - My Private Key (used for DH): " + Base64.encodeToString(myPrivateKey.getEncoded(), Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - forReceiving - Received Peer Ephemeral Key (used for DH): " + Base64.encodeToString(receivedPeerKey.getEncoded(), Base64.NO_WRAP));

        ChatSession session = new ChatSession();
        session.lastPeerEphemeralKey = receivedPeerKey;

        byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myPrivateKey, receivedPeerKey);
        Log.d(TAG, "Cryptography - forReceiving - Derived Shared Secret: " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));


        session.receivingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
        session.sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-recv".getBytes(StandardCharsets.UTF_8));

        Log.d(TAG, "Cryptography - forReceiving - Initialized receivingChainKey: " + Base64.encodeToString(session.receivingChainKey, Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - forReceiving - Initialized sendingChainKey: " + Base64.encodeToString(session.sendingChainKey, Base64.NO_WRAP));

        session.mustUseNewEphemeralForNextSend = true;
        return session;
    }

    private ChatSession() {}

    public byte[] getMyEphemeralPublicKeyEncoded() {
        return KeyUtil.encodePublicKey(myEphemeralKeyPair.getPublic());
    }

    public void applyPeerEphemeralKeyIfChanged(PublicKey newPeerKey, PrivateKey myPrivateKey) throws Exception {
        Log.d(TAG, "Cryptography - applyPeerEphemeralKeyIfChanged - Current lastPeerEphemeralKey: " + Base64.encodeToString(lastPeerEphemeralKey.getEncoded(), Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - applyPeerEphemeralKeyIfChanged - New Peer Ephemeral Key: " + Base64.encodeToString(newPeerKey.getEncoded(), Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - applyPeerEphemeralKeyIfChanged - My Private Key (used for DH): " + Base64.encodeToString(myPrivateKey.getEncoded(), Base64.NO_WRAP));

        if (!newPeerKey.equals(lastPeerEphemeralKey)) {
            Log.d(TAG, "Cryptography - Peer ephemeral key changed. Updating receiving chain.");
            byte[] newSharedSecret = ECDHUtil.deriveSharedSecret(myPrivateKey, newPeerKey);
            Log.d(TAG, "Cryptography - applyPeerEphemeralKeyIfChanged - New Derived Shared Secret: " + Base64.encodeToString(newSharedSecret, Base64.NO_WRAP));
            receivingChainKey = HMACUtil.hmacSHA256(newSharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
            Log.d(TAG, "Cryptography - applyPeerEphemeralKeyIfChanged - Updated receivingChainKey: " + Base64.encodeToString(receivingChainKey, Base64.NO_WRAP));


            lastPeerEphemeralKey = newPeerKey;

            mustUseNewEphemeralForNextSend = true;
        }
        else {
            Log.d(TAG, "Cryptography - Peer ephemeral key unchanged. No update needed.");
        }
    }

    public void rotateMyEphemeralKeyIfNeeded(PublicKey peerKey) throws Exception {
        Log.d(TAG, "Cryptography - rotateMyEphemeralKeyIfNeeded - mustUseNewEphemeralForNextSend: " + mustUseNewEphemeralForNextSend);

        if (mustUseNewEphemeralForNextSend) {
            this.myEphemeralKeyPair = KeyUtil.generateEphemeralKeyPair();
            Log.d(TAG, "Cryptography - rotateMyEphemeralKeyIfNeeded - New My Ephemeral Public Key: " + Base64.encodeToString(myEphemeralKeyPair.getPublic().getEncoded(), Base64.NO_WRAP));
            Log.d(TAG, "Cryptography - rotateMyEphemeralKeyIfNeeded - Peer Public Key (used for DH): " + Base64.encodeToString(peerKey.getEncoded(), Base64.NO_WRAP));

            byte[] sharedSecret = ECDHUtil.deriveSharedSecret(myEphemeralKeyPair.getPrivate(), peerKey);
            Log.d(TAG, "Cryptography - rotateMyEphemeralKeyIfNeeded - Derived Shared Secret: " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));


            sendingChainKey = HMACUtil.hmacSHA256(sharedSecret, "init-send".getBytes(StandardCharsets.UTF_8));
            Log.d(TAG, "Cryptography - rotateMyEphemeralKeyIfNeeded - Updated sendingChainKey: " + Base64.encodeToString(sendingChainKey, Base64.NO_WRAP));
            mustUseNewEphemeralForNextSend = false;
        }
    }

    public byte[] getNextSendAESKey() throws Exception {
        Log.d(TAG, "Cryptography - getNextSendAESKey - Current sendingChainKey (before ratchet): " + Base64.encodeToString(sendingChainKey, Base64.NO_WRAP));
        sendingChainKey = HMACUtil.hmacSHA256(sendingChainKey, "ratchet".getBytes(StandardCharsets.UTF_8));    Log.d(TAG, "Cryptography - getNextSendAESKey - sendingChainKey (after ratchet): " + Base64.encodeToString(sendingChainKey, Base64.NO_WRAP));
        Log.d(TAG, "Cryptography - getNextSendAESKey - sendingChainKey (after ratchet): " + Base64.encodeToString(sendingChainKey, Base64.NO_WRAP));
        byte[] messageKey = HMACUtil.hmacSHA256(sendingChainKey, "message".getBytes(StandardCharsets.UTF_8));
        Log.d(TAG, "Cryptography - getNextReceiveAESKey - Derived Message AES Key: " + Base64.encodeToString(messageKey, Base64.NO_WRAP));
        return messageKey;
    }

    public byte[] getNextReceiveAESKey() throws Exception {
        Log.d(TAG, "Cryptography - getNextReceiveAESKey - Current receivingChainKey (before ratchet): " + Base64.encodeToString(receivingChainKey, Base64.NO_WRAP));
        receivingChainKey = HMACUtil.hmacSHA256(receivingChainKey, "ratchet".getBytes(StandardCharsets.UTF_8));
        Log.d(TAG, "Cryptography - getNextReceiveAESKey - receivingChainKey (after ratchet): " + Base64.encodeToString(receivingChainKey, Base64.NO_WRAP));
        byte[] messageKey = HMACUtil.hmacSHA256(receivingChainKey, "message".getBytes(StandardCharsets.UTF_8));
        Log.d(TAG, "Cryptography - getNextReceiveAESKey - Derived Message AES Key: " + Base64.encodeToString(messageKey, Base64.NO_WRAP));
        return messageKey;
    }
}
