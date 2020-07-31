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

package com.ub.pocketcares.backend;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ub.pocketcares.bluetoothBeacon.SessionManager;

import org.altbeacon.beacon.Beacon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SessionDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bluetoothBeaconSessions.db";
    private static final String DISTANCES_TABLE = "distance_data";
    private static final String GPS_LOG_TABLE = "gps_log_table";
    private static final String SESSION_TABLE = "session_data";
    private static final String ERROR_LOG_TABLE = "error_log_table";
    private static final String VBT_NAME_TABLE = "vbt_name_table";
    private static final int DATABASE_VERSION = 5;
    private static final long DAY_VALUE = 86400000L;
    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String INT_TYPE = " INTEGER";
    private Context context;

    // columns for DISTANCES_TABLE
    private static final String BEACON_ID_COL = "beacon_id";
    private static final String TIMESTAMP_COL = "timestamp";
    private static final String RAW_DISTANCE_COL = "raw_distance";
    private static final String[] DISTANCES_TABLE_COLS = {BEACON_ID_COL, TIMESTAMP_COL, RAW_DISTANCE_COL};

    // columns for SESSION_TABLE
    private static final String START_TIME_COL = "start_time";
    private static final String END_TIME_COL = "end_time";
    private static final String MAX_RSSI_COL = "max_rssi";
    private static final String SMOOTHED_DIST_COL = "smoothed_dist";
    private static final String SESSION_STATUS_COL = "session_status";
    private static final String IS_NOTIFICATION_SHOWN_COL = "is_notification_shown";
    private static final String MESSAGE_COL = "message_col";
    private static final String[] SESSION_TABLE_COLS = {BEACON_ID_COL, START_TIME_COL, END_TIME_COL, MAX_RSSI_COL,
            SMOOTHED_DIST_COL, SESSION_STATUS_COL, IS_NOTIFICATION_SHOWN_COL, MESSAGE_COL};

    // columns for the GPS_LOG_TABLE
    private static final String LOG_TIME_COL = "log_time";
    private static final String CAMPUS_NAME_COL = "campus_name";
    private static final String IS_ON_CAMPUS_COL = "is_on_campus";
    private static final String[] GPS_LOG_TABLE_COLS = {LOG_TIME_COL, CAMPUS_NAME_COL, IS_ON_CAMPUS_COL};

    // columns for the ERROR_LOG_TABLE
    private static final String ERROR_LOG_COL = "log_message";
    private static final String[] ERROR_LOG_TABLE_COLS = {ERROR_LOG_COL};

    // columns for the VBT_NAME_TABLE
    private static final String VBT_NAME_COL = "vbt_name";
    private static final String[] VBT_NAME_TABLE_COLS = {LOG_TIME_COL, VBT_NAME_COL};


    private static final String SQL_CREATE_DISTANCES_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s, %s %s, %s %s )", DISTANCES_TABLE,
            BEACON_ID_COL, TEXT_TYPE,
            TIMESTAMP_COL, INT_TYPE,
            RAW_DISTANCE_COL, REAL_TYPE
    );

    private static final String SQL_CREATE_SESSION_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s)", SESSION_TABLE,
            BEACON_ID_COL, TEXT_TYPE,
            START_TIME_COL, INT_TYPE,
            END_TIME_COL, INT_TYPE,
            MAX_RSSI_COL, INT_TYPE,
            SMOOTHED_DIST_COL, REAL_TYPE,
            SESSION_STATUS_COL, TEXT_TYPE,
            IS_NOTIFICATION_SHOWN_COL, INT_TYPE,
            MESSAGE_COL, TEXT_TYPE);

    private static final String SQL_ALTER_SESSION_TABLE = String.format("ALTER TABLE %s ADD COLUMN %s %s", SESSION_TABLE, MESSAGE_COL, TEXT_TYPE);
    private static final String SQL_ALTER_SESSION_TABLE_MAX_RSSI = String.format("ALTER TABLE %s ADD COLUMN %s %s", SESSION_TABLE, MAX_RSSI_COL, INT_TYPE);

    private static final String SQL_CREATE_GPS_LOG_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s, %s %s, %s %s)", GPS_LOG_TABLE,
            LOG_TIME_COL, INT_TYPE,
            CAMPUS_NAME_COL, TEXT_TYPE,
            IS_ON_CAMPUS_COL, INT_TYPE);

    private static final String SQL_CREATE_ERROR_LOG_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s)", ERROR_LOG_TABLE,
            ERROR_LOG_COL, TEXT_TYPE);

    private static final String SQL_CREATE_VBT_NAME_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s, %s %s)", VBT_NAME_TABLE,
            LOG_TIME_COL, INT_TYPE,
            VBT_NAME_COL, TEXT_TYPE);

    public SessionDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_DISTANCES_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_SESSION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_GPS_LOG_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_ERROR_LOG_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_VBT_NAME_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (newVersion > oldVersion && newVersion == 3) {
            sqLiteDatabase.execSQL(SQL_CREATE_SESSION_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_GPS_LOG_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_ERROR_LOG_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_VBT_NAME_TABLE);
        } else if (newVersion > oldVersion && newVersion > 4) {
            sqLiteDatabase.execSQL(SQL_ALTER_SESSION_TABLE);
            sqLiteDatabase.execSQL(SQL_ALTER_SESSION_TABLE_MAX_RSSI);
        }

    }

    public HashMap<Long, Float> getDistancesForBeacon(String beaconId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s = ?", BEACON_ID_COL);
        String[] values = new String[]{beaconId};
        Cursor c = db.query(DISTANCES_TABLE, DISTANCES_TABLE_COLS, where, values, null, null, null);
        HashMap<Long, Float> distances = new HashMap<>();
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                long time = c.getLong(c.getColumnIndex(TIMESTAMP_COL));
                float distance = c.getLong(c.getColumnIndex(RAW_DISTANCE_COL));
                distances.put(time, distance);
            }
            c.close();
            db.close();
            return distances;
        } else {
            return null;
        }
    }

    public void putDistanceForBeacon(Beacon beacon, long currentMinuteTime, float distance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BEACON_ID_COL, beacon.getId1() + ":" + beacon.getId2() + ":" + beacon.getId3());
        values.put(TIMESTAMP_COL, currentMinuteTime);
        values.put(RAW_DISTANCE_COL, distance);

        db.insert(DISTANCES_TABLE, null, values);
        db.close();
    }

    public int updateDistanceForBeaconAtTime(Beacon beacon, long currentMinuteTime, float distance) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(RAW_DISTANCE_COL, distance);

        String selection = BEACON_ID_COL + " = ? AND " + TIMESTAMP_COL + " = ?";
        String[] selectionArgs = {beacon.getId1() + ":" + beacon.getId2() + ":" + beacon.getId3(), currentMinuteTime + ""};

        int count = db.update(
                DISTANCES_TABLE,
                values,
                selection,
                selectionArgs);
        db.close();
        return count;
    }

    public SessionManager.BeaconData getBeaconData(Beacon beacon) {
        String beaconID = beacon.getId1() + ":" + beacon.getId2() + ":" + beacon.getId3();
        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s = ?", BEACON_ID_COL);
        String[] values = new String[]{beaconID};
        Cursor c = db.query(SESSION_TABLE, SESSION_TABLE_COLS, where, values, null, null, null);
        if (c != null && c.getCount() != 0) {
            ArrayList<SessionManager.EncounterSession> decidedSessions = new ArrayList<>();
            long ongoingSessionStart = -1, ongoingSessionEnd = -1;
            float ongoingSessionDist = 0;
            boolean isNotificationShown = false;
            String ongoingSessMessage = "";
            while (c.moveToNext()) {
                Pair<String, SessionManager.EncounterSession> sessionData = buildSessionObjectFromCursor(c);
                SessionManager.EncounterSession session = sessionData.second;
                if (!isNotificationShown && session.isNotiticationShownForSession) {
                    isNotificationShown = true;
                }
                if (session.isOngoing()) {
                    ongoingSessionStart = session.start;
                    ongoingSessionEnd = session.end;
                    ongoingSessionDist = session.smoothedDistance;
                    ongoingSessMessage = session.message;
                } else {
                    decidedSessions.add(session);
                }
            }
            c.close();
            db.close();
            SessionManager.BeaconData beaconData =
                    new SessionManager.BeaconData(beaconID, (int) beacon.getRunningAverageRssi(), ongoingSessionStart,
                            ongoingSessionEnd, ongoingSessionDist, isNotificationShown, ongoingSessMessage,
                            decidedSessions);
            return beaconData;
        } else {
            return null;
        }
    }

    private Pair<String, SessionManager.EncounterSession> buildSessionObjectFromCursor(Cursor c) {
        String beaconId = c.getString(c.getColumnIndex(BEACON_ID_COL));
        long start = c.getLong(c.getColumnIndex(START_TIME_COL));
        long end = c.getLong(c.getColumnIndex(END_TIME_COL));
        int maxRSSI = c.getInt(c.getColumnIndex(MAX_RSSI_COL));
        String sessionState = c.getString(c.getColumnIndex(SESSION_STATUS_COL));
        float smoothedDistance = c.getFloat(c.getColumnIndex(SMOOTHED_DIST_COL));
        boolean isNotificationShown = c.getInt(c.getColumnIndex(IS_NOTIFICATION_SHOWN_COL)) == 1;
        String message = c.getString(c.getColumnIndex(MESSAGE_COL));
        SessionManager.EncounterSession sess = new SessionManager.EncounterSession(start, end, maxRSSI, smoothedDistance, sessionState,
                isNotificationShown, message);
        return new Pair<>(beaconId, sess);
    }

    public void addNewSessionForBeacon(String beaconId, long start, long end, int maxRSSI, float distance, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BEACON_ID_COL, beaconId);
        cv.put(START_TIME_COL, start);
        cv.put(END_TIME_COL, end);
        cv.put(MAX_RSSI_COL, maxRSSI);
        cv.put(SMOOTHED_DIST_COL, distance);
        cv.put(SESSION_STATUS_COL, SessionManager.EncounterSession.ONGOING_SESSION);
        cv.put(IS_NOTIFICATION_SHOWN_COL, 0); // at the start, the notification will never be shown
        cv.put(MESSAGE_COL, message);
        db.insert(SESSION_TABLE, null, cv);
        db.close();
    }

    public int updateToContinueOngoingSession(SessionManager.BeaconData beaconData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(END_TIME_COL, beaconData.currentEnd);
        values.put(MAX_RSSI_COL, beaconData.maxRSSI);
        values.put(SMOOTHED_DIST_COL, beaconData.currentSmoothedDistance);
        values.put(IS_NOTIFICATION_SHOWN_COL, beaconData.isNotificationShown ? 1 : 0);
        values.put(MESSAGE_COL, beaconData.message);

        String selection = BEACON_ID_COL + " = ? AND " + START_TIME_COL + " = ? AND " + SESSION_STATUS_COL + " = ?";
        String[] selectionArgs = {beaconData.beaconId, beaconData.currentStart + "", SessionManager.EncounterSession.ONGOING_SESSION};

        int count = db.update(
                SESSION_TABLE,
                values,
                selection,
                selectionArgs);
        db.close();
        return count;
    }

    public int changeSessionStatusForBeacon(SessionManager.BeaconData currentBeaconData, String sessionStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SESSION_STATUS_COL, sessionStatus);
        values.put(MESSAGE_COL, currentBeaconData.message);

        String selection = BEACON_ID_COL + " = ? AND " + START_TIME_COL + " = ?";
        String[] selectionArgs = {currentBeaconData.beaconId, currentBeaconData.currentStart + ""};

        int count = db.update(
                SESSION_TABLE,
                values,
                selection,
                selectionArgs);
        db.close();
        return count;
    }

    public ArrayList<SessionManager.BeaconData> getDataForAllBeacons() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashMap<String, SessionManager.BeaconData> beaconIdToData = new HashMap<>();
        Cursor c = db.query(SESSION_TABLE, SESSION_TABLE_COLS, null, null, null, null, START_TIME_COL + " DESC");
        while (c.moveToNext()) {
            Pair<String, SessionManager.EncounterSession> sessionInfo = buildSessionObjectFromCursor(c);
            String beaconId = sessionInfo.first;
            SessionManager.EncounterSession session = sessionInfo.second;
            SessionManager.BeaconData beaconData = beaconIdToData.get(beaconId);
            if (beaconData == null) {
                if (session.isOngoing()) {
                    beaconData = new SessionManager.BeaconData(beaconId, session.maxRSSI, session.start, session.end,
                            session.smoothedDistance, session.isNotiticationShownForSession,
                            session.message);
                } else {
                    beaconData = new SessionManager.BeaconData(beaconId, -1, -1, -1, 0, "");
                    beaconData.addNewSession(session);
                }
                beaconIdToData.put(beaconId, beaconData);
            } else {
                if (session.isOngoing()) {
                    if (beaconData.isInSession()) {
                        Toast.makeText(context, "Multiple ongoing sessions for the same beacon!", Toast.LENGTH_SHORT).show();
                    } else {
                        beaconData.setCurrentSessionValuesFrom(session);
                    }
                } else {
                    beaconData.addNewSession(session);
                }
            }
        }
        c.close();
        db.close();

        ArrayList<SessionManager.BeaconData> allBeaconData = new ArrayList<>(beaconIdToData.values());
        Collections.sort(allBeaconData, (beaconData1, beaconData2) -> {
            long latestTime1 = beaconData1.getLatestTime();
            long latestTime2 = beaconData2.getLatestTime();

            if (latestTime1 > latestTime2) {
                return -1;
            } else if (latestTime1 < latestTime2) {
                return 1;
            } else {
                return 0;
            }
        });

        return allBeaconData;
    }

    public String getRawSessionData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(SESSION_TABLE, SESSION_TABLE_COLS, null, null, null, null, null);
        String info = "";
        while (c.moveToNext()) {
            Pair<String, SessionManager.EncounterSession> sessionInfo = buildSessionObjectFromCursor(c);
            String beaconId = sessionInfo.first;
            SessionManager.EncounterSession session = sessionInfo.second;
            info += "Beacon: " + beaconId + "\n";
            info += session.toString();
        }
        c.close();
        db.close();
        return info;
    }

    public HashMap<String, SessionManager.BeaconData> getBeaconDataForHour(long time) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(time);
        start.set(Calendar.MINUTE, 0);
        long startInMillis = start.getTimeInMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startAsStr = dateFormat.format(new Date(startInMillis));

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(time);
        end.set(Calendar.MINUTE, 59);
        long endInMillis = end.getTimeInMillis();
        String endAsStr = dateFormat.format(new Date(endInMillis));

        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s >= ? AND %s <= ?", START_TIME_COL, START_TIME_COL);
        String[] values = new String[]{startInMillis + "", endInMillis + ""};
        Cursor c = db.query(SESSION_TABLE, SESSION_TABLE_COLS, where, values, null, null, null);

        HashMap<String, SessionManager.BeaconData> beaconIdToData = new HashMap<>();

        if (c != null && c.getCount() != 0) {
            ArrayList<SessionManager.EncounterSession> decidedSessions = new ArrayList<>();
            long ongoingSessionStart = -1, ongoingSessionEnd = -1;
            float ongoingSessionDist = 0;
            while (c.moveToNext()) {
                Pair<String, SessionManager.EncounterSession> sessionInfo = buildSessionObjectFromCursor(c);
                String beaconId = sessionInfo.first;
                SessionManager.EncounterSession session = sessionInfo.second;
                SessionManager.BeaconData beaconData = beaconIdToData.get(beaconId);
                if (beaconData == null) {
                    if (session.isOngoing()) {
                        beaconData = new SessionManager.BeaconData(beaconId, session.maxRSSI, session.start, session.end, session.smoothedDistance, session.message);
                    } else {
                        beaconData = new SessionManager.BeaconData(beaconId, -1, -1, -1, 0, "");
                        beaconData.addNewSession(session);
                    }
                    beaconIdToData.put(beaconId, beaconData);
                } else {
                    if (session.isOngoing()) {
                        if (beaconData.isInSession()) {
                            Toast.makeText(context, "Multiple ongoing sessions for the same beacon!", Toast.LENGTH_SHORT).show();
                        } else {
                            beaconData.setCurrentSessionValuesFrom(session);
                        }
                    } else {
                        beaconData.addNewSession(session);
                    }
                }
            }
            c.close();
            db.close();
            return beaconIdToData;
        } else {
            return null;
        }
    }

    public int setNotificationShownForCurrentSessionInBeacon(SessionManager.BeaconData currentBeaconData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(IS_NOTIFICATION_SHOWN_COL, 1);

        String selection = BEACON_ID_COL + " = ? AND " + START_TIME_COL + " = ?";
        String[] selectionArgs = {currentBeaconData.beaconId, currentBeaconData.currentStart + ""};

        int count = db.update(
                SESSION_TABLE,
                values,
                selection,
                selectionArgs);
        db.close();
        return count;
    }

    public void logLocation(long currentMinuteTime, String campusName, boolean isOnCampus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOG_TIME_COL, currentMinuteTime);
        values.put(CAMPUS_NAME_COL, campusName);
        values.put(IS_ON_CAMPUS_COL, isOnCampus ? 1 : 0);
        db.insert(GPS_LOG_TABLE, null, values);
        db.close();
    }

    public ArrayList<SessionManager.GPSLog> getGPSLogsInRange(Long minTime, Long maxTime) {
        ArrayList<SessionManager.GPSLog> gpsLogs = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s >= ? AND %s <= ?", LOG_TIME_COL, LOG_TIME_COL);
        String[] values = new String[]{minTime + "", maxTime + ""};
        Cursor c = db.query(GPS_LOG_TABLE, GPS_LOG_TABLE_COLS, where, values, null, null, LOG_TIME_COL);
        if (c != null && c.getCount() != 0) {
            gpsLogs = new ArrayList<>();
            while (c.moveToNext()) {
                SessionManager.GPSLog gpsLog = buildGpsLogFromCursor(c);
                gpsLogs.add(gpsLog);
            }
        }
        db.close();
        return gpsLogs;
    }

    private SessionManager.GPSLog buildGpsLogFromCursor(Cursor c) {
        long logTime = c.getLong(c.getColumnIndex(LOG_TIME_COL));
        String campusName = c.getString(c.getColumnIndex(CAMPUS_NAME_COL));
        boolean isOnCampus = c.getInt(c.getColumnIndex(IS_ON_CAMPUS_COL)) == 1;
        return new SessionManager.GPSLog(logTime, campusName, isOnCampus);
    }

    public String getGPSLogDump() {
        SQLiteDatabase db = this.getReadableDatabase();
        String dump = "GPS Dump:\n";
        Cursor c = db.query(GPS_LOG_TABLE, GPS_LOG_TABLE_COLS, null, null, null, null, LOG_TIME_COL + " DESC");
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                SessionManager.GPSLog gpsLog = buildGpsLogFromCursor(c);
                dump += gpsLog.toString() + "\n__\n";
            }
        }
        c.close();
        db.close();
        return dump;
    }

    public void addLogException(String errorMessage) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ERROR_LOG_COL, errorMessage);
        db.insert(ERROR_LOG_TABLE, null, values);
        db.close();
    }

    public String getRawGPSDump() {
        SQLiteDatabase db = this.getReadableDatabase();
        String dump = "";
        Cursor c = db.query(ERROR_LOG_TABLE, ERROR_LOG_TABLE_COLS, null, null, null, null, null);
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                String errorLog = c.getString(c.getColumnIndex(ERROR_LOG_COL));
                dump += errorLog + "\n__\n";
            }
        }
        c.close();
        db.close();
        return dump;
    }

    public void logVBTName(Pair<Integer, Integer> vbt) {
        String vbtStr = vbt.first + ":" + vbt.second;
        Calendar time = Calendar.getInstance();
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        long timeStamp = time.getTimeInMillis();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOG_TIME_COL, timeStamp);
        values.put(VBT_NAME_COL, vbtStr);
        db.insert(VBT_NAME_TABLE, null, values);
        db.close();
    }

    public String getVBTLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        String dump = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Cursor c = db.query(VBT_NAME_TABLE, VBT_NAME_TABLE_COLS, null, null, null, null, null);
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                long time = c.getLong(c.getColumnIndex(LOG_TIME_COL));
                String vbtName = c.getString(c.getColumnIndex(VBT_NAME_COL));
                String date = dateFormat.format(new Date(time));
                dump += "Time: " + date + ", VBT: " + vbtName + "\n__\n";
            }
        }
        c.close();
        db.close();
        return dump;
    }
}
