package com.example.chatty_be;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.ImageButton;

import com.example.chatty_be.crypto.KeyManager;
import com.example.chatty_be.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatty_be.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageButton searchButton;

        setSupportActionBar(binding.appBarMenu.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_map, R.id.nav_chat, R.id.nav_profile)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_menu);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(v -> {
            Intent intent = new  Intent(MainActivity.this, SearchUserActivity.class);
            startActivity(intent);
        });

        String userId = FirebaseUtil.getCurrentUserId();

        try {
            KeyManager.initIdentityKeys(this);
            checkAndSyncPublicKey(this);
        } catch (Exception e) {
            Log.e("MainActivity", "Key initialization or sync failed: " + e.getMessage());

        }

        FriendRequestManager manager = new FriendRequestManager(this);
        manager.fetchFriendsPublicKeys(userId);

        getFCMToken();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_menu);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    void getFCMToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String token =  task.getResult();
                FirebaseUtil.currentUserDetails().update("fcmToken", token);
            }
        });

    }

    void checkAndSyncPublicKey(Context context) {
        FirebaseUtil.currentUserDetails().get().addOnSuccessListener(snapshot -> {
            try {
                String localKey = Base64.encodeToString(
                        KeyManager.getPublicKey(context).getEncoded(),
                        Base64.NO_WRAP
                );

                String firestoreKey = snapshot.getString("publicKey");

                if (!localKey.equals(firestoreKey)) {
                    Log.w("KeyCheck", "Uploading public key to Firebase");
                    FirebaseUtil.currentUserDetails().update("publicKey", localKey)
                            .addOnSuccessListener(aVoid -> Log.d("KeyCheck", "Uploaded public key to firebase"))
                            .addOnFailureListener(e -> Log.e("KeyCheck", "Cannot upload public key to firebase: ", e));
                } else {
                    Log.d("KeyCheck", "Public key matching");
                }

            } catch (Exception e) {
                Log.e("KeyCheck", "Failed to update public key: " + e.getMessage());
            }
        }).addOnFailureListener(e -> Log.e("KeyCheck", "Error fetching public key from Firestore", e));
    }

}