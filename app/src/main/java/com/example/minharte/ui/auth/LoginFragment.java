package com.example.minharte.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentLoginBinding;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "ConstantConditions"})
public class LoginFragment extends Fragment {

    FragmentLoginBinding binding;
    private String email, password;

    FirebaseAuth mAuth;
    FirebaseUser mUser;

    public LoginFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.imgReturn.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateLoginToStart));

        binding.txtHaveAnAccount.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateLoginToRegister));

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        binding.btnLogin.setOnClickListener(view ->
            userLogin());

        return root;
    }


    private void userLogin() {
        email = binding.edtEmail.getText().toString().trim();
        password = binding.edtPassword.getText().toString().trim();

        if (!validateEmail() | !validatePassword()){
            if (!validateEmail()){
                binding.edtEmail.requestFocus();
            } else {
                binding.edtPassword.requestFocus();
            }
        } else {
            binding.progressBar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    binding.progressBar.setVisibility(View.GONE);
                    mUser = mAuth.getCurrentUser();
                    if (mUser != null) {
                        if (mUser.isEmailVerified()){
                            redirectUser();
                        } else {
                            mAuth.signOut();
                            showEmailVerificationDialog();
                        }
                    }
                }else {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        Toast.makeText(getContext(), "Login ou senha inválidos", Toast.LENGTH_SHORT).show();
                    }
                    catch (FirebaseTooManyRequestsException e) {
                        Toast.makeText(getContext(), "Muitas tentativas incorretas! Tente novamente mais tarde", Toast.LENGTH_LONG).show();
                    }
                    catch (Exception e) {
                        Toast.makeText(getContext(), "Usuário não cadastrado!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void redirectUser() {
        String userID = mUser.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query query = reference.orderByChild("userId").equalTo(userID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0){
                   Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToMain);
                } else {
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateLoginToCreateProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEmailVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Verifique seu Email");
        builder.setMessage("Por favor, verifique seu email");
        builder.setPositiveButton("Reenviar email de verificação", (dialogInterface, i) -> {
            mUser.sendEmailVerification();
            Intent goToEmail = new Intent(Intent.ACTION_MAIN);
            goToEmail.addCategory(Intent.CATEGORY_APP_EMAIL);
            goToEmail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(goToEmail);
        });
        builder.setNegativeButton("Cancelar", (dialogInterface, i) ->
            dialogInterface.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle bundle = this.getArguments();

        if(bundle != null){
            String email = bundle.getString("email");
            binding.edtEmail.setText(email);
            binding.edtPassword.requestFocus();
        }}


    private boolean validateEmail(){
        email = binding.edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            binding.tilEmail.setErrorEnabled(true);
            binding.tilEmail.setError("Campo obrigatório!");
            binding.edtEmail.addTextChangedListener(emailTextWatcher);
            return false;

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setErrorEnabled(true);
            binding.tilEmail.setError("Digite um email válido!");
            binding.edtEmail.addTextChangedListener(emailTextWatcher);
            return false;

        } else {
            binding.tilEmail.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassword(){
        password = binding.edtPassword.getText().toString().trim();

        if (password.isEmpty()) {
            binding.tilPassword.setErrorEnabled(true);
            binding.tilPassword.setError("Campo obrigatório!");
            binding.edtPassword.addTextChangedListener(passwordTextWatcher);
            binding.edtPassword.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    private final TextWatcher emailTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            binding.tilEmail.setErrorEnabled(false);
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private final TextWatcher passwordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            binding.tilPassword.setErrorEnabled(false);
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