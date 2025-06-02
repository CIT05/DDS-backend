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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import org.webrtc.*;

import com.google.firebase.firestore.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private ListenerRegistration activeListener;

    private PrivateKey privateKey;

    private boolean isPeerInRoom = false;

    private boolean iAmInRoom = false;

    private boolean iAmCaller;
    private String tag = "ChatActivity WebRTC";

    private ExecutorService decryptionExecuter;

    private List<IceCandidate> bufferedIceCandidates = new ArrayList<>();
    private boolean isRemoteDescriptionSet = false;


    @Override
    protected void onStart() {
        super.onStart();
        getOrCreateChatRoomModel(() -> {
            enterRoomPresence();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        leaveRoomPresence();
        hangUp();
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


        initializeWebRTCFactory();

        decryptionExecuter = Executors.newSingleThreadExecutor();

        chatroomReference = FirebaseUtil.getChatRoomReference(chatRoomId);
        iAmCaller = FirebaseUtil.getCurrentUserId().compareTo(otherUser.getUserId()) < 0;
        iAmCaller = FirebaseUtil.getCurrentUserId().compareTo(otherUser.getUserId()) < 0;
        Log.d(tag, "Is this user the caller? " + iAmCaller + ". My ID: " + FirebaseUtil.getCurrentUserId() + ", Other ID: " + otherUser.getUserId());

        startWebRTCConnection();

        setupChatRecyclerView();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(decryptionExecuter != null) {
            decryptionExecuter.shutdownNow();
        }
        hangUp();
    }

    void sendMessageToUser(String message) {
        chatRoomModel.setLastMessageTimestamp(Timestamp.now());
        chatRoomModel.setLastMessageSenderId(FirebaseUtil.getCurrentUserId());
        chatRoomModel.setLastMessage(message);
        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);

        if (dataChannel == null || dataChannel.state() != DataChannel.State.OPEN) {
            Log.e(tag, "Data channel not ready. State: " + (dataChannel != null ? dataChannel.state() : "null"));
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


    private void startWebRTCConnection() {

        if (peerConnection != null && peerConnection.signalingState() != PeerConnection.SignalingState.CLOSED) {
            Log.w(tag, "PeerConnection already exists, reusing it.");
            return;
        }

        Log.d(tag, "Starting WebRTC connection...");

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        String xirsysUsername = "XXX";
        String xirsysPassword = "XXX";

        // TURN server configuration --UDP
        iceServers.add(PeerConnection.IceServer.builder("turn:fr-turn2.xirsys.com:80?transport=udp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:fr-turn2.xirsys.com:3478?transport=udp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());

        // TURN server configuration --TCP
        iceServers.add(PeerConnection.IceServer.builder("turn:fr-turn2.xirsys.com:80?transport=tcp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:fr-turn2.xirsys.com:3478?transport=tcp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());

        // TURN server configuration --TLS
        iceServers.add(PeerConnection.IceServer.builder("turns:fr-turn2.xirsys.com:443?transport=tcp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());

        iceServers.add(PeerConnection.IceServer.builder("turns:fr-turn2.xirsys.com:5349?transport=tcp")
                .setUsername(xirsysUsername)
                .setPassword(xirsysPassword)
                .createIceServer());


        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {

            private final String tag_observer = tag + "-Observer";

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(tag_observer, "Signaling State Changed: " + signalingState);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Logging.d(tag, "ICE Connection State Changed: " + iceConnectionState.name());

                switch (iceConnectionState) {
                    case NEW:
                        Log.d(tag, "ICE Connection State: NEW - Initial state.");
                        break;
                    case CHECKING:
                        Log.d(tag, "ICE Connection State: CHECKING - Probing connectivity.");
                        break;
                    case CONNECTED:
                        Log.d(tag, "ICE Connection State: CONNECTED - One or more ICE candidate pairs successfully connected. Media should flow.");
                        // This state is often very brief before COMPLETED
                        break;
                    case COMPLETED:
                        Log.d(tag, "ICE Connection State: COMPLETED - All ICE candidate pairs successfully connected. Optimal media flow.");
                        break;
                    case FAILED:
                        Log.e(tag, "ICE Connection State: FAILED - ICE checks failed. No connectivity or all active pairs failed.");
                        // Potentially trigger a UI update or error handling here
                        break;
                    case DISCONNECTED:
                        Log.w(tag, "ICE Connection State: DISCONNECTED - ICE connectivity lost. May attempt to reconnect.");
                        // This can happen during temporary network fluctuations
                        break;
                    case CLOSED:
                        Log.i(tag, "ICE Connection State: CLOSED - PeerConnection is closed. No further ICE activity.");
                        // This might be the problematic state if it happens unexpectedly
                        break;
                    default:
                        Log.d(tag, "ICE Connection State: " + iceConnectionState.name() + " (Unknown state)");
                        break;
                }

                if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                    Log.e(tag_observer, "ICE Connection FAILED! Check network, firewall, STUN/TURN.");
                }
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(tag_observer, "ICE Candidate Gathered: sdpMid=" + iceCandidate.sdpMid + ", sdpMLineIndex=" + iceCandidate.sdpMLineIndex + ", candidate=" + iceCandidate.sdp);
                String sub = iAmCaller ? "callerCandidates" : "calleeCandidates";
                chatroomReference.collection(sub).add(toMap(iceCandidate))
                        .addOnSuccessListener(docRef -> Log.d(tag_observer, "ICE Candidate uploaded to Firestore: " + docRef.getId()))
                        .addOnFailureListener(e -> Log.e(tag_observer, "Failed to upload ICE Candidate to Firestore", e));
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
                Log.d(tag_observer, "onDataChannel triggered!"); // Added log
                Log.d(tag_observer, "Data channel received by callee!");
                dataChannel = dc;
                attachDataChannelCallbacks(dataChannel);
            }

            @Override
            public void onRenegotiationNeeded() {

            }
        });
        Log.d(tag, "PeerConnection created");

        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        constraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));


        if (iAmCaller) {

            DataChannel.Init dataChannelInit = new DataChannel.Init();
            dataChannel = peerConnection.createDataChannel("chat-channel", dataChannelInit);
            attachDataChannelCallbacks(dataChannel);
            Log.d(tag, "Data channel created by caller: " + dataChannel.label());

            peerConnection.createOffer(new SdpAdapter("offer") {
                @Override
                public void onCreateSuccess(SessionDescription offer) {
                    peerConnection.setLocalDescription(new SdpAdapter("local"), offer);
                    chatroomReference.set(Collections.singletonMap("offer", toMap(offer)));

                }
            }, constraints);

            chatRoomSnap = chatroomReference.addSnapshotListener((snapshot, event) -> {
                if (snapshot != null && snapshot.contains("answer") && peerConnection.getRemoteDescription() == null) {
                    Map<String, String> answerMap = (Map<String, String>) snapshot.get("answer");

                    if (answerMap != null) {
                        SessionDescription sd = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(answerMap.get("type")),
                                answerMap.get("sdp"));

                        peerConnection.setRemoteDescription(new SdpAdapter("remote"), sd);
                    }

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
            private final String tag_dataChannel = tag + "-DataChannel";

            @Override
            public void onBufferedAmountChange(long previousAmount) {
                Log.d(tag_dataChannel, "Data Channel Buffered Amount Change: " + previousAmount);
            }

            @Override
            public void onStateChange() {
                DataChannel.State state = dc.state();
                Log.d(tag_dataChannel, "Data Channel State Changed: " + state);

                if (state == DataChannel.State.OPEN) {
                    Log.d(tag_dataChannel, "Data Channel is OPEN! Ready to send messages.");
                    // Optionally, update UI to enable message sending input field
                    runOnUiThread(() -> {
                        // Example: chatMessageInput.setEnabled(true);
                        // chatSendMessageButton.setEnabled(true);
                        // You might want to visually indicate to the user that chat is ready
                    });
                } else if (state == DataChannel.State.CLOSING || state == DataChannel.State.CLOSED) {
                    Log.w(tag_dataChannel, "Data Channel is CLOSING or CLOSED. Cannot send messages.");
                    runOnUiThread(() -> {
                        // Example: chatMessageInput.setEnabled(false);
                        // chatSendMessageButton.setEnabled(false);
                    });
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {

                if (decryptionExecuter == null || decryptionExecuter.isShutdown()) {
                    return;
                }
                byte[] bytes = new byte[buffer.data.remaining()];
                buffer.data.get(bytes);
                String json = new String(bytes, StandardCharsets.UTF_8);

                ChatMessageModel receivedMessageModel = MessagesUtil.convertPayloadToChatMessageModel(json);

                EncryptedMessage enc = receivedMessageModel.getEncryptedMessage();

                String senderId = receivedMessageModel.getSendeerId();

                decryptionExecuter.execute(() -> {
                    String plaintext = null;

                    try {
                        ChatSession session = ChatSessionStorage.getReceiveSession(
                                senderId,
                                enc.getEphemeralPublicKey(),
                                privateKey
                        );
                        plaintext = MessagesUtil.decryptMessage(enc.getCiphertext(), enc.getIv(), session);
                    } catch (Exception e) {
                        plaintext = "[Undecryptable]";
                    }
                    final String finalPlaintext = plaintext;

                    runOnUiThread(() -> {
                        adapter.addMessage(
                                new ChatMessageModel(finalPlaintext, enc,
                                        otherUser.getUserId(), Timestamp.now())
                        );
                    });
                });
            }
        });
    }


    private void hangUp() {
        try {
            if (chatRoomSnap != null) chatRoomSnap.remove();
        } catch (Exception e) {
            Log.e(tag, "Error removing chat room snapshot listener", e);
        }
        try {
            if (callerIceSnap != null) callerIceSnap.remove();
        } catch (Exception e) {
            Log.e(tag, "Error removing caller ICE snapshot listener", e);
        }
        try {
            if (calleeIceSnap != null) calleeIceSnap.remove();
        } catch (Exception e) {
            Log.e(tag, "Error removing callee ICE snapshot listener", e);
        }
        try {
            if (dataChannel != null) dataChannel.close();
        } catch (Exception e) {
            Log.e(tag, "Error closing data channel", e);
        }
        try {
            if (peerConnection != null) peerConnection.close();
        } catch (Exception e) {
            Log.e(tag, "Error closing peer connection", e);
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
                Log.e(tag, "Error listening for ICE candidates", error);
                return;
            }

            for (DocumentChange change : snap.getDocumentChanges()) {
                if (change.getType() == DocumentChange.Type.ADDED) {
                    IceCandidate candidate = new IceCandidate(change.getDocument().getString("sdpMid"),
                            Objects.requireNonNull(change.getDocument().getLong("sdpMLineIndex")).intValue(),
                            change.getDocument().getString("candidate"));

                    if (isRemoteDescriptionSet) {
                        peerConnection.addIceCandidate(candidate);
                    } else {
                        bufferedIceCandidates.add(candidate);
                    }
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
            Log.d(tag, "SDP created successfully: " + sessionDescription.type.canonicalForm() + "\nSDP: " + sessionDescription.description);
        }

        @Override
        public void onSetSuccess() {
            Log.d(tag, "SDP set successfully. PeerConnection State: " + peerConnection.signalingState());
            if (this.tag.equals("remote")) {
                isRemoteDescriptionSet = true;
                Log.d("WebRTC", "Remote description set, processing buffered ICE candidates.");
                for (IceCandidate candidate : bufferedIceCandidates) {
                    peerConnection.addIceCandidate(candidate);
                    Log.d("WebRTC", "Added buffered ICE candidate: " + candidate.sdp);
                }
                bufferedIceCandidates.clear();
            }
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(tag, "SDP creation failed: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(tag, "SDP setting failed: " + s + " Current PeerConnection state: " + peerConnection.signalingState());
        }
    }

    private void initializeWebRTCFactory() {
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(options);
        //Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);

        try {
            privateKey = KeyManager.getPrivateKey(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get private key for encryption", e);
        }

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();
    }

    private void enterRoomPresence() {
        String myId = FirebaseUtil.getCurrentUserId();

        chatroomReference.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                chatRoomModel = snapshot.toObject(ChatRoomModel.class);
            } else {
                chatRoomModel = new ChatRoomModel(chatRoomId, Arrays.asList(myId, otherUser.getUserId()), Timestamp.now(), "");
                chatroomReference.set(chatRoomModel);
            }
        });

        String friendId = otherUser.getUserId();
        chatroomReference.update("active", FieldValue.arrayUnion(myId))
                .addOnSuccessListener(r -> {
                    iAmInRoom = true;

                    activeListener = chatroomReference.addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null) {
                            return;
                        }

                        List<String> active = (List<String>) snap.get("active");
                        if (active == null) active = Collections.emptyList();

                        Log.d(tag, "Active users in room: " + active);
                        if (!active.contains(myId)) {
                            Log.w(tag, "My ID not found in active users, leaving room presence.");
                            leaveRoomPresence();
                            return;
                        }

                        boolean newPeerState = active.contains(friendId);
                        Log.d(tag, "Peer " + friendId + " in room: " + newPeerState);
                        if (newPeerState != isPeerInRoom) {
                            isPeerInRoom = newPeerState;
                            Log.d(tag, "Peer state changed. Is peer in room: " + isPeerInRoom);

                            runOnUiThread(() -> {
                                View status = findViewById(R.id.green_dot);
                                status.setVisibility(isPeerInRoom ? View.VISIBLE : View.GONE);
                            });
                        }
                    });
                }).addOnFailureListener(r -> {
                    iAmInRoom = false;
                });


    }

    private void leaveRoomPresence() {
        iAmInRoom = false;
        if (activeListener != null) activeListener.remove();
        chatroomReference.update("active", FieldValue.arrayRemove(FirebaseUtil.getCurrentUserId()));
    }
}