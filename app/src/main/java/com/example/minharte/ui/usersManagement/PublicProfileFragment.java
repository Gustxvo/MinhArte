package com.example.minharte.ui.usersManagement;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentAccountSettingsBinding;
import com.example.minharte.databinding.FragmentPublicProfileBinding;
import com.example.minharte.model.User;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class PublicProfileFragment extends Fragment {

    FragmentPublicProfileBinding binding;
    String userID, accountType, name, phone, privacy, profession, url, username, userId;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference profileRef;

    public PublicProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPublicProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.imgReturn.setOnClickListener(view -> Navigation.findNavController(root).navigate(R.id.navigateToMain));

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser != null){
            userID = mUser.getUid();
            profileRef = FirebaseDatabase.getInstance().getReference("users");

            Bundle bundle = this.getArguments();
            if(bundle != null) {
                userId = bundle.getString("userId");
                showUserProfile();
            }
        }

        return root;
    }

    private void showUserProfile() {
        Query query = profileRef.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null){
                        accountType = user.getAccountType();
                        name = mUser.getDisplayName();
                        phone = user.getPhone();
                        privacy = user.getPrivacy();
                        profession = user.getProfession();
                        url = "" + dataSnapshot.child("url").getValue();
                        username = user.getUsername();
                    }

//                    accountType = "" + dataSnapshot.child("accountType").getValue();
//                    name = "" + dataSnapshot.child("name").getValue();
//                    phone = "" + dataSnapshot.child("phone").getValue();
//                    privacy = "" + dataSnapshot.child("privacy").getValue();
//                    profession = "" + dataSnapshot.child("profession").getValue();
//                    url = "" + dataSnapshot.child("url").getValue();
//                    username = "" + dataSnapshot.child("username").getValue();

                    binding.txtName.setText(name);
                    binding.txtUsername.setText(username);
                    binding.txtAccountType.setText(accountType);

                    if (privacy.equals("public")){
                        binding.txtPrivacy.setVisibility(View.GONE);
                    } else if (privacy.equals("private")){
                        binding.txtPrivacy.setVisibility(View.VISIBLE);
                    }

                    if (!url.equals("")){
                        Picasso.get().load(url).into(binding.imgProfilePic);
                    }

                    if (accountType.equals("Artista")){
                        binding.tvProfession.setVisibility(View.VISIBLE);
                        binding.txtProfession.setText(profession);
                        binding.txtProfession.setVisibility(View.VISIBLE);
                        binding.txtContactMe.setVisibility(View.VISIBLE);
                        binding.txtPhone.setText(phone);
                        binding.txtPhone.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}