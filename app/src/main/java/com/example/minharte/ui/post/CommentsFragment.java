package com.example.minharte.ui.post;

import static java.lang.String.valueOf;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentCommentsBinding;
import com.example.minharte.model.Comment;
import com.example.minharte.model.User;
import com.example.minharte.viewHolder.CommentsViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;

@SuppressWarnings("ConstantConditions")
public class CommentsFragment extends Fragment {

    FragmentCommentsBinding binding;

    private String postUsername, postUserProfession, postUserUrl, postName, postDescription, postGenre,
            postDate, postTime, postKey, postUrl, postType;
    private int postLikesCounter;

    FirebaseUser mUser;
    String userID, username, userUrl, commentTime, key;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference profileReference, postsReference, likesRef, commentsLikesRef, commentsReference;
    boolean commentsLikesChecker, postLikesChecker = false;

    public CommentsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCommentsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mUser = FirebaseAuth.getInstance().getCurrentUser();

        likesRef = database.getReference("post_likes");
        commentsLikesRef = database.getReference("comment_likes");
        postsReference = database.getReference("posts");

        showUserDetails();
        if (mUser != null){
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity().getApplicationContext());
                binding.rvComments.setHasFixedSize(true);
                binding.rvComments.setLayoutManager(layoutManager);
            }, 100);
        }

        binding.imgReturn.setOnClickListener(view -> Navigation.findNavController(binding.getRoot())
                .navigate(R.id.navigateToMain));

        binding.edtAddComment.addTextChangedListener(commentsTextWatcher);

        binding.imgSendComment.setOnClickListener(view -> addComment());

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            userID = mUser.getUid();

            Bundle bundle = this.getArguments();
            if(bundle != null) {
                postKey = bundle.getString("postKey");
                key = bundle.getString("key");
                showPostDetails();

                postsReference = database.getReference("posts");
                commentsReference = postsReference.child(key).child("comments");
                commentsReference.keepSynced(true);

                FirebaseRecyclerOptions<Comment> options = new FirebaseRecyclerOptions.Builder<Comment>()
                        .setQuery(commentsReference, Comment.class).build();

                FirebaseRecyclerAdapter<Comment, CommentsViewHolder> mRecyclerAdapter = new FirebaseRecyclerAdapter<Comment, CommentsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull CommentsViewHolder holder, int position, @NonNull Comment model) {
                        holder.setComment(getActivity(), model.getUsername(), model.getUserUrl(),
                                model.getUserId(), model.getTime(), model.getComment(), model.getCommentKey());

                        final String key = getRef(position).getKey();
                        final String commentKey = getItem(position).getCommentKey();
                        final String userId = getItem(position).getUserId();

                        holder.likesChecker(key);
                        holder.imgCommentLikes.setOnClickListener(view -> {
                            commentsLikesChecker = true;
                            commentsLikesRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (commentsLikesChecker){
                                        if (snapshot.child(key).hasChild(userID)){
                                            commentsLikesRef.child(key).child(userID).removeValue();
                                        } else {
                                            commentsLikesRef.child(key).child(userID).setValue(true);
                                        }
                                        commentsLikesChecker = false;
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        });

                        if (userId.equals(userID))
                            holder.layoutComment.setOnLongClickListener(view -> {
                                deleteComment(commentKey);
                                return false;
                            });
                    }

                    @NonNull
                    @Override
                    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_layout, parent, false);
                        return new CommentsViewHolder(view);
                    }
                };
                binding.rvComments.setAdapter(mRecyclerAdapter);
                mRecyclerAdapter.startListening();
            }
        }
    }

    private void showUserDetails() {
        userID = mUser.getUid();
        profileReference = database.getReference("users");
        profileReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null){
                    username = user.getUsername();
                    userUrl = user.getUrl();
                    Picasso.get().load(userUrl).into(binding.imgCurrentUserPic);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showPostDetails(){
        postsReference = database.getReference("posts");
        Query query = postsReference.orderByChild("postKey").equalTo(postKey);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    postName = "" + dataSnapshot.child("postName").getValue();
                    postDescription = "" + dataSnapshot.child("postDescription").getValue();
                    postGenre = "" + dataSnapshot.child("postGenre").getValue();
                    postUsername = "" + dataSnapshot.child("username").getValue();
                    postUserProfession = "" + dataSnapshot.child("userProfession").getValue();
                    postUserUrl = "" + dataSnapshot.child("userUrl").getValue();
                    postType = "" + dataSnapshot.child("postType").getValue();
                    postDate = "" + dataSnapshot.child("postDate").getValue();
                    postTime = "" + dataSnapshot.child("postTime").getValue();
                    postUrl = "" + dataSnapshot.child("postUrl").getValue();

                    binding.txtPostName.setText(postName);
                    if (!postGenre.equals("")){
                        binding.txtPostGenre.setVisibility(View.VISIBLE);
                        binding.txtPostGenre.setText(postGenre);
                    } if (!postDescription.equals("")){
                        binding.txtPostDescription.setVisibility(View.VISIBLE);
                        binding.txtPostDescription.setText(postDescription);
                    }
                    binding.txtPostUsername.setText(postUsername);
                    binding.txtPostProfession.setText(postUserProfession);
                    binding.txtPostDate.setText(postDate);
                    binding.txtPostTime.setText(postTime);

                    if (postUserUrl != null){
                        Picasso.get().load(postUserUrl).into(binding.imgPostProfilePic);
                    }
                    if (postType.equals("img")){
                        Picasso.get().load(postUrl).into(binding.imgPost);
                        binding.imgPost.setVisibility(View.VISIBLE);
                        Picasso.get().load(postUrl).into(binding.imgPost);

                    } else if (postType.equals("video")){
                        binding.imgPost.setVisibility(View.GONE);
                        binding.playerView.setVisibility(View.VISIBLE);

                        try {
                            SimpleExoPlayer simpleExoPlayer = new SimpleExoPlayer.Builder(requireActivity()).build();
                            binding.playerView.setPlayer(simpleExoPlayer);
                            Uri video = Uri.parse(postUrl);
                            MediaItem mediaItem = MediaItem.fromUri(video);
                            simpleExoPlayer.addMediaItem(mediaItem);
                            simpleExoPlayer.prepare();
                        } catch (Exception e){
                            Toast.makeText(requireActivity(), "Erro!", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        postLikesChecker(key);
        binding.imgPostLikes.setOnClickListener(view -> {
            postLikesChecker = true;
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (postLikesChecker){
                        if (snapshot.child(key).hasChild(userID)){
                            likesRef.child(key).child(userID).removeValue();
                        } else {
                            likesRef.child(key).child(userID).setValue(true);
                        }
                        postLikesChecker = false;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });
    }

    private void postLikesChecker(final String key){
        likesRef = database.getReference("post_likes");
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        userID = mUser.getUid();

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(key).hasChild(userID)){
                    binding.imgPostLikes.setImageResource(R.drawable.icon_heart_filled);
                } else {
                    binding.imgPostLikes.setImageResource(R.drawable.icon_heart_outlined_white);
                }
                postLikesCounter = (int)snapshot.child(key).getChildrenCount();
                if (postLikesCounter > 0){
                    binding.txtLikesCounter.setVisibility(View.VISIBLE);

                } else if (postLikesCounter == 0){
                    binding.txtLikesCounter.setVisibility(View.GONE);
                }
                String postLikesCounter = Integer.toString(CommentsFragment.this.postLikesCounter);
                binding.txtLikesCounter.setText(postLikesCounter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addComment(){

        String inputComment = binding.edtAddComment.getText().toString();
        Calendar dateCalendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        final String date = currentDate.format(dateCalendar.getTime());

        Calendar timeCalendar = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss");
        final String time = currentTime.format(timeCalendar.getTime());
        commentTime = date + " " + time;

        String commentKey = userID + System.currentTimeMillis();

        Comment comment = new Comment();
        comment.setUsername(username);
        comment.setUserUrl(userUrl);
        comment.setUserId(userID);
        comment.setTime(commentTime);
        comment.setComment(inputComment);
        comment.setCommentKey(commentKey);

        postsReference = database.getReference("posts");

        commentsReference = postsReference.child(key).child("comments").child(valueOf(System.currentTimeMillis()));
        commentsReference.setValue(comment).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                binding.edtAddComment.setText("");
            }
        });
    }

    private void deleteComment(String commentKey){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Excluir Comentário?");
        builder.setMessage("Deseja mesmo excluir esse comentário?");

        builder.setPositiveButton("Excluir", (dialogInterface, i) -> {
            commentsReference = postsReference.child(key).child("comments");
            Query query = commentsReference.orderByChild("commentKey").equalTo(commentKey);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                        dataSnapshot.getRef().removeValue();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        });

        builder.setNegativeButton("Cancelar", (dialogInterface, i) ->
            dialogInterface.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private final TextWatcher commentsTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String comment = binding.edtAddComment.getText().toString();
            if (comment.length() > 0){
                binding.cvSendComment   .setVisibility(View.VISIBLE);
            } else {
                binding.cvSendComment.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}