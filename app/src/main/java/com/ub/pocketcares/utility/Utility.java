package com.ub.pocketcares.utility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.ub.pocketcares.R;
import com.ub.pocketcares.bluetoothBeacon.MonitoringApplication;
import com.ub.pocketcares.home.MainActivity;
import com.ub.pocketcares.settings.SettingTabFragment;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Pair;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.NotificationCompat;

public class Utility {

    public static final int THRESHOLD_WRITEDB = 20;  // 50 MB limit

    public static final int HTML_BOLD = 0x000001;
    public static final int HTML_ITALIC = 0x000002;

    public static final String STR_JAN = "January";
    public static final String STR_FEB = "February";
    public static final String STR_MAR = "March";
    public static final String STR_APR = "April";
    public static final String STR_MAY = "May";
    public static final String STR_JUN = "June";
    public static final String STR_JUL = "July";
    public static final String STR_AUG = "August";
    public static final String STR_SEP = "September";
    public static final String STR_OCT = "October";
    public static final String STR_NOV = "November";
    public static final String STR_DEC = "December";

    private static final int MINUTE_VALUE = 59;
    public static final long DAY_IN_MILLS = 86400000L;
    public static final int NOTIFICATION_ID = 10001;
    public static final int PERMISSIONS_ID = 10009;

    //Calibration Values
    public static final int DEFAULT_CALIBRATION_ANDROID_X = 63;
    public static final int DEFAULT_CALIBRATION_ANDROID_Y = 35;
    public static final int DEFAULT_CALIBRATION_IOS_X = 49;
    public static final int DEFAULT_CALIBRATION_IOS_Y = 35;

    // If no snooze is set and notifications turned on then we can show notification to the user
    public static boolean canShowNotification(Context context) {
        SharedPreferences notificationPreference = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        long notificationSnoozeValue = notificationPreference.getLong("reminderAlarmValue", -1);
        Log.v("Preference", "Snooze Value: " + notificationSnoozeValue);
        return notificationPreference.getBoolean("close_encounter_notification_status", true) && notificationSnoozeValue < 0;
    }

    public static int getMinutesToNextHour(Calendar calendar) {
        int minuteDifference = MINUTE_VALUE - calendar.get(Calendar.MINUTE);
        if (minuteDifference == 0) {
            return MINUTE_VALUE + 1;
        } else {
            return minuteDifference;
        }
    }

    public static Calendar getCalenderForHour(int hour) {
        Calendar currentCalendar = Calendar.getInstance();
        Calendar calenderForHour = Calendar.getInstance();
        calenderForHour.set(Calendar.HOUR_OF_DAY, hour);
        calenderForHour.set(Calendar.MINUTE, 2);
        calenderForHour.set(Calendar.SECOND, 0);
        calenderForHour.set(Calendar.MILLISECOND, 0);
        if (currentCalendar.compareTo(calenderForHour) > 0) {
            calenderForHour.add(Calendar.DATE, 1);
        }
        return calenderForHour;
    }

    // check if internal storage has over 50MB space
    @SuppressWarnings("deprecation")
    public static boolean IsAvailableInternalStorage(int threshold) {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        int megAvailable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            megAvailable = (int) (stat.getAvailableBytes() / (1024 * 1024));
        } else {
            megAvailable = (int) ((long) stat.getBlockSize() * (long) stat.getAvailableBlocks() / (1024 * 1024));
        }
        Log.d(LogTags.TEST, "SDK: " + Build.VERSION.SDK_INT + " | internal storage left: " + megAvailable + "MB");
        if (megAvailable > threshold)
            return true;
        else
            return false;
    }

    public static int getVersionCode(Context con) {
        int vernum = 0;
        try {
            PackageInfo info = con.getPackageManager().getPackageInfo(con.getPackageName(), 0);
            vernum = info.versionCode;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return vernum;
    }

    public static String getVersionName(Context con) {
        String verName = null;
        try {
            PackageInfo info = con.getPackageManager().getPackageInfo(con.getPackageName(), 0);
            verName = info.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

//	public static int getAvailableNumOfTuplesInternalStoreage ( int tupleSizeByte ) {
//		StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
//		long bytesAvailable = stat.getAvailableBytes();
//		return (int) (bytesAvailable * WRITEFILE_PERCENTAGE_THRESHOLD / tupleSizeByte);
//	}

    public static JSONObject Map2Json(Map<Integer, Double> map) {
        JSONObject json = new JSONObject();
        Iterator<Entry<Integer, Double>> it = map.entrySet().iterator();
        try {
            while (it.hasNext()) {
                Entry<Integer, Double> pair = it.next();
                json.put(String.valueOf(pair.getKey()), pair.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Map<Integer, Double> Json2Map(JSONObject json) {
        Map<Integer, Double> map = new HashMap<Integer, Double>();
        Iterator<String> it = json.keys();
        try {
            while (it.hasNext()) {
                String key = it.next();
                map.put(Integer.parseInt(key), json.getDouble(key));
            }
        } catch (NumberFormatException | JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static Calendar getHealthReminderCalendar(int minutesAfterMidnight) {
        Calendar alarmCalender = Calendar.getInstance();
        alarmCalender.set(Calendar.HOUR_OF_DAY, 0);
        alarmCalender.set(Calendar.MINUTE, 0);
        alarmCalender.set(Calendar.SECOND, 0);
        alarmCalender.set(Calendar.MILLISECOND, 0);
        alarmCalender.add(Calendar.MINUTE, minutesAfterMidnight);
        int minutes = alarmCalender.get(Calendar.MINUTE);
        alarmCalender = getCalenderForHour(alarmCalender.get(Calendar.HOUR_OF_DAY));
        alarmCalender.set(Calendar.MINUTE, minutes);
        return alarmCalender;
    }

    public static long convertDayMills(long currentMills) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(currentMills);
        day.set(Calendar.HOUR_OF_DAY, 12);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }

    public static long convertHourMills(long currentMills) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMills);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static void createReminderNotification(Context context, String title, String description, int code) {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(Html.fromHtml("<b>" + title + "</b>"));
        bigTextStyle.bigText(Html.fromHtml("<br>" + description));
        Intent defaultIntent = new Intent(MonitoringApplication.NOTIFICATION_RECEIVE_INTENT);
        defaultIntent.putExtra("snoozeValue", MonitoringApplication.DEFAULT_REMINDER_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 9, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent snoozeHour = new Intent(MonitoringApplication.NOTIFICATION_RECEIVE_INTENT);
        snoozeHour.putExtra("snoozeValue", MonitoringApplication.SNOOZE_HOUR_ACTION);
        PendingIntent pendingHour = PendingIntent.getBroadcast(
                context, 10, snoozeHour, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent snoozeDay = new Intent(MonitoringApplication.NOTIFICATION_RECEIVE_INTENT);
        snoozeDay.putExtra("snoozeValue", MonitoringApplication.SNOOZE_DAY_ACTION);
        PendingIntent pendingDay = PendingIntent.getBroadcast(
                context, 11, snoozeDay, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.pocketcares_notification_icon).setColor(context.getResources().getColor(R.color.ub))
                .setStyle(bigTextStyle).setAutoCancel(true).setContentTitle(Html.fromHtml("<b>" + title + "</b>"))
                .setContentText(Html.fromHtml("<br>" + description)).addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze for an hour", pendingHour)
                .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze for a day", pendingDay);
        notificationBuilder.setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(code, notificationBuilder.build());
    }

    public static void createHighNotification(Context context, String title, String description, Intent intent,
                                              int notificationID, int pendingRequestCode) {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(Html.fromHtml("<b>" + title + "</b>"));
        bigTextStyle.bigText(Html.fromHtml("<br>" + description));
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.pocketcares_notification_icon).setColor(context.getResources().getColor(R.color.ub))
                .setStyle(bigTextStyle).setContentTitle(title);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, pendingRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        notificationBuilder.setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationID, notificationBuilder.build());
    }

    public static void updateNotification(Context context, String title, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, getNotification(context, title, intent).build());
    }

    public static NotificationCompat.Builder getNotification(Context context, String title, Intent intent) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.pocketcares_notification_icon).setColor(context.getResources().getColor(R.color.ub)).setContentTitle(title);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        notificationBuilder.setContentIntent(pendingIntent);
        return notificationBuilder;
    }

    public static void updateNotification(NotificationCompat.Builder builder, String updateTitle, Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder.setContentTitle(updateTitle);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void launchInAppLink(int color, Context context, String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(color);
        builder.setShowTitle(true);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    public static SpannableString getSpannableHyperlink(String content, ClickableSpan clickableSpan, int start, int end) {
        SpannableString spannableString = new SpannableString(content);
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public static void createDialog(Context context, String title, String message,
                                    DialogInterface.OnClickListener positiveListener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.MyDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", positiveListener);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    public static void createDialog(Context context, String title, String message,
                                    DialogInterface.OnClickListener positiveListener, DialogInterface.OnDismissListener dismiss) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.MyDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", positiveListener);
        alertDialog.setOnDismissListener(dismiss);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    public static void createDialog(Context context, String title, String message,
                                    DialogInterface.OnClickListener positiveListener,
                                    DialogInterface.OnClickListener negativeListener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.MyDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", positiveListener);
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", negativeListener);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    public static void createDialog(Context context, String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.MyDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();
    }

    public static int dpToInt(int value) {
        return (int) (value * Resources.getSystem().getDisplayMetrics().density);
    }

    public static String getDateFromUnix(long timeStamp) {
        return new SimpleDateFormat("MM/dd, h:mm a", Locale.US).format(new Date(timeStamp));
    }

    public static String getTimeFromUnix(long timeStamp, boolean seconds) {
        if (seconds) {
            return new SimpleDateFormat("h:mm:ss a", Locale.US).format(new Date(timeStamp));
        } else {
            return new SimpleDateFormat("h:mm a", Locale.US).format(new Date(timeStamp));
        }
    }

    public static long getUnixFromString(String time) {
        try {
            return Objects.requireNonNull(new SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(time)).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean fourteenDayCheck(Context context, int cut_off) {
        SharedPreferences sp = context.getSharedPreferences("databaseDate", Context.MODE_PRIVATE);
        String startDate = sp.getString("fourteenPeriod", "");
        Date start = Utility.changeStringToDate(startDate);
        assert start != null;
        Date current = Calendar.getInstance().getTime();
        long difference = current.getTime() - start.getTime();
        int dateDiff = (int) (difference / (1000 * 60 * 60 * 24));
        if (dateDiff > cut_off) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            cal.add(Calendar.DATE, 1);
            Date newDate = cal.getTime();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("fourteenPeriod", Utility.changeDateToString(newDate));
            editor.putLong("deleteValue", start.getTime());
            editor.apply();
            return true;
        }
        return false;
    }

    public static String changeDateToString(Date d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS", Locale.getDefault());
        return dateFormat.format(d);
    }

    public static String changeDateToString2(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    public static Date changeStringToDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS", Locale.getDefault());
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFormattedCalendar(Calendar c, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(c.getTime());
    }

    public static String getCurrentFormattedYMDHMS() {
        String tmformat = "yyyyMMdd-HHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(tmformat);
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    public static String getCurrentFormattedYMD() {
        String tmformat = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(tmformat);
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    public static String getFormattedYMD(Calendar c) {
        String tmformat = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(tmformat);
        return sdf.format(c.getTime());
    }

    public static String getFormattedYMDHMS(Calendar c) {
        String tmformat = "yyyyMMdd-HHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(tmformat);
        return sdf.format(c.getTime());
    }

    public static String getFullFormattedYMD(Calendar c, Context con) {
        return DateUtils.formatDateTime(con, c.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_YEAR);
    }

    public static Calendar getCalendarFromStringYMD(String ymd) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date d = null;
        try {
            d = sdf.parse(ymd);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // invalid format
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    public static int getCalendarInt(Calendar c) {
        int date = 0;
        date += c.get(Calendar.YEAR);
        date = date * 100 + c.get(Calendar.MONTH) + 1;
        date = date * 100 + c.get(Calendar.DAY_OF_MONTH);
        return date;
    }

    public static int getCalendarIntYM(Calendar c) {
        int date = 0;
        date += c.get(Calendar.YEAR);
        date = date * 100 + c.get(Calendar.MONTH) + 1;
        return date;
    }

    public static String getStringfromCalendarIntYM(Integer date) {
        try {
            String d = String.valueOf(date);
            return d.substring(0, 4) + ", " + getMonthString(Integer.parseInt(d.substring(4)) - 1);
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(LogTags.UTILITY, "date int to convert: " + date);
            Log.e(LogTags.UTILITY, "error msg: " + e.toString());
            return "illegal string format";
        }
    }

    public static String getStringMDfromInt(Integer date) {
        try {
            String d = String.valueOf(date);
            String year = d.substring(0, 4);
            String month = d.substring(4, 6);
            String day = d.substring(6, 8);
            return getMonthString(Integer.parseInt(month) - 1) + " " + day;
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(LogTags.UTILITY, "date int to convert: " + date);
            Log.e(LogTags.UTILITY, "error msg: " + e.toString());
            return "illegal string format";
        }
    }

    public static Integer getDownloadFileTime(String filename) {
        try {
            int dotidx = filename.lastIndexOf(".");
            filename = filename.substring(0, dotidx);
            Log.e(LogTags.UTILITY, filename);
            int ulidx = filename.lastIndexOf("_");
            Log.e(LogTags.UTILITY, filename.substring(ulidx + 1));
            return Integer.parseInt(filename.substring(ulidx + 1));
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(LogTags.UTILITY, "download file name: " + filename);
            Log.e(LogTags.UTILITY, "error msg: " + e.toString());
            return null;
        }
    }

    public static String getTimeZone(Calendar c) {
        Date cur = c.getTime();
        DateFormat tz = new SimpleDateFormat("Z");
        return tz.format(cur);
    }

    public static String getSimpleCalendarString(Calendar c) {
        return String.format("%1$tF %1$tT", c);
    }

    // http://developer.android.com/reference/java/util/Formatter.html
    public static String getFullAbbrCalendarString(Calendar c) {
        return String.format("%1$tA, %1$tb %1$td %1$tY", c);
    }

    // no year
    public static String getAbbrCalendarString(Calendar c) {
        return String.format("%1$tA, %1$tb %1$td", c);
    }

    public static String getFullString(Calendar c) {
        return String.format("%1$tA %1$tb %1$td %1$tY at %1$tI:%1$tM %1$Tp", c);
    }

    public static String formatHtmlSmall(String str) {
        return "<small>" + str + "</small>";
    }

    public static String formatHtml(String str, int flag) {
        if ((flag & HTML_BOLD) == HTML_BOLD)
            str = "<b>" + str + "</b>";
        if ((flag & HTML_ITALIC) == HTML_ITALIC)
            str = "<i>" + str + "</i>";
        return str;
    }

    public static String formatHtmlColor(String str, int color) {
        String strcolor = String.format("#%06X", 0xFFFFFF & color);
        str = "<font color=\"" + strcolor + "\">" + str + "</font>";
        return str;
    }


    public static String getMonthString(int mint) {
        switch (mint) {
            case Calendar.JANUARY:
                return STR_JAN;
            case Calendar.FEBRUARY:
                return STR_FEB;
            case Calendar.MARCH:
                return STR_MAR;
            case Calendar.APRIL:
                return STR_APR;
            case Calendar.MAY:
                return STR_MAY;
            case Calendar.JUNE:
                return STR_JUN;
            case Calendar.JULY:
                return STR_JUL;
            case Calendar.AUGUST:
                return STR_AUG;
            case Calendar.SEPTEMBER:
                return STR_SEP;
            case Calendar.OCTOBER:
                return STR_OCT;
            case Calendar.NOVEMBER:
                return STR_NOV;
            case Calendar.DECEMBER:
                return STR_DEC;
            default:
                return STR_JAN;
        }
    }

    public static int getMonthInt(String smonth) {
        if (smonth.equals(STR_JAN))
            return Calendar.JANUARY;
        else if (smonth.equals(STR_FEB))
            return Calendar.FEBRUARY;
        else if (smonth.equals(STR_MAR))
            return Calendar.MARCH;
        else if (smonth.equals(STR_APR))
            return Calendar.APRIL;
        else if (smonth.equals(STR_MAY))
            return Calendar.MAY;
        else if (smonth.equals(STR_JUN))
            return Calendar.JUNE;
        else if (smonth.equals(STR_JUL))
            return Calendar.JULY;
        else if (smonth.equals(STR_AUG))
            return Calendar.AUGUST;
        else if (smonth.equals(STR_SEP))
            return Calendar.SEPTEMBER;
        else if (smonth.equals(STR_OCT))
            return Calendar.OCTOBER;
        else if (smonth.equals(STR_NOV))
            return Calendar.NOVEMBER;
        else if (smonth.equals(STR_DEC))
            return Calendar.DECEMBER;
        else
            return Calendar.JANUARY;
    }

    public static int[] shuffleArray(int length) {
        int[] intarray = new int[length];
        for (int i = 0; i < length; i++)
            intarray[i] = i;

        Random rnd = new Random();
        for (int i = intarray.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int a = intarray[index];
            intarray[index] = intarray[i];
            intarray[i] = a;
        }
        return intarray;
    }

    public static boolean isAutoTimeEnabled(Context con) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // For JB+
            return Settings.Global.getInt(con.getContentResolver(), Settings.Global.AUTO_TIME, 0) > 0;
        }
        // For older Android versions
        return Settings.System.getInt(con.getContentResolver(), Settings.System.AUTO_TIME, 0) > 0;
    }

    public static void setAutoTimeEnabled(boolean b, Context con) {
        Settings.System.putInt(con.getContentResolver(), Settings.System.AUTO_TIME, 1);
    }

    /**
     * Returns a CharSequence that applies boldface to the concatenation
     * of the specified CharSequence objects.
     */
    public static CharSequence bold(CharSequence... content) {
        return apply(content, new StyleSpan(Typeface.BOLD));
    }

    /**
     * Returns a CharSequence that applies italics to the concatenation
     * of the specified CharSequence objects.
     */
    public static CharSequence italic(CharSequence... content) {
        return apply(content, new StyleSpan(Typeface.ITALIC));
    }

    /**
     * Returns a CharSequence that applies a foreground color to the
     * concatenation of the specified CharSequence objects.
     */
    public static CharSequence color(int color, CharSequence... content) {
        return apply(content, new ForegroundColorSpan(color));
    }

    /**
     * Returns a CharSequence that concatenates the specified array of CharSequence
     * objects and then applies a list of zero or more tags to the entire range.
     *
     * @param content an array of character sequences to apply a style to
     * @param tags    the styled span objects to apply to the content
     *                such as android.text.style.StyleSpan
     */
    private static CharSequence apply(CharSequence[] content, Object... tags) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        openTags(text, tags);
        for (CharSequence item : content) {
            text.append(item);
        }
        closeTags(text, tags);
        return text;
    }

    /**
     * Iterates over an array of tags and applies them to the beginning of the specified
     * Spannable object so that future text appended to the text will have the styling
     * applied to it. Do not call this method directly.
     */
    private static void openTags(Spannable text, Object[] tags) {
        for (Object tag : tags) {
            text.setSpan(tag, 0, 0, Spannable.SPAN_MARK_MARK);
        }
    }

    /**
     * "Closes" the specified tags on a Spannable by updating the spans to be
     * endpoint-exclusive so that future text appended to the end will not take
     * on the same styling. Do not call this method directly.
     */
    private static void closeTags(Spannable text, Object[] tags) {
        int len = text.length();
        for (Object tag : tags) {
            if (len > 0) {
                text.setSpan(tag, 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                text.removeSpan(tag);
            }
        }
    }

    public static int getCalibrationValue(String prefKey, SharedPreferences preferences) {
        switch (prefKey) {
            case "calibrate_iOS_x":
                return preferences.getInt("calibrate_iOS_x", DEFAULT_CALIBRATION_IOS_X);
            case "calibrate_android_x":
                return preferences.getInt("calibrate_android_x", DEFAULT_CALIBRATION_ANDROID_X);
            case "calibrate_iOS_y":
                return preferences.getInt("calibrate_iOS_y", DEFAULT_CALIBRATION_IOS_Y);
            case "calibrate_android_y":
                return preferences.getInt("calibrate_android_y", DEFAULT_CALIBRATION_ANDROID_Y);
            default:
                return 40;
        }
    }

    public static float convertRssiToDistance(int maxRssi, boolean iOS, Context context) {
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        double x = iOS ? getCalibrationValue("calibrate_iOS_x", preferences) : getCalibrationValue("calibrate_android_x", preferences);
        double y = iOS ? getCalibrationValue("calibrate_iOS_y", preferences) : getCalibrationValue("calibrate_android_y", preferences);
        double expression = (Math.abs(maxRssi) - x) / y;
        double distance = Math.pow(10, expression);
        return (float) Math.max(1, distance);
    }

    public static long toMinutes(long timeInMillisecs) {
        long secs = timeInMillisecs / 1000;
        return secs / 60;
    }

    public static Pair<Long, Long> toMinAndSeconds(long timeInMillisecs) {
        long secs = timeInMillisecs / 1000;
        long mins = secs / 60;
        long leftOverSeconds = secs % 60;
        return new Pair<>(mins, leftOverSeconds);
    }

    public static boolean isLastMinuteInHour(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return (calendar.get(Calendar.MINUTE) == 59);
    }
}
