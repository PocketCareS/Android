package com.ub.pocketcares.home;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.ub.pocketcares.backend.BluetoothBeaconDatabaseHelper;
import com.ub.pocketcares.backend.DailyStatus;
import com.ub.pocketcares.backend.DailyStatusDatabaseHelper;
import com.ub.pocketcares.backend.HealthStatus;
import com.ub.pocketcares.R;
import com.ub.pocketcares.bluetoothBeacon.CloseContactDailyData;
import com.ub.pocketcares.network.HealthReportWorker;
import com.ub.pocketcares.network.ServerHelper;
import com.ub.pocketcares.survey.CheckboxAdapter;
import com.ub.pocketcares.survey.HealthData;
import com.ub.pocketcares.survey.HealthReportAdapter;
import com.ub.pocketcares.utility.PreferenceTags;
import com.ub.pocketcares.utility.Utility;

import org.json.JSONException;

import static android.content.Context.MODE_PRIVATE;

public class HomeTabFragment extends Fragment {
    private static Resources resources;
    private static AlertDialog healthReportDlg = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        resources = getResources();
        final View rootView = inflater.inflate(R.layout.home_fragment_view, container, false);
        TextView bluetoothSummary = rootView.findViewById(R.id.bluetooth_summary);
        TextView healthSummary = rootView.findViewById(R.id.health_summary);
        LinearLayout scrollLayout = rootView.findViewById(R.id.scroll_layout);
        createTipsView(scrollLayout, requireContext());
        if (HealthData.isHealthDailySubmitted(requireContext())) {
            healthSummary.setText("You have reported today's health status. Thank you.");
        } else {
            healthSummary.setText("You have not submitted today's health report yet. Please take a few seconds to submit it.");
        }
        InformationTask iT = new InformationTask(MainActivity.m_mainActivity, bluetoothSummary);
        iT.execute();
        return rootView;
    }

    private void createTipsView(LinearLayout scrollLayout, Context context) {

        ArrayList<Integer> asset2 = new ArrayList<>(Arrays.asList(R.drawable.hand_wash, R.drawable.distance, R.drawable.mask,
                R.drawable.cough, R.drawable.wash, R.drawable.stay_home, R.drawable.health_graph));
        ArrayList<String> description2 = new ArrayList<String>(Arrays.asList(context.getString(R.string.clean_hands),
                context.getString(R.string.distance),
                context.getString(R.string.mask), context.getString(R.string.sneeze),
                context.getString(R.string.clean), context.getString(R.string.stay_home),
                context.getString(R.string.monitor_health)));
        HomeItem homeItem = new HomeItem(asset2, description2);

        for (int i = 0; i < homeItem.getAssets().size(); i++) {
            CardView card = new CardView(context);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            cardParams.setMargins(Utility.dpToInt(10), Utility.dpToInt(10), Utility.dpToInt(10), Utility.dpToInt(10));
            card.setPadding(Utility.dpToInt(20), Utility.dpToInt(20), Utility.dpToInt(20), Utility.dpToInt(20));
            card.setCardBackgroundColor(Color.WHITE);
            card.setRadius(Utility.dpToInt(20));
            card.setLayoutParams(cardParams);
            if (homeItem.getDescriptions().get(i).equals(context.getString(R.string.distance))) {
                card.setOnClickListener(v -> {
                    NavController navController = Navigation.findNavController(MainActivity.m_mainActivity, R.id.nav_host_fragment);
                    navController.navigate(R.id.close_contacts);
                });
            } else if (homeItem.getDescriptions().get(i).equals(context.getString(R.string.monitor_health))) {
                card.setOnClickListener(v -> {
                    if (HealthData.isHealthDailySubmitted(context)) {
                        NavController navController = Navigation.findNavController(MainActivity.m_mainActivity, R.id.nav_host_fragment);
                        navController.navigate(R.id.health_summary_item);
                    } else {
                        createReportHealthDialog(MainActivity.m_mainActivity);
                    }
                });
            }
            LinearLayout cardLayout = new LinearLayout(context);
            cardLayout.setBackgroundColor(Color.WHITE);
            cardLayout.setOrientation(LinearLayout.VERTICAL);


            ImageView image = new ImageView(context);
            LinearLayout.LayoutParams pictureParams = new LinearLayout.LayoutParams(Utility.dpToInt(120), Utility.dpToInt(120));
            pictureParams.gravity = Gravity.CENTER;
            image.setPadding(Utility.dpToInt(15), Utility.dpToInt(15), Utility.dpToInt(15), Utility.dpToInt(15));
            image.setLayoutParams(pictureParams);
            image.setImageResource(homeItem.getAssets().get(i));

            TextView description = new TextView(context);
            description.setPadding(Utility.dpToInt(10), Utility.dpToInt(10), Utility.dpToInt(10), Utility.dpToInt(10));
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(Utility.dpToInt(200), ViewGroup.LayoutParams.WRAP_CONTENT);
            descriptionParams.setMargins(Utility.dpToInt(10), 0, Utility.dpToInt(10), 0);
            description.setLayoutParams(descriptionParams);
            description.setTextColor(Color.BLACK);
            description.setText(Html.fromHtml(homeItem.getDescriptions().get(i)));
            description.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            cardLayout.addView(image);
            cardLayout.addView(description);
            card.addView(cardLayout);
            scrollLayout.addView(card);
        }
    }

    private static void createResponseDialog(final MainActivity mActivity, String message, String title) {
        TextView body = new TextView(mActivity);
        body.setText(Html.fromHtml(message));
        body.setMovementMethod(LinkMovementMethod.getInstance());
        body.setPadding(50, 50, 50, 50);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.MyDialogTheme);
        final AlertDialog dialog = builder.setTitle(title).setView(body).setPositiveButton(android.R.string.ok, null).create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private static void displayResponse(HealthReportAdapter healthReport, final MainActivity mActivity) {
        HashSet<String> selectedSymptoms = healthReport.getSelectedSymptoms();
        String[] groupList = mActivity.getResources().getStringArray(R.array.health_report_group);
        String body = "";
        String title = resources.getString(R.string.response_feedback);
        if (selectedSymptoms.contains(HealthStatus.HEALTH_SYMPTOM1) && (selectedSymptoms.contains(HealthStatus.HEALTH_SYMPTOM2)
                || selectedSymptoms.contains(HealthStatus.HEALTH_SYMPTOM3))) {
            body = resources.getString(R.string.corona_symptom) + "<br>";
            title = resources.getString(R.string.corona_title);
        } else if (symptomHelper(selectedSymptoms)) {
            body = resources.getString(R.string.normal_symptoms) + "<br>";
        } else if (selectedSymptoms.contains(groupList[0])) {
            body = resources.getString(R.string.healthy) + "<br>";
        }
        if (selectedSymptoms.contains(groupList[2])) {
            body += resources.getString(R.string.roommate_sick) + "<br>";
        }
        if (selectedSymptoms.contains(groupList[3])) {
            body += resources.getString(R.string.random_sick);
        }
        createResponseDialog(mActivity, body, title);
    }

    private static boolean symptomHelper(HashSet<String> selectedSymptoms) {
        for (String symptom : HealthStatus.SYMPTOM_LIST) {
            if (selectedSymptoms.contains(symptom)) {
                return true;
            }
        }
        return false;
    }

    // This ensures that never only I have the following symptoms is
    // selected on its own without the actual symptoms.
    private static boolean edgeCase(HashSet<String> selectedSymptoms, String[] groups) {
        HashSet<String> copySymptoms = new HashSet<>(selectedSymptoms);
        // If group[0] (feeling fine) is not present then if we remove
        // symptoms 2 and 3 if only group[1] remains then that is invalid.
        if (!copySymptoms.contains(groups[0])) {
            copySymptoms.remove(groups[2]);
            copySymptoms.remove(groups[3]);
            return copySymptoms.size() == 1;
        }
        return false;
    }

    public static void createReportHealthDialog(final MainActivity mActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.MyDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.dialog_healthstatus, null);
        final String[] groupList = mActivity.getResources().getStringArray(R.array.health_report_group);

        ExpandableListView lv_questions = dialogView.findViewById(R.id.dialog_health_expandlist);
        final HealthReportAdapter healthAdp = new HealthReportAdapter(mActivity, lv_questions);
        lv_questions.setAdapter(healthAdp);

        Calendar today = Calendar.getInstance();
        String msg = "Any symptoms or observations today (" +
                CheckboxAdapter.FormatCalendarString(today) + ")?";
        if (HealthData.isHealthDailySubmitted(mActivity)) {
            msg = "Do you want to update your health report (" + CheckboxAdapter.FormatCalendarString(today) + ")?";
        }
        builder.setView(dialogView)
                .setTitle("Health Status")
                .setMessage(msg)
                .setPositiveButton("Submit", (dialog, which) -> {
                    if (healthAdp.isHealthReportEmpty(healthAdp.getHealthString()) || edgeCase(healthAdp.getSelectedSymptoms(), groupList)) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(mActivity, R.style.MyDialogTheme);
                        builder1.setMessage("Your health status report is empty or incorrect, return to fill it")
                                .setNeutralButton("Return", (dialog13, which13) -> healthReportDlg.show());
                        AlertDialog emptyDialoag = builder1.create();
                        Objects.requireNonNull(emptyDialoag.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        emptyDialoag.show();
                        return;
                    }
                    if (HealthData.isHealthDailySubmitted(mActivity)) {
                        AlertDialog.Builder ynbuilder = new AlertDialog.Builder(mActivity, R.style.MyDialogTheme);
                        ynbuilder.setMessage("You've already submitted your daily health information, are" +
                                " you sure you want to update it?")
                                .setPositiveButton("Yes", (dialog12, which12) -> {
                                    LogHealthStatus(mActivity, healthAdp.getHealthString());
                                    displayResponse(healthAdp, mActivity);
                                    HealthSummary healthSummary = (HealthSummary) mActivity.getSupportFragmentManager().findFragmentById(R.id.health_summary_item);
                                    if (healthSummary != null) {
                                        healthSummary.updateFragment1ListView();
                                    } else {
                                        System.out.println("" + null);
                                    }
                                })
                                .setNegativeButton("No", (dialog1, which1) -> {
                                });
                        AlertDialog confirm = ynbuilder.create();
                        Objects.requireNonNull(confirm.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        confirm.show();
                    } else {
                        LogHealthStatus(mActivity, healthAdp.getHealthString());

                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mActivity);
                        SharedPreferences.Editor ed = sp.edit();
                        ed.putInt(PreferenceTags.USAGE_NUM_HEALTHREPORT, sp.getInt(PreferenceTags.USAGE_NUM_HEALTHREPORT, 0) + 1);
                        ed.apply();
                        displayResponse(healthAdp, mActivity);
                    }
                })
                .setNegativeButton("Cancel", null);

        healthReportDlg = builder.create();
        Objects.requireNonNull(healthReportDlg.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        healthReportDlg.show();

    }

    public static void LogHealthStatus(Context context, String healthreport) {
        HealthStatus hs = new HealthStatus(Utility.getCurrentFormattedYMD());
        int comaidx = healthreport.indexOf(HealthStatus.DIVIDER);
        hs.setExtraField1(healthreport.substring(comaidx + 1));
        hs.setExtraField2(healthreport.substring(0, comaidx));
        // write this to latest health status for feature use, show symptom tip
        HealthData.writeLatestHealthStatus(context, hs.getExtraField1());
        // write this into daily database
        DailyStatusDatabaseHelper ddh = new DailyStatusDatabaseHelper(context);
        ddh.insertHealth(hs);
        ddh.closeDB();

        HealthData.setHealthDailySubmitted(context, true);
        SharedPreferences status = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor dataStatusEditor = status.edit();
        dataStatusEditor.putString(PreferenceTags.LAST_HEALTH_SUBMIT, Utility.changeDateToString2(Calendar.getInstance()));
        dataStatusEditor.apply();

        WorkRequest healthUploadWork = new OneTimeWorkRequest.Builder(HealthReportWorker.class)
                .build();
        WorkManager.getInstance(context)
                .enqueue(healthUploadWork);

        Log.e("sender", "Broadcasting message");
        Intent intent = new Intent("ui-update-health_summary");
        intent.putExtra("message", "This is my message!");
        intent.putExtra("object", createHealthList(ddh.getAllDailyStatus(), context.getResources()));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static ArrayList<HealthSummaryItem> createHealthList(ArrayList<DailyStatus> allStatus, Resources resource) {
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

    static class InformationTask extends AsyncTask<Void, Void, ArrayList<Integer>> {
        private WeakReference<MainActivity> activityReference;
        private WeakReference<TextView> bluetoothSummary;

        InformationTask(MainActivity context, TextView summary) {
            activityReference = new WeakReference<>(context);
            bluetoothSummary = new WeakReference<>(summary);
        }

        @Override
        protected ArrayList<Integer> doInBackground(Void... voids) {
            ArrayList<Integer> values = new ArrayList<>();
            SharedPreferences sharedPreferences = activityReference.get().getSharedPreferences("databaseDate", MODE_PRIVATE);
            String firstRunString = sharedPreferences.getString("firstRunDate", null);
            if (firstRunString == null) {
                BluetoothBeaconDatabaseHelper beaconDatabaseHelper = new BluetoothBeaconDatabaseHelper(activityReference.get());
                firstRunString = Utility.changeDateToString(beaconDatabaseHelper.databaseDateRange().first.getTime());
            }
            try {
                Calendar start = Calendar.getInstance();
                start.setTime(Utility.changeStringToDate(firstRunString));
                Calendar end = Calendar.getInstance();
                int days = (int) TimeUnit.MILLISECONDS.toDays(Math.abs(end.getTimeInMillis() - start.getTimeInMillis()));
                TreeMap<Long, CloseContactDailyData> dailyDataTreeMap = ServerHelper.getDailyAnalytics(firstRunString,
                        Utility.changeDateToString(end.getTime()), activityReference.get());
                int totalPeopleCount = 0;
                for (CloseContactDailyData data : dailyDataTreeMap.values()) {
                    totalPeopleCount += data.getDailyTotalCount();
                }
                values.add(totalPeopleCount);
                values.add(days);
            } catch (IOException e) {
                e.printStackTrace();
                values.add(-1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return values;
        }

        @Override
        protected void onPostExecute(ArrayList<Integer> value) {
            super.onPostExecute(value);
            int totalBluetoothCount;
            if (!value.isEmpty()) {
                totalBluetoothCount = value.get(0);
            } else {
                totalBluetoothCount = -1;
            }

            String peopleSummary;
            if (totalBluetoothCount == -1) {
                peopleSummary = "Error connecting to the server. Please check your internet connection and try again.";
            } else {
                int days = value.get(1);
                peopleSummary = getPeopleDescription(totalBluetoothCount, days);
            }
            bluetoothSummary.get().setText(peopleSummary);
        }

        private String getPeopleDescription(int totalPeopleCount, int days) {
            String peopleSummary;
            if (totalPeopleCount == 1) {
                if (days > 1) {
                    peopleSummary = String.format(Locale.US, "You have had close encounter %d time in the past %d days.", totalPeopleCount, days);
                } else {
                    peopleSummary = String.format(Locale.US, "You have had close encounter %d time in the past 24 hours.", totalPeopleCount);
                }
            } else if (totalPeopleCount > 1) {
                if (days > 1) {
                    peopleSummary = String.format(Locale.US, "You have had close encounter %d times in the past %d days.", totalPeopleCount, days);
                } else {
                    peopleSummary = String.format(Locale.US, "You have had close encounter %d times in the past 24 hours.", totalPeopleCount);
                }
            } else {
                peopleSummary = "No report on social distance is available yet. Check back later.";
            }
            return peopleSummary;
        }
    }
}
