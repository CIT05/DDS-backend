package com.example.chatty_be;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatty_be.crypto.CryptoManager;
import com.example.chatty_be.crypto.KeyManager;
import com.example.chatty_be.model.UserModel;
import com.example.chatty_be.utils.FirebaseUtil;
import com.example.chatty_be.utils.KeyUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.security.PublicKey;

public class LoginUsernameActivity extends AppCompatActivity {

    EditText usernameInput;
    Button logInButton;
    ProgressBar progressBar;
    String phoneNumber;
    UserModel userModel;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_username);

        usernameInput = findViewById(R.id.login_username);
        logInButton = findViewById(R.id.login_btn);
        progressBar = findViewById(R.id.login_progressbar);

        phoneNumber = getIntent().getExtras().getString("phone");

        getUsername();

        logInButton.setOnClickListener(v -> {
            setUsername();
        });


    }

    void setUsername(){

        String username = usernameInput.getText().toString();

        if(username.isEmpty() || username.length()<2){
            usernameInput.setError("Username length should be at least 2 characters!");
            return;
        }

        setInProgress(true);

        boolean isNewUser = (userModel == null);

        if(isNewUser){

            try {
                KeyManager.generateIdentityKeyPair(this);
                CryptoManager.generateAESKey();
                PublicKey publicKey = KeyManager.getPublicKey(this);
                Log.d("KeyGen", "Generating public key" + publicKey);

                String encodedPublicKey = Base64.encodeToString(KeyUtil.encodePublicKey(publicKey), Base64.NO_WRAP);

                userModel = new UserModel(phoneNumber, username, Timestamp.now(), FirebaseUtil.getCurrentUserId(), encodedPublicKey);

                // TODO: MOVE IT TO WHERE WE IMPLEMENT THE FRIEND REQUEST
                FriendRequestManager manager = new FriendRequestManager(this);

            } catch (Exception e){
                Log.e("KeyGen", "Failed to generate or upload identity key", e);
            }
        }else {
            userModel.setUsername(username);
        }


        FirebaseUtil.currentUserDetails().set(userModel).addOnCompleteListener(task -> {
            setInProgress(false);
            if(task.isSuccessful()){
                Intent intent = new Intent(LoginUsernameActivity.this, MainActivity.class);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    void getUsername(){
        setInProgress(true);
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            if(task.isSuccessful()){
                userModel = task.getResult().toObject(UserModel.class);
                if(userModel != null){
                    usernameInput.setText(userModel.getUsername());
                }

            }
        });
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            logInButton.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            logInButton.setVisibility(View.VISIBLE);
        }
    }
}