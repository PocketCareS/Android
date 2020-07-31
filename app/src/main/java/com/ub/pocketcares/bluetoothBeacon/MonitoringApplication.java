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
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ArmaRssiFilter;
import org.altbeacon.beacon.service.RssiFilter;
import org.altbeacon.beacon.service.RunningAverageRssiFilter;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.ub.pocketcares.BuildConfig;
import com.ub.pocketcares.backend.BluetoothBeaconDatabaseHelper;
import com.ub.pocketcares.backend.SecureKeys;
import com.ub.pocketcares.R;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.settings.PreferenceActivity;
import com.ub.pocketcares.settings.SettingStatic;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.Utility;

import static com.ub.pocketcares.bluetoothBeacon.SessionManager.SHARED_PREF_NAME_IS_ON_CAMPUS;
import static com.ub.pocketcares.utility.LogTags.BLE_TAG;


public class MonitoringApplication extends Application implements BootstrapNotifier, BeaconConsumer {
    private static final String iBeacon = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String overflowArea = "m:2-3=017f,i:3-3,i:4-5,i:6-7,i:8-11,p:-:-59";
    private boolean haveDetectedBeaconsSinceBoot = false;
    public static final int overflowAreaTypeCode = 383;
    private RegionBootstrap regionBootstrap = null;
    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter = null;
    public static final int STOP_BLE = 0;
    public static final int START_BLE = 1;
    public static final int DISTANCING_NOTIFICATION_CODE = 101;
    private HashMap<Beacon, BeaconStat> beaconTracker;
    private static final long SCAN_PERIOD = 10000L; // in milliseconds
    private static final long SCAN_BETWEEN_PERIOD = 50000L; // in milliseconds
    public static final int TRANSMISSION_SWITCH_COOL_DOWN = 5000; // in milliseconds
    BroadcastReceiver scanStatusReceiver;
    BroadcastReceiver bluetoothLocationStateReceiver;
    BroadcastReceiver notificationReceiver;
    BroadcastReceiver notificationSnoozeReceiver;
    public static final String SCAN_BLE_ALARM = "com.ub.pocketcares.bleAlarm";
    public static final String NOTIFICATION_RECEIVE_INTENT = "com.ub.pocketcares.notificationReceiver";
    public static final String SNOOZE_HOUR_ACTION = "snoozeHour";
    public static final String SNOOZE_DAY_ACTION = "snoozeDay";
    public static final String DEFAULT_REMINDER_ACTION = "default";
    public static final String NOTIFICATION_SNOOZE_OVER_ALARM = "com.ub.pocketcares.snoozeOver";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            MainActivity.createNotificationChannel(MainActivity.CHANNEL_ID, "Foreground Notification",
                    NotificationManager.IMPORTANCE_LOW, getApplicationContext());
            MainActivity.createNotificationChannel(MainActivity.CHANNEL_ID_2, "Reminder Notification",
                    NotificationManager.IMPORTANCE_HIGH, getApplicationContext());
        }
        beaconTracker = new HashMap<>();
        populateDataFromDB();
        BeaconManager.setRssiFilterImplClass(AverageRSSIFilter.class);
        SessionManager.initializeSessionData(getApplicationContext());
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(iBeacon));
        beaconManager.getBeaconParsers().add(new BeaconParser("overflowArea").setBeaconLayout(overflowArea));
        NotificationCompat.Builder notificationBuilder = Utility.getNotification(getApplicationContext(),
                getString(R.string.app_name) + " is running", new Intent(getApplicationContext(), MainActivity.class));
        beaconManager.enableForegroundServiceScanning(notificationBuilder.build(), Utility.NOTIFICATION_ID);
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setForegroundScanPeriod(SCAN_PERIOD);
        beaconManager.setForegroundBetweenScanPeriod(SCAN_BETWEEN_PERIOD);
        beaconManager.setBackgroundScanPeriod(SCAN_PERIOD);
        beaconManager.setBackgroundBetweenScanPeriod(SCAN_BETWEEN_PERIOD);
        updateNotificationBehavior(true);
        regionBootstrap = new RegionBootstrap(this, new Region("backgroundRegion",
                null, null, null));
        beaconManager.bind(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // this is to simulate beacons
        /*BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();*/
    }

    public void populateDataFromDB() {
        Runnable populateDB = () -> {
            BluetoothBeaconDatabaseHelper bluetoothBeaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(getApplicationContext());
            beaconTracker = bluetoothBeaconDatabaseHelper.getCurrentHourData();
            Log.v(BLE_TAG, beaconTracker.toString());
        };
        new Thread(populateDB).start();
    }

    private void emptyCurrentData() {
        beaconTracker.clear();
    }

    public boolean updateNotificationBehavior(boolean isLocationPermissionEnabled) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean locationEnabled = isGpsEnabled || isNetworkEnabled;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isOffCampusScanStopped = preferences.getBoolean("stop_off_campus_scan", false);
        boolean isOnCampus = preferences.getBoolean(SHARED_PREF_NAME_IS_ON_CAMPUS, false);
        boolean isDownTimeActive = preferences.getBoolean("downtime_active", false);
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        String notificationText = "No bluetooth adapter found.";
        boolean canRun = false;
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                if (MainActivity.m_isActive) {
                    Intent enableBtIntent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                    enableBtIntent.putExtra("dialog", "bluetooth");
                    getApplicationContext().sendBroadcast(enableBtIntent);
                }
                notificationIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                notificationText = getString(R.string.bluetooth_off);
            } else if (!locationEnabled) {
                if (MainActivity.m_isActive) {
                    Intent enableLocationIntent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                    enableLocationIntent.putExtra("dialog", "location");
                    getApplicationContext().sendBroadcast(enableLocationIntent);
                }
                notificationIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                notificationText = getString(R.string.location_off);
            } else if (!isLocationPermissionEnabled) {
                notificationIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                notificationText = "PocketCare S needs background location permission enabled";
            } else if (isDownTimeActive) {
                notificationIntent = new Intent(getApplicationContext(), PreferenceActivity.class);
                notificationText = getString(R.string.app_name) + " is not running (Downtime)";
            } else if (isOffCampusScanStopped && !isOnCampus) {
                notificationIntent = new Intent(getApplicationContext(), PreferenceActivity.class);
                notificationText = getString(R.string.app_name) + " is not running (Off-Campus)";
            } else {
                notificationText = getString(R.string.app_name) + " is running";
                canRun = true;
            }
        }
        Utility.updateNotification(getApplicationContext(), notificationText, notificationIntent);
        return canRun;
    }

    public void registerBluetoothLocationStateReceiver() {
        bluetoothLocationStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.v(BLE_TAG, "Bluetooth State broadcast, action: " + action);
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            stopTransmissionScan(true);
                            if (MainActivity.m_isActive) {
                                Intent dialogIntent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                                dialogIntent.putExtra("dialog", "bluetooth");
                                getApplicationContext().sendBroadcast(dialogIntent);
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            startTransmissionScan();
                            break;
                    }
                }
                if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    Log.v(BLE_TAG, "GPS Location: " + isGpsEnabled);
                    Log.v(BLE_TAG, "Network Location: " + isNetworkEnabled);
                    if (!isGpsEnabled && !isNetworkEnabled) {
                        stopTransmissionScan(true);
                        if (MainActivity.m_isActive) {
                            Intent dialogIntent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                            dialogIntent.putExtra("dialog", "location");
                            getApplicationContext().sendBroadcast(dialogIntent);
                        }
                    } else {
                        startTransmissionScan();
                    }
                }
            }
        };
        getApplicationContext().registerReceiver(bluetoothLocationStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        getApplicationContext().registerReceiver(bluetoothLocationStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    public void registerScanStatusReceiver() {
        try {
            scanStatusReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v(BLE_TAG, "Alarm Received");
                    String downTimeType = intent.getStringExtra("type");
                    if (downTimeType != null) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor preferenceEditor = preferences.edit();
                        if (downTimeType.equals("downtime_start")) {
                            preferenceEditor.putBoolean("downtime_active", true);
                        } else {
                            preferenceEditor.putBoolean("downtime_active", false);
                        }
                        preferenceEditor.apply();
                    }
                    if (START_BLE == intent.getIntExtra(LogTags.ALARM, -1)) {
                        Log.v(BLE_TAG, "START BLE");
                        startTransmissionScan();
                    }
                    if (STOP_BLE == intent.getIntExtra(LogTags.ALARM, -1)) {
                        Log.v(BLE_TAG, "STOP BLE");
                        stopTransmissionScan(true);
                    }
                }
            };
            getApplicationContext().registerReceiver(scanStatusReceiver, new IntentFilter(SCAN_BLE_ALARM));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void registerNotificationReceiver() {
        try {
            notificationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String snoozeValue = intent.getStringExtra("snoozeValue");
                    Log.v("Reminder", "Snooze Value: " + snoozeValue);
                    Calendar snoozeTime = Calendar.getInstance();
                    SharedPreferences snoozePreference =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor snoozePreferenceEditor = snoozePreference.edit();
                    if (snoozeValue.equals(SNOOZE_HOUR_ACTION)) {
                        snoozeTime.add(Calendar.HOUR, 1);
                        createNotificationSnoozeOverAlarm(context, snoozeTime);
                        snoozePreferenceEditor.putString("close_encounter_snooze_option", SNOOZE_HOUR_ACTION);
                        Toast.makeText(context, "You snoozed close encounter reminders for a hour.", Toast.LENGTH_SHORT).show();
                    }
                    if (snoozeValue.equals(SNOOZE_DAY_ACTION)) {
                        snoozeTime.add(Calendar.DATE, 1);
                        createNotificationSnoozeOverAlarm(context, snoozeTime);
                        snoozePreferenceEditor.putString("close_encounter_snooze_option", SNOOZE_DAY_ACTION);
                        Toast.makeText(context, "You snoozed close encounter reminders for a day.", Toast.LENGTH_SHORT).show();
                    }
                    if (snoozeValue.equals(DEFAULT_REMINDER_ACTION)) {
                        snoozePreferenceEditor.putString("close_encounter_snooze_option", DEFAULT_REMINDER_ACTION);
                        createNotificationSnoozeOverAlarm(context, Calendar.getInstance());
                        Toast.makeText(context, "Thank You for being mindful.", Toast.LENGTH_SHORT).show();
                    }
                    snoozePreferenceEditor.apply();
                    NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.cancel(DISTANCING_NOTIFICATION_CODE);
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        getApplicationContext().registerReceiver(notificationReceiver, new IntentFilter(NOTIFICATION_RECEIVE_INTENT));
    }

    public void registerNotificationSnoozeReceiver() {
        notificationSnoozeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    SharedPreferences notificationPref = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor notificationEditor = notificationPref.edit();
                    notificationEditor.putLong("reminderAlarmValue", -1);
                    notificationEditor.putString("close_encounter_snooze_option", DEFAULT_REMINDER_ACTION);
                    notificationEditor.apply();
                } catch (Exception e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    e.printStackTrace();
                }
            }
        };
        getApplicationContext().registerReceiver(notificationSnoozeReceiver, new IntentFilter(NOTIFICATION_SNOOZE_OVER_ALARM));
    }

    public void createNotificationSnoozeOverAlarm(Context context, Calendar alarmTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(NOTIFICATION_SNOOZE_OVER_ALARM);
        PendingIntent pi = PendingIntent.getBroadcast(context, 50, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT < 23) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pi);
        }
        SharedPreferences notificationPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor notificationEditor = notificationPref.edit();
        notificationEditor.putLong("reminderAlarmValue", alarmTime.getTimeInMillis());
        notificationEditor.apply();
    }

    public void startTransmissionScan() {
        if (MainActivity.getTermAcceptance(getApplicationContext()) && updateNotificationBehavior(true)) {
            startScan();
            startTransmission();
            Log.i(BLE_TAG, "Advertisement & Scan start succeeded.");
        }
    }


    public void stopTransmissionScan(boolean locationPermission) {
        stopScan();
        stopTransmission();
        updateNotificationBehavior(locationPermission);
        Log.i(BLE_TAG, "Advertisement & Scan stop succeeded.");
    }

    public void startTransmission() {
        if (beaconTransmitter == null) {
            Pair<Integer, Integer> minorMajor = SecureKeys.getVBT(getApplicationContext());
            Beacon beacon = new Beacon.Builder()
                    .setId1(SecureKeys.UUID)
                    .setId2(Integer.toString(minorMajor.first))
                    .setId3(Integer.toString(minorMajor.second))
                    .setManufacturer(0x004C)
                    .setTxPower(-59)
                    .build();

            BeaconParser beaconParser = new BeaconParser()
                    .setBeaconLayout(iBeacon);
            beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
            beaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
            beaconTransmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
            beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(BLE_TAG, "Advertisement start failed with code: " + errorCode);
                }

                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(BLE_TAG, "Advertisement start succeeded: " + settingsInEffect.toString());
                }
            });
        }
    }

    public void stopTransmission() {
        if (beaconTransmitter != null) {
            beaconTransmitter.stopAdvertising();
            beaconTransmitter = null;
        }
    }

    public void startScan() {
        beaconManager.bind(this);
        if (regionBootstrap == null) {
            Region region = new Region("backgroundRegion",
                    null, null, null);
            regionBootstrap = new RegionBootstrap(this, region);
        }
    }

    public void stopScan() {
        if (regionBootstrap != null) {
            beaconManager.unbind(this);
            regionBootstrap.disable();
            regionBootstrap = null;
        }
    }

    public void startTransmissionPostalOnRestart() {
        if (!MainActivity.isMyServiceRunning(PostalCodeService.class, getApplicationContext())) {
            Intent startIntent = new Intent(getApplicationContext(), PostalCodeService.class);
            MainActivity.postalServiceIntent(startIntent, getApplicationContext());
        }
        if (beaconTransmitter == null) {
            startTransmissionScan();
        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.v(BLE_TAG, "Entered Region callback");
    }

    @Override
    public void didExitRegion(Region region) {
        Log.v(BLE_TAG, "Exit Region.");
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.v(BLE_TAG, "Current region state is: " + (state == 1 ? "INSIDE" : "OUTSIDE (" + state + ")"));
    }

    @Override
    public void onBeaconServiceConnect() {
        @SuppressLint("MissingPermission") RangeNotifier rangeNotifier = (beacons, region) -> {
            try {
                if (!haveDetectedBeaconsSinceBoot) {
                    startTransmissionPostalOnRestart();
                    haveDetectedBeaconsSinceBoot = true;
                }
                Calendar currentTime = Calendar.getInstance();
                currentTime.set(Calendar.SECOND, 0);
                currentTime.set(Calendar.MILLISECOND, 0);
                long currentMinuteTime = currentTime.getTimeInMillis();

                Collection<Beacon> updatedBeaconsList = updateBeaconsWithOverflow(beacons);
                if (SettingStatic.TOAST_LOGS) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String date = dateFormat.format(new Date(currentMinuteTime));
                    Toast.makeText(getApplicationContext(), "BC Ranger invoked at: " + date + ". BC seen: " + updatedBeaconsList.size(),
                            Toast.LENGTH_LONG).show();
                }

                Log.v(BLE_TAG, "didrange callback");
                MonitoringApplication monitoringApplication = (MonitoringApplication) getApplicationContext();
                HashMap<Beacon, HashMap<Long, Float>> beaconDistances = SessionManager.updateAndGetDistancesFromEncounters(updatedBeaconsList, currentMinuteTime);
                if (SessionManager.isGPSTriggerRequired(updatedBeaconsList, beaconDistances, currentMinuteTime)) {
                    Pair<Boolean, String> cachedLocation = SessionManager.getCachedLocation(currentMinuteTime);
                    if (cachedLocation == null) {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                            if (location != null) {
                                SessionManager.logLocation(currentMinuteTime, location);
                                SessionManager.manageSessions(monitoringApplication, updatedBeaconsList, beaconDistances, currentMinuteTime);
                            }
                        }).addOnFailureListener(e -> {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            SessionManager.logException(currentMinuteTime, e.getLocalizedMessage(), e.getMessage(), e.toString());
                            SessionManager.manageSessions(monitoringApplication, updatedBeaconsList, beaconDistances, currentMinuteTime);
                        });
                    } else {
                        boolean isOnCampus = cachedLocation.first;
                        String campusName = isOnCampus ? cachedLocation.second : "-";
                        SessionManager.logLocationWithTag(currentMinuteTime, campusName);
                        SessionManager.manageSessions(monitoringApplication, updatedBeaconsList, beaconDistances, currentMinuteTime);
                    }
                } else {
                    SessionManager.manageSessions(monitoringApplication, updatedBeaconsList, beaconDistances, currentMinuteTime);
                }
                currentTime.set(Calendar.MINUTE, 0);
                boolean isVBTRecent = SecureKeys.getVBTGeneratedTime(getApplicationContext()).equals(currentTime);
                if (Calendar.getInstance().get(Calendar.MINUTE) == 59 || !isVBTRecent) {
                    new Handler().postDelayed(this::hourlyVBTChange, TRANSMISSION_SWITCH_COOL_DOWN);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (SettingStatic.TOAST_LOGS) {
                    Calendar currentMinute = Calendar.getInstance();
                    currentMinute.set(Calendar.SECOND, 0);
                    currentMinute.set(Calendar.MILLISECOND, 0);
                    long currentMinuteTime = currentMinute.getTimeInMillis();

                    StringBuilder message = new StringBuilder(e.getMessage() + "\n");
                    for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                        message.append(stackTraceElement.getFileName()).append(", ").append(stackTraceElement.getLineNumber()).append("\n");
                    }
                    FirebaseCrashlytics.getInstance().recordException(e);
                    SessionManager.logException(currentMinuteTime, e.getLocalizedMessage(), e.getMessage(), message.toString());
                }
            }
        };
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("backgroundRegion",
                    null, null, null));
            beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            e.printStackTrace();
        }
    }

    private Collection<Beacon> updateBeaconsWithOverflow(Collection<Beacon> beaconCollection) {
        Collection<Beacon> updateCollection = new ArrayList<>();
        for (Beacon beacon : beaconCollection) {
            int typeCode = beacon.getBeaconTypeCode();
            if (typeCode == MonitoringApplication.overflowAreaTypeCode) {
                byte[] major = beacon.getId2().toByteArray();
                byte[] minor = beacon.getId3().toByteArray();
                int majorValue = SecureKeys.getIntFromByte(major, 0);
                int minorValue = SecureKeys.getIntFromByte(minor, 0);
                beacon = new Beacon.Builder()
                        .setId1(SecureKeys.UUID_iOS)
                        .setId2(Integer.toString(majorValue))
                        .setId3(Integer.toString(minorValue))
                        .setRssi(beacon.getRssi())
                        .setRunningAverageRssi(beacon.getRunningAverageRssi()).build();
            }
            updateCollection.add(beacon);
        }
        return updateCollection;
    }

    private void hourlyVBTChange() {
        Log.v(BLE_TAG, "Old VBT: " + SecureKeys.getVBT(getApplicationContext()).toString());
        stopTransmission();
        emptyCurrentData();
        SecureKeys.generateVBT(getApplicationContext());
        new Handler().postDelayed(this::startTransmission, TRANSMISSION_SWITCH_COOL_DOWN);
        Log.v(BLE_TAG, "New VBT: " + SecureKeys.getVBT(getApplicationContext()).toString());
    }

    public void updateBeaconTracker(long currentMinuteTime) {
        // get the beacon data for the current hour
        HashMap<String, SessionManager.BeaconData> beaconHashSet =
                SessionManager.getBeaconDataForHour(currentMinuteTime);
        if (beaconHashSet == null) {
            if (SettingStatic.TOAST_LOGS) {
                Toast.makeText(getApplicationContext(), "0 sessions seen in this hour", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (SettingStatic.TOAST_LOGS) {
            Toast.makeText(getApplicationContext(), beaconHashSet.size() + " sessions seen in this hour", Toast.LENGTH_LONG).show();
        }
        for (SessionManager.BeaconData beaconData : beaconHashSet.values()) {
            String minor = beaconData.getMinorId();
            String major = beaconData.getMajorId();
            String UUID = beaconData.getUUID();
            Beacon beacon = new Beacon.Builder()
                    .setId1(UUID)
                    .setId2(major)
                    .setId3(minor).build();

            long time = beaconData.getAnyTime();

            BeaconStat beaconStat = this.beaconTracker.get(beacon);
            if (beaconStat == null) {
                if (time != 1) {
                    beaconStat = new BeaconStat(beacon, time);
                } else {
                    beaconStat = new BeaconStat(beacon, Calendar.getInstance().getTimeInMillis());
                }
                beaconTracker.put(beacon, beaconStat);
            }

            long beaconSessionDuration = 0;
            int sessionCount = 0;
            double smoothDistance = 0;
            if (beaconData.isInSession() && beaconData.isSessionReportable()) {
                beaconSessionDuration += beaconData.getCurrentSessionDuration();
                sessionCount += 1;
                smoothDistance += beaconData.getSmoothedDistanceAtLastSeenTime();
            }
            for (SessionManager.EncounterSession session : beaconData.sessions.values()) {
                if (session.isValid()) {
                    beaconSessionDuration += session.getSessionDuration();
                    sessionCount += 1;
                    smoothDistance += session.getSmoothedDistance();
                }
            }
            Log.v(BLE_TAG, "Session Count: " + sessionCount);
            Log.v(BLE_TAG, "Session Duration: " + beaconSessionDuration);
            beaconStat.setPostalCode(Integer.parseInt(getPostalCode()));
            beaconStat.setTwoCount(sessionCount);
            beaconStat.setTwoDuration(beaconSessionDuration);
            beaconStat.setTwoDistance(smoothDistance);
        }
    }

    public void addContactToDB() {
        BluetoothBeaconDatabaseHelper bluetoothBeaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(getApplicationContext());
        bluetoothBeaconDatabaseHelper.addOrUpdateContact(beaconTracker.values());
        if (CloseContactFragment.getEncounterVisible()) {
            Log.v(BLE_TAG, "Sending UI Update");
            Intent updateUI = new Intent(CloseContactFragment.ACTION_NOTIFY_BEACON_UI_UPDATE);
            getApplicationContext().sendBroadcast(updateUI);
        }
    }

    private String getPostalCode() {
        SharedPreferences zipPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String postal = zipPref.getString("postalCode", null);
        if (postal == null) {
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("User_Profile", MODE_PRIVATE);
            String userEnteredPostal = sharedPreferences.getString("zipcode", "");
            if (userEnteredPostal.isEmpty()) {
                return "000000";
            }
            return userEnteredPostal;
        }
        return postal;
    }

    public HashMap<Beacon, BeaconStat> getbeaconTracker() {
        return this.beaconTracker;
    }
}