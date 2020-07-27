package com.ub.pocketcares.backend;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.ub.pocketcares.utility.LogTags;

import android.provider.BaseColumns;
import android.util.Log;

public class DailyStatus {

    public static final String DAILYFILE = "daily.json";
    public static final String DAILYSTATUS = "dailystatus";

    private int date;
    private int nwifiap = 0;
    private int nbt = 0;
    private double mileage = 0;
    private String actsum;
    private String health;

    // set default values as well
    public DailyStatus(int date) {
        this.date = date;
        nwifiap = 0;
        nbt = 0;
        mileage = 0;
        actsum = "";
        health = "";
    }

    public static abstract class DailyStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "dailyStatus";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_NWIFIAP = "numwifiap";
        public static final String COLUMN_NAME_NBT = "numbt";
        public static final String COLUMN_NAME_MILEAGE = "mileage";
        public static final String COLUMN_NAME_ACTSUM = "actsum";
        public static final String COLUMN_NAME_HEALTH = "health";
    }

    public int getDate() {
        return this.date;
    }

    public int getNumOfWiFiAP() {
        return this.nwifiap;
    }

    public int getNumOfBT() {
        return this.nbt;
    }

    public double getMileage() {
        return this.mileage;
    }

    public String getActSumString() {
        return this.actsum;
    }

    public String getHealthString() {
        return this.health;
    }

    public void setNumOfWiFiAP(int nwifiap) {
        this.nwifiap = nwifiap;
    }

    public void setNumOfBT(int nbt) {
        this.nbt = nbt;
    }

    public void setMileage(double mile) {
        this.mileage = mile;
    }

    public void setActSum(String actsumstr) {
        this.actsum = actsumstr;
    }

    public void setHealth(String healthstr) {
        this.health = healthstr;
    }
    

    public String toJsonString() {
        JSONObject dailysts = new JSONObject();
        JSONObject JsonAttributes = new JSONObject();
        try {
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_DATE, date);
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_NWIFIAP, nwifiap);
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_NBT, nbt);
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_MILEAGE, mileage);
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_ACTSUM, actsum);
            JsonAttributes.put(DailyStatusEntry.COLUMN_NAME_HEALTH, health);
            dailysts.put(DAILYSTATUS, JsonAttributes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dailysts.toString();
    }

}
