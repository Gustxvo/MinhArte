package com.example.minharte.ui.main;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.minharte.R;
import com.example.minharte.databinding.FragmentHomeBinding;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

@SuppressWarnings("ConstantConditions")
public class HomeFragment extends Fragment {

    FragmentHomeBinding binding;

    String userID;

    FirebaseUser mUser;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference postsRef, userPostsRef, postLikesRef;
    boolean likesChecker;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null){
            userID = mUser.getUid();

            postLikesRef = database.getReference("post_likes");
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setReverseLayout(true);
            layoutManager.setStackFromEnd(true);
            binding.rvPosts.setHasFixedSize(true);
            binding.rvPosts.setLayoutManager(layoutManager);
            userPostsRef = database.getReference("user_posts").child(userID);
            postsRef = database.getReference("posts");
            postsRef.keepSynced(true);

            userID = mUser.getUid();

            FirebaseRecyclerOptions<Post> options = new FirebaseRecyclerOptions.Builder<Post>()
                    .setQuery(postsRef, Post.class).build();

            FirebaseRecyclerAdapter<Post, PostViewHolder> mRecyclerAdapter = new FirebaseRecyclerAdapter<Post, PostViewHolder>(options) {
                @Override
                protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull Post model) {
                    holder.setPost(getActivity(), model.getUsername(), model.getUserUrl(), model.getUserId(), model.getUserProfession(), model.getPostName(), model.getPostDescription(),
                            model.getPostGenre(), model.getPostUrl(), model.getPostDate(), model.getPostTime(), model.getPostType(), model.getPostKey());

                    final String name = getItem(position).getPostName();
                    final String postUrl = getItem(position).getPostUrl();
                    final String userId = getItem(position).getUserId();
                    final String postType = getItem(position).getPostType();
                    final String postKey = getItem(position).getPostKey();

                    final String key = getRef(position).getKey();
                    holder.likesChecker(key);
                    holder.imgPostLikes.setOnClickListener(view -> {
                        likesChecker = true;
                        postLikesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (likesChecker){
                                    if (snapshot.child(key).hasChild(userID)){
                                        postLikesRef.child(key).child(userID).removeValue();
                                    } else {
                                        postLikesRef.child(key).child(userID).setValue(true);
                                    }
                                    likesChecker = false;
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    });

                    holder.imgCommentsPost.setOnClickListener(view -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("postKey", postKey);
                        bundle.putString("key", key);
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToComments, bundle);
                    });

                    holder.postLayout.setOnClickListener(view -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("postKey", postKey);
                        bundle.putString("key", key);
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToComments, bundle);
                    });

                    holder.imgMoreOptions.setOnClickListener(view ->
                            showOptionsDialog(name, postUrl, userId, postType, postKey));

                    holder.profileLayout.setOnClickListener(view -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("userId", userId);
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.navigateToUserProfile, bundle);
                    });
                }

                @NonNull
                @Override
                public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_layout, parent, false);
                    return new PostViewHolder(view);
                }
            };
            binding.rvPosts.setAdapter(mRecyclerAdapter);
            mRecyclerAdapter.startListening();
        }
        return root;
    }

    private void showOptionsDialog(String postName, String postUrl, String userId, String postType, String postKey) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View viewOptions = inflater.inflate(R.layout.post_options, null);

        TextView download = viewOptions.findViewById(R.id.txtDownloadPost);
        TextView share = viewOptions.findViewById(R.id.txtCompartilharPost);
        TextView delete = viewOptions.findViewById(R.id.txtExcluirPost);
        TextView copyUrl = viewOptions.findViewById(R.id.txtCopiarUrlPost);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(viewOptions).create();
        alertDialog.show();

        if (userId.equals(userID)){
            delete.setVisibility(View.VISIBLE);
        } else {
            delete.setVisibility(View.GONE);
        }

        download.setOnClickListener(view -> {
            if (postType.equals("img")){
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(postUrl));
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                request.setTitle("Download");
                request.setDescription("Baixando imagem...");
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, postName + System.currentTimeMillis() + ".jpg");
                DownloadManager manager = (DownloadManager)requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(getContext(), "Fazendo Download...", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            } else {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(postUrl));
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
                request.setTitle("Download");
                request.setDescription("Baixando vídeo...");
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, postName + System.currentTimeMillis() + ".mp4");
                DownloadManager manager = (DownloadManager)requireActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(getContext(), "Fazendo Download...", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            }
        });

        share.setOnClickListener(view -> {
            String shareText = postName + "\n" + "\n" + postUrl;
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Compartilhar via"));
            alertDialog.dismiss();
        });

        copyUrl.setOnClickListener(view -> {
            ClipboardManager clipboardManager = (ClipboardManager)requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("String", postUrl);
            clipboardManager.setPrimaryClip(clipData);
            clipData.getDescription();
            Toast.makeText(getContext(), "Copiado para a área de transferência", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });

        delete.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Excluir Publicação?");
            builder.setMessage("Tem certeza que deseja excluir essa publicação?" + "\n" + "\n" + "\n" + "Obs: Essa ação não pode ser desfeita!");

            builder.setPositiveButton("Excluir", (dialogInterface, i) -> {
                Query query = postsRef.orderByChild("postKey").equalTo(postKey);
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

                Query query1 = userPostsRef.orderByChild("postKey").equalTo(postKey);
                query1.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                            dataSnapshot.getRef().removeValue();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

                StorageReference userPostsRef = FirebaseStorage.getInstance().getReferenceFromUrl(postUrl);
                userPostsRef.delete().addOnCompleteListener(task -> {
                    Toast.makeText(getContext(), "Post excluído!", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                });
            });

            builder.setNegativeButton("Cancelar", (dialogInterface, i) ->
                alertDialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}