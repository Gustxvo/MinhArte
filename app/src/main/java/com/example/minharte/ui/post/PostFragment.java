package com.example.minharte.ui.post;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentPostBinding;
import com.example.minharte.model.Post;
import com.example.minharte.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@SuppressWarnings("ConstantConditions")
public class PostFragment extends Fragment {

    FragmentPostBinding binding;

    private Uri selectedUri;
    UploadTask uploadTask;
    String url, username, type, userID, profession;
    StorageReference storageReference;
    DatabaseReference userPostsRef, postsRef;
    FirebaseDatabase database;
    MediaController mediaController;
    FirebaseUser mUser;

    public PostFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPostBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mUser != null){
            userID = mUser.getUid();
            binding.btnUploadImg.setOnClickListener(view -> imgResultLauncher.launch("image/*"));

            binding.btnUploadVideo.setOnClickListener(view -> videoResultLauncher.launch("video/*"));

            binding.btnUploadPost.setOnClickListener(view -> postMedia());

            mediaController = new MediaController(getContext());
            database = FirebaseDatabase.getInstance();
            storageReference = FirebaseStorage.getInstance().getReference("user_posts");

            userPostsRef = database.getReference("user_posts").child(userID);
            postsRef = database.getReference("posts");
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mUser != null){
            userID = mUser.getUid();

            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userID);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null){
                        username = user.getUsername();
                        profession = user.getProfession();
                        if (mUser.getPhotoUrl() != null){
                            url = user.getUrl();
                        } else {
                            url = "";
                        }
                    } else {
                        Toast.makeText(requireActivity(), "Erro", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    ActivityResultLauncher<String> imgResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        selectedUri = result;
                        Picasso.get().load(selectedUri).into(binding.imgPost);
                        binding.imgPost.setVisibility(View.VISIBLE);
                        binding.videoPost.setVisibility(View.GONE);
                        type = "img";
                    }
                }
            });

    ActivityResultLauncher<String> videoResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        selectedUri = result;
                        binding.videoPost.setMediaController(mediaController);
                        binding.imgPost.setVisibility(View.GONE);
                        binding.videoPost.setVisibility(View.VISIBLE);
                        type = "video";
                        binding.videoPost.setVideoURI(result);
                        binding.videoPost.start();
                    }
                }
            });


    private void postMedia() {
        binding.progressBarPost.setVisibility(View.VISIBLE);
        binding.progressBarPost.requestFocus();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        userID = mUser.getUid();

        String postName = binding.edtPostName.getText().toString();
        String postGender = binding.textPostGenre.getText().toString();
        String postDescription = binding.edtPostDescription.getText().toString();

        Calendar dateCalendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        final String date = currentDate.format(dateCalendar.getTime());

        Calendar timeCalendar = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss");
        final String time = currentTime.format(timeCalendar.getTime());

        String postKey = userID + System.currentTimeMillis();


        if (!postName.isEmpty() || !postGender.isEmpty() || selectedUri != null){
            final StorageReference reference = storageReference.child(System.currentTimeMillis() + "." + getFileExt(selectedUri));

            uploadTask = reference.putFile(selectedUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw task.getException();
                }
                return reference.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Uri downloadUri = task.getResult();
                    Post post = new Post();
                    post.setUsername(username);
                    post.setUserUrl(url);
                    post.setUserProfession(profession);
                    post.setUserId(userID);
                    post.setPostName(postName);
                    post.setPostDescription(postDescription);
                    post.setPostGenre(postGender);
                    post.setPostUrl(downloadUri.toString());
                    post.setPostDate(date);
                    post.setPostTime(time);
                    post.setPostKey(postKey);

                    if (type.equals("img")){
                        post.setPostType("img");
                    }
                    else if (type.equals("video")){
                        post.setUserUrl(url);
                        post.setPostType("video");
                    }

                    String key = userPostsRef.push().getKey();
                    userPostsRef.child(key).setValue(post);
                    postsRef.child(key).setValue(post);
                    binding.progressBarPost.setVisibility(View.INVISIBLE);
                    Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToMain);
                } else {
                    Toast.makeText(requireActivity(), "Erro!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getFileExt(Uri uri){
        ContentResolver contentResolver = requireActivity().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}