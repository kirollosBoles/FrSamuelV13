package com.example.frsamuel.frsamuelv13;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private EditText postText;
    private Button AddPostBtn;

    private RecyclerView postList;
    private List<Posts> post_List_data;
    private PostsRecycleAdapter postAdap;
    private Boolean firstPage = true;
    private DocumentSnapshot lastVisible;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firebasefirestor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        firebasefirestor = FirebaseFirestore.getInstance();
        postText =  findViewById(R.id.textPost);
        AddPostBtn = findViewById(R.id.addPost);
        AddPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String post = postText.getText().toString();
                if(!TextUtils.isEmpty(post))
                {
                    Map<String,Object> postMap = new HashMap<>();
                    postMap.put("post", post);
                    postMap.put("user_id",mAuth.getCurrentUser().getUid());
                    postMap.put("time", FieldValue.serverTimestamp());

                    firebasefirestor.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if(task.isSuccessful())
                            {
                                Toast.makeText(HomeActivity.this, "تم", Toast.LENGTH_SHORT).show();
                                Intent intthis = getIntent();
                                finish();
                                startActivity(intthis);
                            }else{
                                Toast.makeText(HomeActivity.this, "حدث خطأ ما حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                            } }
                    });
                }else{
                    Toast.makeText(HomeActivity.this, "اكتب شىء", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        firstPage = true;
        post_List_data = new ArrayList<>();
        postList = findViewById(R.id.Post_view);
        postAdap = new PostsRecycleAdapter(post_List_data);
        postList.setLayoutManager(new LinearLayoutManager(this));
        postList.setAdapter(postAdap);

        postList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                Boolean reachedButton = !recyclerView.canScrollVertically(1);

                if(reachedButton){
                    LoadMorePost();
                }
            }
        });
        firstLoad();
    }

    public void firstLoad(){
        post_List_data.clear();

        Query newQuery = firebasefirestor.collection("Posts")
                .orderBy("time", Query.Direction.DESCENDING)
                .limit(5);
        newQuery.addSnapshotListener(this,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if(firstPage) {
                    if(documentSnapshots.size() > 0 ){
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);}
                }
                for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){
                        String postID = doc.getDocument().getId();
                        if(postID.isEmpty()){
                            continue;
                        }
                        Posts post = doc.getDocument().toObject(Posts.class).withID(postID);
                        if(post.getTime() == null){
                            continue;
                        }
                        if(firstPage){

                            post_List_data.add(post);}
                        else{
                            post_List_data.add(0,post);
                        }
                        postAdap.notifyDataSetChanged();
                    }
                }
                firstPage = false;
            }
        });
    }


    public void LoadMorePost()
    {
        Query newQuery = firebasefirestor.collection("Posts")
                .orderBy("time", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(5);

        newQuery.addSnapshotListener(this,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

                if(!documentSnapshots.isEmpty()){
                    lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                    for(DocumentChange doc: documentSnapshots.getDocumentChanges()){
                        if(doc.getType() == DocumentChange.Type.ADDED){
                            String postID = doc.getDocument().getId();
                            if(postID.isEmpty()){
                                continue;
                            }
                            Posts post = doc.getDocument().toObject(Posts.class).withID(postID);
                            if(post.getTime() == null){
                                continue;
                            }
                            post_List_data.add(post);
                            postAdap.notifyDataSetChanged();
                        }
                    }
                }}
        });
    }
}
