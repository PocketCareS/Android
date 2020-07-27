package com.ub.pocketcares.backend;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.ub.pocketcares.backend.DailyStatus.DailyStatusEntry;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.PreferenceTags;
import com.ub.pocketcares.utility.Utility;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DailyStatusDatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "daily_db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_DAILYSTATUS_TABLE =
            "CREATE TABLE " + DailyStatusEntry.TABLE_NAME + " (" +
                    DailyStatusEntry.COLUMN_NAME_DATE + INT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    DailyStatusEntry.COLUMN_NAME_NWIFIAP + INT_TYPE + COMMA_SEP +
                    DailyStatusEntry.COLUMN_NAME_NBT + INT_TYPE + COMMA_SEP +
                    DailyStatusEntry.COLUMN_NAME_MILEAGE + REAL_TYPE + COMMA_SEP +
                    DailyStatusEntry.COLUMN_NAME_ACTSUM + TEXT_TYPE + COMMA_SEP +
                    DailyStatusEntry.COLUMN_NAME_HEALTH + TEXT_TYPE +
                    " )";
    private static final String SQL_DELETE_DAILYSTATUS_TABLE =
            "DROP TABLE IF EXISTS" + DailyStatusEntry.TABLE_NAME;


    public DailyStatusDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DAILYSTATUS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            try {
                String update = "ALTER TABLE " + DailyStatusEntry.TABLE_NAME +
                        " ADD COLUMN " + DailyStatusEntry.COLUMN_NAME_ACTSUM + TEXT_TYPE;
                db.execSQL(update);
                onCreate(db);
            } catch (Exception e) {
            }
        }
    }

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    public DailyStatus getDailyStatusbyDate(int date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {
                DailyStatusEntry.COLUMN_NAME_DATE,
                DailyStatusEntry.COLUMN_NAME_HEALTH,
                DailyStatusEntry.COLUMN_NAME_MILEAGE,
                DailyStatusEntry.COLUMN_NAME_NBT,
                DailyStatusEntry.COLUMN_NAME_NWIFIAP,
                DailyStatusEntry.COLUMN_NAME_ACTSUM
        };
        Cursor c = db.query(
                DailyStatusEntry.TABLE_NAME,
                columns,
                DailyStatusEntry.COLUMN_NAME_DATE + "=?",
                new String[]{String.valueOf(date)},
                null,
                null,
                null,
                null);

        if (null == c || c.getCount() == 0)
            return null;
        else {
            c.moveToFirst();
            DailyStatus ds = new DailyStatus(date);
            ds.setHealth(c.getString(1));
            ds.setMileage(c.getDouble(2));
            ds.setNumOfBT(c.getInt(3));
            ds.setNumOfWiFiAP(c.getInt(4));
            ds.setActSum(c.getString(5));
            return ds;
        }
    }

    public int update(DailyStatus ds) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DailyStatusEntry.COLUMN_NAME_NWIFIAP, ds.getNumOfWiFiAP());
        values.put(DailyStatusEntry.COLUMN_NAME_NBT, ds.getNumOfBT());
        values.put(DailyStatusEntry.COLUMN_NAME_MILEAGE, ds.getMileage());
        values.put(DailyStatusEntry.COLUMN_NAME_HEALTH, ds.getHealthString());
        values.put(DailyStatusEntry.COLUMN_NAME_ACTSUM, ds.getActSumString());

        return db.update(DailyStatusEntry.TABLE_NAME,
                values,
                DailyStatusEntry.COLUMN_NAME_DATE + "=?",
                new String[]{String.valueOf(ds.getDate())});
    }

    public void insertHealth(HealthStatus hs) {
        int dateint = Integer.parseInt(hs.getDateString());
        DailyStatus ds = getDailyStatusbyDate(dateint);
        if (null == ds) {
            ds = new DailyStatus(dateint);
            ds.setHealth(hs.toDBString());
            insert(ds);
        } else {
            ds.setHealth(hs.toDBString());
            update(ds);
        }
    }

    public long insert(DailyStatus ds) {
        SQLiteDatabase db = this.getWritableDatabase();
        DailyStatus query = getDailyStatusbyDate(ds.getDate());
        if (null == query) {
            ContentValues values = new ContentValues();

            values.put(DailyStatusEntry.COLUMN_NAME_DATE, ds.getDate());
            values.put(DailyStatusEntry.COLUMN_NAME_NWIFIAP, ds.getNumOfWiFiAP());
            values.put(DailyStatusEntry.COLUMN_NAME_NBT, ds.getNumOfBT());
            values.put(DailyStatusEntry.COLUMN_NAME_MILEAGE, ds.getMileage());
            values.put(DailyStatusEntry.COLUMN_NAME_HEALTH, ds.getHealthString());
            values.put(DailyStatusEntry.COLUMN_NAME_ACTSUM, ds.getActSumString());

            return db.insert(
                    DailyStatusEntry.TABLE_NAME,
                    null,
                    values);
        } else
            return update(ds);
    }

    public ArrayList<DailyStatus> getAllDailyStatus() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {
                DailyStatusEntry.COLUMN_NAME_DATE,
                DailyStatusEntry.COLUMN_NAME_MILEAGE,
                DailyStatusEntry.COLUMN_NAME_HEALTH,
                DailyStatusEntry.COLUMN_NAME_NBT,
                DailyStatusEntry.COLUMN_NAME_NWIFIAP,
                DailyStatusEntry.COLUMN_NAME_ACTSUM
        };
        Cursor c = db.query(
                DailyStatusEntry.TABLE_NAME,
                columns,
                null,
                null,
                null,
                null,
                DailyStatusEntry.COLUMN_NAME_DATE + " DESC");

        if (null != c && c.getCount() > 0) {
            ArrayList<DailyStatus> dslist = new ArrayList<DailyStatus>();
            while (c.moveToNext()) {
                DailyStatus ds = new DailyStatus(c.getInt(0));
                ds.setMileage(c.getDouble(1));
                ds.setHealth(c.getString(2));
                ds.setNumOfBT(c.getInt(3));
                ds.setNumOfWiFiAP(c.getInt(4));
                ds.setActSum(c.getString(5));
                dslist.add(ds);

                Log.v(LogTags.DATABASE, "getAllds: " + ds.toJsonString());
            }
            return dslist;
        } else
            return null;
    }

    public String getUploadData() throws JSONException {
        JSONObject uploadData = new JSONObject();
        JSONArray userSymptoms = new JSONArray();
        JSONArray roommatesSymptoms = new JSONArray();
        JSONArray othersSymptoms = new JSONArray();
        Calendar current = Calendar.getInstance();
        current.set(Calendar.HOUR, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);
        current.set(Calendar.MILLISECOND, 0);
        DailyStatus dailyStatus = getDailyStatusbyDate(Utility.getCalendarInt(current));
        if (dailyStatus != null) {
            String healthString = dailyStatus.getHealthString();
            ArrayList<String> symptomList = HealthStatus.SYMPTOM_LIST;
            String[] res = healthString.split(HealthStatus.DIVIDER);
            String groupSelected = res[1];
            String symptomSelected = res[0];
            for (int i = 0; i < symptomList.size(); i++) {
                if (symptomSelected.charAt(i) == '1') {
                    userSymptoms.put(symptomList.get(i));
                }
            }
            if (groupSelected.charAt(2) == '1') {
                roommatesSymptoms.put("true");
            }
            if (groupSelected.charAt(3) == '1') {
                othersSymptoms.put("true");
            }
            uploadData.put("date", Utility.convertDayMills(current.getTimeInMillis()));
            uploadData.put("usersSymptoms", userSymptoms);
            uploadData.put("roommatesSymptoms", roommatesSymptoms);
            uploadData.put("othersSymptoms", othersSymptoms);
            return uploadData.toString();
        }
        return null;
    }

    // return dailystatus recent data first
    public ArrayList<DailyStatus> getPastDailyStatus(Calendar first, int pastdays) {
        ArrayList<DailyStatus> dslist = new ArrayList<>();
        while (pastdays > 0) {
            int dateint = Utility.getCalendarInt(first);
            DailyStatus ds = getDailyStatusbyDate(dateint);
            if (null == ds)
                ds = new DailyStatus(dateint);
            dslist.add(ds);
            // backward one day for loop
            first.add(Calendar.DAY_OF_MONTH, -1);
            pastdays--;
        }
        return dslist;
    }

}
