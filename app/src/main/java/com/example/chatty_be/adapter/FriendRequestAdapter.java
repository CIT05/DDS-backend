package com.example.chatty_be.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatty_be.ChatActivity;
import com.example.chatty_be.FriendRequestManager;
import com.example.chatty_be.R;
import com.example.chatty_be.model.FriendRequestItem;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;
import com.example.chatty_be.utils.FirebaseUtil;

import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private final List<FriendRequestItem> requestItems;
    private final Context context;
    private final Runnable refreshCallback;
    private final FriendRequestManager friendRequestManager;

    public FriendRequestAdapter(List<FriendRequestItem> requestItems, Context context, Runnable refreshCallback) {
        this.requestItems = requestItems;
        this.context = context;
        this.refreshCallback = refreshCallback;
        this.friendRequestManager = new FriendRequestManager(context);
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequestItem item = requestItems.get(position);
        holder.usernameText.setText(item.getDisplayName());


        holder.actionButton.setOnClickListener(null);
        holder.actionButton.setEnabled(true);

        if (item.isFriend()) {
            holder.actionButton.setText("Chat");
            holder.actionButton.setOnClickListener(v -> {
                String otherUserId = item.getUid();
                FirebaseUtil.getUserByUserId(otherUserId).get().addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        UserModel otherUserModel = userSnapshot.toObject(UserModel.class);
                        if (otherUserModel != null) {
                            String chatRoomId = FirebaseUtil.getChatRoomId(FirebaseUtil.getCurrentUserId(), otherUserId);
                            FirebaseUtil.getChatRoomReference(chatRoomId).get().addOnSuccessListener(chatRoomDoc -> {
                                if (chatRoomDoc.exists()) {
                                    Intent intent = new Intent(context, ChatActivity.class);
                                    AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                                    context.startActivity(intent);
                                } else {
                                    Toast.makeText(context, "Chatroom not found. Please try again.", Toast.LENGTH_SHORT).show();
                                    // This case should ideally not happen if friendship is established correctly
                                }
                            }).addOnFailureListener(e -> {
                                Toast.makeText(context, "Error checking chatroom: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Toast.makeText(context, "Error: Could not convert user data.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(context, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            });
        } else if ("pending".equals(item.getStatus())) {
            if (item.isIncoming()) {
                holder.actionButton.setText("Accept");
                holder.actionButton.setOnClickListener(v -> {
                    holder.actionButton.setEnabled(false);
                    friendRequestManager.acceptFriendRequest(item.getUid(), success -> {
                        if (success) {
                            Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                            if (refreshCallback != null) {
                                refreshCallback.run(); // Refresh list after accepting
                            }
                        } else {
                            Toast.makeText(context, "Failed to accept request.", Toast.LENGTH_SHORT).show();
                            holder.actionButton.setEnabled(true); // Re-enable if failed
                        }
                    });
                });
            } else { // Outgoing pending request
                holder.actionButton.setText("Requested");
                holder.actionButton.setEnabled(false); // Disable button for outgoing pending requests
            }
        } else { // Not friend, not pending (can send request)
            holder.actionButton.setText("Add Friend");
            holder.actionButton.setOnClickListener(v -> {
                holder.actionButton.setEnabled(false); // Disable immediately to prevent double-tap
                friendRequestManager.sendFriendRequest(item.getUid(), success -> {
                    if (success) {
                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                        if (refreshCallback != null) {
                            refreshCallback.run(); // Refresh list after sending request
                        }
                    } else {
                        Toast.makeText(context, "Failed to send request.", Toast.LENGTH_SHORT).show();
                        holder.actionButton.setEnabled(true); // Re-enable if failed
                    }
                });
            });
        }
    }

    @Override
    public int getItemCount() {
        return requestItems.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        Button actionButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.username_text);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }
}