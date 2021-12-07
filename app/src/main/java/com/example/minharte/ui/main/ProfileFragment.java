package com.example.minharte.ui.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentProfileBinding;
import com.example.minharte.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

@SuppressWarnings("ConstantConditions")
public class ProfileFragment extends Fragment {

    FragmentProfileBinding binding;

    String userID, accountType, name, phone, privacy, profession, url, username;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference profileRef;

    public ProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser != null){
            userID = mUser.getUid();
            profileRef = FirebaseDatabase.getInstance().getReference("users");

            url = mUser.getPhotoUrl().toString();
            if (!url.equals("")){
                binding.imgProfilePic.setBackground(null);
                Picasso.get().load(url).into(binding.imgProfilePic);
            }
            showUserProfile();
        }

        binding.imgMenu.setOnClickListener(view -> {
//            mAuth.signOut();
            Navigation.findNavController(root).navigate(R.id.navigateToSettings);
        });

        binding.imgEditProfile.setOnClickListener(view ->
            Navigation.findNavController(root).navigate(R.id.navigateToEditProfile));

        binding.imgAddPost.setOnClickListener(view ->
            Navigation.findNavController(root).navigate(R.id.navigateToPost));

        return root;
    }

    private void showUserProfile() {
        profileRef.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.constraintProfile.setVisibility(View.VISIBLE);
                User user = snapshot.getValue(User.class);
                if (user != null){
                    name = mUser.getDisplayName();
                    username = user.getUsername();
                    accountType = user.getAccountType();
                    phone = user.getPhone();
                    privacy = user.getPrivacy();
                    profession = user.getProfession();

                    binding.txtName.setText(name);
                    binding.txtUsername.setText(username);
                    binding.txtAccountType.setText(accountType);

                    if (privacy.equals("public")){
                        binding.txtPrivacy.setVisibility(View.GONE);
                    } else if (privacy.equals("private")){
                        binding.txtPrivacy.setVisibility(View.VISIBLE);
                    }

                    if (accountType.equals("Artista")){
                        binding.tvProfession.setVisibility(View.VISIBLE);
                        binding.txtProfession.setText(profession);
                        binding.txtProfession.setVisibility(View.VISIBLE);
                        binding.txtContactMe.setVisibility(View.VISIBLE);
                        binding.txtPhone.setText(phone);
                        binding.txtPhone.setVisibility(View.VISIBLE);
                        binding.imgAddPost.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Ocorreu um erro!", Toast.LENGTH_SHORT).show();
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