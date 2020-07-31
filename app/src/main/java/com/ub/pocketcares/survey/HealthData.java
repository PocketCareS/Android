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
