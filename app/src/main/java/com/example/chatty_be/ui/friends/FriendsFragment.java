package com.example.chatty_be.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.chatty_be.R;
import com.example.chatty_be.databinding.FragmentFriendsBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FriendsFragment extends Fragment {

    private FragmentFriendsBinding binding;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FriendsViewModel slideshowViewModel =
                new ViewModelProvider(this).get(FriendsViewModel.class);

        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.friends_content, new FriendsNearbyFragment())
                .commit();

        BottomNavigationView bottomNav = binding.bottomNavigation;

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;

            int itemId = item.getItemId();

            if (itemId == R.id.menu_friends_nearby) {
                selected = new FriendsNearbyFragment();
            } else if (itemId == R.id.menu_friends_requests) {
                selected = new FriendsRequestFragment();
            } else {
                return false;
            }

            if (selected != null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.friends_content, selected)
                        .commit();
            }

            return true;
        });

       // final TextView textView = binding.textFriends;
//        slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}