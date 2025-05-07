package com.example.chatty_be;

import android.os.Bundle;
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
import com.example.chatty_be.model.ChatMessageModel;
import com.example.chatty_be.model.ChatRoomModel;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

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
        chatRoomId  = FirebaseUtil.getChatRoomId(FirebaseUtil.getCurrentUserId(), otherUser.getUserId());

        messageInput= findViewById(R.id.chat_message_input);
        sendMessageButton= findViewById(R.id.message_send_btn);
        backButton = findViewById(R.id.back_button_chat);
        otherUsername =findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);

        backButton.setOnClickListener((v)->{
            getOnBackPressedDispatcher().onBackPressed();
        });

        otherUsername.setText(otherUser.getUsername());

        sendMessageButton.setOnClickListener((v->{
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty())
                return;

            sendMessageToUser(message);

        }));

        getOrCreateChatRoomModel();

        setupChatRecyclerView();


    }

    void sendMessageToUser(String message){

        chatRoomModel.setLastMessageTimestamp(Timestamp.now());
        chatRoomModel.setLastMessageSenderId(FirebaseUtil.getCurrentUserId());
        FirebaseUtil.getChatRoomReference(chatRoomId).set(chatRoomModel);


        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FirebaseUtil.getCurrentUserId(), Timestamp.now());

        FirebaseUtil.getChatRoomMessageReference(chatRoomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if(task.isSuccessful()){
                            messageInput.setText("");
                        }
                    }
                });

    }

    void getOrCreateChatRoomModel(){
        FirebaseUtil.getChatRoomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                chatRoomModel = task.getResult().toObject(ChatRoomModel.class);
                if(chatRoomModel == null ){
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


    void setupChatRecyclerView(){
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


}