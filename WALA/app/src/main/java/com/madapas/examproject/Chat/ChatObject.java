package com.madapas.examproject.Chat;

import com.madapas.examproject.User.UserObject;

import java.io.Serializable;
import java.util.ArrayList;

public class ChatObject implements Serializable { // in order to pass on objects through activities
    private String chatId;

    private ArrayList<UserObject> userObjectArrayList = new ArrayList<>();

    public ChatObject(String chatId){
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }
    public ArrayList<UserObject> getUserObjectArrayList() {
        return userObjectArrayList;
    }


    public void addUserToArrayList(UserObject mUser){
        userObjectArrayList.add(mUser);
    }
}