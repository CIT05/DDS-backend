package com.example.chatty_be;

import android.util.Base64;
import android.util.Log;
import com.example.chatty_be.utils.KeyUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class ChatSessionStorage {

    private static final Map<String, SessionPair> sessions = new HashMap<>();

    public static ChatSession getSendSession(String chatRoomId, String peerId, FriendRequestManager manager) throws Exception {
        SessionPair pair = sessions.get(peerId);

        if (pair != null && pair.sendSession != null) return pair.sendSession;

        String base64 = manager.extractFriendPublicKey(peerId);
        if (base64 == null || base64.isEmpty()) {
            Log.e("ChatSessionStorage", "Peer public key not found for user: " + peerId);
            return null;
        }

        byte[] raw = Base64.decode(base64, Base64.NO_WRAP);
        PublicKey peerKey = KeyUtil.decodePublicKey(raw);

        ChatSession sendSession = new ChatSession(peerKey);

        if (pair == null) {
            pair = new SessionPair(sendSession, null);
            sessions.put(peerId, pair);
        } else {
            pair.sendSession = sendSession;
        }

        return sendSession;
    }

    public static ChatSession getReceiveSession( String peerId, String base64PeerEphemeralKey, PrivateKey myPrivateKey) throws Exception {
        SessionPair pair = sessions.get(peerId);
        if (pair == null || pair.receiveSession == null) {
            ChatSession receiveSession = ChatSession.forReceiving(myPrivateKey, base64PeerEphemeralKey);
            if (pair == null) {
                pair = new SessionPair(null, receiveSession);
                sessions.put(peerId, pair);
            } else {
                pair.receiveSession = receiveSession;
            }
        }
        return pair.receiveSession;
    }

    private static PublicKey decodePeerKey(FriendRequestManager manager, String peerId) throws Exception {
        String base64 = manager.extractFriendPublicKey(peerId);
        byte[] raw = Base64.decode(base64, Base64.NO_WRAP);
        return KeyUtil.decodePublicKey(raw);
    }

    private static class SessionPair {
        ChatSession sendSession;
        ChatSession receiveSession;

        SessionPair(ChatSession send, ChatSession recv) {
            this.sendSession = send;
            this.receiveSession = recv;
        }
    }
}
