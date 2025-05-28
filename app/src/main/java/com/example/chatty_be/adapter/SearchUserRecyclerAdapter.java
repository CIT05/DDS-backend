package com.example.chatty_be.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.ChatActivity;
import com.example.chatty_be.FriendRequestManager;
import com.example.chatty_be.R;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder> {
    Context context;
    String currentUserId;
    FriendRequestManager friendRequestManager;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options, Context context) {
        super(options);
        this.context = context;
        this.currentUserId = FirebaseUtil.getCurrentUserId();
        this.friendRequestManager = new FriendRequestManager(context);
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserModel model) {
        holder.usernameText.setText(model.getUsername());
        holder.phoneNumber.setText(model.getPhone());

        if (model.getUserId().equals(currentUserId)) {
            holder.usernameText.setText(model.getUsername() + " (Me)");
            holder.actionButton.setVisibility(View.GONE); // Hide button for current user
        } else {
            holder.actionButton.setVisibility(View.VISIBLE);
            // Check friendship status
            FirebaseUtil.getFriendDocumentRef(currentUserId, model.getUserId()).get().addOnSuccessListener(friendDocSnapshot -> {
                if (friendDocSnapshot.exists()) {
                    holder.actionButton.setText("Chat");
                    holder.actionButton.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ChatActivity.class);
                        AndroidUtil.passUserModelAsIntent(intent, model);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    });
                } else {
                    // Check if a friend request is pending (incoming or outgoing)
                    FirebaseUtil.getFriendRequestDocumentRef(currentUserId, model.getUserId()).get().addOnSuccessListener(outgoingRequestSnapshot -> {
                        FirebaseUtil.getFriendRequestDocumentRef(model.getUserId(), currentUserId).get().addOnSuccessListener(incomingRequestSnapshot -> {
                            if (outgoingRequestSnapshot.exists() && "pending".equals(outgoingRequestSnapshot.getString("status"))) {
                                holder.actionButton.setText("Requested");
                                holder.actionButton.setEnabled(false);
                            } else if (incomingRequestSnapshot.exists() && "pending".equals(incomingRequestSnapshot.getString("status"))) {
                                holder.actionButton.setText("Respond");
                                holder.actionButton.setEnabled(true);
                                holder.actionButton.setOnClickListener(v -> {
                                    // Navigate to friend requests screen to respond
                                    // You might need to use an interface/callback to inform the activity
                                    // to navigate. For now, a Toast.
                                    Toast.makeText(context, "Go to friend requests to respond.", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                holder.actionButton.setText("Add Friend");
                                holder.actionButton.setEnabled(true);
                                holder.actionButton.setOnClickListener(v -> {
                                    friendRequestManager.sendFriendRequest(model.getUserId(), success -> {
                                        if (success) {
                                            Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                            holder.actionButton.setText("Requested");
                                            holder.actionButton.setEnabled(false);
                                        } else {
                                            Toast.makeText(context, "Failed to send request.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                });
                            }
                        }).addOnFailureListener(e -> Toast.makeText(context, "Error checking incoming request.", Toast.LENGTH_SHORT).show());
                    }).addOnFailureListener(e -> Toast.makeText(context, "Error checking outgoing request.", Toast.LENGTH_SHORT).show());
                }
            }).addOnFailureListener(e -> Toast.makeText(context, "Error checking friendship.", Toast.LENGTH_SHORT).show());

            holder.itemView.setOnClickListener(v -> {
                // Only navigate to chat if already friends (redundant with button click, can be removed if preferred)
                FirebaseUtil.getFriendDocumentRef(currentUserId, model.getUserId()).get().addOnSuccessListener(friendDocSnapshot -> {
                    if (friendDocSnapshot.exists()) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        AndroidUtil.passUserModelAsIntent(intent, model);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                });
            });
        }
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false);
        return new UserModelViewHolder(view);
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView phoneNumber;
        ImageView profilePic;
        Button actionButton;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            phoneNumber = itemView.findViewById(R.id.phone_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }
}