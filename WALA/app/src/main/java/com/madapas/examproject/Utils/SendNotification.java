package com.madapas.examproject.Utils;

import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

public class SendNotification {
    // sending message
    public SendNotification(String message, String heading, String notificationKey){
        // for now, notifications will always go to this user
//        notificationKey = "c52b559e-bce5-4b91-a765-fa08ad7de2e0"; // user3

        // building a json object that has the content with the messages and the images that they should appear in the notification
        try {
            JSONObject notificationContent = new JSONObject(
                            "{'contents':{'en':'" + message + "'},"+                // message
                            "'include_player_ids':['" + notificationKey + "']," +   // id of the user that has to receive the notification
                            "'headings':{'en': '" + heading + "'}}");               // title of the notification
            // sending the notification
            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}