package com.ub.pocketcares.backend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ub.pocketcares.BuildConfig;
import com.ub.pocketcares.utility.Utility;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SecureKeys {

    private static final String MASTER_KEY = "masterKey";
    public static final String DAILY_KEY = "dailyKey";
    private static final String VBT_MAJOR = "vbtMajor";
    private static final String VBT_MINOR = "vbtMinor";
    private static final String VBT_TIME = "vbtHour";
    public static final String UUID = "a1157b5a-2a58-472d-9bb6-32fce5734809";
    public static final String UUID_iOS = "a1157b5a-2a58-472d-9bb6-32fce5734808";

    public static void generateMasterKey(Context context) {
        SecureRandom random = new SecureRandom();
        byte[] masterKey = new byte[32];
        random.nextBytes(masterKey);
        String masterKeyString = getHexStringFromByteArray(masterKey);
        storePreferenceHelper(context, MASTER_KEY, masterKeyString);
    }

    private static void dailyKeyHelper(long day, Context context) throws InvalidKeyException, NoSuchAlgorithmException {
        long dayNumber = day / (1000 * 3600 * 24);
        byte[] DayNo = longToBytes(dayNumber);

        byte[] DKey = new byte[16];

        String masterKeyString = getPreferenceHelper(context, MASTER_KEY);
        byte[] masterKey = getByteArrFromHexString(masterKeyString);
        DKey = HDKFsha256(masterKey, DayNo, DKey.length);
        Gson gson = new Gson();
        HashMap<Long, String> dailyKeyMap = new HashMap<>();
        String jsonMap = getPreferenceHelper(context, DAILY_KEY);
        if (jsonMap != null) {
            Type type = new TypeToken<HashMap<Long, String>>() {
            }.getType();
            dailyKeyMap = gson.fromJson(jsonMap, type);
        }
        long currentDay = Utility.convertDayMills(day);
        String dailyKeyString = getHexStringFromByteArray(DKey);
        dailyKeyMap.put(currentDay, dailyKeyString);
        storePreferenceHelper(context, DAILY_KEY, gson.toJson(dailyKeyMap));
    }

    public static void generateDailyKey(Context context, long day) throws InvalidKeyException, NoSuchAlgorithmException {
        dailyKeyHelper(day, context);
    }

    public static void generateVBT(Context context) {
        String dailyKeyString = getDailyKey(context, Utility.convertDayMills(Calendar.getInstance().getTimeInMillis()));
        byte[] dailyKey = getByteArrFromHexString(dailyKeyString);
        //for generating VBT
        Calendar currentCalendar = Calendar.getInstance();
        if (currentCalendar.get(Calendar.MINUTE) == 59) {
            currentCalendar.add(Calendar.MINUTE, 1);
        }
        long hourNumber = currentCalendar.getTimeInMillis() / (1000 * 3600);
        byte[] HourNo = longToBytes(hourNumber);
        byte[] VBTfull = null; //VBT_full is 32B long
        int vbt_p = 0;
        boolean minor_zero = true;
        boolean major_zero = true;
        int minorInt = 0, majorInt = 0;
        while (minor_zero || major_zero) {
            if (vbt_p > 0) {
                hourNumber -= 100000;
                HourNo = longToBytes(hourNumber);
            }
            VBTfull = calHmacSha256(dailyKey, HourNo);
            vbt_p = 0;
            if (minor_zero) {
                for (int i = vbt_p; i < 31; i += 2) {
                    minorInt = getIntFromByte(VBTfull, i);
                    if (minorInt != 0) {
                        vbt_p += 2;
                        minor_zero = false;
                        break;
                    }
                    vbt_p += 2;
                }
            }
            for (int i = vbt_p; i < 31; i += 2) {
                majorInt = getIntFromByte(VBTfull, i);
                if (majorInt != 0) {
                    vbt_p += 2;
                    major_zero = false;
                    break;
                }
                vbt_p += 2;
            }
        }
        Log.v("HourlyAlarm", "Generated Major: " + majorInt);
        Log.v("HourlyAlarm", "Generated Minor: " + minorInt);
        storePreferenceHelper(context, VBT_MAJOR, Integer.toString(majorInt));
        storePreferenceHelper(context, VBT_MINOR, Integer.toString(minorInt));
        storePreferenceHelper(context, VBT_TIME, Long.toString(Utility.convertHourMills(currentCalendar.getTimeInMillis())));
        if (BuildConfig.DEBUG) {
            // writing key to the database
            Pair<Integer, Integer> vbt = new Pair<>(majorInt, minorInt);
            SessionDatabaseHelper db = new SessionDatabaseHelper(context);
            db.logVBTName(vbt);
            db.close();
        }
    }

    public static Calendar getVBTGeneratedTime(Context context) {
        String time = getPreferenceHelper(context, VBT_TIME);
        Calendar vbtGeneratedTime = Calendar.getInstance();
        if (time != null) {
            vbtGeneratedTime.setTimeInMillis(Long.parseLong(time));
        }
        return vbtGeneratedTime;
    }

    public static Pair<Integer, Integer> getVBT(Context context) {
        String majorString = getPreferenceHelper(context, VBT_MAJOR);
        String minorString = getPreferenceHelper(context, VBT_MINOR);
        return new Pair<>(Integer.parseInt(majorString), Integer.parseInt(minorString));
    }

    public static String getDailyKey(Context context, long day) {
        HashMap<Long, String> dailyKeysMap = getDailyKeyMap(context);
        if (!dailyKeysMap.containsKey(day)) {
            try {
                generateDailyKey(context, day);
                dailyKeysMap = getDailyKeyMap(context);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return dailyKeysMap.get(day);
    }

    public static HashMap<Long, String> getDailyKeyMap(Context context) {
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<Long, String>>() {
        }.getType();
        return gson.fromJson(getPreferenceHelper(context, DAILY_KEY), type);
    }

    private static String getHexStringFromByteArray(byte[] byteArr) {
        Formatter formatter = new Formatter();
        for (byte b : byteArr) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private static byte[] getByteArrFromHexString(String hexString) {
        byte[] val = new byte[hexString.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        return val;
    }

    private static String getPreferenceHelper(Context context, String key) {
        SharedPreferences secureKeys = context.getSharedPreferences("secureKeys", Context.MODE_PRIVATE);
        return secureKeys.getString(key, null);
    }

    private static void storePreferenceHelper(Context context, String key, String value) {
        SharedPreferences secureKeys = context.getSharedPreferences("secureKeys", Context.MODE_PRIVATE);
        SharedPreferences.Editor secureKeysEditor = secureKeys.edit();
        secureKeysEditor.putString(key, value);
        secureKeysEditor.apply();
    }

    public static int getIntFromByte(byte[] VBT, int index) {
        return ((VBT[index] & 0xff) << 8) | (VBT[index + 1] & 0xff);
    }

    private static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }
        return result;
    }


    private static byte[] calHmacSha256(byte[] secretKey, byte[] message) {
        byte[] hmacSha256;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
        return hmacSha256;

    }


    private static byte[] HDKFsha256(byte[] masterkey, byte[] day, int length) throws NoSuchAlgorithmException, InvalidKeyException {
        // masterkey: key material,
        // day: info
        // length: length of output
        // Here, we omit salt and consider it as an array of hash length of zeros.

        //SecretKeySpec salt_key = new SecretKeySpec(salt, "HmacSHA256");//ios/swift might be different here
        if (masterkey == null || masterkey.length <= 0) {
            throw new IllegalArgumentException("provided masterkey must not be null");
        }

        byte[] salt = new byte[16]; //the size of salt doesn't matter much as long as keep consisttent on different platform
        // extract key
        byte[] extract = calHmacSha256(salt, masterkey);

        // expand key
   /*from https://tools.ietf.org/html/rfc5869
       The output OKM is calculated as follows:
         N = ceil(L/HashLen)
         T = T(1) | T(2) | T(3) | ... | T(N)
         OKM = first L bytes of T
       where:
         T(0) = empty string (zero length)
         T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
         T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
         T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
         ...
   */
        int it = (int) Math.ceil(((double) length) / ((double) 32)); //since we use sha256, so the output length of hmac here is 32.
        // This calculation might not be necessary since we know this hkdf is only used for generating Daily key, and the length we need is always 16 bytes.
        Mac hkdhmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec expandkey = new SecretKeySpec(extract, "HmacSHA256");//ios/swift might be different here
        hkdhmac.init(expandkey);

        byte[] blockN = new byte[0];

        ByteBuffer buffer = ByteBuffer.allocate(length);
        int remainingBytes = length;
        int stepSize;

        for (int i = 0; i < it; i++) {
            hkdhmac.update(blockN);
            hkdhmac.update(day);
            hkdhmac.update((byte) (i + 1));

            blockN = hkdhmac.doFinal();

            stepSize = Math.min(remainingBytes, blockN.length);

            buffer.put(blockN, 0, stepSize);
            remainingBytes -= stepSize;
        }
        return buffer.array();
    }
}
