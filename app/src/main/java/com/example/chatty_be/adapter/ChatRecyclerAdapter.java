package com.example.chatty_be.adapter;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.ChatSession;
import com.example.chatty_be.ChatSessionStorage;
import com.example.chatty_be.R;
import com.example.chatty_be.crypto.KeyManager;
import com.example.chatty_be.model.ChatMessageModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.example.chatty_be.utils.KeyUtil;
import com.example.chatty_be.utils.MessagesUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.security.PrivateKey;
import java.security.PublicKey;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {
    Context context;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        if(model.getSendeerId() != null && model.getSendeerId().equals(FirebaseUtil.getCurrentUserId())){
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextView.setText(model.getMessage());

        }else{


            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);

            String decryptedMessage = "[Encrypted]";
            if (model.getEncryptedMessage() != null) {
                try {
                    String senderId = model.getSendeerId();

                    ChatSession session = ChatSessionStorage.getReceiveSession(
                            senderId,
                            model.getEncryptedMessage().getEphemeralPublicKey(),
                            KeyManager.getPrivateKey(context)
                    );


                    byte[] rawKey = Base64.decode(model.getEncryptedMessage().getEphemeralPublicKey(), Base64.NO_WRAP);
                    PublicKey peerEphemeralKey = KeyUtil.decodePublicKey(rawKey);
                    PrivateKey myPrivateKey = KeyManager.getPrivateKey(context);
                    session.applyPeerEphemeralKeyIfChanged(peerEphemeralKey, myPrivateKey);

                    Log.d("ReceiverLog", "Incoming message from: " + model.getSendeerId());

                    Log.d("ReceiverLog", "Base64 Ephemeral: " + model.getEncryptedMessage().getEphemeralPublicKey());
                    Log.d("ReceiverLog", "Base64 IV: " + model.getEncryptedMessage().getIv());
                    Log.d("ReceiverLog", "Base64 Ciphertext: " + model.getEncryptedMessage().getCiphertext());

                    session.applyPeerEphemeralKeyIfChanged(peerEphemeralKey, myPrivateKey);

                    decryptedMessage = MessagesUtil.decryptMessage(

                            model.getEncryptedMessage().getCiphertext(),
                            model.getEncryptedMessage().getIv(),
                            session
                    );

                    System.out.println("Decrypted message: " + decryptedMessage);
                } catch (Exception e) {
                    Log.e("ChatAdapter", "Decryption failed", e);
                    decryptedMessage = "[Decryption failed]";
                }
            }

            holder.leftChatTextView.setText(decryptedMessage);
        }

    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder{

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextView, rightChatTextView;


      

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout  = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout  = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextView  = itemView.findViewById(R.id.left_chat_text_view);
            rightChatTextView  = itemView.findViewById(R.id.right_chat_text_view);
        }
    }
}
