package com.ub.pocketcares.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.ub.pocketcares.bluetoothBeacon.CloseContactDailyData;
import com.ub.pocketcares.bluetoothBeacon.ContactAnalyticsResponse;
import com.ub.pocketcares.utility.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;

public class ServerHelper {
    private final static String serverHost = "https://pocketcares-server-app-pocketcares.mycluster-dal10-b-746843-c6bcb6f7fc0a61609dee42a1778bf377-0000.us-south.containers.appdomain.cloud";
    public final static String USER_ENDPOINT = serverHost + "/user";
    public final static String CONTACT_ENDPOINT = serverHost + "/upload/contactlist";
    public final static String SYMPTOMS_ENDPOINT = serverHost + "/user/symptoms";
    public final static String ANALYTICS_ENDPOINT = serverHost + "/analytics/contactData?startDate=%d&endDate=%d&contactType=close";

    public static String generateToken(Context context, HTTPHelper helper) throws JSONException, IOException {
        JSONObject bluetoothName = new JSONObject();
        bluetoothName.put("deviceId", getDeviceId(context));
        String tokenResponse = helper.postRequest(ServerHelper.USER_ENDPOINT, bluetoothName.toString(), "");
        JSONObject serverResponse = new JSONObject(tokenResponse);
        return (String) serverResponse.get("token");
    }

    public static String getDeviceId(Context context) {
        SharedPreferences firebaseTokenPref = context.getSharedPreferences("FirebasePreference", MODE_PRIVATE);
        String token = firebaseTokenPref.getString("InstanceID", null);
        if (token == null) {
            generateFirebaseId(context);
        }
        return token;
    }

    public static void generateFirebaseId(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest firebaseIdGeneration = new OneTimeWorkRequest.Builder(FirebaseIdWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork("firebaseIdWork", ExistingWorkPolicy.KEEP, firebaseIdGeneration);
    }


    private static TreeMap<Long, CloseContactDailyData> dailyAnalyticsHelper(long startDate, long endDate, Context context) throws IOException, JSONException {
        try {
            String inputURL = String.format(Locale.getDefault(), ANALYTICS_ENDPOINT, startDate, endDate);
            HTTPHelper httpHelper = new HTTPHelper();
            String token = generateToken(context, httpHelper);
            String responseJson = httpHelper.getRequest(inputURL, token);
            Gson gson = new Gson();
            ContactAnalyticsResponse contactAnalyticsResponse = gson.fromJson(responseJson, ContactAnalyticsResponse.class);
            return contactAnalyticsResponse.getContactCount();
        } catch (NullPointerException e) {
            ContactAnalyticsResponse emptyResponse = new ContactAnalyticsResponse();
            return emptyResponse.getContactCount();
        }
    }

    public static TreeMap<Long, CloseContactDailyData> getDailyAnalytics(String startDate, String endDate, Context context) throws IOException, JSONException {
        long startInput = Utility.convertDayMills(Utility.getUnixFromString(startDate));
        long endInput = Utility.convertDayMills(Utility.getUnixFromString(endDate));
        return dailyAnalyticsHelper(startInput, endInput, context);
    }

    public static CloseContactDailyData getDailyAnalytics(String date, Context context) throws IOException, JSONException {
        long timeStamp = Utility.getUnixFromString(date);
        long dateInput = Utility.convertDayMills(timeStamp);
        CloseContactDailyData returnData = dailyAnalyticsHelper(dateInput, dateInput, context).get(dateInput);
        if (returnData == null) {
            return new CloseContactDailyData();
        } else {
            return returnData;
        }
    }
}
