package com.ub.pocketcares.network;


import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String newToken) {
        Log.v("Firebase_Log", "Firebase: " + newToken);
        JSONObject userJson = new JSONObject();
        SharedPreferences firebaseTokenPref = getSharedPreferences("FirebasePreference", MODE_PRIVATE);
        SharedPreferences.Editor firebaseEditor = firebaseTokenPref.edit();
        firebaseEditor.putString("InstanceID", newToken);
        firebaseEditor.apply();
        try {
            userJson.put("deviceId", newToken);
            sendTokenToServer(userJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendTokenToServer(final String firebaseToken) {
        Runnable sendToken = () -> {
            HTTPHelper httpHelper = new HTTPHelper();
            try {
                httpHelper.postRequest(ServerHelper.USER_ENDPOINT, firebaseToken, "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread network = new Thread(sendToken);
        network.start();
    }
}
