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

import com.example.chatty_be.R;
import com.example.chatty_be.crypto.ChatSession;
import com.example.chatty_be.model.ChatMessageModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.nio.charset.StandardCharsets;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    private final Context context;
    private final ChatSession chatSession;

    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options, Context context, ChatSession chatSession) {
        super(options);
        this.context = context;
        this.chatSession = chatSession;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);

        String cipherBase64 = snapshot.getString("cipher");
        String ivBase64 = snapshot.getString("iv");
        Long indexLong = snapshot.getLong("ratchetIndex");
        String senderId = snapshot.getString("senderId");

        String plainText;

        if (cipherBase64 != null && ivBase64 != null && indexLong != null && chatSession != null && chatSession.isReady()) {
            try {
                byte[] cipher = Base64.decode(cipherBase64, Base64.NO_WRAP);
                byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);
                int ratchetIndex = indexLong.intValue();

                // byte[] plaintextBytes = chatSession.decrypt(cipher, iv, ratchetIndex);
                plainText = new String(cipher, StandardCharsets.UTF_8);
            } catch (Exception e) {
                Log.e("ChatRecycler", "Decryption failed", e);
                plainText = "[Decryption failed]";
            }
        } else {
            plainText = "[Message unavailable]";
            Log.w("ChatRecycler", "ChatSession not ready or fields missing. Skipping decryption.");
        }

        if (senderId != null && senderId.equals(FirebaseUtil.getCurrentUserId())) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextView.setText(plainText);
        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextView.setText(plainText);
        }
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextView, rightChatTextView;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightChatTextView = itemView.findViewById(R.id.right_chat_text_view);
        }
    }
}
