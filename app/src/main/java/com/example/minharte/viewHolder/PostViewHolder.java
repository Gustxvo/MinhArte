package com.example.minharte.viewHolder;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minharte.R;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class PostViewHolder extends RecyclerView.ViewHolder {

    public LinearLayout postLayout;
    public ImageView imgPostLikes, imgCommentsPost, imgMoreOptions, imgPost, imgProfilePic;
    public LinearLayout profileLayout;
    private TextView txtUsername, txtProfession, txtPostName, txtPostDate, txtPostTime, txtDescription, txtLikesCounter;
    private PlayerView playerView;
    private String userID;
    private int likesCount;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();

    public PostViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void setPost (FragmentActivity fragment, String username, String userUrl, String userId, String userProfession, String postName, String postDescription,
                         String postGenre, String postUrl, String postDate, String postTime, String postType, String postKey){

        postLayout = itemView.findViewById(R.id.post_layout);                profileLayout = itemView.findViewById(R.id.profile_layout);
        imgProfilePic = itemView.findViewById(R.id.img_profile_pic);        imgPost = itemView.findViewById(R.id.img_post);
        imgPostLikes = itemView.findViewById(R.id.img_post_likes);          txtLikesCounter = itemView.findViewById(R.id.txt_post_likes_counter);
        imgCommentsPost = itemView.findViewById(R.id.img_post_comments);
        imgMoreOptions = itemView.findViewById(R.id.img_post_options);      txtPostName = itemView.findViewById(R.id.txt_post_name);
        txtUsername = itemView.findViewById(R.id.txt_username);             txtDescription = itemView.findViewById(R.id.txt_post_description);
        txtProfession = itemView.findViewById(R.id.txt_profession);         txtPostTime = itemView.findViewById(R.id.txt_post_time);
        playerView = itemView.findViewById(R.id.exo_post_item);             txtPostDate = itemView.findViewById(R.id.txt_post_date);

        if (!userUrl.equals("")){
            Picasso.get().load(userUrl).into(imgProfilePic);
        }

        txtUsername.setText(username);
        txtProfession.setText(userProfession);

        txtPostName.setText(postName);
        txtPostDate.setText(postDate);
        txtPostTime.setText(postTime);

        if (!postDescription.isEmpty()){
            txtDescription.setVisibility(View.VISIBLE);
            txtDescription.setText(postDescription);
        }

        if (postType.equals("img")){
            Picasso.get().load(postUrl).into(imgPost);
            imgPost.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
        } else if (postType.equals("video")){
            imgPost.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            try {
                SimpleExoPlayer simpleExoPlayer = new SimpleExoPlayer.Builder(fragment).build();
                playerView.setPlayer(simpleExoPlayer);
                Uri video = Uri.parse(postUrl);
                MediaItem mediaItem = MediaItem.fromUri(video);
                simpleExoPlayer.addMediaItem(mediaItem);
                simpleExoPlayer.prepare();


            } catch (Exception e){
                Toast.makeText(fragment.getApplicationContext(), "Erro!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void likesChecker(final String postKey){
        imgPostLikes = itemView.findViewById(R.id.img_post_likes);

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        assert mUser != null;
        userID = mUser.getUid();

        DatabaseReference likesRef = database.getReference("post_likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(userID)){
                    imgPostLikes.setImageResource(R.drawable.icon_heart_filled);
                } else {
                    imgPostLikes.setImageResource(R.drawable.icon_heart_outlined);
                }
                likesCount = (int)snapshot.child(postKey).getChildrenCount();
                if (likesCount > 0){
                    txtLikesCounter.setVisibility(View.VISIBLE);
                } else if (likesCount == 0){
                    txtLikesCounter.setVisibility(View.GONE);
                }
                String likesCounter = Integer.toString(likesCount);
                txtLikesCounter.setText(likesCounter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
