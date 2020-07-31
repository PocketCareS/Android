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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ub.pocketcares.BuildConfig;
import com.ub.pocketcares.R;
import com.ub.pocketcares.backend.BluetoothBeaconDatabaseHelper;
import com.ub.pocketcares.backend.SecureKeys;
import com.ub.pocketcares.backend.SessionDatabaseHelper;
import com.ub.pocketcares.geofencing.GeofenceUtils;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.settings.SettingStatic;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.Utility;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static com.ub.pocketcares.utility.LogTags.BLE_TAG;


public class SessionManager {
    public static String SHARED_PREF_NAME_LOC_UPDATE_TIME = "cachedLocationLastUpdate";
    public static String SHARED_PREF_NAME_LOC_NAME = "cachedLocation";
    public static String SHARED_PREF_NAME_IS_ON_CAMPUS = "cachedOnCampusStatus";

    //public static HashMap<Beacon, BeaconData> beaconEncounters;
    public static final long MINIMUM_CLOSE_ENCOUNTER_NOTIFICATION_TIME = 600000; // milliseconds (10 minutes)
    public static Context appContext;

    public static void initializeSessionData(Context applicationContext) {
        //beaconEncounters = new  HashMap<Beacon, BeaconData>();
        appContext = applicationContext;
    }

    public static boolean manageSessionForBeacon(Beacon beacon,
                                                 @NonNull HashMap<Long, Float> currentBeaconDistances,
                                                 long currentMinuteTime) {
        float distance = currentBeaconDistances.get(currentMinuteTime);
        // for each beacon seen in this scan, do processing to determine the session state
        boolean isSessionUpdated = SessionManager.beaconSeen(beacon, currentBeaconDistances, distance, currentMinuteTime);
        return isSessionUpdated;
    }

    private static void updateDistanceForBeacon(Beacon beacon, long currentMinuteTime, float distance) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        int rowsAffected = db.updateDistanceForBeaconAtTime(beacon, currentMinuteTime, distance);
        if (rowsAffected == 0) {
            if (SettingStatic.TOAST_LOGS) {
                Toast.makeText(appContext, "No row affected when something should have been"
                        , Toast.LENGTH_LONG).show();
            }
        }
    }

    /*This method updates(or adds) distance for the time passed*/
    private static void putDistanceForBeacon(Beacon beacon, long currentMinuteTime, float distance) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        db.putDistanceForBeacon(beacon, currentMinuteTime, distance);
    }

    private static HashMap<Long, Float> getDistancesForBeacon(Beacon beacon) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        HashMap<Long, Float> distances = db.getDistancesForBeacon(beacon.getId1() + ":" + beacon.getId2() + ":" + beacon.getId3());
        return distances;
    }

    public static HashMap<String, BeaconData> getBeaconDataForHour(long currentMinuteTime) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        HashMap<String, BeaconData> bd = db.getBeaconDataForHour(currentMinuteTime);
        db.close();
        return bd;
    }

    public static void dbSetNotificationShownForSession(BeaconData beaconData) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        int rowsAffected = db.setNotificationShownForCurrentSessionInBeacon(beaconData);
        if (rowsAffected == 0) {
            Toast.makeText(appContext, "No rows affected when trying to update notification. Should not hapn.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(appContext, "Notif changed successfully!", Toast.LENGTH_LONG).show();
        }
    }

    /*
    This method checks if there is a need to invoke GPS by looking at the beacons encountered in this session
    and the beacon data from the database.
    * */
    public static boolean isGPSTriggerRequired(Collection<Beacon> beacons,
                                               HashMap<Beacon, HashMap<Long, Float>> distancesForBeaconInThisEncounter,
                                               long currentTime) {
        if (Utility.isLastMinuteInHour(currentTime)) {
            return true; // always trigger GPS in the last minute of an hour since session will be terminated then
        }
        HashMap<String, Beacon> beaconSeenInThisMinute = new HashMap<>();
        for (Beacon beacon : beacons) {
            String beaconId = beacon.getId1() + ":" + beacon.getId2() + ":" + beacon.getId3();
            beaconSeenInThisMinute.put(beaconId, beacon);
        }

        // there are three reasons which will trigger the GPS
        // Case 1) A valid session(session duration < 5) just becomes reportable, i.e. after the current encounter, the new session duration is equal to 5 minutes
        // Case 2) A reportable session has to be terminated due to a blackout period
        // Case 3) A reportable session has to be terminated because after considering the current encounter, the session becomes invalid

        boolean needToTriggerGPS = false;
        // get all the beaconEncounters
        ArrayList<BeaconData> beaconEncounters = dbGetAllBeaconEncounters();
        for (BeaconData beaconData : beaconEncounters) {
            String beaconId = beaconData.getUUID() + ":" + beaconData.getMajorId() + ":" + beaconData.getMinorId(); // this will transition the
            if (beaconData.isInSession()) {
                Beacon beaconSeenInThisMin = beaconSeenInThisMinute.get(beaconId);
                if (beaconSeenInThisMin == null) { // means that we have not seen a beacon in this minute
                    if (beaconData.isSessionReportable()) {
                        long timeSinceLastEncounter = beaconData.timeSinceLastEncounterIncludingCurrentMinute(currentTime);
                        if (timeSinceLastEncounter >= 300000) { // blackout period is more than 5 mins
                            // this is the Case 2
                            needToTriggerGPS = true;
                            break;
                        }
                    }
                } else { // means that we have seen a beacon in this minute
                    HashMap<Long, Float> distanceLogsForBeacon = distancesForBeaconInThisEncounter.get(beaconSeenInThisMin);
                    if (beaconData.isSessionReportable()) { // means that we are in a valid state
                        float smoothedDistance = calculateSmoothedDistance(distanceLogsForBeacon,
                                beaconData.currentStart, currentTime);
                        if (smoothedDistance > 2) {
                            // this will move the state of this session from valid to invalid, and it will have to be reported
                            // this is the Case 3
                            needToTriggerGPS = true;
                            break;
                        }
                    } else { // means that the state is valid but not reportable, i.e. session duration < 5
                        float smoothedDistance = calculateSmoothedDistance(distanceLogsForBeacon,
                                beaconData.currentStart, currentTime);
                        if (smoothedDistance <= 2) {
                            long updatedSessionDuration = beaconData.getCurrentSessionDuration() + (1000 * 60);
                            if (updatedSessionDuration == 300000) {
                                // this is the Case 1
                                needToTriggerGPS = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return needToTriggerGPS;
    }

    public static HashMap<Beacon, HashMap<Long, Float>> updateAndGetDistancesFromEncounters(Collection<Beacon> beacons, long currentMinuteTime) {
        HashMap<Beacon, HashMap<Long, Float>> allBeaconDistances = new HashMap<Beacon, HashMap<Long, Float>>();
        for (Beacon beacon : beacons) {
            int averageRssi = (int) beacon.getRunningAverageRssi();

            // for each beacon seen in this scan, calculate the distance from the max rssi and store it
            boolean iOS = false;
            String scannedUUID = beacon.getId1().toString();
            if (scannedUUID.equals(SecureKeys.UUID_iOS)) {
                iOS = true;
                if (BuildConfig.DEBUG && SettingStatic.TOAST_LOGS) {
                    MainActivity.m_mainActivity.runOnUiThread(() -> Toast.makeText(appContext, "iOS Beacon Detected", Toast.LENGTH_SHORT).show());
                }
            }
            float distance = Utility.convertRssiToDistance(averageRssi, iOS, appContext);
            HashMap<Long, Float> distancesForBeacon = SessionManager.getDistancesForBeacon(beacon);

            boolean entryExists = false;
            if (distancesForBeacon == null) {
                distancesForBeacon = new HashMap<>();
                //SessionManager.beaconDistancesAtAllTimes.put(beacon, distancesForBeacon);
            } else {
                // this is to take care of the case when the callback is called multiple times
                // in the same minute
                // we want to have the smallest distance(implying that we are selecting maxRssi)
                Float oldDistance = distancesForBeacon.get(currentMinuteTime);
                if (oldDistance != null) {
                    entryExists = true;
                    distance = Math.min(oldDistance, distance);
                }
            }

            // add the distance to the map against time
            distancesForBeacon.put(currentMinuteTime, distance);

            // add the time->distance map for this beacon in the beacon->(time->distance) map
            allBeaconDistances.put(beacon, distancesForBeacon);

            // update the database with the latest distance for the beacon at currentTime
            if (entryExists) {
                SessionManager.updateDistanceForBeacon(beacon, currentMinuteTime, distance);
            } else {
                SessionManager.putDistanceForBeacon(beacon, currentMinuteTime, distance);
            }
        }
        return allBeaconDistances;
    }

    public static void manageSessions(MonitoringApplication monitoringApplication, Collection<Beacon> beacons,
                                      HashMap<Beacon, HashMap<Long, Float>> beaconDistances, long currentMinuteTime) {
        if (beacons.size() > 0) {
            for (Beacon beacon : beacons) {
                HashMap<Long, Float> distancesForBeacon = beaconDistances.get(beacon);
                if (distancesForBeacon == null) {
                    SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
                    db.addLogException("Distances record for beacon is null: " + beacon.getId2() + ":" + beacon.getId3());
                    db.close();
                }
                if (distancesForBeacon != null) {
                    SessionManager.manageSessionForBeacon(beacon, distancesForBeacon, currentMinuteTime);
                }
            }
        }
        SessionManager.removeStaleEncounters(currentMinuteTime);
        monitoringApplication.updateBeaconTracker(currentMinuteTime);
        monitoringApplication.addContactToDB();
        Log.v(BLE_TAG, "Beacon Tracker: " + monitoringApplication.getbeaconTracker().toString());
    }

    public static void logLocation(long currentMinuteTime, Location location) {
        try {
            // see if cached location is available
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
            long lastUpdateTime = sharedPreferences.getLong(SHARED_PREF_NAME_LOC_UPDATE_TIME, -1);
            if (lastUpdateTime == currentMinuteTime) {
                return; // no need to update if in the same minute some value exists
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String campusName = GeofenceUtils.identifyIfOnCampus(location.getLatitude(), location.getLongitude());
            boolean isOnCampus = campusName != null;
            editor.putLong(SHARED_PREF_NAME_LOC_UPDATE_TIME, currentMinuteTime);
            editor.putString(SHARED_PREF_NAME_LOC_NAME, campusName);
            editor.putBoolean(SHARED_PREF_NAME_IS_ON_CAMPUS, isOnCampus);
            editor.apply();
            SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
            db.logLocation(currentMinuteTime, campusName, isOnCampus);
            db.close();
            boolean offCampusScan = sharedPreferences.getBoolean("stop_off_campus_scan", false);
            if (offCampusScan) {
                if (!isOnCampus) {
                    Intent stopScanTransmit = new Intent(MonitoringApplication.SCAN_BLE_ALARM);
                    stopScanTransmit.putExtra(LogTags.ALARM, MonitoringApplication.STOP_BLE);
                    appContext.sendBroadcast(stopScanTransmit);
                } else {
                    Intent startScanTransmit = new Intent(MonitoringApplication.SCAN_BLE_ALARM);
                    startScanTransmit.putExtra(LogTags.ALARM, MonitoringApplication.START_BLE);
                    appContext.sendBroadcast(startScanTransmit);
                }
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static Pair<Boolean, String> getCachedLocation(long currentMinuteTime) {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
            long cachedLocationLastUpdate = sharedPreferences.getLong(SHARED_PREF_NAME_LOC_UPDATE_TIME, -1);
            if (cachedLocationLastUpdate == -1) {
                // location has never been logged
                return null;
            }
            long timeSinceLastUpdate = currentMinuteTime - cachedLocationLastUpdate;
            if (timeSinceLastUpdate <= (5 * 60 * 1000)) {
                String cachedLoc = sharedPreferences.getString(SHARED_PREF_NAME_LOC_NAME, null);
                if (cachedLoc == null) {
                    return null;
                }
                boolean cachedOnCampusStatus = sharedPreferences.getBoolean(SHARED_PREF_NAME_IS_ON_CAMPUS, false);
                return new Pair<>(cachedOnCampusStatus, cachedLoc);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void logLocationWithTag(long currentMinuteTime, String campusName) {
        try {
            // see if cached location is available
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
            long lastUpdateTime = sharedPreferences.getLong(SHARED_PREF_NAME_LOC_UPDATE_TIME, -1);
            if (lastUpdateTime == currentMinuteTime) {
                return;
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            boolean isOnCampus = campusName != null;
            editor.putLong(SHARED_PREF_NAME_LOC_UPDATE_TIME, currentMinuteTime);
            editor.putString(SHARED_PREF_NAME_LOC_NAME, campusName);
            editor.putBoolean(SHARED_PREF_NAME_IS_ON_CAMPUS, isOnCampus);
            editor.apply();
            SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
            db.logLocation(currentMinuteTime, campusName, isOnCampus);
            db.close();
        } catch (Exception e) {
            Toast.makeText(appContext, "Failure to log location: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void logException(long time, String localizedMessage, String message, String toString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String date = dateFormat.format(new Date(time));
        String errorMessage = "time: " + date + ", locMsg: " + localizedMessage + ", Msg: " + message + ", Str: " + toString;
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        db.addLogException(errorMessage);
        db.close();
    }

    public static class EncounterSession {
        public long start, end;
        public String sessionState;
        public int maxRSSI;
        public float smoothedDistance;
        public boolean isNotiticationShownForSession;
        public String message;

        public static String ONGOING_SESSION = "ONGOING_SESSION";
        public static String VALID_SESSION = "VALID_SESSION";
        public static String INVALID_SESSION = "INVALID_SESSION";

        public EncounterSession(long start, long end, int maxRSSI, float smoothedDistance, String sessionState, boolean isNotiticationShownForSession,
                                String message) {
            this.start = start;
            this.end = end;
            this.maxRSSI = maxRSSI;
            this.sessionState = sessionState;
            this.smoothedDistance = smoothedDistance;
            this.isNotiticationShownForSession = isNotiticationShownForSession;
            this.message = message;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            String startStr = dateFormat.format(new Date(this.start));
            String endStr = dateFormat.format(new Date(this.end));
            return "start=" + startStr +
                    ", end=" + endStr +
                    ", State=" + sessionState +
                    ", Max RSSI=" + maxRSSI +
                    ", sm.dist=" + smoothedDistance +
                    ", notif=" + isNotiticationShownForSession;
        }

        public boolean isOngoing() {
            return this.sessionState.equals(ONGOING_SESSION);
        }

        public boolean isValid() {
            return this.sessionState.equals(VALID_SESSION);
        }

        public boolean isInvalid() {
            return this.sessionState.equals(INVALID_SESSION);
        }

        public long getSessionDuration() {
            return this.end - this.start + 60 * 1000;
        }

        public float getSmoothedDistance() {
            return this.smoothedDistance;
        }
    }

    public static class TaggedEncounterSession {
        public long start, end;
        boolean isOnCampus;

        public TaggedEncounterSession(long start, long end, boolean isOnCampus) {
            this.start = start;
            this.end = end;
            this.isOnCampus = isOnCampus;
        }
    }

    public static class GPSLog {
        public long logTime;
        public String campusName;
        public boolean isOnCampus;

        public GPSLog(long logTime, String campusName, boolean isOnCampus) {
            this.logTime = logTime;
            this.campusName = campusName;
            this.isOnCampus = isOnCampus;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            String logTimeStr = dateFormat.format(new Date(logTime));
            return "GPSLog{" +
                    "logTime=" + logTimeStr +
                    ", campusName='" + campusName + '\'' +
                    ", isOnCampus=" + isOnCampus +
                    '}';
        }
    }

    public static class BeaconData {
        //beaconId format - UUID:MajorID:MinorID
        public String beaconId;
        public int maxRSSI;
        public long currentStart, currentEnd;
        public float currentSmoothedDistance;
        public boolean isNotificationShown;
        public String message;
        public HashMap<Long, EncounterSession> sessions;

        public BeaconData(String beaconId, int rssi, long currentStart, long currentEnd, float currentSmoothedDistance, String message) {
            this.beaconId = beaconId;
            this.maxRSSI = rssi;
            this.currentStart = currentStart;
            this.currentEnd = currentEnd;
            this.currentSmoothedDistance = currentSmoothedDistance;
            this.isNotificationShown = false;
            this.sessions = new HashMap<>(); // we do not yet have any sessions
            this.message = message;
        }

        /*public BeaconData(String beaconId, long currentStart, long currentEnd, float currentSmoothedDistance, boolean isNotificationShown) {
            this(beaconId, currentStart, currentEnd, currentSmoothedDistance);
            this.isNotificationShown = isNotificationShown;
        }*/

        public BeaconData(String beaconId, int rssi, long currentStart, long currentEnd, float currentSmoothedDistance, boolean isNotificationShown, String message) {
            this(beaconId, rssi, currentStart, currentEnd, currentSmoothedDistance, message);
            this.isNotificationShown = isNotificationShown;
        }

        public BeaconData(String beaconId, int rssi, long currentStart, long currentEnd, float currentSmoothedDistance, boolean isNotificationShown,
                          String message,
                          ArrayList<EncounterSession> sessions) {
            this(beaconId, rssi, currentStart, currentEnd, currentSmoothedDistance, message);
            this.isNotificationShown = isNotificationShown;
            for (EncounterSession session : sessions) {
                this.sessions.put(session.start, session);
            }
        }

        public boolean isInSession() {
            return this.currentStart != -1;
        }

        public void startNewSession(long startAtTime, float currentSmoothedDistance, String message, int maxRSSI) {
            this.currentStart = startAtTime;
            this.currentEnd = startAtTime;
            this.currentSmoothedDistance = currentSmoothedDistance;
            this.isNotificationShown = false;
            this.message = message;
            this.maxRSSI = maxRSSI;

            SessionManager.dbAddNewSessionForBeacon(this.beaconId, this.currentStart, this.currentEnd, this.maxRSSI, this.currentSmoothedDistance, this.message);
        }

        public long timeSinceLastEncounterIncludingCurrentMinute(long currentTime) {
            return currentTime - this.currentEnd;
        }

        public void updateLastSeen(long currentTime) {
            this.currentEnd = currentTime;
        }

        public void invalidateCurrentSession(String message) {
            this.message = message;
            SessionManager.dbUpdateInvalidateCurrentSessionForBeacon(this);
            EncounterSession session = new EncounterSession(this.currentStart, this.currentEnd, this.maxRSSI, this.currentSmoothedDistance,
                    EncounterSession.INVALID_SESSION, this.isNotificationShown, message);
            this.sessions.put(this.currentStart, session);
            this.currentStart = -1;
            this.currentEnd = -1;
            this.currentSmoothedDistance = 0;
            this.isNotificationShown = false;
            this.message = "";
        }

        public long getCurrentSessionDuration() {
            if (!this.isInSession())
                return -1;
            return this.currentEnd - this.currentStart + 60 * 1000;
        }

        public void reportCurrentSession(String message) {
            this.message = message;
            SessionManager.dbUpdateReportCurrentSessionForBeacon(this);
            EncounterSession session = new EncounterSession(this.currentStart, this.currentEnd, this.maxRSSI, this.currentSmoothedDistance,
                    EncounterSession.VALID_SESSION, this.isNotificationShown, message);
            this.sessions.put(this.currentStart, session);
            this.currentStart = -1;
            this.currentEnd = -1;
            this.isNotificationShown = false;
            this.currentSmoothedDistance = 0;
            this.message = "";
        }

        public boolean isSessionReportable() {
            if (this.currentStart == -1 || this.currentEnd == -1) {
                return false;
            }
            long sessionDuration = this.getCurrentSessionDuration();
            return sessionDuration >= 300000 && this.currentSmoothedDistance <= 2;
        }

        public void updateSmoothedDistance(float smoothedDistance) {
            this.currentSmoothedDistance = smoothedDistance;
        }

        public float getSmoothedDistanceAtLastSeenTime() {
            return this.currentSmoothedDistance;
        }

        @Override
        public String toString() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            String start = dateFormat.format(new Date(currentStart));
            String end = dateFormat.format(new Date(currentEnd));
            return "BeaData{" +
                    "curStart=" + start +
                    ", curEnd=" + end +
                    ", curSmDist=" + currentSmoothedDistance + '}';
        }

        public boolean startsAt(long time) {
            if (!this.isInSession())
                return false;
            return (time - this.currentStart) == 0;
        }

        public String toDetailedString() {
            Pair<Long, Long> timeRange = this.getEncounterRange();
            long tenMins = 10 * 60 * 1000;
            ArrayList<GPSLog> gpsLogs = SessionManager.getGPSLogsInRange(timeRange.first - tenMins, timeRange.second + tenMins);
            String info = "___________________________________\n";
            info += "BeaconID: " + this.getMajorId() + ":" + this.getMinorId() + "\n";
            info += "Device Type: " + this.getDeviceType() + "\n";
            if (!this.isInSession()) {
                info += "Ongoing session: false\n\n";
            } else {
                info += "Ongoing session: true\n";
                info += "";
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
                info += "Start: " + dateFormat.format(new Date(currentStart)) + "\n";
                info += "Last seen: " + dateFormat.format(new Date(currentEnd)) + "\n";
                info += "Max RSSI: " + maxRSSI + "\n";
                info += "Current Sm.Dis: " + currentSmoothedDistance + "m\n";
                info += "Notif: " + this.isNotificationShown + "\n";
                info += "Msg: " + this.message + "\n\n";
            }
            //info += "Session count: "+sessions.size()+"\n";
            ArrayList<Long> sessionStarts = new ArrayList<>(sessions.keySet());
            Collections.sort(sessionStarts);
            Collections.reverse(sessionStarts);
            for (Long sessionStart : sessionStarts) {
                EncounterSession session = this.sessions.get(sessionStart);
                if (session.isValid()) {
                    try {
                        info += SessionReportHelper.buildReportForSession(session.start, session.end, gpsLogs) + "\n";
                        info += "Max RSSI: " + session.maxRSSI + "\n";
                        info += "Sm.Dis:" + session.smoothedDistance + "\n";
                        info += "Notif: " + session.isNotiticationShownForSession + "\n";
                        info += "Msg: " + session.message + "\n";
                    } catch (Exception e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        info += e.getMessage() + "\n";
                        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                            info += "\t\t" + stackTraceElement.getFileName() + ", " + stackTraceElement.getLineNumber() + "\n";
                        }
                        info += session.toString() + "\n";
                    }
                    info += "****\n";
                } else {
                    info += session.toString() + "\n";
                    info += "Msg: " + session.message + "\n";
                    info += "****\n";
                }
            }
            info += "___________________________________\n\n";
            return info;
        }

        private String getDeviceType() {
            String uuid = this.getUUID();
            if (uuid.equals(SecureKeys.UUID)) {
                return "Android";
            } else if (uuid.equals(SecureKeys.UUID_iOS)) {
                return "iOS";
            } else {
                return "Unrecognized device type";
            }
        }

        private Pair<Long, Long> getEncounterRange() {
            Long minTime = this.currentStart;
            Long maxTime = this.currentEnd;
            for (EncounterSession session : this.sessions.values()) {
                minTime = Math.min(session.start, minTime);
                maxTime = Math.max(session.end, maxTime);
            }
            return new Pair<>(minTime, maxTime);
        }

        public void addNewSession(EncounterSession session) {
            this.sessions.put(session.start, session);
        }

        public void setCurrentSessionValuesFrom(EncounterSession session) {
            this.currentStart = session.start;
            this.currentEnd = session.end;
            this.maxRSSI = session.maxRSSI;
            this.currentSmoothedDistance = session.smoothedDistance;
            this.isNotificationShown = session.isNotiticationShownForSession;
            this.message = session.message;
        }

        public void updateToContinueSession(long newEnd, float newEndSmoothedDistance, String message, int maxRSSI) {
            this.currentEnd = newEnd;
            this.currentSmoothedDistance = newEndSmoothedDistance;
            this.message = message;
            this.maxRSSI = maxRSSI;
            float sessionDuration = this.getCurrentSessionDuration();
            if (sessionDuration >= MINIMUM_CLOSE_ENCOUNTER_NOTIFICATION_TIME && Utility.canShowNotification(appContext) && !isNotificationShown) {
                this.isNotificationShown = true;
                Utility.createReminderNotification(appContext, appContext.getString(R.string.notification_reminder_title),
                        appContext.getString(R.string.notification_reminder_description), MonitoringApplication.DISTANCING_NOTIFICATION_CODE);
            }
            SessionManager.dbUpdateToContinueOngoingSession(this);
        }

        public String getMinorId() {
            String[] id = this.beaconId.split(":");
            return id[2];
        }

        public String getMajorId() {
            String[] id = this.beaconId.split(":");
            return id[1];
        }

        public String getUUID() {
            String[] id = this.beaconId.split(":");
            return id[0];
        }

        public int getMaxRSSI() {
            return this.maxRSSI;
        }

        public long getAnyTime() {
            if (this.currentStart != -1) {
                return this.currentStart;
            } else {
                return 1;
            }
        }

        public void setNotificationShown() {
            this.isNotificationShown = true;
        }

        public long getLatestTime() {
            long latestTime = currentStart;
            for (EncounterSession session : this.sessions.values()) {
                if (session.start > latestTime) {
                    latestTime = session.start;
                }
            }
            return latestTime;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static ArrayList<GPSLog> getGPSLogsInRange(Long minTime, Long maxTime) {
        SessionDatabaseHelper sessionDatabaseHelper = new SessionDatabaseHelper(appContext);
        ArrayList<GPSLog> gpsLogs = sessionDatabaseHelper.getGPSLogsInRange(minTime, maxTime);
        sessionDatabaseHelper.close();
        return gpsLogs;
    }

    /*
    This method takes action for the beacon and return true if there is any change to the session database
    Contents of the session database are shown to the UI, and we invoke UI update procedure only when there is update to the db
    * */
    public static boolean beaconSeen(Beacon uniqueBeacon, HashMap<Long, Float> distanceLogsForBeacon, float distanceCurrent, long currentTime) {
        BeaconData currentBeaconData = SessionManager.getBeaconData(uniqueBeacon);
        if (currentBeaconData == null) {
            // beacon has not been seen yet, hence no session yet
            Log.println(Log.DEBUG, "D", "Beacon not been seen before. Starting new sess. " + uniqueBeacon.toString());

            if (SettingStatic.TOAST_LOGS) {
                Toast.makeText(appContext,
                        "Beacon not been seen before. Starting new sess. " + uniqueBeacon.toString(),
                        Toast.LENGTH_LONG).show();
            }
            if (distanceCurrent <= 2) {
                currentBeaconData = new BeaconData(uniqueBeacon.getId1() + ":" + uniqueBeacon.getId2() + ":" + uniqueBeacon.getId3(),
                        (int) uniqueBeacon.getRunningAverageRssi(), currentTime, currentTime, distanceCurrent, false,
                        "Starting new session since dist: " + distanceCurrent + "<= 2");
                // since the constructor will set the ongoing session start and end values,
                // the session has started
                SessionManager.dbAddNewSessionForBeacon(currentBeaconData.beaconId, currentBeaconData.currentStart,
                        currentBeaconData.currentEnd, currentBeaconData.maxRSSI, currentBeaconData.currentSmoothedDistance,
                        currentBeaconData.message);
                SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                return true;
            } else {
                return false;
            }
        }

        if (!currentBeaconData.isInSession()) {
            if (distanceCurrent > 2) {
                Log.println(Log.DEBUG, "D", "This beacon is not in a session. But cannot start session since distance > 2 " + uniqueBeacon.toString());
                if (SettingStatic.TOAST_LOGS) {
                    Toast.makeText(appContext,
                            "This beacon is not in a session. But cannot start session since distance > 2 " + uniqueBeacon.toString(),
                            Toast.LENGTH_LONG).show();
                }
                // we do not start a session
                return false;
            } else {
                String message = "This beacon is not in a session. Can start session since distance < 2";
                Log.println(Log.DEBUG, "D", message + uniqueBeacon.toString());
                if (SettingStatic.TOAST_LOGS) {
                    Toast.makeText(appContext,
                            message + uniqueBeacon.toString(),
                            Toast.LENGTH_LONG).show();
                }
                currentBeaconData.startNewSession(currentTime, distanceCurrent, message, (int) uniqueBeacon.getRunningAverageRssi());
                SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                return true;
            }
        }

        if (currentBeaconData.startsAt(currentTime)) {
            // this is the case, when didRangeBeaconsInRegion is called multiple times in the same minute
            // we do not do anything in this case since, the beacon is already in a session
            // and it will never get out of the session since we select the min distance from the
            // last encounter in the same minute
            return false;
        }

        double timeSinceLastSeen = currentBeaconData.timeSinceLastEncounterIncludingCurrentMinute(currentTime) - 60 * 1000;
        if (timeSinceLastSeen < 300000) {
            // the blackout period is less than 5 minutes(300000 millis), so might have a valid session
            float smoothedDistance = calculateSmoothedDistance(distanceLogsForBeacon,
                    currentBeaconData.currentStart, currentTime);
            if (smoothedDistance <= 2) {
                String debugMsg = "Blackout period <= 5mins and Sm.Dist <= 2. Continue. Raw dist: " + distanceCurrent;
                Log.println(Log.DEBUG, "D", debugMsg);
                if (SettingStatic.TOAST_LOGS) {
                    Toast.makeText(appContext, debugMsg, Toast.LENGTH_LONG).show();
                }
                // the session continues
                currentBeaconData.updateToContinueSession(currentTime, smoothedDistance, debugMsg, (int) uniqueBeacon.getRunningAverageRssi());
                SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                return true;
            } else {
                // report the session till the last seen since it will surely be valid, otherwise it would have been invalidated
                if (currentBeaconData.isSessionReportable()) {
                    currentBeaconData.reportCurrentSession("Reporting valid session as sm.dist>2");
                } else {
                    currentBeaconData.invalidateCurrentSession("Invalidating session as sm.dist>2");
                }
                // after it is reported it will get invalidated. Check if we can start a session at the current time
                if (distanceCurrent <= 2) {
                    currentBeaconData.startNewSession(currentTime, distanceCurrent,
                            "Start new session since rawDist right now:" + distanceCurrent, (int) uniqueBeacon.getRunningAverageRssi());
                    SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                    if (SettingStatic.TOAST_LOGS) {
                        String debugMsg = "Blackout period < 5min & smDist > 2. Aborting session." +
                                "Start new sess since curDist <= 2" + uniqueBeacon.getId2() + ":" + uniqueBeacon.getId3();
                        Toast.makeText(appContext, debugMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (SettingStatic.TOAST_LOGS) {
                        String debugMsg = "Blackout period < 5min & smDist > 2. Aborting session." +
                                "Cant start new sess since curDist > 2" + uniqueBeacon.getId2() + ":" + uniqueBeacon.getId3();
                        Toast.makeText(appContext, debugMsg, Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            }
        } else {
            // the blackout period is more than 5 minutes, so the current encounter
            // will not be in the ongoing session. We wil have to terminate the ongoing
            // session(and possibly start a new session). We could declare the ongoing session as reportable if the
            // duration is long enough(5 minutes)
            long sessionDuration = currentBeaconData.getCurrentSessionDuration();
            if (sessionDuration < 300000) {
                // invalidate the current session
                // since the session is duration is too small
                currentBeaconData.invalidateCurrentSession("Blackout >= 5 mins & duration < 5 min. Hence terminating.");

                // check if we can start a new session at the current time
                if (distanceCurrent <= 2) {
                    // we can start a new session
                    String debugMess = "BlackoutPeriod > 5min && sessDur < 5mins. Aborting prev session.. " +
                            "Can start new sess since curDist <= 2: " + currentBeaconData.toString();
                    Log.println(Log.DEBUG, "D", debugMess);
                    if (SettingStatic.TOAST_LOGS) {
                        Toast.makeText(appContext, debugMess, Toast.LENGTH_LONG).show();
                    }
                    currentBeaconData.startNewSession(currentTime, distanceCurrent, debugMess, (int) uniqueBeacon.getRunningAverageRssi());
                    SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                    return true;
                } else {
                    String debugMess = "BlackoutPeriod > 5min && sessDur < 5mins. Aborting. " +
                            "Cannot start new sess since curDist > 2 " + currentBeaconData.toString();
                    Log.println(Log.DEBUG, "D", debugMess);
                    if (SettingStatic.TOAST_LOGS) {
                        Toast.makeText(appContext, debugMess, Toast.LENGTH_LONG).show();
                    }
                    // cannot start a new session at currentTime, the session has already been invalidated
                    return true;
                }
            } else {
                // the ongoing session is long enough.
                // check if the smoothed distance at the last seen time is
                // within close encounter limits(2 m)
                // float smoothedDistance = calculateSmoothedDistance(uniqueBeacon, beaconEncounter.currentEnd);
                float smoothedDistance = currentBeaconData.getSmoothedDistanceAtLastSeenTime();
                if (smoothedDistance > 2) {
                    // this session is valid in terms of time, but not in terms of distance
                    // invalidate the existing session
                    currentBeaconData.invalidateCurrentSession("Blackout >= 5, sm.dist > 2. Raw dist: " + distanceCurrent);

                    // check if a new session can be started at the current time
                    if (distanceCurrent <= 2) {
                        String debugMess = "Sess dur >= 5 && smDist > 2. Abort prev sess. Start new sess since curDist <= 2";
                        // a new session can be started right now
                        Log.println(Log.DEBUG, "D", debugMess);
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, debugMess, Toast.LENGTH_LONG).show();
                        }
                        currentBeaconData.startNewSession(currentTime, distanceCurrent, debugMess, (int) uniqueBeacon.getRunningAverageRssi());
                        SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                        return true;
                    } else {
                        String debugMess = "Sess dur >= 5 && smDist > 2. Abort. Cant start new sess since curDist > 2|" + currentBeaconData.toString();
                        Log.println(Log.DEBUG, "D", debugMess);
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, debugMess, Toast.LENGTH_LONG).show();
                        }
                        // a new session cannot be started, and the old session has already been invalidated
                        return true;
                    }
                } else {
                    // this is a valid session in terms of both, time and distance
                    // so report it
                    String debugMsg = "BlackoutPeroid >= 5 & sessDur >= 5 && smDist <= 2(valid terminated sess)";
                    currentBeaconData.reportCurrentSession(debugMsg);

                    // the current distance is within close encounter limits, so
                    // we can start a new session right now
                    if (distanceCurrent <= 2) {
                        currentBeaconData.startNewSession(currentTime, distanceCurrent, "Start a new session since raw dist<2, RawDist: " + distanceCurrent,
                                (int) uniqueBeacon.getRunningAverageRssi());
                        SessionManager.doLastMinuteCheck(currentBeaconData, currentTime);
                        debugMsg += "Start new sess rn. curDis <= 2";
                        Log.println(Log.DEBUG, "D", debugMsg);
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, debugMsg, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        debugMsg += "Can't start new sess. curDis > 2";
                        Log.println(Log.DEBUG, "D", debugMsg);
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, debugMsg, Toast.LENGTH_LONG).show();
                        }
                        // cannot start a new session
                        // in the reportCurrentSession, the start, and end values
                        // will be set to null
                        // so no additional steps to stop the session
                    }
                    return true;
                }
            }
        }
    }

    private static void doLastMinuteCheck(BeaconData currentBeaconData, long currentTime) {
        // after the above, we have made a decision about the beacon's session-status inclusive of the
        // current minute. If the current time is the last minute is this hour. Then we could immediately
        // take the decision because we are not going to see this beacon in the next minute(every hour VBT changes)
        // this is done to keep no pending decisions about a beacon in the next-hour
        // this is helpful when hourly summary is calculated at the end of each minute and displayed
        if (Utility.isLastMinuteInHour(currentTime)) {
            if (currentBeaconData.isSessionReportable()) {
                String message = "Terminate valid session in the last min.";
                if (SettingStatic.TOAST_LOGS) {
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                }
                currentBeaconData.reportCurrentSession(message);
            } else {
                String message = "Terminate invalid session in the last min";
                if (SettingStatic.TOAST_LOGS) {
                    Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                }
                currentBeaconData.invalidateCurrentSession(message);
            }
        }
    }

    private static void dbUpdateInvalidateCurrentSessionForBeacon(BeaconData currentBeaconData) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        int rowsUpdated = db.changeSessionStatusForBeacon(currentBeaconData, EncounterSession.INVALID_SESSION);
        if (rowsUpdated == 0) {
            Toast.makeText(appContext, "Failed to change status to valid", Toast.LENGTH_LONG).show();
        }
    }

    private static void dbUpdateReportCurrentSessionForBeacon(BeaconData currentBeaconData) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        int updateCount = db.changeSessionStatusForBeacon(currentBeaconData, EncounterSession.VALID_SESSION);
        if (updateCount == 0) {
            Toast.makeText(appContext, "Failed to change status to valid", Toast.LENGTH_LONG).show();
        }
    }

    /*
    Update the end time, and the smoothedDistance at end time for the session. We can uniquely identify a session by
    the combination of beacon id and the session start(since a beacon will never start more than one sessions in the same minute)
    * */
    private static void dbUpdateToContinueOngoingSession(BeaconData beaconData) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        int rowsAffected = db.updateToContinueOngoingSession(beaconData);
        if (rowsAffected == 0) {
            if (SettingStatic.TOAST_LOGS) {
                Toast.makeText(appContext, "0 Rows affected after update to continue session. This should not be the case",
                        Toast.LENGTH_LONG).show();
            }
        }
        db.close();
    }

    private static void dbAddNewSessionForBeacon(String beaconId, long currentStart, long currentEnd, int maxRSSI,
                                                 float currentSmoothedDistance, String message) {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        db.addNewSessionForBeacon(beaconId, currentStart, currentEnd, maxRSSI, currentSmoothedDistance, message);
        db.close();
    }

    private static BeaconData getBeaconData(Beacon uniqueBeacon) {
        // do a db lookup for the beacon and return
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        BeaconData beaconData = db.getBeaconData(uniqueBeacon);
        return beaconData;
    }

    public static String getRawSessionData() {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        return db.getRawSessionData();
    }

    /*
    This method looks at all the beacons and their ongoing sessions, and terminates/reports those sessions when the
    difference between the current time and the last seen time is greater than the time threshold(5 minutes)
     */
    public static boolean removeStaleEncounters(long currentTime) {
        boolean dbChanged = false;
        // get all the beaconEncounters
        ArrayList<BeaconData> beaconEncounters = dbGetAllBeaconEncounters();
        for (BeaconData beaconData : beaconEncounters) {
            if (beaconData.isInSession()) {
                long timeSinceLastEncounter = beaconData.timeSinceLastEncounterIncludingCurrentMinute(currentTime);
                if (timeSinceLastEncounter >= 300000) {
                    // there is a blackout period of more than 5 minutes
                    // 300000milliseconds is 5 minutes
                    if (beaconData.isSessionReportable()) {
                        String message = "Reporting a valid stale session, as blackout period >= 5";
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, message + beaconData.toString(), Toast.LENGTH_LONG).show();
                        }
                        beaconData.reportCurrentSession(message);
                        dbChanged = true;
                    } else {
                        String message = "Terminating an invalid session, as blackout period >= 5";
                        beaconData.invalidateCurrentSession(message);
                        dbChanged = true;
                        if (SettingStatic.TOAST_LOGS) {
                            Toast.makeText(appContext, message + beaconData.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
        return dbChanged;
    }

    public static ArrayList<BeaconData> dbGetAllBeaconEncounters() {
        SessionDatabaseHelper db = new SessionDatabaseHelper(appContext);
        ArrayList<BeaconData> allBeaconData = db.getDataForAllBeacons();
        return allBeaconData;
    }

    public static float calculateSmoothedDistance(HashMap<Long, Float> distanceLogForBeacon,
                                                  long startTime, long endTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(endTime);
        ArrayList<Float> distances = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            long tempTime = calendar.getTimeInMillis();
            if (tempTime < startTime) {
                // don't go behind the start time
                break;
            }
            Float dist = distanceLogForBeacon.get(tempTime);
            if (dist != null) {
                distances.add(dist);
            }
            // decrement time by one minute
            calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - 1);
        }
        int n = distances.size();
        if (n == 1) {
            return distances.get(0);
        }
        float sum = 0;
        for (Float distance : distances) {
            sum += distance;
        }
        return (float) sum / n;
    }


}
