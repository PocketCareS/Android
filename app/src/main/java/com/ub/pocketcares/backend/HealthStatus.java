package com.ub.pocketcares.backend;

import java.io.Serializable;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

public class HealthStatus implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String HEALTH_STATUS = "health";

    public static final String DIVIDER = ";";
    public static final String ONE = "1";
    public static final String ZERO = "0";
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public static final int HEALTH_SYMPTOM_NUM = 5;
    public static String HEALTH_STATUS_DEFAULT = "";

    static {
        for (int i = 0; i < HEALTH_SYMPTOM_NUM; i++)
            HEALTH_STATUS_DEFAULT += ZERO;
    }

    public static final String HEALTH_SYMPTOM1 = "Fever";
    public static final String HEALTH_SYMPTOM2 = "Cough";
    public static final String HEALTH_SYMPTOM3 = "Shortness of Breath or Difficulty Breathing";
    public static final String HEALTH_SYMPTOM4 = "Chills";
    public static final String HEALTH_SYMPTOM5 = "Other (Muscle Pain, Sore Throat, New loss of taste or smell)";
    public static final ArrayList<String> SYMPTOM_LIST = new ArrayList<String>();

    static {
        SYMPTOM_LIST.add(HEALTH_SYMPTOM2);
        SYMPTOM_LIST.add(HEALTH_SYMPTOM3);
        SYMPTOM_LIST.add(HEALTH_SYMPTOM1);
        SYMPTOM_LIST.add(HEALTH_SYMPTOM4);
        SYMPTOM_LIST.add(HEALTH_SYMPTOM5);
    }

    public static final String HEALTH_SYMPTOM6 = "I am feeling very well!";
    public static final String HEALTH_NEARBY = "observed people with such symptoms around you";

    public static final String[] SYMPTOM_TIP_LIST = {
            "fever tips",
            "runny nose tips",
            "coughing tips",
            "sore tips",
            "nausea tips",
    };

    private String dateString;  // in "YYYYMMDD" format
    private String extra1;        // extra field1 to store info
    private String extra2;

    public HealthStatus(String date) {
        this.dateString = date;
        for (int i = 0; i < HEALTH_SYMPTOM_NUM; i++)
            this.extra1 += ZERO;
        this.extra2 = FALSE;
    }

    public void setExtraField1(String e1) {
        this.extra1 = e1;
    }

    public void setExtraField2(String e2) {
        this.extra2 = e2;
    }

    public String getDateString() {
        return this.dateString;
    }

    public String getExtraField1() {
        return this.extra1;
    }

    public String getExtraField2() {
        return this.extra2;
    }

    public boolean isSick() {
        try {
            if (0 == ONE.compareTo(String.valueOf(extra2.charAt(1))))
                return false;
            else {
                for (int i = 0; i < HEALTH_SYMPTOM_NUM; i++) {
                    if (0 == ONE.compareTo(String.valueOf(extra1.charAt(i))))
                        return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // this is the string format saved in daily status db
    public String toDBString() {
        return extra1 + DIVIDER + extra2;
    }

    // hstr is db string
    public static int getHealthScore(String hstr) {
        try {
            int score = HEALTH_SYMPTOM_NUM;
            for (int i = 0; i < HEALTH_SYMPTOM_NUM; i++)
                if (0 == ONE.compareTo(String.valueOf(hstr.charAt(i))))
                    score--;
            return score;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isNearbySick() {
        try {
            if (0 == ONE.compareTo(String.valueOf(extra2.charAt(2))) ||
                    0 == ONE.compareTo(String.valueOf(extra2.charAt(3))))
                return true;
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public String toJsonString() {
        JSONObject health = new JSONObject();
        JSONObject attributes = new JSONObject();

        try {
            attributes.put("date", dateString);
            attributes.put("sick", String.valueOf(isSick()));
            attributes.put("hstr", toDBString());
            attributes.put("nbsick", String.valueOf(isNearbySick()));
            health.put(HEALTH_STATUS, attributes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return health.toString();
    }

}
