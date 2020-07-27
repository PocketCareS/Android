package com.ub.pocketcares.receiver;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import com.ub.pocketcares.backend.BluetoothBeaconDatabaseHelper;
import com.ub.pocketcares.backend.SecureKeys;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.network.HTTPHelper;
import com.ub.pocketcares.network.ServerHelper;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.PreferenceTags;
import com.ub.pocketcares.utility.Utility;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;


import android.util.Log;

import org.json.JSONException;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("Alarm_Test", "current Time: " + Calendar.getInstance().getTime().toString());
        Log.v("Alarm_Test", "Daily Health Alarm received! " + intent.getIntExtra(LogTags.ALARM, -1));
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dailyWork:wakeLock");
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

        SharedPreferences datastatus = PreferenceManager.getDefaultSharedPreferences(context);
        NotificationManager notiMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // if this is the alarm from 12 oclock to reset health status or to reset alarms
        if (MainActivity.ALARMID_DAILY12OCLOCK == intent.getIntExtra(LogTags.ALARM, MainActivity.ALARMID_OTHER)) {
            Log.v("Alarm_Test", "Report health reset alarm received and daily key generated!");
            try {
                SecureKeys.generateDailyKey(context, Calendar.getInstance().getTimeInMillis());
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            final Context alarmContext = context;
            Runnable networkUpload = () -> {
                try {
                    BluetoothBeaconDatabaseHelper beaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(alarmContext);
                    HTTPHelper helper = new HTTPHelper();
                    String token = ServerHelper.generateToken(alarmContext, helper);
                    helper.postRequest(ServerHelper.CONTACT_ENDPOINT, beaconDatabaseHelper.getUploadData(false, false), token);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            };
            new Thread(networkUpload).start();
            SharedPreferences.Editor dataStatusEditor = datastatus.edit();
            dataStatusEditor.putBoolean(PreferenceTags.DAILY_HEALTH_SUBMIT, false);
            dataStatusEditor.apply();
        }
        // this is alarm fired at 8pm to notify submit health status
        if (MainActivity.ALARMID_DAILYHEALTH == intent.getIntExtra(LogTags.ALARM, MainActivity.ALARMID_OTHER)) {
            Log.v("Alarm_Test", "Report health reminder alarm received!");
            boolean ishealthSubmitted = datastatus.getBoolean(PreferenceTags.DAILY_HEALTH_SUBMIT, false);
            if (!ishealthSubmitted) {
                Intent healthintent = new Intent(context, MainActivity.class);
                healthintent.putExtra(MainActivity.INTENTEXTRA, MainActivity.NOTI_HEALTHSTATUS);
                Utility.createHighNotification(context, "Report Health Status",
                        "You have not submitted your health status today, tap to submit health status.",
                        healthintent, MainActivity.NOTI_HEALTHSTATUS, MainActivity.NOTI_HEALTHSTATUS);
                assert notiMan != null;
            }
            Log.v(LogTags.ALARM, "report health status notification sent out");
        }
        wakeLock.release();
    }

}
