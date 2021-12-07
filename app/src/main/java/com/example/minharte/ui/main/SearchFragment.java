package com.example.minharte.ui.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentSearchBinding;
import com.example.minharte.model.Post;
import com.example.minharte.viewHolder.PostViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

@SuppressWarnings("ConstantConditions")
public class SearchFragment extends Fragment {

    FragmentSearchBinding binding;

    String userID;
    FirebaseUser mUser;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference likesRef, postsReference;
    boolean likesChecker = false;

    public SearchFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        likesRef = database.getReference("post_likes");

        if (mUser != null){
            userID = mUser.getUid();
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setReverseLayout(true);
            layoutManager.setStackFromEnd(true);
            binding.rvSearchPosts.setHasFixedSize(true);
            binding.rvSearchPosts.setLayoutManager(layoutManager);
            postsReference = database.getReference("posts");

            binding.actionSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String text) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    if (!query.isEmpty()){
                        searchPosts(query);
                    } else {
                        binding.rvSearchPosts.setAdapter(null);
                    }
                    return true;
                }
            });
        }
        return root;
    }

    private void searchPosts(String text){
        String query = text.toLowerCase();
        Query searchQuery = postsReference.orderByChild("postName").startAt(text).endAt(text + "\uf8ff");

        FirebaseRecyclerOptions<Post> options = new FirebaseRecyclerOptions.Builder<Post>()
                .setQuery(searchQuery, Post.class).build();

        FirebaseRecyclerAdapter<Post, PostViewHolder> mRecyclerAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull Post model) {
                holder.setPost(getActivity(), model.getUsername(), model.getUserUrl(), model.getUserId(), model.getUserProfession(), model.getPostName(), model.getPostDescription(),
                        model.getPostGenre(), model.getPostUrl(), model.getPostDate(), model.getPostTime(), model.getPostType(), model.getPostKey());
                final String key = getRef(position).getKey();
                holder.likesChecker(key);
                holder.imgPostLikes.setOnClickListener(view -> {
                    likesChecker = true;
                    likesRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (likesChecker){
                                if (snapshot.child(key).hasChild(userID)){
                                    likesRef.child(key).child(userID).removeValue();
                                } else {
                                    likesRef.child(key).child(userID).setValue(true);
                                }
                                likesChecker = false;
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                });
            }
            @NonNull
            @Override
            public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_layout, parent, false);
                return new PostViewHolder(view);
            }
        };
        binding.rvSearchPosts.setAdapter(mRecyclerAdapter);
        mRecyclerAdapter.startListening();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}