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

package com.ub.pocketcares.home;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPush;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushException;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationListener;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushNotificationOptions;
import com.ibm.mobilefirstplatform.clientsdk.android.push.api.MFPPushResponseListener;
import com.ub.pocketcares.bluetoothBeacon.CloseContactFragment;
import com.ub.pocketcares.bluetoothBeacon.MonitoringApplication;
import com.ub.pocketcares.R;
import com.ub.pocketcares.bluetoothBeacon.PostalCodeService;
import com.ub.pocketcares.network.ServerHelper;
import com.ub.pocketcares.receiver.BootBroadCastReceiver;
import com.ub.pocketcares.survey.HealthData;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.PreferenceTags;
import com.ub.pocketcares.utility.Utility;

import static com.ub.pocketcares.home.HomeTabFragment.createReportHealthDialog;
import static com.ub.pocketcares.utility.PreferenceTags.IBM_PUSH_NOTIFICATION;

public class MainActivity extends AppCompatActivity {
    public static final int NOTI_HEALTHSTATUS = 2;
    public static final int ALARMID_DAILYHEALTH = 0;
    public static final int ALARMID_DAILY12OCLOCK = 2;
    public static final int ALARMID_OTHER = 99;
    public static final String ACTION_APP_START = "android.intent.action.appstart";
    public static final String INTENTEXTRA = "intentextra";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;
    public static final int LOCATION_PERMISSION_CODE = 101;
    private static final int PERMISSION_RESULT = 3;

    public static MainActivity m_mainActivity = null;
    public static boolean m_isActive = false;
    public static final String CHANNEL_ID = "default-channel";
    public static final String CHANNEL_ID_2 = "high-channel";
    public static final String BLUETOOTH_DIALOG = "com.ub.pocketcares.bluetoothDialog";
    private BluetoothAdapter myDevice;
    private BroadcastReceiver bluetoothLocationDialogReceiver;

    private MFPPush push; // Push client
    private MFPPushNotificationListener notificationListener; // Notification listener to handle a push sent to the phone
    private static final String APP_GUID = "2abe5c40-d5aa-4ff0-9b2e-d76327e76ee6";
    private static final String CLIENT_SECRET = "050967e6-3e93-440f-a09d-22c493ee5566";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_mainActivity = this;

        BMSClient.getInstance().initialize(this, BMSClient.REGION_US_SOUTH);
        push = MFPPush.getInstance();
        MFPPushNotificationOptions options = new MFPPushNotificationOptions();
        options.setDeviceid(ServerHelper.getDeviceId(m_mainActivity));
        options.setIcon("R.drawable.pocketcares_notification_icon");
        options.setPriority(MFPPushNotificationOptions.Priority.HIGH);
        push.initialize(this, APP_GUID, CLIENT_SECRET, options);
        notificationListener = message -> {
            Log.v(IBM_PUSH_NOTIFICATION, "Received a Push Notification: " + message.toString());
            runOnUiThread(() -> Utility.createDialog(m_mainActivity, "You have been exposed to COVID-19 virus", message.getAlert()));
        };
        registerDevice();
        myDevice = BluetoothAdapter.getDefaultAdapter();
        BottomNavigationView navView = findViewById(R.id.nav_view);
        startCloseEncounterScan();

        ComponentName receiver = new ComponentName(this, BootBroadCastReceiver.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.health_summary_item, R.id.close_contacts,
                R.id.sessions, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        SharedPreferences devPreference = PreferenceManager.getDefaultSharedPreferences(m_mainActivity);
        boolean devLog = devPreference.getBoolean("devLogVisible", false);
        if (!devLog) {
            Menu nav_Menu = navView.getMenu();
            nav_Menu.findItem(R.id.sessions).setVisible(false);
        }

        ColorStateList csl = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked}, // unchecked
                        new int[]{android.R.attr.state_checked}  // checked
                },
                new int[]{
                        Color.WHITE,
                        Color.BLACK
                }
        );
        navView.setItemTextColor(csl);
        navView.setItemIconTintList(csl);


        View view = getLayoutInflater().inflate(R.layout.action_bar, null);
        androidx.appcompat.app.ActionBar.LayoutParams params = new androidx.appcompat.app.ActionBar.LayoutParams(
                androidx.appcompat.app.ActionBar.LayoutParams.WRAP_CONTENT,
                androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);

        TextView Title = view.findViewById(R.id.actionbar_title);
        Title.setText(getString(R.string.app_name));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ub)));
        getSupportActionBar().setCustomView(view, params);
        getSupportActionBar().setDisplayShowCustomEnabled(true); //show custom title

        ImageView reportHealth = view.findViewById(R.id.report_image);
        reportHealth.setVisibility(View.VISIBLE);
        reportHealth.setOnClickListener(v -> {
            Log.v(LogTags.GENERALINFO, "Inside button onClick!");
            createReportHealthDialog(m_mainActivity);
        });

        // load data submission status
        SharedPreferences datastatus = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor ed = datastatus.edit();

        // set first run time
        Calendar now = Calendar.getInstance();
        ed.putInt(PreferenceTags.STATUS_ISTRUN_TIME, Utility.getCalendarInt(now));

        // set first run false
        ed.putBoolean(PreferenceTags.STATUS_1STRUN, false);

        ed.putLong(PreferenceTags.WARNING_LAST_TIME, System.currentTimeMillis());

        ed.apply();

        sendBroadcast(new Intent(ACTION_APP_START));
        Runnable backgroundWork = () -> {
            MonitoringApplication monitoringApplication = (MonitoringApplication) getApplicationContext();
            monitoringApplication.registerBluetoothLocationStateReceiver();
            monitoringApplication.registerScanStatusReceiver();
            monitoringApplication.registerNotificationReceiver();
            monitoringApplication.registerNotificationSnoozeReceiver();
            registerBluetoothLocationDialogReceiver();
        };
        new Thread(backgroundWork).start();
        SharedPreferences notificationPref = PreferenceManager.getDefaultSharedPreferences(m_mainActivity);
        long value = notificationPref.getLong("reminderAlarmValue", -1);
        if (value >= 0) {
            MonitoringApplication monitoringApplication = (MonitoringApplication) getApplicationContext();
            Calendar snoozeOverTime = Calendar.getInstance();
            snoozeOverTime.setTimeInMillis(value);
            monitoringApplication.createNotificationSnoozeOverAlarm(m_mainActivity, snoozeOverTime);
        }
    }

    public void registerDevice() {

        // Checks for null in case registration has failed previously
        if (push == null) {
            push = MFPPush.getInstance();
        }

        // Creates response listener to handle the response when a device is registered.
        MFPPushResponseListener registrationResponseListener = new MFPPushResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                // Split response and convert to JSON object to display User ID confirmation from the backend
                try {
                    Log.v(IBM_PUSH_NOTIFICATION, response);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.v(IBM_PUSH_NOTIFICATION, "Successfully registered for push notifications with device id:" + ServerHelper.getDeviceId(m_mainActivity));
                // Start listening to notification listener now that registration has succeeded
                push.listen(notificationListener);
            }

            @Override
            public void onFailure(MFPPushException exception) {
                String errLog = "Error registering for push notifications: ";
                String errMessage = exception.getErrorMessage();
                int statusCode = exception.getStatusCode();

                // Set error log based on response code and error message
                if (statusCode == 401) {
                    errLog += "Cannot authenticate successfully with Bluemix Push instance, ensure your CLIENT SECRET was set correctly.";
                } else if (statusCode == 404 && errMessage.contains("Push GCM Configuration")) {
                    errLog += "Push GCM Configuration does not exist, ensure you have configured GCM Push credentials on your Bluemix Push dashboard correctly.";
                } else if (statusCode == 404 && errMessage.contains("PushApplication")) {
                    errLog += "Cannot find Bluemix Push instance, ensure your APPLICATION ID was set correctly and your phone can successfully connect to the internet.";
                } else if (statusCode >= 500) {
                    errLog += "Bluemix and/or your Push instance seem to be having problems, please try again later.";
                }

                Log.v(IBM_PUSH_NOTIFICATION, errLog);
            }
        };

        push.registerDevice(registrationResponseListener);
    }

    public void onSummaryClick(View view) {
        NavController navController = Navigation.findNavController(MainActivity.m_mainActivity, R.id.nav_host_fragment);
        if (view.getId() == R.id.encounterSummary) {
            navController.navigate(R.id.close_contacts);
        } else if (view.getId() == R.id.healthSummary) {
            if (HealthData.isHealthDailySubmitted(m_mainActivity)) {
                navController.navigate(R.id.health_summary_item);
            } else {
                createReportHealthDialog(m_mainActivity);
            }
        }
    }

    private void registerBluetoothLocationDialogReceiver() {
        bluetoothLocationDialogReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String dialogValue = intent.getStringExtra("dialog");
                if (dialogValue.equals("bluetooth")) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else if (dialogValue.equals("location")) {
                    locationEnableDisplay();
                } else if (dialogValue.equals("location_permission")) {
                    DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
                        Intent location_permission_settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        location_permission_settings.setData(uri);
                        startActivityForResult(location_permission_settings, PERMISSION_RESULT);
                    };
                    Utility.createDialog(m_mainActivity, "Enable Location Permission",
                            "You need to enable location permission to see close encounters", onClickListener);

                }
            }
        };
        m_mainActivity.registerReceiver(bluetoothLocationDialogReceiver, new IntentFilter(BLUETOOTH_DIALOG));
    }

    private void locationEnableDisplay() {
        DialogInterface.OnClickListener positiveListener = (dialog, which) -> {
            dialog.dismiss();
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(locationIntent, REQUEST_ENABLE_LOCATION);
        };
        DialogInterface.OnClickListener negativeListener = (dialog, which) -> {
            dialog.cancel();
            Toast.makeText(m_mainActivity, getString(R.string.location_off), Toast.LENGTH_SHORT).show();
        };
        Utility.createDialog(m_mainActivity, "Location Turned Off", getString(R.string.location_off),
                positiveListener, negativeListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (push != null) {
            push.listen(notificationListener);
        }
        Intent updateUI = new Intent(CloseContactFragment.ACTION_NOTIFY_BEACON_UI_UPDATE);
        sendBroadcast(updateUI);
        m_isActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (push != null) {
            push.hold();
        }
        m_isActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        m_isActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_isActive = false;
        unregisterReceiver(bluetoothLocationDialogReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannel(String chanel, String name, int importance, Context context) {
        NotificationChannel notificationChannel = new NotificationChannel(chanel, name, importance);
        NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(notificationChannel);
    }

    private void startCloseEncounterScan() {
        LocationManager locationManager = (LocationManager) m_mainActivity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean locationEnabled = isGpsEnabled || isNetworkEnabled;

        MonitoringApplication application = (MonitoringApplication) getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(application, locationEnabled, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION});
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission(application, locationEnabled, new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    public static void postalServiceIntent(final Intent serviceIntent, final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission(MonitoringApplication application, boolean locationEnabled,
                                   final String[] perms) {
        boolean bothEnabled = false;
        boolean showRationale = false;
        for (String permission : perms) {
            bothEnabled = this.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            showRationale = this.shouldShowRequestPermissionRationale(permission);
        }
        if (bothEnabled) {
            // Perfect
            if (myDevice != null) {
                if (myDevice.isEnabled() && locationEnabled) {
                    Intent serviceIntent = new Intent(m_mainActivity, PostalCodeService.class);
                    postalServiceIntent(serviceIntent, m_mainActivity);
                    application.startTransmissionScan();
                } else if (!myDevice.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else if (!locationEnabled) {
                    locationEnableDisplay();
                }
            }
        } else {
            application.stopTransmissionScan(false);
            if (showRationale) {
                DialogInterface.OnDismissListener dismissListener = dialog -> requestPermissions(perms, LOCATION_PERMISSION_CODE);
                Utility.createDialog(m_mainActivity, "This app needs background location access",
                        "Please grant location access so this app can detect beacons even in the background.",
                        null, dismissListener);
            } else {
                Toast.makeText(m_mainActivity, "Permanently Denied Permission Request.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean getTermAcceptance(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PreferenceTags.STATUS_ACCEPTENCE, false);
    }

    public static Context getAppContext() {
        return m_mainActivity;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCloseEncounterScan();
            } else {
                Toast.makeText(m_mainActivity, "Location Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v("Perms", "Activity Result: " + requestCode);
        if (requestCode == REQUEST_ENABLE_BT || requestCode == REQUEST_ENABLE_LOCATION ||
                requestCode == PERMISSION_RESULT) {
            if (resultCode == RESULT_OK) {
                startCloseEncounterScan();
            }
        }
    }

}