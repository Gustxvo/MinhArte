package com.example.minharte.ui.auth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import com.example.minharte.databinding.FragmentRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

@SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted"})
public class RegisterFragment extends Fragment {

    FragmentRegisterBinding binding;


    FirebaseAuth mAuth;
    FirebaseUser mUser;

    String email, password, confirmPassword;
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    //"(?=.*[a-z])" +         //at least 1 lower case letter
                    //"(?=.*[A-Z])" +         //at least 1 upper case letter
                    "(?=.*[0-9])" +         //at least 1 digit
                    "(?=.*[a-zA-Z])" +      //any letter
                    "(?=.*[!@#$%^&+=])" +    //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{8,}" +               //at least 8 characters
                    "$");

    public RegisterFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.imgReturn.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateRegisterToStart));

        binding.txtHaveAnAccount.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateRegisterToLogin));

        binding.btnRegister.setOnClickListener(view -> {
            ProgressDialog progressDialog = new ProgressDialog(getContext());

            email = binding.edtEmail.getText().toString().trim();
            password = binding.edtPassword.getText().toString().trim();
            confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

            if (!validateEmail() | !validatePassword() | !validateConfirmPassword()){
                if (!validateEmail()) {
                    binding.edtEmail.requestFocus();
                } else if (!validatePassword()) {
                    binding.edtPassword.requestFocus();
                } else {
                    binding.edtConfirmPassword.requestFocus();
                }
            } else {
                progressDialog.setMessage("Aguarde um momento...");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        mUser = mAuth.getCurrentUser();
                        if (mUser != null){
                            progressDialog.dismiss();
                            mUser.sendEmailVerification();
                            mAuth.signOut();
                            showEmailVerificationDialog(email);
                        }
                    } else {
                        progressDialog.dismiss();
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthUserCollisionException e) {
                            binding.tilEmail.setErrorEnabled(true);
                            binding.tilEmail.setError("Email já está associado a outra conta!");
                            binding.edtEmail.requestFocus();
                            binding.edtEmail.addTextChangedListener(emailTextWatcher);
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            binding.tilEmail.setErrorEnabled(true);
                            binding.tilEmail.setError("Formato de email inválido!");
                            binding.edtEmail.requestFocus();
                            binding.edtEmail.addTextChangedListener(emailTextWatcher);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "" + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        return root;
    }

    private void showEmailVerificationDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Verifique seu Email");
        builder.setMessage("Por favor, verifique seu email para concluir seu cadastro");
        builder.setPositiveButton("Continuar", (dialogInterface, i) -> {
            Intent goToEmail = new Intent(Intent.ACTION_MAIN);
            goToEmail.addCategory(Intent.CATEGORY_APP_EMAIL);
            goToEmail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle = new Bundle();
            bundle.putString("email", email);
            Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateRegisterToLogin, bundle);
            startActivity(goToEmail);
        });
        builder.setOnCancelListener(dialogInterface ->
            Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateRegisterToLogin));
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

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
        } else if (password.length() < 8){
            binding.tilPassword.setErrorEnabled(true);
            binding.tilPassword.setError("Sua senha deve conter pelo menos 8 caracteres!");
            binding.tilPassword.setErrorIconDrawable(null);
            binding.edtPassword.addTextChangedListener(passwordTextWatcher);
            binding.edtPassword.requestFocus();
            return false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()){
            binding.tilPassword.setErrorEnabled(true);
            binding.tilPassword.setError("Digite uma senha mais forte! Use letras, símbolos e números");
            binding.tilPassword.setErrorIconDrawable(null);
            binding.edtPassword.addTextChangedListener(passwordTextWatcher);
            return false;
        } else {
            return true;
        }
    }

    private boolean validateConfirmPassword(){
        confirmPassword = binding.edtConfirmPassword.getText().toString().trim();
        password = binding.edtPassword.getText().toString().trim();

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setErrorEnabled(true);
            binding.tilConfirmPassword.setError("Confirme sua senha!");
            binding.tilConfirmPassword.requestFocus();
            binding.edtConfirmPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    binding.tilConfirmPassword.setErrorEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
            return false;
        } if (!password.matches(confirmPassword)){
            binding.tilConfirmPassword.setError("Senhas não correspondem!");
            binding.tilConfirmPassword.setErrorIconDrawable(null);
            binding.edtConfirmPassword.addTextChangedListener(confirmPasswordTextWatcher);
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

    private final TextWatcher confirmPasswordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            binding.tilConfirmPassword.setErrorEnabled(false);
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}