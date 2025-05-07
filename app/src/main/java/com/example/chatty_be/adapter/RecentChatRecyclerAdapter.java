package com.example.chatty_be.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.ChatActivity;
import com.example.chatty_be.R;
import com.example.chatty_be.model.ChatRoomModel;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.ui.chat.ChatFragment;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatRoomModel, RecentChatRecyclerAdapter.ChatRoomModelViewHolder> {
    Context context;
    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatRoomModel> options, Context context ) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatRoomModelViewHolder holder, int position, @NonNull ChatRoomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task -> {
                    System.out.println(task);
                    if(task.isSuccessful()){
                        boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.getCurrentUserId());

                        UserModel otherUserModel = task.getResult().toObject(UserModel.class);
                        holder.usernameText.setText(otherUserModel.getUsername());
                        if(lastMessageSentByMe) {
                            holder.lastMessageText.setText("You: " + model.getLastMessage());
                        }else {
                            holder.lastMessageText.setText(model.getLastMessage());
                        }
                        holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

                        holder.itemView.setOnClickListener(v->{

                            Intent intent = new Intent(context, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);

                        });
                    }
                });

    }

    @NonNull
    @Override
    public ChatRoomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatRoomModelViewHolder(view);
    }

    class ChatRoomModelViewHolder extends RecyclerView.ViewHolder{
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;

        public ChatRoomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);


        }
    }
}
