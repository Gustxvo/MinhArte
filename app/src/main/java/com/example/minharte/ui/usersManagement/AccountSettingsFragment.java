package com.example.minharte.ui.usersManagement;

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
import com.example.minharte.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class AccountSettingsFragment extends Fragment {

    String userID, gender, phone, privacy, email;

   FragmentAccountSettingsBinding binding;

   FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference profileRef;

    public AccountSettingsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        if (mUser != null){
            userID = mUser.getUid();
            profileRef = FirebaseDatabase.getInstance().getReference("users");

            showAccountSettings();

            binding.txtLogout.setOnClickListener(view -> {
                mAuth.signOut();
                Navigation.findNavController(root).navigate(R.id.navigateToAuth);
            });
        }

        return root;
    }

    private void showAccountSettings() {
        profileRef.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null){
                    phone = user.getPhone();
                    privacy = user.getPrivacy();
                    email = mUser.getEmail();
                    binding.txtPhone.setText(phone);
                    binding.txtEmail.setText(email);

                    if (privacy.equals("public")){
                        binding.switchPrivacy.setChecked(false);
                    } else if (privacy.equals("private")){
                        binding.switchPrivacy.setChecked(true);
                    }

                    binding.switchPrivacy.setOnClickListener(view -> {
                        if (binding.switchPrivacy.isChecked()){
                            profileRef.child(userID).child("privacy").setValue("private");
                        } else if (!binding.switchPrivacy.isChecked()){
                            profileRef.child(userID).child("privacy").setValue("public");
                        }
                    });
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