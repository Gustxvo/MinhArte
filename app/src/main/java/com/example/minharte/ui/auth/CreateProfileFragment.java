package com.example.minharte.ui.auth;

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

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentCreateProfileBinding;
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

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "ConstantConditions", "FieldCanBeLocal"})
public class CreateProfileFragment extends Fragment {

    FragmentCreateProfileBinding binding;

    private String accountType, gender, name, phone, privacy, profession, url = "", username, userID;
    private Uri imageUri;

    FirebaseUser mUser;
    DatabaseReference usersRef;
    StorageReference storageReference;

    public CreateProfileFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mUser != null){
            userID = mUser.getUid();

            if (mUser.getPhotoUrl() != null) {
                url = mUser.getPhotoUrl().toString();
                if (!url.equals("")){
                    Picasso.get().load(url).into(binding.imgProfilePic);
                    binding.txtAddPicture.setVisibility(View.GONE);
                }
            }

            if (mUser.getDisplayName() != null){
                binding.edtName.setText(mUser.getDisplayName());
            }
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users").child(userID);
        storageReference = FirebaseStorage.getInstance().getReference("profile_pics");

        binding.imgReturn.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateCreateProfileToStart);
        });

        binding.txtAddPicture.setOnClickListener(view ->
            mGResultLauncher.launch("image/*"));

        binding.imgProfilePic.setOnClickListener(view ->
            mGResultLauncher.launch("image/*"));

        binding.rgGender.setOnCheckedChangeListener((rgGender, i) -> {
            binding.txtGenderError.setVisibility(View.GONE);

            binding.rbFemale.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.GONE);
                binding.edtOtherGender.setText("");
            });
            binding.rbMale.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.GONE);
                binding.edtOtherGender.setText("");
            });
            binding.rbOther.setOnClickListener(view -> {
                binding.tilOtherGender.setVisibility(View.VISIBLE);
                binding.edtOtherGender.requestFocus();
            });
        });

        binding.rgAccountType.setOnCheckedChangeListener((radioGroup, i) ->
                binding.txtAccountTypeError.setVisibility(View.GONE));

        binding.btnCreateProfile.setOnClickListener(view -> {
            username = binding.edtUsername.getText().toString().trim();
            Query query = usersRef.orderByChild("username").equalTo(username);
            query.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getChildrenCount() > 0) {
                        binding.tilUsername.setErrorEnabled(true);
                        binding.tilUsername.setError("Nome de usuário já está em uso!");
                        binding.tilUsername.requestFocus();
                        binding.edtUsername.addTextChangedListener(usernameTextWatcher);
                    } else {
                        if (!validateName() | !validateUsername() | !validateGender() |
                                !validateAccountType() | !validatePhone()) {
                            if (!validateName()) {
                                binding.edtName.requestFocus();
                            } else if (!validateUsername()) {
                                binding.edtUsername.requestFocus();
                            } else if (!validatePhone()) {
                                binding.edtPhone.requestFocus();
                            }
                        } else {
                            uploadProfilePic();
                        }
                    }

                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        });

        return root;
    }

    private void createUserProfile(String url) {
        binding.progressBar.setVisibility(View.VISIBLE);
        if (binding.rbFemale.isChecked()) {
            gender = binding.rbFemale.getText().toString();
        } else if (binding.rbMale.isChecked()) {
            gender = binding.rbMale.getText().toString();
        } else {
            gender = binding.edtOtherGender.getText().toString();
        }

        if (binding.rbSupporter.isChecked()){
            accountType = binding.rbSupporter.getText().toString();
        } else {
            accountType = binding.rbArtist.getText().toString();
        }

        name = binding.edtName.getText().toString().trim();
        phone = binding.edtPhone.getText().toString().trim();
        privacy = "public";
        profession = "";
        userID = mUser.getUid();
        username = binding.edtUsername.getText().toString().trim();

        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(name).build();
        mUser.updateProfile(profileChangeRequest);

        User user = new User();
        user.setAccountType(accountType);
        user.setGender(gender);
        user.setName(name);
        user.setPhone(phone);
        user.setPrivacy(privacy);
        user.setProfession(profession);
        user.setUrl(url);
        user.setUserId(userID);
        user.setUsername(username);

        usersRef.setValue(user).addOnCompleteListener(task -> {
            binding.progressBar.setVisibility(View.GONE);
            Handler handler = new Handler();
            handler.postDelayed(() ->
                Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToMain), 1000);
        });
    }

    ActivityResultLauncher<String> mGResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        binding.imgProfilePic.setImageURI(result);
                        imageUri = result;
                    }
                }
            });

    private void uploadProfilePic() {
        StorageReference fileReference = storageReference.child(userID);
        if (imageUri != null){
            fileReference.putFile(imageUri).addOnCompleteListener(taskSnapshot ->
                fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (uri != null){
                        UserProfileChangeRequest profileImageRequest = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();
                        mUser.updateProfile(profileImageRequest);
                        url = uri.toString();
                        createUserProfile(url);
                    }
            }));
        } else createUserProfile(url);
    }


    private boolean validateName(){
        name = binding.edtName.getText().toString().trim();

        if(name.isEmpty()){
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

    private boolean validateAccountType(){
        int accountTypeId = binding.rgAccountType.getCheckedRadioButtonId();

        if (accountTypeId == -1){
            binding.txtAccountTypeError.setVisibility(View.VISIBLE);
            return false;
        } else {
            return true;
        }
    }

    private boolean validatePhone(){
//        phone = binding.edtPhone.getText().toString().trim();
//
//        if(phone.isEmpty()){
//            binding.tilPhone.setErrorEnabled(true);
//            binding.tilPhone.setError("Campo obrigatório!");
//            binding.tilPhone.requestFocus();
//            binding.edtPhone.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
//
//                @Override
//                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                    binding.tilPhone.setErrorEnabled(false);
//                }
//
//                @Override
//                public void afterTextChanged(Editable editable) {}
//            });
//            return false;
//        } else if(phone.length() != 15){
//            binding.tilPhone.setErrorEnabled(true);
//            binding.tilPhone.setError("Telefone inválido");
//            binding.tilPhone.requestFocus();
//            binding.edtPhone.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
//
//                @Override
//                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                    binding.tilPhone.setErrorEnabled(false);
//                }
//
//                @Override
//                public void afterTextChanged(Editable editable) {}
//            });
//            return false;
//        } else {
        return true;
//        }
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
            binding.tilUsername.setErrorEnabled(false);}

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