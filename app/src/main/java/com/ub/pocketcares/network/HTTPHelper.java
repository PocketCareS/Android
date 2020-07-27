package com.ub.pocketcares.network;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HTTPHelper {
    private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public HTTPHelper() {
        this.client = new OkHttpClient();
    }

    public String getRequest(String url, String token) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("token", token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return Objects.requireNonNull(response.body()).string();
        }
    }

    public String postRequest(String url, String json, String token) throws IOException {
        Log.v("HTTP_check", "URL: " + url);
        Log.v("HTTP_check", "Body: " + json);
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .addHeader("token", token)
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseJson = response.body().string();
            Log.v("HTTP_check", "Response: " + responseJson);
            return responseJson;
        }
    }
}