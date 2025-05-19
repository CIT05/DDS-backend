package com.example.chatty_be;

import static android.util.Base64.NO_WRAP;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.adapter.ChatRecyclerAdapter;
import com.example.chatty_be.adapter.SearchUserRecyclerAdapter;
import com.example.chatty_be.crypto.KeyManager;
import com.example.chatty_be.model.ChatMessageModel;
import com.example.chatty_be.model.ChatRoomModel;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.example.chatty_be.utils.KeyUtil;
import com.example.chatty_be.utils.MessagesUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

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
            getOnBackPressedDispatcher().onBackPressed();
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageButton.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty())
                return;

            sendMessageToUser(message);

        }));

        getOrCreateChatRoomModel();

        setupChatRecyclerView();


    }

    void sendMessageToUser(String message) {

        chatRoomModel.setLastMessageTimestamp(Timestamp.now());
        chatRoomModel.setLastMessageSenderId(FirebaseUtil.getCurrentUserId());
        chatRoomModel.setLastMessage(message);
        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);

        ChatSession chatSession = generateChatSession(new FriendRequestManager(this), otherUser.getUserId());

        if(chatSession == null) {
            Log.e("ChatSession", "Failed to generate chat session");
            return;
        }

        EncryptedMessage encrypted = MessagesUtil.encryptMessage(message, chatSession);

        if (encrypted == null) {
            Log.e("ChatEncrypt", "Encryption failed, message not sent");
            return;
        }

        ChatMessageModel chatMessageModel = new ChatMessageModel(
                message,
                encrypted,
                FirebaseUtil.getCurrentUserId(),
                Timestamp.now()
        );

        FirebaseUtil.getChatRoomMessageReference(chatRoomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            messageInput.setText("");
                        }
                    }
                });

    }

    void getOrCreateChatRoomModel() {
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

                    FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);
                }
            }
        });
    }


    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatRoomMessageReference(chatRoomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }


    private ChatSession generateChatSession(FriendRequestManager manager, String userId) {
        try {
            String encodedKey = manager.extractFriendPublicKey(userId);
            if (encodedKey == null || encodedKey.isEmpty()) {
                Log.e("ChatSession", "Encoded key is null");
                return null;
            }

            byte[] rawKey = Base64.decode(encodedKey, NO_WRAP);
            PublicKey friendPublicKey = KeyUtil.decodePublicKey(rawKey);

           return new ChatSession(friendPublicKey);
        } catch (Exception e) {
            Log.e("ChatSession", "Failed to initialize chat session", e);
            return null;
        }
    }


}