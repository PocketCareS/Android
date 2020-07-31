/*
 * Copyright 2020 University at Buffalo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ub.pocketcares.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;

public class FirebaseIdWorker extends Worker {
    private Context workerContext;

    public FirebaseIdWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        workerContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        final Result[] workResult = new Result[1];
        workResult[0] = Result.success();
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.v("FirebaseID", "Could not generate firebase ID.");
                        workResult[0] = Result.retry();
                        return;
                    }
                    // Get new Instance ID token
                    String token = task.getResult().getToken();
                    Log.v("FirebaseID", "Firebase ID generated: " + token);
                    sendTokenToServer(token);
                    SharedPreferences firebaseTokenPref = workerContext.getSharedPreferences("FirebasePreference", MODE_PRIVATE);
                    SharedPreferences.Editor firebaseEditor = firebaseTokenPref.edit();
                    firebaseEditor.putString("InstanceID", token);
                    firebaseEditor.apply();
                });
        return workResult[0];
    }

    private static void sendTokenToServer(final String firebaseToken) {
        Runnable sendToken = () -> {
            HTTPHelper httpHelper = new HTTPHelper();
            try {
                JSONObject bluetoothName = new JSONObject();
                bluetoothName.put("deviceId", firebaseToken);
                httpHelper.postRequest(ServerHelper.USER_ENDPOINT, bluetoothName.toString(), "");
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        };
        Thread network = new Thread(sendToken);
        network.start();
    }
}
