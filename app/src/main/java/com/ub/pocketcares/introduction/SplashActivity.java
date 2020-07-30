package com.ub.pocketcares.introduction;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ub.pocketcares.backend.SecureKeys;
import com.ub.pocketcares.bluetoothBeacon.MonitoringApplication;
import com.ub.pocketcares.R;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.network.UploadWorker;
import com.ub.pocketcares.receiver.AlarmReceiver;
import com.ub.pocketcares.survey.HealthData;
import com.ub.pocketcares.utility.FirstTimeChecker;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.PreferenceTags;
import com.ub.pocketcares.utility.Utility;

import static com.ub.pocketcares.network.ServerHelper.generateFirebaseId;

public class SplashActivity extends AppCompatActivity {

    public Boolean isFirstTime = false;
    public static final int HEALTH_REMINDER_HOUR = 20;
    public static final int DAILY_HEALTH_RESET = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        isFirstTime = !FirstTimeChecker.getBooleanPreferenceValue(this, "isFirstTimeExecution");

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();
        complete();
    }

    private void complete() {
        new Handler().postDelayed(() -> {
            checkFirstTime(isFirstTime);
            SplashActivity.this.finish();
        }, 1000);
    }

    public void checkFirstTime(Boolean isFirstTime) {
        setDailyHealthAlarm(getApplicationContext(), Utility.getCalenderForHour(DAILY_HEALTH_RESET), MainActivity.ALARMID_DAILY12OCLOCK, 1);
        setHealthReminderAlarm(getApplicationContext());
        setDownTimeAlarm(getApplicationContext());
        if (isFirstTime) {
            generateFirebaseId(this);
            SharedPreferences sharedPreferences = getSharedPreferences("databaseDate", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Date currentDate = Calendar.getInstance().getTime();
            editor.putString("fourteenPeriod", Utility.changeDateToString(currentDate));
            editor.putString("firstRunDate", Utility.changeDateToString(currentDate));
            editor.apply();
            Runnable keyGeneration = () -> {
                SecureKeys.generateMasterKey(getApplicationContext());
                try {
                    SecureKeys.generateDailyKey(getApplicationContext(), Calendar.getInstance().getTimeInMillis());
                    SecureKeys.generateVBT(getApplicationContext());
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            };
            new Thread(keyGeneration).start();
            setUploadWork(this);
            Intent startActivityIntent = new Intent(SplashActivity.this, AppIntroActivity.class);
            startActivity(startActivityIntent);
        } else {
            SharedPreferences status = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String healthLastSubmit = status.getString(PreferenceTags.LAST_HEALTH_SUBMIT, null);
            if (healthLastSubmit != null) {
                if (healthLastSubmit.equals(Utility.changeDateToString2(Calendar.getInstance()))) {
                    HealthData.setHealthDailySubmitted(getApplicationContext(), true);
                } else {
                    HealthData.setHealthDailySubmitted(getApplicationContext(), false);
                }
            }
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
    }

    public static void setHealthReminderAlarm(Context context) {
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        int minutesAfterMidnight = preferences.getInt("health_reminder_time", -1);
        Calendar reminderCalendar;
        if (minutesAfterMidnight == -1) {
            reminderCalendar = Utility.getCalenderForHour(HEALTH_REMINDER_HOUR);
        } else {
            reminderCalendar = Utility.getHealthReminderCalendar(minutesAfterMidnight);
        }
        setDailyHealthAlarm(context, reminderCalendar, MainActivity.ALARMID_DAILYHEALTH, 2);
    }

    public static void setDailyHealthAlarm(Context context, Calendar calendar, int extra, int code) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra(LogTags.ALARM, extra);
        PendingIntent pi = PendingIntent.getBroadcast(context, code, i, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        Log.v("Alarm_Test", "Alarm Set at: " + calendar.getTime().toString());
    }

    public static void setDownTimeAlarm(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String startString = preferences.getString("downtime_start", null);
        String endString = preferences.getString("downtime_end", null);
        if (startString != null && endString != null) {
            String[] startArray = startString.split(";");
            String[] endArray = endString.split(";");
            downTimeAlarmHelper(startArray[0], startArray[1], context, true);
            downTimeAlarmHelper(endArray[0], endArray[1], context, false);
        }
    }

    public static void downTimeAlarmHelper(String startString, String endString, Context context, boolean start) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar downTimeCalendar = Calendar.getInstance();
        int startHour = Integer.parseInt(startString);
        if (startHour < downTimeCalendar.get(Calendar.HOUR_OF_DAY)) {
            downTimeCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        downTimeCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        downTimeCalendar.set(Calendar.MINUTE, Integer.parseInt(endString));
        downTimeCalendar.set(Calendar.SECOND, 0);
        downTimeCalendar.set(Calendar.MILLISECOND, 0);
        Intent downTimeIntent = new Intent(MonitoringApplication.SCAN_BLE_ALARM);
        PendingIntent downTimePendingIntent;
        if (start) {
            downTimeIntent.putExtra(LogTags.ALARM, MonitoringApplication.STOP_BLE);
            downTimeIntent.putExtra("type", "downtime_start");
            downTimePendingIntent = PendingIntent.getBroadcast(context, 22, downTimeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Log.v("Alarm_Test", "Downtime Start Set at: " + downTimeCalendar.getTime().toString());
        } else {
            downTimeIntent.putExtra(LogTags.ALARM, MonitoringApplication.START_BLE);
            downTimeIntent.putExtra("type", "downtime_stop");
            downTimePendingIntent = PendingIntent.getBroadcast(context, 23, downTimeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Log.v("Alarm_Test", "Downtime End Set at: " + downTimeCalendar.getTime().toString());
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, downTimeCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, downTimePendingIntent);
    }

    public static void setUploadWork(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();

        PeriodicWorkRequest uploadRequest =
                new PeriodicWorkRequest.Builder(UploadWorker.class, 1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setInitialDelay(Utility.getMinutesToNextHour(Calendar.getInstance()), TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context)
                .enqueue(uploadRequest);
    }

}
