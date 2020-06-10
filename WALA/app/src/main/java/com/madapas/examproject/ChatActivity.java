package com.madapas.examproject;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.madapas.examproject.Chat.ChatObject;
import com.madapas.examproject.Chat.MediaAdapter;
import com.madapas.examproject.Chat.MessageAdapter;
import com.madapas.examproject.Chat.MessageObject;
import com.madapas.examproject.User.UserObject;
import com.madapas.examproject.Utils.SendNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class ChatActivity extends AppCompatActivity {
    private RecyclerView mChat, mMedia;
    private RecyclerView.Adapter mChatAdapter, mMediaAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager, mMediaLayoutManager;
    ArrayList<MessageObject> messageList;

    ChatObject mChatObject;

    DatabaseReference mChatMessagesDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatObject = (ChatObject) getIntent().getSerializableExtra("chatObject");

        mChatMessagesDb = FirebaseDatabase.getInstance().getReference().child("chat").child(mChatObject.getChatId()).child("messages");
        Button mSend = findViewById(R.id.send);
        Button mAddMedia = findViewById(R.id.addMedia);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        mAddMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });
        initializeMessage();
        initializeMedia();
        getChatMessages();
    }
    private void getChatMessages() {
        // looping through each and every child that is below the database reference
        mChatMessagesDb.addChildEventListener(new ChildEventListener() {
            @Override
            // firstly, getting all childs, then, whenever adding a child -> create a dataSnapshot for that specific child message
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists()){
                    String  text = "",
                            creatorID = "";
                    ArrayList<String> mediaUrlList = new ArrayList<>();
                    if(dataSnapshot.child("text").getValue() != null) // if some text exists
                        text = dataSnapshot.child("text").getValue().toString();
                    if(dataSnapshot.child("creator").getValue() != null)
                        creatorID = dataSnapshot.child("creator").getValue().toString();
                    // if there is something inside the child media
                    if(dataSnapshot.child("media").getChildrenCount() > 0)
                        // looping through all of the children inside the media child
                        for (DataSnapshot mediaSnapshot : dataSnapshot.child("media").getChildren())
                            // adding the media to the mediaUriList
                            mediaUrlList.add(mediaSnapshot.getValue().toString());
                    // creating an object of the message above
                    // --> checking what it needs to have inside (message id, sender id, message)
                    MessageObject mMessage = new MessageObject(dataSnapshot.getKey(), creatorID, text, mediaUrlList);
                    // adding the message to the messageList, updating the chat adaptor and notifying that something changed
                    messageList.add(mMessage);
                    // scrolling down to the last message anytime a message is added
                    mChatLayoutManager.scrollToPosition(messageList.size()-1);
                    mChatAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    int totalMediaUploaded = 0; // as iterator
    ArrayList<String> mediaIdList = new ArrayList<>();
    EditText mMessage;
    private void sendMessage(){
        mMessage = findViewById(R.id.messageInput);
        String messageId = mChatMessagesDb.push().getKey();
        final DatabaseReference newMessageDb = mChatMessagesDb.child(messageId);
        final Map newMessageMap = new HashMap<>();
        newMessageMap.put("creator", FirebaseAuth.getInstance().getUid());
        if(!mMessage.getText().toString().isEmpty()) // if message is empty, add some text because otherwise the media won't be sent
            newMessageMap.put("text", mMessage.getText().toString());
        if(!mediaUriList.isEmpty()){ // if it is not empty
            for (String mediaUri : mediaUriList){ // looping through all of the uri's that are in the mediaUriList
                // create an unique ID for this specific uri
                String mediaId = newMessageDb.child("media").push().getKey();
                mediaIdList.add(mediaId);
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("chat").child(mChatObject.getChatId()).child(messageId).child(mediaId);
                // uploading the file (by passing along the uri of the file that we want to save to the database storage)
                UploadTask uploadTask = filePath.putFile(Uri.parse(mediaUri));
                // getting the download url before wrapping it up
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // getting url
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // adding the uri to the map
                                // --> for the database storage
                                newMessageMap.put("/media/" + mediaIdList.get(totalMediaUploaded) + "/", uri.toString());
                                totalMediaUploaded++;  // increment
                                // meaning that the upload has been done successfully
                                if(totalMediaUploaded == mediaUriList.size())
                                    // making the change in the database
                                    updateDatabaseWithNewMessage(newMessageDb, newMessageMap);
                            }
                        });
                    }
                });
            }
        }else{
            if(!mMessage.getText().toString().isEmpty()) // if message is not empty, --> then database is updated with the new messageMap
                updateDatabaseWithNewMessage(newMessageDb, newMessageMap);
        }
    }
    private void updateDatabaseWithNewMessage(DatabaseReference newMessageDb, Map newMessageMap){
        newMessageDb.updateChildren(newMessageMap);
        mMessage.setText(null);
        mediaUriList.clear();
        mediaIdList.clear();
        totalMediaUploaded=0;
        mMediaAdapter.notifyDataSetChanged();

        // loop through the users in the chatObject, find the ones that are not equal with the current user's uid and send notification to them

        String message;

        if(newMessageMap.get("text") != null)
            message = newMessageMap.get("text").toString();
        else
            message = "Sent Media";

        for(UserObject mUser : mChatObject.getUserObjectArrayList()){
            if(!mUser.getUid().equals(FirebaseAuth.getInstance().getUid())){
                new SendNotification(message, "New Message", mUser.getNotificationKey());
            }
        }
    }

    private void initializeMessage() {
        messageList = new ArrayList<>();
        mChat= findViewById(R.id.messageList);
        mChat.setNestedScrollingEnabled(false);
        mChat.setHasFixedSize(false);
        mChatLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.VERTICAL, false);
        mChat.setLayoutManager(mChatLayoutManager);
        mChatAdapter = new MessageAdapter(messageList);
        mChat.setAdapter(mChatAdapter);
    }
    int PICK_IMAGE_INTENT = 1;
    ArrayList<String> mediaUriList = new ArrayList<>();
    private void initializeMedia() {
        mediaUriList = new ArrayList<>();
        mMedia= findViewById(R.id.mediaList);
        mMedia.setNestedScrollingEnabled(false);
        mMedia.setHasFixedSize(false);
        mMediaLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.HORIZONTAL, false);
        mMedia.setLayoutManager(mMediaLayoutManager);
        mMediaAdapter = new MediaAdapter(getApplicationContext(), mediaUriList);
        mMedia.setAdapter(mMediaAdapter);
    }
    private void openGallery() { // creating an intent that will call for the gallery to be opened and user will be able to choose media
        Intent intent = new Intent();
        // telling exactly the type of file that the user shall choose
        intent.setType("image/*");
        // telling the intent that the user is allowed to puck multiple images at once
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        // telling the intent the type of action that the user shall do --> (get some content)
        intent.setAction(intent.ACTION_GET_CONTENT);
        // getting the data that the user picked
        // --> getting a list of uri's (generalisation for the location of things)
        startActivityForResult(Intent.createChooser(intent, "Select Picture(s)"), PICK_IMAGE_INTENT);
        // PICK_IMAGE_INTENT -> the request code
        // which is returned in the onActivityResult
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){ // making sure everything went fine and the user was able to get the media
            if(requestCode == PICK_IMAGE_INTENT){ // if user picks just ONE media item
                if(data.getClipData() == null){ // meaning that the user picked one item
                    // then the uri of the media item is added to the list
                    mediaUriList.add(data.getData().toString());
                }else{ // else, if the user picked multiple media items
                    // looping through the list of picked media items and getting all of the uri's inside it
                    for(int i = 0; i < data.getClipData().getItemCount(); i++){
                        // adding the uri's to the mediaUriList
                        mediaUriList.add(data.getClipData().getItemAt(i).getUri().toString());
                    }
                }
                // POPULATE the recyclerView with the images (needing an adaptor)
                mMediaAdapter.notifyDataSetChanged();
            }
        }
    }
}
