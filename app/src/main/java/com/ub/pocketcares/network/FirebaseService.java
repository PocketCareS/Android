package com.ub.pocketcares.network;


import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import com.ub.pocketcares.home.MainActivity;

import static com.ub.pocketcares.home.MainActivity.CHANNEL_ID;
import static com.ub.pocketcares.home.MainActivity.getAppContext;

public class FirebaseService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Firebase Notification")
                .setContentText("Firebase Works")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(2, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String newToken) {
        Log.v("Firebase_Log", "Firebase: " + newToken);
        JSONObject userJson = new JSONObject();
        SharedPreferences firebaseTokenPref = getSharedPreferences("FirebasePreference", MODE_PRIVATE);
        SharedPreferences.Editor firebaseEditor = firebaseTokenPref.edit();
        firebaseEditor.putString("InstanceID", newToken);
        firebaseEditor.apply();
//        boolean firstRun = !FirstTimeChecker.getBooleanPreferenceValue(this, "isFirstTimeExecution");
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
