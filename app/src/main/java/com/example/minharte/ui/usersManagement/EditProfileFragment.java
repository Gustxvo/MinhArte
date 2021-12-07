package com.example.minharte.ui.usersManagement;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentEditProfileBinding;
import com.example.minharte.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted"})
public class EditProfileFragment extends Fragment {

    FragmentEditProfileBinding binding;

    private String accountType, gender, name, phone, privacy, profession, url, userId, username, currentUsername, userID;

    private final String[] items = {"Apreciador", "Artista"};
    private ArrayAdapter<String> arrayAdapter;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    StorageReference storageReference;
    Uri imageUri;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference, profileReference, dbPostsReference, dbProfilePostReference, dbCommentsReference, updateProfilePost, updatePost;

    public EditProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userID = mUser.getUid();
        databaseReference = database.getReference("users");
        profileReference = databaseReference.child(userID);

        dbProfilePostReference = database.getReference("user_posts").child(userID);
        dbPostsReference = database.getReference("posts");
        storageReference = FirebaseStorage.getInstance().getReference("profile_pics");

        arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.dropdown_account_type, items);
        binding.textAccountType.setAdapter(arrayAdapter);
        binding.textAccountType.setOnItemClickListener((adapterView, view, position, l) -> {
            String item = adapterView.getItemAtPosition(position).toString();
            if (item.equals("Artista")){
                binding.tilProfession.setVisibility(View.VISIBLE);
            } if (item.equals("Apreciador")) {
                binding.tilProfession.setVisibility(View.GONE);
            }
        });

        binding.rgGender.setOnCheckedChangeListener((radioGroup, i) -> {
            binding.txtGenderError.setVisibility(View.GONE);
            binding.rbFemale.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.GONE);
                binding.edtOtherGender.setText("");
            });
            binding.rbMale.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.GONE);
                binding.edtOtherGender.setText("");
            });
            binding.rbOtherGender.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.VISIBLE);
                binding.edtOtherGender.requestFocus();
            });
        });

        showProfile();

        binding.imgReturn.setOnClickListener(view -> Navigation.findNavController(binding.getRoot())
                .navigate(R.id.navigateToProfile));

        binding.imgProfilePic.setOnClickListener(view -> mGResultLauncher.launch("image/*"));
        binding.txtPickImage.setOnClickListener(view -> mGResultLauncher.launch("image/*"));

        binding.imgEditProfile.setOnClickListener(view -> {
            username = binding.edtUsername.getText().toString().trim();
            if (!validateName() | !validateUsername() | !validateGender()){
                if (!validateName()) {
                    binding.edtName.requestFocus();
                } else if (!validateUsername()) {
                    binding.edtUsername.requestFocus();
                } else if (!validateGender()){
                    binding.txtGenderError.setVisibility(View.VISIBLE);
                }
            } else {
                binding.progressBar.setVisibility(View.VISIBLE);
                databaseReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null){
                            phone = user.getPhone();
                            privacy = user.getPrivacy();
                            userId = user.getUserId();
                            url = user.getUrl();
                            updateAccountSettings();
                            editProfile(phone, privacy, userId, url);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        });

        return root;
    }

    private void showProfile() {
        databaseReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null){
                    name = mUser.getDisplayName();
                    url = user.getUrl();
                    username = user.getUsername();
                    currentUsername = user.getUsername();
                    accountType = user.getAccountType();
                    profession = user.getProfession();
                    gender = user.getGender();

                    binding.edtName.setText(name);
                    binding.edtUsername.setText(username);
                    binding.edtProfession.setText(profession);

                    if (!url.equals("")){
                        binding.imgProfilePic.setBackground(null);
                        Picasso.get().load(url).into(binding.imgProfilePic);
                    }

                    if (gender.equals("Feminino")){
                        binding.rbFemale.setChecked(true);
                    } else if (gender.equals("Masculino")){
                        binding.rbMale.setChecked(true);
                    } else {
                        binding.rbOtherGender.setChecked(true);
                        binding.tilOtherGender.setVisibility(View.VISIBLE);
                        binding.edtOtherGender.setText(gender);
                    }

                    if (accountType.equals("Apreciador")){
                        binding.textAccountType.setText(R.string.supporter);
                        arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.dropdown_account_type, items);
                        binding.textAccountType.setAdapter(arrayAdapter);
                    } else if (accountType.equals("Artista")){
                        binding.textAccountType.setText(R.string.artist);
                        arrayAdapter = new ArrayAdapter<>(requireActivity(), R.layout.dropdown_account_type, items);
                        binding.textAccountType.setAdapter(arrayAdapter);
                        binding.tilAccountType.setVisibility(View.VISIBLE);
                    }
                }
                binding.progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void editProfile(String phone, String privacy, String userId, String url) {
        name = binding.edtName.getText().toString();
        username = binding.edtUsername.getText().toString();
        accountType = binding.textAccountType.getText().toString();
        profession = binding.edtProfession.getText().toString();

        if (binding.rbFemale.isChecked()) {
            gender = binding.rbFemale.getText().toString();
        } else if (binding.rbMale.isChecked()) {
            gender = binding.rbMale.getText().toString();
        } else {
            gender = binding.edtOtherGender.getText().toString();
        }

        if (accountType.equals("Apreciador")){
            profession = "";
        }

        Query userQuery = databaseReference.orderByChild("username").equalTo(username);
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0 && !username.equals(currentUsername)){
                    binding.tilUsername.setErrorEnabled(true);
                    binding.tilUsername.setError("Nome de usuário já está em uso!");
                    binding.tilUsername.requestFocus();
                    binding.edtUsername.addTextChangedListener(usernameTextWatcher);
                } else {
                    User user = new User();
                    user.setAccountType(accountType);
                    user.setGender(gender);
                    user.setName(name);
                    user.setPhone(phone);
                    user.setPrivacy(privacy);
                    user.setProfession(profession);
                    user.setUrl(url);
                    user.setUserId(userId);
                    user.setUsername(username);

                    profileReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            profileReference.setValue(user).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                                    mUser.updateProfile(profileChangeRequest);

                                    Handler handler = new Handler();
                                    handler.postDelayed(() -> Navigation.findNavController(binding.getRoot())
                                            .navigate(R.id.navigateToProfile), 1000);
                                } else {
                                    try{
                                        throw task.getException();
                                    }  catch (Exception e){
                                        Toast.makeText(requireActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                binding.progressBar.setVisibility(View.VISIBLE);
                                binding.progressBar.requestFocus();
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateAccountSettings() {
        username = binding.edtUsername.getText().toString();
        profession = binding.edtUsername.getText().toString();

        dbProfilePostReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String key = ds.getKey();
                    updateProfilePost = dbProfilePostReference.child(key);
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("username", username);
                    childUpdates.put("userProfession", profession);
                    updateProfilePost.updateChildren(childUpdates);

                    dbPostsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            updatePost = dbPostsReference.child(key);
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("username", username);
                            childUpdates.put("userProfession", profession);

                            updatePost.updateChildren(childUpdates);

                            dbCommentsReference = updatePost.child("comments");
                            Query query = dbCommentsReference.orderByChild("userId").equalTo(userID);
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                                        Map<String, Object> childUpdates = new HashMap<>();
                                        childUpdates.put("username", username);
                                        dataSnapshot.getRef().updateChildren(childUpdates);
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });

                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        if (imageUri != null) {
            StorageReference fileReference = storageReference.child(userID);
            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        profileReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                url = uri.toString();
                                User user = new User();
                                user.setUrl(url);
                                profileReference.child("url").setValue(url);
                                UserProfileChangeRequest profileImageRequest = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                                mUser.updateProfile(profileImageRequest);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });

                        dbProfilePostReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    url = uri.toString();
                                    String key = ds.getKey();
                                    updateProfilePost = dbProfilePostReference.child(key);
                                    Map<String, Object> childUpdates = new HashMap<>();
                                    childUpdates.put("userUrl", url);

                                    updateProfilePost.updateChildren(childUpdates);

                                    dbPostsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            updatePost = dbPostsReference.child(key);
                                            Map<String, Object> childUpdates = new HashMap<>();
                                            childUpdates.put("userUrl", url);

                                            updatePost.updateChildren(childUpdates);

                                            dbCommentsReference = updatePost.child("comments");
                                            Query query = dbCommentsReference.orderByChild("userId").equalTo(userID);
                                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                                                        Map<String, Object> childUpdates = new HashMap<>();
                                                        childUpdates.put("userUrl", url);
                                                        dataSnapshot.getRef().updateChildren(childUpdates);
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {}
                                            });

                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }));
        }
    }

    ActivityResultLauncher<String> mGResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        binding.imgProfilePic.setBackground(null);
                        binding.imgProfilePic.setImageURI(result);
                        imageUri = result;
                    }
                }
            });

    private boolean validateName(){
        String nome = binding.edtName.getText().toString().trim();
        if(nome.isEmpty()){
            binding.tilName.setErrorEnabled(true);
            binding.tilName.setError("Campo obrigatório!");
            binding.tilName.requestFocus();
            binding.edtName.addTextChangedListener(nameTextWatcher);
            return false;
        } else {
            return true;
        }
    }

    private boolean validateUsername(){
        username = binding.edtUsername.getText().toString().trim();
        String noWhiteSpace = "(^\\S+$)";

        if(username.isEmpty()){
            binding.tilUsername.setErrorEnabled(true);
            binding.tilUsername.setError("Campo obrigatório!");
            binding.tilUsername.requestFocus();
            binding.edtUsername.addTextChangedListener(usernameTextWatcher);
            return false;
        } else if(!username.matches(noWhiteSpace)){
            binding.tilUsername.setErrorEnabled(true);
            binding.tilUsername.setError("Usuário não deve conter espaços em branco!");
            binding.tilUsername.requestFocus();
            binding.edtUsername.addTextChangedListener(usernameTextWatcher);
            return false;
        } else {
            return true;
        }
    }

    private boolean validateGender(){
        int genderId = binding.rgGender.getCheckedRadioButtonId();

        if (genderId == -1){
            binding.txtGenderError.setVisibility(View.VISIBLE);
            return false;
        } else {
            return true;
        }
    }

    private final TextWatcher nameTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            binding.tilName.setErrorEnabled(false);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private final TextWatcher usernameTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            binding.tilUsername.setErrorEnabled(false);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}