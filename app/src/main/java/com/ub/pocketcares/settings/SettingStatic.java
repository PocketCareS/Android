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

import java.util.HashMap;

public class SettingStatic {

    public static final Integer LISTTYPE_DIVIDER = 0;
    public static final Integer LISTTYPE_ITEM = 1;

    public static final int INT_TERMS = 2;
    public static final int INT_INTERFACE = 3;
    public static final int INT_REFER = 4;
    public static final int INT_CONTACT = 5;
    public static boolean TOAST_LOGS = false;


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
