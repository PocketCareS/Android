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

package com.ub.pocketcares.bluetoothBeacon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.highsoft.highcharts.common.HIColor;
import com.highsoft.highcharts.common.hichartsclasses.HIChart;
import com.highsoft.highcharts.common.hichartsclasses.HIColumn;
import com.highsoft.highcharts.common.hichartsclasses.HIEvents;
import com.highsoft.highcharts.common.hichartsclasses.HIExporting;
import com.highsoft.highcharts.common.hichartsclasses.HILoading;
import com.highsoft.highcharts.common.hichartsclasses.HIOptions;
import com.highsoft.highcharts.common.hichartsclasses.HIPlotOptions;
import com.highsoft.highcharts.common.hichartsclasses.HISeries;
import com.highsoft.highcharts.common.hichartsclasses.HITitle;
import com.highsoft.highcharts.common.hichartsclasses.HITooltip;
import com.highsoft.highcharts.common.hichartsclasses.HIXAxis;
import com.highsoft.highcharts.common.hichartsclasses.HIYAxis;
import com.highsoft.highcharts.core.HIChartView;
import com.highsoft.highcharts.core.HIFunction;
import com.ub.pocketcares.BuildConfig;
import com.ub.pocketcares.backend.BluetoothBeaconDatabaseHelper;
import com.ub.pocketcares.R;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.network.ServerHelper;
import com.ub.pocketcares.utility.Utility;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;

import static android.content.Context.MODE_PRIVATE;
import static com.ub.pocketcares.utility.LogTags.BLE_TAG;

public class CloseContactFragment extends Fragment {
    BroadcastReceiver uiChangeReceiver;
    public static final String ACTION_NOTIFY_BEACON_UI_UPDATE = "com.ub.pocketcares.beaconUI";
    private Context closeContactContext;
    private TextView emptyList;
    private static final String FIRST_PREFERENCE_VALUE = "firstTimeCloseEncounters";
    private static CloseContactDailyData dailyData;
    private static boolean isEncounterVisible;
    private CardView dailyCard;
    private HIChartView chartView;
    private static HorizontalCalendar horizontalCalendar;
    private static Calendar endDate;

    @Override
    public void onResume() {
        super.onResume();
        isEncounterVisible = true;
        scrollToCurrentDay();
        Log.v(BLE_TAG, "CLose encounter resumed");
    }

    @Override
    public void onStop() {
        super.onStop();
        isEncounterVisible = false;
        Log.v(BLE_TAG, "CLose encounter stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(uiChangeReceiver);
    }

    public static boolean getEncounterVisible() {
        return isEncounterVisible;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.v(BLE_TAG, "CLose encounter create");
        final View rootView = inflater.inflate(R.layout.close_contact_fragment, container, false);
        MonitoringApplication application = (MonitoringApplication) getActivity().getApplicationContext();
        closeContactContext = getContext();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) closeContactContext.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean locationEnabled = isGpsEnabled || isNetworkEnabled;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission(application, locationEnabled, bluetoothAdapter,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermission(application, locationEnabled, bluetoothAdapter,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }

        emptyList = rootView.findViewById(R.id.emptyContacts);
        dailyCard = rootView.findViewById(R.id.dailyCard);
        chartView = rootView.findViewById(R.id.barGraph);
        chartView.setWillNotDraw(true);
        dailyData = new CloseContactDailyData();


        ImageView contactInfo = rootView.findViewById(R.id.contactInfo);
        contactInfo.setOnClickListener(v -> Utility.createDialog(closeContactContext, getString(R.string.close_contacts), getString(R.string.closeContactInfo)));

        BluetoothBeaconDatabaseHelper beaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(closeContactContext);
        SharedPreferences sharedPreferences = closeContactContext.getSharedPreferences("databaseDate", MODE_PRIVATE);
        String firstRunString = sharedPreferences.getString("firstRunDate", null);
        Calendar startDate;
        if (firstRunString != null) {
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTime(Utility.changeStringToDate(firstRunString));
            startDate = startCalendar;
        } else {
            Pair<Calendar, Calendar> dateRange = beaconDatabaseHelper.databaseDateRange();
            startDate = dateRange.first;
        }
        endDate = getCurrentDateInCalendarFormat();
        Runnable init = () -> {
            makeDailyData(beaconDatabaseHelper.getDisplayContacts(Utility.changeDateToString2((endDate))));
            try {
                makeDailyData(ServerHelper.getDailyAnalytics(Utility.changeDateToString2(endDate), closeContactContext));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            MainActivity.m_mainActivity.runOnUiThread(() -> updateView(emptyList, dailyCard, chartView, true)
            );
        };
        new Thread(init).start();
        horizontalCalendar = new HorizontalCalendar.Builder(rootView, R.id.calendarView).range(startDate, endDate)
                .datesNumberOnScreen(5)
                .defaultSelectedDate(endDate)
                .build();
        horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar date, int position) {
                isEncounterVisible = date.equals(getCurrentDateInCalendarFormat());
                dailyData = new CloseContactDailyData();
                PopulateListTask populateListTask = new PopulateListTask(closeContactContext, emptyList, dailyCard, chartView, false);
                populateListTask.execute(Utility.changeDateToString2((date)));
            }

        });

        SharedPreferences firstTimePreference = PreferenceManager.getDefaultSharedPreferences(closeContactContext);
        boolean firstTime = firstTimePreference.getBoolean(FIRST_PREFERENCE_VALUE, true);
        if (firstTime) {
            Utility.createDialog(closeContactContext, "Close Encounters", getString(R.string.first_time_close_encounters));
            SharedPreferences.Editor firstTimePreferenceEditor = firstTimePreference.edit();
            firstTimePreferenceEditor.putBoolean(FIRST_PREFERENCE_VALUE, false);
            firstTimePreferenceEditor.apply();
        }
        registerUIChangeReceiver();
        return rootView;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermission(MonitoringApplication application, boolean locationEnabled,
                                   BluetoothAdapter bluetoothAdapter, final String[] perms) {
        boolean condition = false;
        boolean showRationale = false;
        for (String permission : perms) {
            condition = closeContactContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            showRationale = this.shouldShowRequestPermissionRationale(permission);
        }
        if (condition) {
            // Perfect
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                    intent.putExtra("dialog", "bluetooth");
                    closeContactContext.sendBroadcast(intent);
                } else if (!locationEnabled) {
                    Intent intent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                    intent.putExtra("dialog", "location");
                    closeContactContext.sendBroadcast(intent);
                }
            }
        } else {
            application.stopTransmissionScan(false);
            if (showRationale) {
                DialogInterface.OnDismissListener dismissListener = dialog -> requestPermissions(perms, MainActivity.LOCATION_PERMISSION_CODE);
                Utility.createDialog(closeContactContext, "This app needs background location access",
                        "Please grant location access so this app can detect beacons even in the background.",
                        null, dismissListener);
            } else {
                Intent intent = new Intent(MainActivity.BLUETOOTH_DIALOG);
                intent.putExtra("dialog", "location_permission");
                closeContactContext.sendBroadcast(intent);
            }
        }
    }

    private static Calendar getCurrentDateInCalendarFormat() {
        Calendar currentDate = Calendar.getInstance();
        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        currentDate.set(Calendar.MILLISECOND, 0);
        return currentDate;
    }

    private static void scrollToCurrentDay() {
        if (horizontalCalendar != null && endDate != null) {
            Log.v("Scroll", "Selected Day: " + horizontalCalendar.getSelectedDate().getTime().toString());
            if (horizontalCalendar.getSelectedDate() != endDate) {
                Log.v("Scroll", "Scrolled");
                horizontalCalendar.selectDate(endDate, false);
            }
        }
    }

    private static void createChart(HIChartView chartView) {
        HIOptions options = new HIOptions();
        HIExporting exporting = new HIExporting();
        exporting.setEnabled(false);
        options.setExporting(exporting);
        HIChart chart = new HIChart();
        chart.setType("column");
        options.setChart(chart);
        HITitle title = new HITitle();
        title.setText("Hourly Close Encounters");
        options.setTitle(title);

        final HIXAxis xAxis = new HIXAxis();
        Pair<ArrayList<String>, ArrayList<HashMap<String, Object>>> chartData = generateChartData();

        xAxis.setCategories(chartData.first);
        options.setXAxis(new ArrayList<HIXAxis>() {{
            add(xAxis);
        }});

        final HIYAxis yAxis = new HIYAxis();
        yAxis.setMin(0);
        yAxis.setAllowDecimals(false);
        yAxis.setTitle(new HITitle());
        yAxis.getTitle().setText("Close Encounters");
        options.setYAxis(new ArrayList<HIYAxis>() {{
            add(yAxis);
        }});

        HITooltip tooltip = new HITooltip();
        tooltip.setHeaderFormat("<span style=\"font-size:10px\">{point.key}</span><table>");
        tooltip.setPointFormat("<tr><td style=\"color:{series.color};padding:0\">{series.name}: </td><td style=\"padding:0\"><b>{point.y:.0f}</b></td></tr> <tr><td style=\"color:{series.color};padding:0\">Total Duration: </td><td style=\"padding:0\"><b>{point.duration} minutes</b></td></tr>");
        tooltip.setFooterFormat("</table>");
        tooltip.setShared(true);
        tooltip.setBorderRadius(10);
        tooltip.setUseHTML(true);
        tooltip.setHideDelay(0);
        options.setTooltip(tooltip);


        HIPlotOptions plotOptions = new HIPlotOptions();
        plotOptions.setColumn(new HIColumn());
        plotOptions.getColumn().setPointPadding(0.2);
        plotOptions.getColumn().setBorderWidth(0);
        plotOptions.getColumn().setBorderRadius(4);
        options.setPlotOptions(plotOptions);


        HIColumn series1 = new HIColumn();
        series1.setShowInLegend(false);
        series1.setName("Close Encounters");
        series1.setColor(HIColor.initWithHexValue("005bbb"));
        series1.setData(chartData.second);
        ArrayList<HISeries> series = new ArrayList<>();
        series.add(series1);
        options.setSeries(series);
        chartView.setOptions(options);
        chartView.setWillNotDraw(false);
    }

    private static void updateChart(HIChartView chartView) {
        if (chartView.getOptions() != null) {
            Pair<ArrayList<String>, ArrayList<HashMap<String, Object>>> chartData = generateChartData();
            chartView.getOptions().getXAxis().get(0).setCategories(chartData.first);
            chartView.getOptions().getSeries().get(0).setData(chartData.second);
        }
    }

    private static Pair<ArrayList<String>, ArrayList<HashMap<String, Object>>> generateChartData() {
        ArrayList<String> categoriesList = new ArrayList<>();
        ArrayList<HashMap<String, Object>> encounters = new ArrayList<>();
        for (long key : dailyData.getHourlyData().keySet()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh a", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(key);
            categoriesList.add(dateFormat.format(c.getTime()));
            HashMap<String, Object> dataPair = new HashMap<>();
            dataPair.put("y", dailyData.getHourlyData().get(key).getCloseContactCount());
            dataPair.put("duration", dailyData.getHourlyData().get(key).getCloseContactDuration());
            encounters.add(dataPair);
        }
        return new Pair<>(categoriesList, encounters);
    }

    private static void makeDailyData(CloseContactDailyData serverData) {
        if (!serverData.getHourlyData().isEmpty()) {
            Log.v(BLE_TAG, "Server Data: " + serverData.toString());
            long latestServerHour = serverData.getHourlyData().descendingKeySet().iterator().next();
            TreeMap<Long, CloseContactHourlyData> localDataCopy = dailyData.getHourlyData();
            dailyData = new CloseContactDailyData(serverData.getHourlyData(), serverData.getDailyTotalDuration(), serverData.getDailyTotalCount());
            if (!localDataCopy.isEmpty()) {
                int dailyEncounters = serverData.getDailyTotalCount();
                int dailyEncountersDuration = serverData.getDailyTotalDuration();
                for (long time : localDataCopy.keySet()) {
                    if (time > latestServerHour) {
                        dailyData.getHourlyData().put(time, localDataCopy.get(time));
                        dailyEncounters += localDataCopy.get(time).getCloseContactCount();
                        dailyEncountersDuration += localDataCopy.get(time).getCloseContactDuration();
                    }
                }
                dailyData.setTotalCount(dailyEncounters);
                dailyData.setDuration(dailyEncountersDuration);
            }
            if (BuildConfig.DEBUG) {
                MainActivity.m_mainActivity.runOnUiThread(() -> {
                            Toast.makeText(MainActivity.m_mainActivity, "Data updated using Server", Toast.LENGTH_SHORT).show();
                        }
                );
            }
        }
    }

    private static void makeDailyData(TreeMap<Long, HashSet<BeaconStat>> data) {
        Set<Long> hours = data.keySet();
        if (dailyData.getHourlyData().isEmpty()) {
            dailyData.setDuration(0);
            dailyData.setTotalCount(0);
            for (Long time : hours) {
                localDataHelper(data, time);
            }
        } else {
            long dailyLatestTime = dailyData.getHourlyData().descendingKeySet().iterator().next();
            for (Long time : hours) {
                if (time >= dailyLatestTime) {
                    localDataHelper(data, time);
                }
            }
        }
    }

    private static void localDataHelper(TreeMap<Long, HashSet<BeaconStat>> data, long time) {
        HashSet<BeaconStat> encounterBeacon = data.get(time);
        CloseContactHourlyData hourlyData = new CloseContactHourlyData(0, 0);
        TreeMap<Long, CloseContactHourlyData> currentHourlyData = dailyData.getHourlyData();
        if (currentHourlyData.containsKey(time)) {
            dailyData.setTotalCount(dailyData.getDailyTotalCount() - currentHourlyData.get(time).getCloseContactCount());
            dailyData.setDuration(dailyData.getDailyTotalDuration() - currentHourlyData.get(time).getCloseContactDuration());
        }
        for (BeaconStat beacon : encounterBeacon) {
            hourlyData.setDuration(hourlyData.getCloseContactDuration() + beacon.getTwoSessionDurationMinutes());
            hourlyData.setNumberOfContacts(hourlyData.getCloseContactCount() + beacon.getTwoCount());
        }
        dailyData.getHourlyData().put(time, hourlyData);
        dailyData.setDuration(dailyData.getDailyTotalDuration() + hourlyData.getCloseContactDuration());
        dailyData.setTotalCount(dailyData.getDailyTotalCount() + hourlyData.getCloseContactCount());
    }


    private static void updateView(TextView emptyList, CardView dailyCard, HIChartView chartView, boolean initial) {
        if (dailyData.getHourlyData().isEmpty()) {
            emptyList.setVisibility(View.VISIBLE);
            chartView.setVisibility(View.GONE);
            dailyCard.setVisibility(View.GONE);
        } else {
            LinearLayout cardLayout = (LinearLayout) dailyCard.getChildAt(0);
            LinearLayout countViewLayout = (LinearLayout) cardLayout.getChildAt(1);
            TextView countView = (TextView) countViewLayout.getChildAt(1);
            LinearLayout durationViewLayout = (LinearLayout) cardLayout.getChildAt(2);
            TextView durationView = (TextView) durationViewLayout.getChildAt(1);
            countView.setText(Integer.toString(dailyData.getDailyTotalCount()));
            int duration = dailyData.getDailyTotalDuration();
            durationView.setText(getDurationUiText(duration));
            chartView.setVisibility(View.VISIBLE);
            dailyCard.setVisibility(View.VISIBLE);
            emptyList.setVisibility(View.GONE);
        }
        if (initial) {
            createChart(chartView);
        } else {
            updateChart(chartView);
        }
    }

    private void registerUIChangeReceiver() {
        try {
            uiChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.v(BLE_TAG, "Got UI Broadcast");
                    PopulateListTask populateListTask = new PopulateListTask(closeContactContext, emptyList, dailyCard, chartView, true);
                    populateListTask.execute(Utility.changeDateToString2((endDate)));
                }
            };
            getActivity().registerReceiver(uiChangeReceiver, new IntentFilter(ACTION_NOTIFY_BEACON_UI_UPDATE));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static String getDurationUiText(int duration) {
        String hourEnglish = "hours";
        String minuteEnglish = "minutes";
        String durationText = duration + " " + minuteEnglish;
        if (duration > 59) {
            int hours = duration / 60;
            int minutes = duration % 60;
            if (hours == 1) {
                hourEnglish = "hour";
            }
            if (minutes == 1) {
                minuteEnglish = "minute";
            }
            if (minutes != 0) {
                durationText = String.format(Locale.getDefault(), "%d %s %02d %s", hours, hourEnglish,
                        minutes, minuteEnglish);
            } else {
                durationText = String.format(Locale.getDefault(), "%d %s", hours, hourEnglish);
            }
        }
        return durationText;
    }

    static class PopulateListTask extends AsyncTask<String, Void, Void> {
        private WeakReference<Context> context;
        private WeakReference<TextView> emptyList;
        private WeakReference<CardView> dailyCard;
        private WeakReference<HIChartView> chartView;
        private WeakReference<Boolean> minuteUiUpdate;

        PopulateListTask(Context context, TextView emptyList, CardView dailyCard, HIChartView chartView, boolean minuteUpdate) {
            this.context = new WeakReference<>(context);
            this.emptyList = new WeakReference<>(emptyList);
            this.dailyCard = new WeakReference<>(dailyCard);
            this.chartView = new WeakReference<>(chartView);
            this.minuteUiUpdate = new WeakReference<>(minuteUpdate);
        }

        @Override
        protected Void doInBackground(String... strings) {
            String date = strings[0];
            CloseContactDailyData serverDailyData = new CloseContactDailyData();
            if (!minuteUiUpdate.get()) {
                try {
                    serverDailyData = ServerHelper.getDailyAnalytics(date, context.get());
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            BluetoothBeaconDatabaseHelper beaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(context.get());
            makeDailyData(beaconDatabaseHelper.getDisplayContacts(date));
            makeDailyData(serverDailyData);
            return null;
        }

        @Override
        protected void onPostExecute(Void displayData) {
            super.onPostExecute(displayData);
            updateView(emptyList.get(), dailyCard.get(), chartView.get(), false);
        }
    }

}
