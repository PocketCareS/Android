package com.ub.pocketcares.survey;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.ub.pocketcares.backend.HealthStatus;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.utility.PreferenceTags;

public class HealthData {

    public MainActivity mactivity;

    public HealthData(MainActivity ma) {
        this.mactivity = ma;
    }

    public static void writeLatestHealthStatus(Context context, String hs) {
        SharedPreferences ds = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = ds.edit();
        ed.putString(PreferenceTags.LATEST_HEALTH, hs);
        ed.commit();
    }

    public static void setHealthDailySubmitted(Context context, boolean b) {
        SharedPreferences ds = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = ds.edit();
        ed.putBoolean(PreferenceTags.DAILY_HEALTH_SUBMIT, b);
        ed.apply();
    }

    public static boolean isHealthDailySubmitted(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PreferenceTags.DAILY_HEALTH_SUBMIT, false);
    }


}
