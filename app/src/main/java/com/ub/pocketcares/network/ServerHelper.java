package com.ub.pocketcares.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.util.Pair;

import com.google.gson.Gson;
import com.ub.pocketcares.bluetoothBeacon.CloseContactDailyData;
import com.ub.pocketcares.bluetoothBeacon.CloseContactHourlyData;
import com.ub.pocketcares.bluetoothBeacon.ContactAnalyticsResponse;
import com.ub.pocketcares.utility.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;

public class ServerHelper {
    private final static String productionHost = "https://pcpprd-app.acsu.buffalo.edu";
    public final static String USER_ENDPOINT = productionHost + "/user";
    public final static String USER_INFO = productionHost + "/user/info";
    public final static String LOCATION_ENDPOINT = productionHost + "/upload/location";
    public final static String CONTACT_ENDPOINT = productionHost + "/upload/contactlist";
    public final static String SYMPTOMS_ENDPOINT = productionHost + "/user/symptoms";
    public final static String ANALYTICS_ENDPOINT = productionHost + "/analytics/contactData?startDate=%d&endDate=%d&contactType=close";

    public static String generateToken(Context context, HTTPHelper helper) throws JSONException, IOException {
        JSONObject bluetoothName = new JSONObject();
        bluetoothName.put("deviceId", getDeviceId(context));
        String tokenResponse = helper.postRequest(ServerHelper.USER_ENDPOINT, bluetoothName.toString(), "");
        JSONObject serverResponse = new JSONObject(tokenResponse);
        return (String) serverResponse.get("token");
    }

    private static String getDeviceId(Context context) {
        SharedPreferences firebaseTokenPref = context.getSharedPreferences("FirebasePreference", MODE_PRIVATE);
        return firebaseTokenPref.getString("InstanceID", null);
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
        } catch (Exception e) {
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
