package com.example.minharte.viewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minharte.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class CommentsViewHolder extends RecyclerView.ViewHolder {

    public ImageView imgCommentProfilePic, imgCommentLikes;
    public LinearLayout layoutComment;
    private TextView txtUsername, txtComment, txtCommentTime, txtCommentLikesCounter;
    private String userID, likes;
    private int likesCount;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();

    public CommentsViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void setComment (FragmentActivity fragment, String username, String userUrl, String uid, String time, String comment, String commentKey){
        layoutComment = itemView.findViewById(R.id.comments_layout);
        imgCommentProfilePic = itemView.findViewById(R.id.img_profile_pic);    txtUsername = itemView.findViewById(R.id.txt_username);
        txtCommentTime = itemView.findViewById(R.id.txt_comment_time);         txtComment = itemView.findViewById(R.id.txt_comment);
        txtCommentLikesCounter = itemView.findViewById(R.id.txt_comment_likes_counter);
        imgCommentLikes = itemView.findViewById(R.id.img_comment_likes);

        if (userUrl != null) {
            Picasso.get().load(userUrl).into(imgCommentProfilePic);
        }

        txtUsername.setText(username);
        txtCommentTime.setText(time);
        txtComment.setText(comment);
    }

    public void likesChecker(final String postKey){
        imgCommentLikes = itemView.findViewById(R.id.img_comment_likes);

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        assert mUser != null;
        userID = mUser.getUid();

        DatabaseReference commentsLikesRef = database.getReference("comment_likes");
        commentsLikesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(userID)){
                    imgCommentLikes.setImageResource(R.drawable.icon_heart_filled);
                    likesCount = (int)snapshot.child(postKey).getChildrenCount();
                    likes = " | " + likesCount + " likes";
                } else {
                    imgCommentLikes.setImageResource(R.drawable.icon_heart_outlined);
                    likesCount = (int)snapshot.child(postKey).getChildrenCount();
                    likes = " | " + likesCount + "  curtidas";
                }
                if (likesCount > 0){
                    txtCommentLikesCounter.setVisibility(View.VISIBLE);
                    if (likesCount == 1) {
                        likes = " | " + likesCount + " curtida";
                    }
                } else if (likesCount == 0){
                    txtCommentLikesCounter.setVisibility(View.GONE);
                }
                txtCommentLikesCounter.setText(likes);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


}
