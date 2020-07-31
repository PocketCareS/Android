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

package com.ub.pocketcares.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ub.pocketcares.R;
import com.ub.pocketcares.bluetoothBeacon.MonitoringApplication;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.introduction.SplashActivity;
import com.ub.pocketcares.utility.LogTags;
import com.ub.pocketcares.utility.Utility;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;
import com.wdullaer.materialdatetimepicker.time.Timepoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.ub.pocketcares.bluetoothBeacon.SessionManager.SHARED_PREF_NAME_IS_ON_CAMPUS;

public class PreferenceActivity extends AppCompatActivity {
    private static final int HOURS_IN_DAY = 24;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int DOWNTIME_END_HOUR = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        View view = getLayoutInflater().inflate(R.layout.action_bar, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);

        TextView Title = view.findViewById(R.id.actionbar_title);
        Title.setText(getString(R.string.app_name));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ub)));
        getSupportActionBar().setCustomView(view, params);
        getSupportActionBar().setDisplayShowCustomEnabled(true); //show custom title

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference closeEncounterSwitchPref = findPreference("close_encounter_notification_status");
            Preference closeEncounterSnooze = findPreference("close_encounter_snooze_option");
            Preference healthReminderTime = findPreference("health_reminder_time");
            Preference downtimeStart = findPreference("downtime_start");
            Preference downtimeEnd = findPreference("downtime_end");
            Preference offCampusScan = findPreference("stop_off_campus_scan");

            String downTimeStartString = downtimeStart.getSharedPreferences().getString("downtime_start", null);
            String downTimeEndString = downtimeStart.getSharedPreferences().getString("downtime_end", null);
            if (downTimeStartString != null) {
                setDowntimeSummary(downTimeStartString, downtimeStart, false);
            }
            if (downTimeEndString != null) {
                setDowntimeSummary(downTimeEndString, downtimeEnd, true);
            }

            int healthHour = healthReminderTime.getSharedPreferences().getInt("health_reminder_time", -1);
            if (healthHour != -1) {
                Calendar reminderCalendar = Utility.getHealthReminderCalendar(healthHour);
                String timeString = new SimpleDateFormat("h:mm a", Locale.US).format(reminderCalendar.getTime());
                healthReminderTime.setSummary("Daily health reminder set at " + timeString + " everyday.");
            }

            boolean isEnabled = closeEncounterSwitchPref.getSharedPreferences().getBoolean("close_encounter_notification_status", true);
            closeEncounterSnooze.setVisible(isEnabled);
            ListPreference listPref = (ListPreference) closeEncounterSnooze;
            if (listPref.getEntry() != null && !listPref.getEntry().equals(getString(R.string.snooze_none_label))) {
                listPref.setSummary(listPref.getEntry());
            } else {
                listPref.setSummary(getString(R.string.snooze_options_summary));
            }
            closeEncounterSwitchPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean notificationEnabled = Boolean.parseBoolean(newValue.toString());
                closeEncounterSnooze.setVisible(notificationEnabled);
                return true;
            });

            closeEncounterSnooze.setOnPreferenceChangeListener((preference, newValue) -> {
                String snoozeValue = String.valueOf(newValue);
                if (snoozeValue.equals(getString(R.string.snooze_none_value))) {
                    listPref.setSummary(getString(R.string.snooze_options_summary));
                } else if (snoozeValue.equals(getString(R.string.snooze_day_value))) {
                    listPref.setSummary(getString(R.string.snooze_day_label));
                } else if (snoozeValue.equals(getString(R.string.snooze_hour_value))) {
                    listPref.setSummary(getString(R.string.snooze_hour_label));
                }
                Intent snoozeIntent = new Intent(MonitoringApplication.NOTIFICATION_RECEIVE_INTENT);
                Log.v("Preference", snoozeValue);
                snoozeIntent.putExtra("snoozeValue", snoozeValue);
                getActivity().sendBroadcast(snoozeIntent);
                return true;
            });

            healthReminderTime.setOnPreferenceChangeListener((preference, newValue) -> {
                Calendar reminderCalendar = Utility.getHealthReminderCalendar((int) newValue);
                String timeString = new SimpleDateFormat("h:mm a", Locale.US).format(reminderCalendar.getTime());
                preference.setSummary("Daily health reminder set at " + timeString + " everyday.");
                SplashActivity.setDailyHealthAlarm(requireContext(), reminderCalendar, MainActivity.ALARMID_DAILYHEALTH, 2);
                return true;
            });

            downtimeStart.setOnPreferenceChangeListener((preference, newValue) -> {
                setDowntimeSummary(String.valueOf(newValue), downtimeStart, false);
                if (downTimeEndString == null) {
                    SharedPreferences.Editor editor = downtimeEnd.getSharedPreferences().edit();
                    editor.putString("downtime_end", "9;0");
                    editor.apply();
                }
                String[] timeArray = String.valueOf(newValue).split(";");
                SplashActivity.downTimeAlarmHelper(timeArray[0], timeArray[1], requireContext(), true);
                return true;
            });

            downtimeEnd.setOnPreferenceChangeListener((preference, newValue) -> {
                setDowntimeSummary(String.valueOf(newValue), downtimeEnd, true);
                String[] timeArray = String.valueOf(newValue).split(";");
                SplashActivity.downTimeAlarmHelper(timeArray[0], timeArray[1], requireContext(), false);
                return true;
            });

            offCampusScan.setOnPreferenceChangeListener(((preference, newValue) -> {
                if (Boolean.parseBoolean(newValue.toString())) {
                    boolean isOnCampus = offCampusScan.getSharedPreferences().getBoolean(SHARED_PREF_NAME_IS_ON_CAMPUS, false);
                    if (!isOnCampus) {
                        Intent stopScanTransmit = new Intent(MonitoringApplication.SCAN_BLE_ALARM);
                        stopScanTransmit.putExtra(LogTags.ALARM, MonitoringApplication.STOP_BLE);
                        getContext().sendBroadcast(stopScanTransmit);
                    }
                } else {
                    Intent startScanTransmit = new Intent(MonitoringApplication.SCAN_BLE_ALARM);
                    startScanTransmit.putExtra(LogTags.ALARM, MonitoringApplication.START_BLE);
                    getContext().sendBroadcast(startScanTransmit);
                }
                return true;
            }));
        }

        private static void setDowntimeSummary(String downtimeString, Preference downtime, boolean end) {
            String[] downTimeArray = downtimeString.split(";");
            Calendar display = Calendar.getInstance();
            display.set(Calendar.HOUR_OF_DAY, Integer.parseInt(downTimeArray[0]));
            display.set(Calendar.MINUTE, Integer.parseInt(downTimeArray[1]));
            String timeString = new SimpleDateFormat("h:mm a", Locale.US).format(display.getTime());
            String summaryString = "Downtime will start at " + timeString + " everyday.";
            if (end) {
                summaryString = "Downtime will end at " + timeString + " everyday.";
            }
            downtime.setSummary(summaryString);
        }


        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            // Try if the preference is one of our custom Preferences
            if (preference instanceof TimePreference) {
                // Create a new instance of TimePreferenceDialogFragment with the key of the related
                // Preference
                DialogFragment dialogFragment = TimePreferenceDialogFragmentCompat
                        .newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), "CustomPreference");
            } else if (preference instanceof TimeRangePreference) {
                Calendar now = Calendar.getInstance();
                TimePickerDialog.OnTimeSetListener listener = (view1, hourOfDay, minute, second) -> {
                    String value = hourOfDay + ";" + minute;
                    if (preference.callChangeListener(value)) {
                        ((TimeRangePreference) preference).setTime(value);
                    }
                };
                TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                        listener,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        false
                );
                Pair<Integer, Integer> savedTime = ((TimeRangePreference) preference).getTime();
                timePickerDialog.setThemeDark(true);
                timePickerDialog.setTimeInterval(1, 1, 60);
                if (savedTime != null) {
                    timePickerDialog.setInitialSelection(savedTime.first, savedTime.second);
                } else {
                    timePickerDialog.setInitialSelection(20, 1);
                }
                timePickerDialog.setAccentColor(Color.GRAY);
                ArrayList<Timepoint> disabled = new ArrayList<>();
                if (preference.getKey().equals("downtime_start")) {
                    timePickerDialog.setTitle("Select a time to pause scanning");
                    for (int i = 7; i < 20; i++) {
                        for (int j = 0; j < 60; j++) {
                            disabled.add(new Timepoint(i, j, 0));
                        }
                    }
                    disabled.add(new Timepoint(20, 0, 0));
                } else {
                    timePickerDialog.setTitle("Select a time to resume scanning");
                    String startString = preference.getSharedPreferences().getString("downtime_start", null);
                    int endIdx = 21;
                    if (startString != null) {
                        String[] startTimeArray = startString.split(";");
                        endIdx = Integer.parseInt(startTimeArray[0]);
                        if (endIdx >= 0 && endIdx <= DOWNTIME_END_HOUR) {
                            endIdx = HOURS_IN_DAY + endIdx;
                        }
                    }
                    for (int i = 8; i < endIdx + 1; i++) {
                        int hourValue = i;
                        if (i > HOURS_IN_DAY - 1) {
                            hourValue = i - HOURS_IN_DAY;
                        }
                        for (int j = 0; j < MINUTES_IN_HOUR; j++) {
                            disabled.add(new Timepoint(hourValue, j, 0));
                        }
                    }
                }
                timePickerDialog.setDisabledTimes(disabled.toArray(new Timepoint[disabled.size()]));
                timePickerDialog.setTargetFragment(this, 0);
                timePickerDialog.show(getParentFragmentManager(), "Timepickerdialog");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}