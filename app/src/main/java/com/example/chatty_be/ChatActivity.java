package com.example.chatty_be;

import static android.util.Base64.NO_WRAP;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.adapter.ChatRecyclerAdapter;
import com.example.chatty_be.crypto.KeyManager;
import com.example.chatty_be.model.ChatMessageModel;
import com.example.chatty_be.model.ChatRoomModel;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.example.chatty_be.utils.KeyUtil;
import com.example.chatty_be.utils.MessagesUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import org.webrtc.*;

import com.google.firebase.firestore.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String chatRoomId;
    ChatRoomModel chatRoomModel;

    ChatRecyclerAdapter adapter;
    EditText messageInput;
    ImageButton sendMessageButton;
    ImageButton backButton;
    TextView otherUsername;
    RecyclerView recyclerView;

    //This is for WebRTC
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel;
    private DocumentReference chatroomReference;
    private ListenerRegistration chatRoomSnap;

    private ListenerRegistration callerIceSnap;

    private ListenerRegistration calleeIceSnap;

    private PrivateKey privateKey;

    private ListenerRegistration activeListener;
    private boolean isPeerInRoom = false;

    private boolean iAmInRoom = false;

    @Override
    protected void onStart(){
        super.onStart();
        getOrCreateChatRoomModel(() -> {
            enterRoomPresence();
        });
    }
    @Override
    protected void onStop(){
        super.onStop();
        leaveRoomPresence();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        otherUser = AndroidUtil.getuserModelFromIntent(getIntent());

        FriendRequestManager manager = new FriendRequestManager(this);

        chatRoomId = FirebaseUtil.getChatRoomId(FirebaseUtil.getCurrentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageButton = findViewById(R.id.message_send_btn);
        backButton = findViewById(R.id.back_button_chat);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);

        backButton.setOnClickListener((v) -> {
            hangUp();
            getOnBackPressedDispatcher().onBackPressed();
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageButton.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty())
                return;

            sendMessageToUser(message);

        }));

        FriendRequestManager friendRequestManager = new FriendRequestManager(this);
        friendRequestManager.checkFriendPublicKeyAndFetchItIfNeeded(otherUser.getUserId());

        setupWebRTC();

        setupChatRecyclerView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hangUp();
    }

    void sendMessageToUser(String message) {
        chatRoomModel.setLastMessageTimestamp(Timestamp.now());
        chatRoomModel.setLastMessageSenderId(FirebaseUtil.getCurrentUserId());
        chatRoomModel.setLastMessage(message);
        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);

        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) {
            Log.e("ChatDataChannel", "Data channel not ready. State: " + (dataChannel != null ? dataChannel.state() : "null"));
            return;
        }

        ChatSession chatSession;
        try {
            FriendRequestManager manager = new FriendRequestManager(this);
            chatSession = ChatSessionStorage.getSendSession(chatRoomModel.getChatRoomId(), otherUser.getUserId(), manager);

            if (chatSession == null) {
                Log.e("ChatSession", "Send session is null! Aborting message send.");
                return;
            }

            String base64PeerKey = manager.extractFriendPublicKey(otherUser.getUserId());
            byte[] rawPeerKey = Base64.decode(base64PeerKey, NO_WRAP);
            PublicKey peerPublicKey = KeyUtil.decodePublicKey(rawPeerKey);
            chatSession.rotateMyEphemeralKeyIfNeeded(peerPublicKey);

        } catch (Exception e) {
            Log.e("ChatSession", "Failed to initialize session", e);
            return;
        }

        EncryptedMessage encrypted = MessagesUtil.encryptMessage(message, chatSession);

        if (encrypted == null) {
            Log.e("ChatEncrypt", "Encryption failed, message not sent");
            return;
        }

        Log.d("SenderLog", "Ciphertext: " + encrypted.getCiphertext());
        Log.d("SenderLog", "IV: " + encrypted.getIv());
        Log.d("SenderLog", "Ephemeral Key: " + encrypted.getEphemeralPublicKey());

        ChatMessageModel chatMessageModel = new ChatMessageModel(
                message,
                encrypted,
                FirebaseUtil.getCurrentUserId(),
                Timestamp.now()
        );

        String payload = MessagesUtil.convertChatMessageModelToPayload(chatMessageModel);
        dataChannel.send(new DataChannel.Buffer(
                ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8)),
                false
        ));

        messageInput.setText("");
    }

    void getOrCreateChatRoomModel(Runnable onComplete) {
        FirebaseUtil.getChatRoomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatRoomModel = task.getResult().toObject(ChatRoomModel.class);
                if (chatRoomModel == null) {
                    // first time chat
                    chatRoomModel = new ChatRoomModel(
                            chatRoomId,
                            Arrays.asList(FirebaseUtil.getCurrentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel).addOnCompleteListener(setTask -> {
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    });
                } else {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            } else {
                Log.e("ChatActivity", "Error getting or creating chat room", task.getException());
            }
        });
    }

    void setupChatRecyclerView() {
        adapter = new ChatRecyclerAdapter(getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.scrollToPosition(0);
            }
        });
    }


    private void setupWebRTC() {


        try {
            privateKey = KeyManager.getPrivateKey(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions();

        PeerConnectionFactory.initialize(options);

        peerConnectionFactory = PeerConnectionFactory.builder()
                .createPeerConnectionFactory();

        boolean iAmCaller = FirebaseUtil.getCurrentUserId().compareTo(otherUser.getUserId()) < 0;

        chatroomReference = FirebaseUtil.getChatRoomReference(chatRoomId);

        List<PeerConnection.IceServer> iceServers = Collections.singletonList(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                        .createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                String sub = iAmCaller ? "callerCandidates" : "calleeCandidates";
                chatroomReference.collection(sub).add(toMap(iceCandidate));
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

            }

            @Override
            public void onAddStream(MediaStream mediaStream) {

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dc) {
                Log.d("WebRTC-DEBUG", "onDataChannel triggered!"); // Added log
                Log.d("WebRTC", "Data channel received by callee!");
                dataChannel = dc;
                attachDataChannelCallbacks(dataChannel);
            }
            @Override
            public void onRenegotiationNeeded() {

            }
        });
        Log.d("WebRTC-SETUP", "PeerConnection created"); // Added log

        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) {
            Log.e("ChatDataChannel", "Data channel is null or not open, cannot send message. State: " + (dataChannel != null ? dataChannel.state() : "null"));
            return;
        }

        if (iAmCaller) {
            dataChannel = peerConnection.createDataChannel("chat", new DataChannel.Init());
            attachDataChannelCallbacks(dataChannel);
            Log.d("WebRTC", "Caller created data channel.");
        }

        if (iAmCaller) {
            peerConnection.createOffer(new SdpAdapter("offer") {
                @Override
                public void onCreateSuccess(SessionDescription offer) {
                    peerConnection.setLocalDescription(new SdpAdapter("local"), offer);
                    chatroomReference.set(Collections.singletonMap("offer", toMap(offer)));

                }
            }, new MediaConstraints());

            chatRoomSnap = chatroomReference.addSnapshotListener((snapshot, event) -> {
                if (snapshot != null && snapshot.contains("answer") && peerConnection.getRemoteDescription() == null) {
                    Map<String, String> answerMap = (Map<String, String>) snapshot.get("answer");

                    SessionDescription sd = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(answerMap.get("type")),
                            answerMap.get("sdp"));

                    peerConnection.setRemoteDescription(new SdpAdapter("remote"), sd);
                }
            });

            calleeIceSnap = chatroomReference.collection("calleeCandidates")
                    .addSnapshotListener(new IceListener());


        } else {
            callerIceSnap = chatroomReference.collection("callerCandidates")
                    .addSnapshotListener(new IceListener());

            chatroomReference.get().addOnSuccessListener(snapshot -> {
                Map<String, String> offerMap = (Map<String, String>) snapshot.get("offer");

                if (offerMap != null && offerMap.get("sdp") != null) {
                    SessionDescription sdOffer = new SessionDescription(
                            SessionDescription.Type.OFFER,
                            offerMap.get("sdp"));

                    peerConnection.setRemoteDescription(new SdpAdapter("remote"), sdOffer);

                    peerConnection.createAnswer(new SdpAdapter("answer") {
                        @Override
                        public void onCreateSuccess(SessionDescription answer) {
                            peerConnection.setLocalDescription(new SdpAdapter("local"), answer);
                            chatroomReference.update("answer", toMap(answer));
                        }
                    }, new MediaConstraints());
                } else {
                    Log.e("ChatActivity", "Offer not found in chat room snapshot");
                }
            });
        }
        ;
    }

    private void attachDataChannelCallbacks(DataChannel dc) {
        dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                byte[] bytes = new byte[buffer.data.remaining()];
                buffer.data.get(bytes);
                String json = new String(bytes, StandardCharsets.UTF_8);

                ChatMessageModel receivedMessageModel = MessagesUtil.convertPayloadToChatMessageModel(json);

                EncryptedMessage enc = receivedMessageModel.getEncryptedMessage();

                String senderId = receivedMessageModel.getSendeerId();

                try {
                    ChatSession session = ChatSessionStorage.getReceiveSession(
                            senderId,
                            enc.getEphemeralPublicKey(),
                            privateKey
                    );

                    String plaintext = MessagesUtil.decryptMessage(enc.getCiphertext(), enc.getIv(), session);

                    runOnUiThread(() -> adapter.addMessage(
                            new ChatMessageModel(plaintext, enc,
                                    otherUser.getUserId(), Timestamp.now())
                    ));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }


            }

            @Override
            public void onBufferedAmountChange(long l) {
            }


            @Override
            public void onStateChange() {
                Log.d("DC", "Data Channel State: " + dc.state());
                if (dc.state() == DataChannel.State.OPEN) {
                    Log.d("DC", "Data Channel is now OPEN!");
                }
            }
        });
    }

    private void hangUp() {
        try {
            if (chatRoomSnap != null) chatRoomSnap.remove();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error removing chat room snapshot listener", e);
        }
        try {
            if (callerIceSnap != null) callerIceSnap.remove();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error removing caller ICE snapshot listener", e);
        }
        try {
            if (calleeIceSnap != null) calleeIceSnap.remove();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error removing callee ICE snapshot listener", e);
        }
        try {
            if (dataChannel != null) dataChannel.close();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error closing data channel", e);
        }
        try {
            if (peerConnection != null) peerConnection.close();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error closing peer connection", e);
        }

    }

    private Map<String, Object> toMap(SessionDescription sessionDescription) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", sessionDescription.type.canonicalForm());
        map.put("sdp", sessionDescription.description);
        return map;
    }

    private Map<String, Object> toMap(IceCandidate iceCandidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("sdpMid", iceCandidate.sdpMid);
        map.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
        map.put("candidate", iceCandidate.sdp);
        return map;
    }

    private class IceListener implements EventListener<QuerySnapshot> {
        @Override
        public void onEvent(QuerySnapshot snap, FirebaseFirestoreException error) {
            if (error != null || snap == null) {
                Log.e("ChatActivity", "Error listening for ICE candidates", error);
                return;
            }

            for (DocumentChange change : snap.getDocumentChanges()) {
                if (change.getType() == DocumentChange.Type.ADDED) {
                    IceCandidate candidate = new IceCandidate(change.getDocument().getString("sdpMid"),
                            Objects.requireNonNull(change.getDocument().getLong("sdpMLineIndex")).intValue(),
                            change.getDocument().getString("candidate"));
                    peerConnection.addIceCandidate(candidate);
                }
            }
        }
    }

    private class SdpAdapter implements SdpObserver {
        private final String tag;

        public SdpAdapter(String tag) {
            this.tag = tag;
        }

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            // Log only the type of the SDP for cleaner output
            Log.d(tag, "SDP created successfully: " + sessionDescription.type.canonicalForm());
        }

        @Override
        public void onSetSuccess() {
            Log.d(tag, "SDP set successfully");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(tag, "SDP creation failed: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(tag, "SDP setting failed: " + s);
        }
    }

    private void enterRoomPresence() {
        String myId = FirebaseUtil.getCurrentUserId();
        String friendId = otherUser.getUserId();
        chatroomReference.update("active", FieldValue.arrayUnion(myId))
                .addOnSuccessListener(r -> {
                    iAmInRoom = true;
                });

        activeListener = chatroomReference.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {return;}

            List<String> active = (List<String>) snap.get("active");
            if (active == null) active = Collections.emptyList();

            boolean newPeerState = active.contains(friendId);
            if (newPeerState != isPeerInRoom) {
                isPeerInRoom = newPeerState;

                if (isPeerInRoom && iAmInRoom) {
                    startOrContinueWebRtc();
                } else if (!isPeerInRoom && peerConnection != null) {
                    hangUp();
                }

                runOnUiThread(() -> {
                    View status = findViewById(R.id.green_dot);
                    status.setVisibility(isPeerInRoom ? View.VISIBLE : View.GONE);
                });
            }
        });
    }

    private void leaveRoomPresence() {
        iAmInRoom = false;
        if (activeListener != null) activeListener.remove();
        chatroomReference.update("active", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));
    }
    private void startOrContinueWebRtc(){
        if (peerConnection == null || dataChannel == null) {
            setupWebRTC();
        }
    }
}