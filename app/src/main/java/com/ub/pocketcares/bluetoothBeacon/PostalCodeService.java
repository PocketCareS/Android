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

package com.ub.pocketcares.bluetoothBeacon;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import com.ub.pocketcares.R;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.geofencing.GeofenceUtils;
import com.ub.pocketcares.utility.Utility;

public class PostalCodeService extends Service {
    private Context serviceContext = null;
    private static final float DISTANCE_THRESHOLD = 3000; // in meters
    private static final long TIME_INTERVAL = 1800000; // in milliseconds
    private FusedLocationProviderClient mLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("PostalService", "Service Started");
        serviceContext = this;
        startForeground(Utility.NOTIFICATION_ID, Utility.getNotification(serviceContext,
                getString(R.string.app_name) + " is running", new Intent(getApplicationContext(), MainActivity.class)).build());
        MonitoringApplication monitoringApplication = (MonitoringApplication) getApplicationContext();
        monitoringApplication.updateNotificationBehavior(true);
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(TIME_INTERVAL);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                final Location location = locationResult.getLastLocation();
                if (location != null) {
                    Geocoder geo = new Geocoder(serviceContext);
                    try {

                        Calendar currentMinute = Calendar.getInstance();
                        currentMinute.set(Calendar.SECOND, 0);
                        currentMinute.set(Calendar.MILLISECOND, 0);
                        long currentMinuteTime = currentMinute.getTimeInMillis();
                        SessionManager.logLocation(currentMinuteTime, location);

                        List<Address> place = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        String newPostalCode = place.get(0).getPostalCode();
                        if (newPostalCode != null) {
                            updatePostalCode(newPostalCode);
                        }
                    } catch (IOException | IndexOutOfBoundsException | NullPointerException e) {
                        // retry if not found
                        locationRequest.setFastestInterval(900000);
                        e.printStackTrace();
                    }
                }
            }

        };
        mLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (mLocationClient != null && locationCallback != null) {
            mLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updatePostalCode(String zip) {
        SharedPreferences zipPref = PreferenceManager.getDefaultSharedPreferences(serviceContext);
        String zipCode = zipPref.getString("postalCode", null);
        if (!zip.equals(zipCode)) {
            Log.v("PostalService", "Postal Code: " + zip);
            SharedPreferences.Editor zipEditor = zipPref.edit();
            zipEditor.putString("postalCode", zip);
            zipEditor.apply();
        }
    }

}

