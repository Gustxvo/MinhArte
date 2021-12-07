package com.example.minharte.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentStartBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

@SuppressWarnings("ConstantConditions")
public class StartFragment extends Fragment {

    FragmentStartBinding binding;

    private GoogleSignInClient mGoogleSignInClient;
    private String userID, url = "";

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser mUser;

    public StartFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStartBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnLogin.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateStartToLogin));

        binding.btnRegister.setOnClickListener(view ->
                Navigation.findNavController(root).navigate(R.id.navigateStartToRegister));

        googleSignIn();
        binding.imgGoogle.setOnClickListener(view ->
            resultLauncher.launch(new Intent(mGoogleSignInClient.getSignInIntent())));

        binding.imgFacebook.setOnClickListener(view -> {
//            facebookSignIn();
        });

        binding.txtSignInAnonymously.setOnClickListener(view -> {
//            signInAnonymously();
        });

        return root;
    }


    private void googleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        Log.w("TAG", "Google sign in failed", e);
                    }
                }
            });

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnSuccessListener(authResult -> {
            if (authResult.getAdditionalUserInfo().isNewUser()){
                Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateStartToCreateProfile);
            } else {
                mUser = mAuth.getCurrentUser();
                userID = mUser.getUid();

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
                Query query = reference.orderByChild("userId").equalTo(userID);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.getChildrenCount() > 0){
                            url = mUser.getPhotoUrl().toString();
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userID);
                            if (!url.equals("")){
                                userRef.child("url").setValue(url).addOnCompleteListener(task -> {
                                });
                            }
                            Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToMain);
                        } else {
                            Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateStartToCreateProfile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Ocorreu um erro. Verifique sua conta Google", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}