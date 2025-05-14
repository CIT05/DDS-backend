package com.example.chatty_be.crypto;          // keep the package you’re using

import android.util.Base64;
import android.util.Log;

import com.example.chatty_be.utils.KdfUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class ChatSession {

    private static final String COLLECTION_EPHEMERALS = "ephemeralKeys";
    private static final String TAG = "ChatSession";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String peerUid;
    private final String myUid = FirebaseAuth.getInstance().getUid();
    private final boolean isInitiator;

    private KeyPair myEphemeral;
    private ChainKey sendChain;
    private ChainKey recvChain;

    private int sendCounter = 1;
    private int recvCounter = 1;

    private final Map<Integer, byte[]> messageKeyCache = new HashMap<>();
    private final int maxCacheSize = 50;

    public ChatSession(String peerUid) {
        this.peerUid = peerUid;
        this.isInitiator = myUid.compareTo(peerUid) < 0; // deterministic, consistent on both sides
    }

    public com.google.android.gms.tasks.Task<Void> initialise() {
        Executor bg = Executors.newSingleThreadExecutor();

        return Tasks.call(bg, () -> {
            myEphemeral = KeyGenerator.generateEphemeralKeyPair();

            Map<String, Object> doc = new HashMap<>();
            doc.put("from", myUid);
            doc.put("pub", toBase64(myEphemeral.getPublic()));
            doc.put("ts", FieldValue.serverTimestamp());

            Tasks.await(db.collection(COLLECTION_EPHEMERALS)
                    .document(peerUid)
                    .set(doc));

            DocumentSnapshot snap = Tasks.await(
                    db.collection(COLLECTION_EPHEMERALS)
                            .document(myUid)
                            .get()
            );

            PublicKey theirPub;

            if (snap.exists() && snap.contains("pub")) {
                Log.d(TAG, "Using peer ephemeral key");
                theirPub = fromBase64(snap.getString("pub"));
            } else {
                Log.d(TAG, "Falling back to peer identity key");
                DocumentSnapshot userSnap = Tasks.await(
                        db.collection("users").document(peerUid).get());

                if (!userSnap.exists() || !userSnap.contains("identityPub")) {
                    throw new IllegalStateException("Peer has no identity key available.");
                }

                theirPub = fromBase64(userSnap.getString("identityPub"));
            }

            byte[] sharedSecret = KeyGenerator.computeSharedSecret(myEphemeral.getPrivate(), theirPub);

            if (isInitiator) {
                sendChain = new ChainKey(KdfUtils.hmacSha256(sharedSecret, "sending"));
                recvChain = new ChainKey(KdfUtils.hmacSha256(sharedSecret, "receiving"));
            } else {
                sendChain = new ChainKey(KdfUtils.hmacSha256(sharedSecret, "receiving"));
                recvChain = new ChainKey(KdfUtils.hmacSha256(sharedSecret, "sending"));
            }

            Log.d(TAG, "ChatSession initialized successfully with " + peerUid);
            return null;
        });
    }

        public EncryptedMessage encrypt(byte[] plaintext) throws GeneralSecurityException {
        byte[] key = sendChain.nextMessageKey();
        byte[] iv = randomIv();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, iv));

        byte[] ciphertext = cipher.doFinal(plaintext);
        int ratchetIndex = sendCounter++;

        return new EncryptedMessage(ciphertext, iv, ratchetIndex);
    }

    public byte[] decrypt(byte[] cipher, byte[] iv, int ratchetIndex) throws GeneralSecurityException {
        if (recvChain == null) {
            throw new IllegalStateException("recvChain not initialized yet");
        }

        byte[] key;

        if (ratchetIndex < recvCounter) {
            key = messageKeyCache.get(ratchetIndex);
            if (key == null) {
                Log.w(TAG, "Message too old — missing key for index " + ratchetIndex);
                return "[Expired message]".getBytes(StandardCharsets.UTF_8);
            }
        } else {
            while (recvCounter < ratchetIndex) {
                byte[] skippedKey = recvChain.nextMessageKey();
                messageKeyCache.put(recvCounter, skippedKey);

                if (messageKeyCache.size() > maxCacheSize) {
                    int min = messageKeyCache.keySet().stream().min(Integer::compareTo).orElse(-1);
                    if (min != -1) messageKeyCache.remove(min);
                }
                recvCounter++;
            }

            key = recvChain.nextMessageKey();
            recvCounter++;
        }

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, iv));
        return c.doFinal(cipher);
    }

    public boolean isReady() {
        return sendChain != null && recvChain != null;
    }


    private static byte[] randomIv() {
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }

    private static String toBase64(PublicKey k) {
        return Base64.encodeToString(k.getEncoded(), Base64.NO_WRAP);
    }

    private static PublicKey fromBase64(String b64) throws Exception {
        byte[] der = Base64.decode(b64, Base64.NO_WRAP);
        return java.security.KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(der));
    }
}
