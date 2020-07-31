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

package com.ub.pocketcares.utility;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class FirstTimeChecker {

    /**
     * Sets the boolean preference value
     *
     * @param context the current context
     * @param key     the preference key
     * @param value   the value to be set
     */
    public static void setBooleanPreferenceValue(Context context, String key, boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(key, value).apply();
    }

    /**
     * Get the boolean preference value from the SharedPreference
     *
     * @param context the current context
     * @param key     the preference key
     * @return the the preference value
     */
    public static boolean getBooleanPreferenceValue(Context context, String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(key, false);
    }
}
