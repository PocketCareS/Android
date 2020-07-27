package com.ub.pocketcares.receiver;

import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.introduction.SplashActivity;
import com.ub.pocketcares.utility.Utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.ub.pocketcares.introduction.SplashActivity.DAILY_HEALTH_RESET;

public class BootBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        // start service on boot
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            SplashActivity.setHealthReminderAlarm(context);
            SplashActivity.setDownTimeAlarm(context);
            SplashActivity.setDailyHealthAlarm(context, Utility.getCalenderForHour(DAILY_HEALTH_RESET), MainActivity.ALARMID_DAILY12OCLOCK, 1);
        }
    }

}
