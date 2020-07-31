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

package com.ub.pocketcares.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;

import com.ub.pocketcares.backend.DailyStatus;
import com.ub.pocketcares.backend.DailyStatusDatabaseHelper;
import com.ub.pocketcares.backend.HealthStatus;
import com.ub.pocketcares.R;

import com.ub.pocketcares.utility.Utility;

import static com.ub.pocketcares.home.HomeTabFragment.createReportHealthDialog;

public class HealthSummary extends Fragment {
    public Context m_appContext = null;
    HealthSummaryAdapter adapter;
    private View rootView;
    private TextView hiddenReport;
    private Button addHealth;
    private static final String FIRST_PREFERENCE_VALUE = "firstTimeHealthReport";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        m_appContext = getActivity();
        rootView = inflater.inflate(R.layout.health_summary, container, false);
        hiddenReport = rootView.findViewById(R.id.no_report);
        addHealth = rootView.findViewById(R.id.add_report_button);
        final DailyStatusDatabaseHelper ddh = new DailyStatusDatabaseHelper(m_appContext);
        final Handler handler = new Handler();
        LocalBroadcastManager.getInstance(m_appContext).registerReceiver(mMessageReceiver,
                new IntentFilter("ui-update-health_summary"));
        Runnable runnable = () -> {
            final ArrayList<DailyStatus> allStatus = ddh.getAllDailyStatus();
            handler.post(() -> {
                if (allStatus == null) {
                    hiddenReport.setVisibility(View.VISIBLE);
                    addHealth.setVisibility(View.VISIBLE);
                    addHealth.setOnClickListener(v -> createReportHealthDialog(MainActivity.m_mainActivity));
                } else {
                    addHealth.setVisibility(View.GONE);
                    hiddenReport.setVisibility(View.GONE);
                    Resources resource = m_appContext.getResources();
                    adapter = new HealthSummaryAdapter(m_appContext, R.layout.health_summary, createHealthList(allStatus, resource));
                    ListView healthView = rootView.findViewById(R.id.rootList);
                    healthView.setAdapter(adapter);
                }
            });
            ddh.closeDB();
        };
        new Thread(runnable).start();
        SharedPreferences firstTimePreference = PreferenceManager.getDefaultSharedPreferences(m_appContext);
        boolean firstTime = firstTimePreference.getBoolean(FIRST_PREFERENCE_VALUE, true);
        if (firstTime) {
            Utility.createDialog(m_appContext, "Health Reports", getString(R.string.first_time_health_report));
            SharedPreferences.Editor firstTimePreferenceEditor = firstTimePreference.edit();
            firstTimePreferenceEditor.putBoolean(FIRST_PREFERENCE_VALUE, false);
            firstTimePreferenceEditor.apply();
        }
        return rootView;
    }

    public void updateFragment1ListView() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.e("receiver", "Got message: " + message);
            ArrayList<HealthSummaryItem> healthSummaryItems = (ArrayList<HealthSummaryItem>) intent.getSerializableExtra("object");
            if (adapter != null) {
                Log.e("adapter", "is not null");
                Log.e("Count of items Adapter", "" + adapter.getCount());
                Log.e("Count in the object", "" + healthSummaryItems.size());
                adapter.clear();
                adapter.addAll(healthSummaryItems);
                adapter.notifyDataSetChanged();
            } else {
                Log.e("adapter", "is null");
                adapter = new HealthSummaryAdapter(context, R.layout.health_summary, healthSummaryItems);
                Log.e("Size :", "" + healthSummaryItems.size());
                Log.e("Item", healthSummaryItems.get(0).getRecommendation().toString());
                HealthSummaryItem item = healthSummaryItems.get(0);
                ListView healthView = getView().findViewById(R.id.rootList);
                healthView.setAdapter(adapter);
                adapter.clear();
                adapter.addAll(item);
                adapter.notifyDataSetChanged();
                hiddenReport.setVisibility(View.GONE);
                addHealth.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(m_appContext).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private ArrayList<HealthSummaryItem> createHealthList(ArrayList<DailyStatus> allStatus, Resources resource) {
        String[] groupList = resource.getStringArray(R.array.health_report_group);
        ArrayList<String> symptomList = HealthStatus.SYMPTOM_LIST;
        ArrayList<HealthSummaryItem> healthSummary = new ArrayList<>();

        for (DailyStatus status : allStatus) {
            String health = status.getHealthString();
            String dateString = Utility.getStringMDfromInt(status.getDate());
            String[] res = health.split(HealthStatus.DIVIDER);
            String groupSelected = res[1];
            String symptomSelected = res[0];

            HashSet<String> symptomNames = new HashSet<>();
            for (int i = 0; i < symptomList.size(); i++) {
                if (symptomSelected.charAt(i) == '1') {
                    symptomNames.add(symptomList.get(i));
                }
            }
            ArrayList<String> healthRecommendation = new ArrayList<>();
            if (symptomNames.contains(HealthStatus.HEALTH_SYMPTOM1) && (symptomNames.contains(HealthStatus.HEALTH_SYMPTOM2)
                    || symptomNames.contains(HealthStatus.HEALTH_SYMPTOM3))) {
                healthRecommendation.add(resource.getString(R.string.corona_symptom));
            } else if (symptomHelper(symptomNames)) {
                healthRecommendation.add(resource.getString(R.string.normal_symptoms));
            }
            ArrayList<String> reportedHealth = new ArrayList<>();
            for (int i = 0; i < groupList.length; i++) {
                if (groupSelected.charAt(i) == '1') {
                    String grp;
                    if (i == 1) {
                        String s = TextUtils.join(", ", symptomNames);
                        String resi = groupList[i].substring(0, groupList[i].indexOf("("));
                        grp = resi + s;
                    } else {
                        grp = groupList[i];
                        if (i == 0) {
                            healthRecommendation.add(resource.getString(R.string.healthy));
                        } else if (i == 2) {
                            healthRecommendation.add(resource.getString(R.string.roommate_sick));
                        } else {
                            healthRecommendation.add(resource.getString(R.string.random_sick));
                        }
                    }
                    reportedHealth.add(grp);
                }
            }
            String reportResult = TextUtils.join(", ", reportedHealth);
            reportResult += ".";
            String healthResult = TextUtils.join("\n", healthRecommendation);
            HealthSummaryItem hs = new HealthSummaryItem(dateString, reportResult, healthResult);
            healthSummary.add(hs);
        }
        return healthSummary;
    }

    private static boolean symptomHelper(HashSet<String> selectedSymptoms) {
        for (String symptom : HealthStatus.SYMPTOM_LIST) {
            if (selectedSymptoms.contains(symptom)) {
                return true;
            }
        }
        return false;
    }
}

