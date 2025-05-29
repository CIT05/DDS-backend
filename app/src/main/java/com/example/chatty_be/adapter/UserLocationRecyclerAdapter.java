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
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.AndroidUtil;

import java.util.List;

public class UserLocationRecyclerAdapter extends RecyclerView.Adapter<UserLocationRecyclerAdapter.UserModelHolder> {

    private final List<UserModel> userList;
    Context context;

    public UserLocationRecyclerAdapter(List<UserModel> userList, Context context ) {
        this.userList = userList;
        this.context = context;
    }

    public void updateUserList(List<UserModel> newUserList) {
        this.userList.clear();
        this.userList.addAll(newUserList);
        notifyDataSetChanged();
    }

    public void addUsers(List<UserModel> newUserList) {
        this.userList.addAll(newUserList);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull UserModelHolder holder, int position) {
        UserModel model = userList.get(position);
        holder.usernameText.setText(model.getUsername());
        holder.phoneNumber.setText(model.getPhone());

        holder.itemView.setOnClickListener(v->{

            Intent intent = new Intent(context, ChatActivity.class);
            AndroidUtil.passUserModelAsIntent(intent, model);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        });
    }

    @NonNull
    @Override
    public UserModelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_row, parent, false);
        return new UserModelHolder(view);
    }



    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserModelHolder extends RecyclerView.ViewHolder{
        TextView usernameText;
        TextView phoneNumber;
        public UserModelHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_username_text);
            phoneNumber = itemView.findViewById(R.id.user_phone_text);
        }
    }

}
