package com.ub.pocketcares.settings;

import java.util.HashMap;

public class SettingStatic {

    public static final Integer LISTTYPE_DIVIDER = 0;
    public static final Integer LISTTYPE_ITEM = 1;

    public static final int INT_TERMS = 2;
    public static final int INT_INTERFACE = 3;
    public static final int INT_REFER = 4;
    public static final int INT_CONTACT = 5;
    public static boolean APP_IN_DEBUG_MODE = false;
    public static boolean TOAST_LOGS = false;
    public static boolean CALIBRATION_SWITCH = true;


    public static final String STR_REFER = "Refer Friends";
    public static final String STR_INTERFACE = "Preferences";

    public static final String STR_TERMS = "About";
    public static final String STR_CONTACT = "Contact Us";


    public static final String[] ITEMLIST = {
            STR_TERMS,
            STR_INTERFACE,
            STR_REFER,
            STR_CONTACT,
    };

    public static HashMap<String, Integer> str2int = new HashMap<String, Integer>();

    static {
        str2int.put(STR_INTERFACE, INT_INTERFACE);
        str2int.put(STR_REFER, INT_REFER);
        str2int.put(STR_TERMS, INT_TERMS);
        str2int.put(STR_CONTACT, INT_CONTACT);
    }
}
