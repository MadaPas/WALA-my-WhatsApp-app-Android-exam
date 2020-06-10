package com.madapas.examproject;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText mPhoneNumber, mCode;
    private Button mSend;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // getting everything started so FireBase can be used
        FirebaseApp.initializeApp(this);

        // checking if user is logged in so if it is, it does not have to go through all the steps of logging in :)
        userIsLoggedIn();

        mPhoneNumber = findViewById(R.id.phoneNumber);
        mCode = findViewById(R.id.code);

        mSend = findViewById(R.id.send);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mVerificationId != null)
                    verifyPhoneNumberWithCode();
                else
                    startPhoneNumberVerification();
            }
        });


        // generated mCallBacks
        // failure: maybe user inputs something wrong or message is not sent or anything not working, then the 'Failed' method will be called,
        // otherwise if everything goes well and the account is verified, then the 'Completed' method will be called
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
            }


            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(verificationId, forceResendingToken);

                mVerificationId = verificationId;
                // change the button text (after pressing)
                mSend.setText("Verify Code");
            }
        };

    }

    // getting the global values
    private void verifyPhoneNumberWithCode(){
        // creating a credential out of the 2 arguments
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, mCode.getText().toString());
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            // this task tells if the process of signing in was successful or not
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    // taking all info from the signIn (auth page not in the db)
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if(user != null){
                        // pointing to a place in the database ---> pointing to the userUid
                        final DatabaseReference mUserDB = FirebaseDatabase.getInstance().getReference().child("user").child(user.getUid());
                        // this listener fetches the data from the place in the db that we point above
                        mUserDB.addListenerForSingleValueEvent(new ValueEventListener() { // only need it once
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // dataSnapshot contains all the data from inside the userUid
                                if(!dataSnapshot.exists()){ //if there is nothing there --> input the phone and name in the db
                                    // using map because it allows to place multiple variables inside the db at once
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("phone", user.getPhoneNumber());
                                    userMap.put("name", user.getPhoneNumber()); // placeholder for now
                                    mUserDB.updateChildren(userMap); // sending the data to the specific place (pointed above)
                                }
                                userIsLoggedIn();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });
                    }

                }
            }
        });
    }
    private void userIsLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){  // double checking if the user is logged in
            // starting the new activity :)
            startActivity(new Intent(getApplicationContext(), MainPageActivity.class));
            finish(); // so that user can't return to this page
        }
    }

    private void startPhoneNumberVerification() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mPhoneNumber.getText().toString(),  // getting the input
                60,  // timeout
                TimeUnit.SECONDS,
                this,
                mCallbacks); // callback that will handle what happens next
        // failures or successes when user receives and uses the verification code
    }
}