package com.ub.pocketcares.backend;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.ub.pocketcares.bluetoothBeacon.BeaconStat;
import com.ub.pocketcares.utility.Utility;

public class BluetoothBeaconDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bluetoothBeacon.db";
    private static final String TABLE_NAME = "bluetoothBeacon_data";
    private static final int DATABASE_VERSION = 4;
    private static final long DAY_VALUE = 86400000L;
    private static final String TEXT_TYPE = "TEXT";
    private static final String REAL_TYPE = "REAL";
    private static final String INT_TYPE = "INTEGER";
    private static final String VBT_COLUMN = "vbt";
    private static final String UUID_COLUMN = "uuid";
    private static final String DATE_COLUMN = "unix_date";
    private static final String MINOR = "minor";
    private static final String MAJOR = "major";
    private static final String REGION = "region_name";
    private static final String POSTAL_CODE = "postal_code";
    private static final String TWO_DISTANCE = "two_distance";
    private static final String TWO_COUNT = "two_count";
    private static final String TWO_SESSION_DURATION = "two_session_duration";
    private static final String TEN_DISTANCE = "ten_distance";
    private static final String TEN_COUNT = "ten_count";
    private static final String TOTAL_DISTANCE = "total_distance";
    private static final String TOTAL_COUNT = "total_count";
    private static final String[] COLUMNS = {VBT_COLUMN, UUID_COLUMN, DATE_COLUMN, MINOR, MAJOR, POSTAL_CODE, REGION, TWO_DISTANCE, TWO_COUNT, TWO_SESSION_DURATION, TEN_DISTANCE, TEN_COUNT, TOTAL_DISTANCE, TOTAL_COUNT};
    private Context context;

    private static final String SQL_CREATE_TABLE = String.format("CREATE TABLE %s (" +
                    "%s %s PRIMARY KEY NOT NULL,%s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s, %s %s)", TABLE_NAME,
            VBT_COLUMN, TEXT_TYPE,
            UUID_COLUMN, TEXT_TYPE,
            DATE_COLUMN, INT_TYPE,
            MINOR, INT_TYPE,
            MAJOR, INT_TYPE,
            POSTAL_CODE, INT_TYPE,
            REGION, TEXT_TYPE,
            TWO_DISTANCE, REAL_TYPE,
            TWO_COUNT, INT_TYPE,
            TWO_SESSION_DURATION, REAL_TYPE,
            TEN_DISTANCE, REAL_TYPE,
            TEN_COUNT, INT_TYPE,
            TOTAL_DISTANCE, REAL_TYPE,
            TOTAL_COUNT, INT_TYPE
    );

    private static final String SQL_ALTER_TABLE = String.format("ALTER TABLE %s ADD COLUMN %s %s", TABLE_NAME, UUID_COLUMN, TEXT_TYPE);

    public BluetoothBeaconDatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion && newVersion == 4) {
            db.execSQL(SQL_ALTER_TABLE);
        }
    }

    public void removeAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + TABLE_NAME);
        db.close();
    }

    private ContentValues generateContentValues(BeaconStat contact) {
        ContentValues cv = new ContentValues();
        cv.put(VBT_COLUMN, contact.getVbt());
        cv.put(UUID_COLUMN, contact.getBeacon().getId1().toString());
        cv.put(DATE_COLUMN, Utility.convertHourMills(contact.getTimeStamp()));
        cv.put(MINOR, contact.getMinor());
        cv.put(MAJOR, contact.getMajor());
        cv.put(POSTAL_CODE, contact.getPostalCode());
        cv.put(REGION, contact.getRegionName());
        cv.put(TWO_DISTANCE, contact.getTwoDistance());
        cv.put(TWO_COUNT, contact.getTwoCount());
        cv.put(TWO_SESSION_DURATION, contact.getTwoSessionDuration());
        cv.put(TEN_DISTANCE, contact.getTenDistance());
        cv.put(TEN_COUNT, contact.getTenCount());
        cv.put(TOTAL_DISTANCE, contact.getTotalDistance());
        cv.put(TOTAL_COUNT, contact.getTotalCount());
        return cv;
    }

    public void addOrUpdateContact(Collection<BeaconStat> contactCollection) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (BeaconStat contact : contactCollection) {
            if (contact.getTwoCount() > 0 && contact.getTwoSessionDurationMinutes() > 0) {
                db.replace(TABLE_NAME, null, generateContentValues(contact));
            }
        }
        db.close();
    }

    public HashMap<Beacon, BeaconStat> getCurrentHourData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s = ?", DATE_COLUMN);
        String currentHour = Long.toString(Utility.convertHourMills(Calendar.getInstance().getTimeInMillis()));
        String[] values = new String[]{currentHour};
        Cursor c = db.query(TABLE_NAME, COLUMNS, where, values, null, null, DATE_COLUMN + " DESC");
        HashMap<Beacon, BeaconStat> currentHourEncounters = new HashMap<>();
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                BeaconStat beaconStat = getBeaconObject(c);
                currentHourEncounters.put(beaconStat.getBeacon(), beaconStat);
            }
        }
        c.close();
        db.close();
        return currentHourEncounters;
    }

    public TreeMap<Long, HashSet<BeaconStat>> getDisplayContacts(String date) {
        // Date format MM/dd/yyyy
        long curDay = Utility.getUnixFromString(date);
        long nextDay = curDay + DAY_VALUE;
        SQLiteDatabase db = this.getReadableDatabase();
        String where = String.format(Locale.US, "%s >= ? AND %s >= ? AND %s < ?", TWO_COUNT, DATE_COLUMN, DATE_COLUMN);
        String[] values = new String[]{Integer.toString(0), Long.toString(curDay), Long.toString(nextDay)};
        Cursor c = db.query(TABLE_NAME, COLUMNS, where, values, null, null, DATE_COLUMN + " DESC");
        TreeMap<Long, HashSet<BeaconStat>> closeContacts = new TreeMap<>();
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                BeaconStat beaconStat = getBeaconObject(c);
                long keyTimeStamp = Utility.convertHourMills(beaconStat.getTimeStamp());
                HashSet<BeaconStat> closeBeacons = new HashSet<>();
                if (closeContacts.containsKey(keyTimeStamp)) {
                    closeBeacons = closeContacts.get(keyTimeStamp);
                }
                closeBeacons.add(beaconStat);
                closeContacts.put(keyTimeStamp, closeBeacons);
            }
        }
        c.close();
        db.close();
        return closeContacts;
    }

    public ArrayList<BeaconStat> getAllContacts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, DATE_COLUMN + " DESC");
        ArrayList<BeaconStat> allContacts = new ArrayList<>();
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                BeaconStat beaconStat = getBeaconObject(c);
                allContacts.add(beaconStat);
            }
        }
        c.close();
        db.close();
        return allContacts;
    }

    public ArrayList<BeaconStat> getContactsForDayRange(Calendar date) {
        date.set(Calendar.MILLISECOND, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.HOUR_OF_DAY, 0);
        SQLiteDatabase db = this.getReadableDatabase();
        long currentDay = date.getTimeInMillis();
        date.add(Calendar.DAY_OF_MONTH, 1);
        long nextDay = date.getTimeInMillis();
        String where = String.format(Locale.US, "%s >= ? AND %s < ?", DATE_COLUMN, DATE_COLUMN);
        String[] values = new String[]{Long.toString(currentDay), Long.toString(nextDay)};
        Cursor c = db.query(TABLE_NAME, COLUMNS, where, values, null, null, DATE_COLUMN + " DESC");
        ArrayList<BeaconStat> allContacts = new ArrayList<>();
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                BeaconStat beaconStat = getBeaconObject(c);
                allContacts.add(beaconStat);
            }
        }
        c.close();
        db.close();
        return allContacts;
    }

    public String getRawBeaconStatDump() {
        SQLiteDatabase db = this.getReadableDatabase();
        String dump = "";
        Cursor c = db.query(TABLE_NAME, COLUMNS, null, null, null, null, DATE_COLUMN);
        if (c != null && c.getCount() > 0) {
            while (c.moveToNext()) {
                BeaconStat beaconStat = getBeaconObject(c);
                dump += beaconStat.toString() + "\n__\n";
            }
        }
        c.close();
        db.close();
        return dump;
    }

    public Pair<Calendar, Calendar> databaseDateRange() {
        ArrayList<BeaconStat> allEncounters = getAllContacts();
        if (allEncounters.isEmpty()) {
            return new Pair<>(Calendar.getInstance(), Calendar.getInstance());
        } else {
            long endTime = allEncounters.get(0).getTimeStamp();
            Calendar endDate = Calendar.getInstance();
            endDate.setTimeInMillis(endTime);
            long startTime = allEncounters.get(allEncounters.size() - 1).getTimeStamp();
            Calendar startDate = Calendar.getInstance();
            startDate.setTimeInMillis(startTime);
            return new Pair<>(startDate, endDate);
        }
    }

    public int getContactCount() {
        ArrayList<BeaconStat> allContacts = getAllContacts();
        int allSessionsCount = 0;
        for (BeaconStat contact : allContacts) {
            allSessionsCount += contact.getTwoCount();
        }
        return allSessionsCount;
    }

    public String getUploadData(boolean deleteOld, boolean uploadError) {
        try {
            if (deleteOld) {
                removeOldContacts();
            }
            Map<Long, DailyContactInfo> dailyContactInfoMap = getContactInfo(uploadError);
            DailyContactRequest dailyContactRequest = new DailyContactRequest(dailyContactInfoMap);
            return new Gson().toJson(dailyContactRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<Long, DailyContactInfo> getContactInfo(boolean uploadError) {
        ArrayList<BeaconStat> allContacts;
        if (uploadError) {
            allContacts = getAllContacts();
        } else {
            allContacts = getContactsForDayRange(Calendar.getInstance());
        }
        Map<Long, DailyContactInfo> dailyContactInfoMap = new HashMap<>();
        for (BeaconStat beaconStat : allContacts) {
            TreeMap<Long, TreeMap<Integer, ArrayList<ContactVbtInfo>>> dataFormat = new TreeMap<>();
            DailyContactInfo dailyContactInfo = new DailyContactInfo();
            double average = beaconStat.getTwoCount() == 0 ? beaconStat.getTwoSessionDurationMinutes() :
                    (double) beaconStat.getTwoSessionDurationMinutes() / beaconStat.getTwoCount();
            ContactVbtInfo contactVbtInfo = new ContactVbtInfo(beaconStat.getVbt(),
                    beaconStat.getTwoSessionDurationMinutes(), beaconStat.getTenCount(), average, beaconStat.getTotalCount());
            long dailyTimestamp = Utility.convertDayMills(beaconStat.getTimeStamp());
            if (dailyContactInfoMap.containsKey(dailyTimestamp)) {
                dailyContactInfo = dailyContactInfoMap.get(dailyTimestamp);
            }
            if (dailyContactInfo.getHourlyContactInfo() != null) {
                dataFormat = dailyContactInfo.getHourlyContactInfo();
            }
            long hourlyTimeStamp = beaconStat.getTimeStamp();
            if (hourlyTimeStamp != Utility.convertHourMills(Calendar.getInstance().getTimeInMillis())) {
                int postalCode = beaconStat.getPostalCode();
                TreeMap<Integer, ArrayList<ContactVbtInfo>> postalBeacons = new TreeMap<>();
                ArrayList<ContactVbtInfo> beaconStatsList = new ArrayList<>();
                if (dataFormat.containsKey(hourlyTimeStamp)) {
                    postalBeacons = dataFormat.get(hourlyTimeStamp);
                    if (postalBeacons.containsKey(postalCode)) {
                        beaconStatsList = postalBeacons.get(postalCode);
                    }
                }
                beaconStatsList.add(contactVbtInfo);
                postalBeacons.put(postalCode, beaconStatsList);
                dataFormat.put(hourlyTimeStamp, postalBeacons);
                dailyContactInfo.setDailyKey(SecureKeys.getDailyKey(context, dailyTimestamp));
                dailyContactInfo.setHourlyContactInfo(dataFormat);
                dailyContactInfoMap.put(dailyTimestamp, dailyContactInfo);
            }
        }
        return dailyContactInfoMap;
    }

    private BeaconStat getBeaconObject(Cursor cursor) {
        int minor = cursor.getInt(cursor.getColumnIndex(MINOR));
        int major = cursor.getInt(cursor.getColumnIndex(MAJOR));
        String uuid = cursor.getString(cursor.getColumnIndex(UUID_COLUMN));
        if (uuid == null) {
            uuid = SecureKeys.UUID;
        }
        Beacon beacon = new Beacon.Builder()
                .setId1(uuid)
                .setId2(Integer.toString(major))
                .setId3(Integer.toString(minor)).build();
        return new BeaconStat(beacon, cursor.getLong(cursor.getColumnIndex(DATE_COLUMN)),
                cursor.getInt(cursor.getColumnIndex(POSTAL_CODE)), cursor.getString(cursor.getColumnIndex(REGION)),
                cursor.getDouble(cursor.getColumnIndex(TWO_DISTANCE)), cursor.getInt(cursor.getColumnIndex(TWO_COUNT)),
                cursor.getLong(cursor.getColumnIndex(TWO_SESSION_DURATION)), cursor.getDouble(cursor.getColumnIndex(TEN_DISTANCE)),
                cursor.getInt(cursor.getColumnIndex(TEN_COUNT)), cursor.getDouble(cursor.getColumnIndex(TOTAL_DISTANCE)),
                cursor.getInt(cursor.getColumnIndex(TOTAL_COUNT)));
    }

    private void removeOldContacts() {
        SharedPreferences sp = context.getSharedPreferences("databaseDate", Context.MODE_PRIVATE);
        long dateOffset = sp.getLong("deleteValue", -1);
        if (dateOffset != -1) {
            SQLiteDatabase db = this.getWritableDatabase();
            String where = String.format("%s <= ?", DATE_COLUMN);
            String[] values = new String[]{Long.toString(dateOffset)};
            db.delete(TABLE_NAME, where, values);
            db.close();
        }
    }

    private class DailyContactRequest {
        private Map<Long, DailyContactInfo> dateWiseContactInfo;

        DailyContactRequest(Map<Long, DailyContactInfo> dateWiseContactInfo) {
            this.dateWiseContactInfo = dateWiseContactInfo;
        }

        public Map<Long, DailyContactInfo> getDateWiseContactInfo() {
            return dateWiseContactInfo;
        }

        public void setDateWiseContactInfo(Map<Long, DailyContactInfo> dateWiseContactInfo) {
            this.dateWiseContactInfo = dateWiseContactInfo;
        }

    }

    private class DailyContactInfo {

        private String dailyKey;
        private TreeMap<Long, TreeMap<Integer, ArrayList<ContactVbtInfo>>> hourlyContactInfo;

        public TreeMap<Long, TreeMap<Integer, ArrayList<ContactVbtInfo>>> getHourlyContactInfo() {
            return hourlyContactInfo;
        }

        public void setHourlyContactInfo(TreeMap<Long, TreeMap<Integer, ArrayList<ContactVbtInfo>>> hourlyContactInfo) {
            this.hourlyContactInfo = hourlyContactInfo;
        }

        public String getDailyKey() {
            return dailyKey;
        }

        public void setDailyKey(String dailyKey) {
            this.dailyKey = dailyKey;
        }
    }


    private class ContactVbtInfo {

        private String vbtName;

        private int countTwo;

        private int countTen;

        private double avgDist;

        private int totalCount;

        ContactVbtInfo(String name, int countTwo, int countTen, double avgDist, int totalCount) {
            this.vbtName = name;
            this.countTwo = countTwo;
            this.countTen = countTen;
            this.avgDist = avgDist;
            this.totalCount = totalCount;
        }

        public String getVbtName() {
            return vbtName;
        }

        public void setVbtName(String vbtName) {
            this.vbtName = vbtName;
        }

        public int getCountTwo() {
            return countTwo;
        }

        public void setCountTwo(int countTwo) {
            this.countTwo = countTwo;
        }

        public int getCountTen() {
            return countTen;
        }

        public void setCountTen(int countTen) {
            this.countTen = countTen;
        }

        public double getAvgDist() {
            return avgDist;
        }

        public void setAvgDist(double avgDist) {
            this.avgDist = avgDist;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((vbtName == null) ? 0 : vbtName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ContactVbtInfo other = (ContactVbtInfo) obj;
            if (vbtName == null) {
                if (other.vbtName != null)
                    return false;
            } else if (!vbtName.equals(other.vbtName))
                return false;
            return true;
        }

    }

}
