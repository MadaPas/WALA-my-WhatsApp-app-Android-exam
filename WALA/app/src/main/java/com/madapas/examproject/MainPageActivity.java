package com.madapas.examproject;
import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.madapas.examproject.Chat.ChatListAdapter;
import com.madapas.examproject.Chat.ChatObject;
import com.madapas.examproject.User.UserObject;
import com.madapas.examproject.Utils.SendNotification;
import com.onesignal.OneSignal;

import java.util.ArrayList;
public class MainPageActivity extends AppCompatActivity {
    private RecyclerView mChatList;
    private RecyclerView.Adapter mChatListAdapter;
    private RecyclerView.LayoutManager mChatListLayoutManager;
    ArrayList<ChatObject> chatList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);


        // initialize the OneSignal service
        OneSignal.startInit(this).init();

        // telling OneSignal that there are users that want to start receiving notifications
        OneSignal.setSubscription(true); // telling OneSignal platform that the app is open to receive notifications
        // Getting notificationId / key that allows to know to which user notification should be sent
        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) { // unique registrationId (userd for a specific user)
                // saving it in the database
                // getting the uid of the user that is currently logged in
                // so basically we get the notification key and send the notification to the user target
                FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.getInstance().getUid()).child("notificationKey").setValue(userId);

            }
        });
        //OneSignal DEFAULT option --> app is opened and notification appears as a dialog
        // BUT
        //  I am forcing the app to show it in the form of a notification in the toolbar
        OneSignal.setInFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification);
        // new SendNotification("message 1", "heading 1", null);


        //testing
        new SendNotification("message 1", "heading 1", null);
        // initialize the Fresco service
        Fresco.initialize(this);

        Button mLogout = findViewById(R.id.logout);
        Button mFindUser = findViewById(R.id.findUser);
        mFindUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), FindUserActivity.class));
            }
        });
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // unsubscribe the phone from the notification service once they are logged out
                OneSignal.setSubscription(false);

                FirebaseAuth.getInstance().signOut();
                // making sure user gets out of the page that he is in once he signs out
                // as maybe the page he is on is reserved only for logged in users
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class); // going to the login activity
                // flags --> telling the activity that it must kill all access to everything besides the login page
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        });
        getPermissions();
        initializeRecyclerView();
        getUserChatList();
    }

    // fetching all information so that the recycler view can be populated
    private void getUserChatList(){ // pointing to the user id --> then to the chat inside it

        DatabaseReference mUserChatDB = FirebaseDatabase.getInstance().getReference().child("user").child(FirebaseAuth.getInstance().getUid()).child("chat");

        // always listening to the database and checking for changes
        mUserChatDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    // looping through every single unique chat id that is inside this db reference
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                        // creating chat object
                        ChatObject mChat = new ChatObject(childSnapshot.getKey());
                        boolean  exists = false;
                        // giving all objects that are inside the chatList
                        for (ChatObject mChatIterator : chatList){
                            if (mChatIterator.getChatId().equals(mChat.getChatId()))
                                exists = true; // NO possibility to add something to the recyclerView
                        }
                        if (exists)
                            continue;
                        // adding chat to the list
                        chatList.add(mChat);
                        // CHANGING HOW I GET INFO FROM THE CHAT-LIST
                        getChatData(mChat.getChatId());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getChatData(String chatId) {
        DatabaseReference mChatDB = FirebaseDatabase.getInstance().getReference().child("chat").child(chatId).child("info");
        mChatDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String chatId = ""; // get the chat id

                    if(dataSnapshot.child("id").getValue() != null) // if there is a child
                        chatId = dataSnapshot.child("id").getValue().toString();

                    // loop and return the userSnapshots
                    for(DataSnapshot userSnapshot : dataSnapshot.child("users").getChildren()){
                        // loop through the chat array list to find the chat corresponding to this chatId
                        for(ChatObject mChat : chatList){
                            if(mChat.getChatId().equals(chatId)){ // if chat was found
                                // creating user object (get the user id)
                                UserObject mUser = new UserObject(userSnapshot.getKey());
                                // add the user in the chat object
                                mChat.addUserToArrayList(mUser);
                                // getting the user information (finally)
                                getUserData(mUser);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getUserData(UserObject mUser) {
        // accessing user info in the database
        DatabaseReference mUserDb = FirebaseDatabase.getInstance().getReference().child("user").child(mUser.getUid());
        mUserDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserObject mUser = new UserObject(dataSnapshot.getKey());

                if(dataSnapshot.child("notificationKey").getValue() != null)
                    mUser.setNotificationKey(dataSnapshot.child("notificationKey").getValue().toString());
                // loop through all the chats and see which chats have this particular user in the list
                for(ChatObject mChat : chatList){
                    for (UserObject mUserIt : mChat.getUserObjectArrayList()){ //user iterator
                        if(mUserIt.getUid().equals(mUser.getUid())){
                            // updating the user iterator
                            mUserIt.setNotificationKey(mUser.getNotificationKey());
                        }
                    }
                }
                mChatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void initializeRecyclerView() {
        chatList = new ArrayList<>();
        mChatList= findViewById(R.id.chatList);
        mChatList.setNestedScrollingEnabled(false);
        mChatList.setHasFixedSize(false);
        mChatListLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.VERTICAL, false);
        mChatList.setLayoutManager(mChatListLayoutManager);
        mChatListAdapter = new ChatListAdapter(chatList);
        mChatList.setAdapter(mChatListAdapter);
    }
    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS}, 1);
        }
    }
}
