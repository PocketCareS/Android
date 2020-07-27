package com.ub.pocketcares.bluetoothBeacon;

import java.util.TreeMap;

public class ContactAnalyticsResponse {
    TreeMap<Long, CloseContactDailyData> contactCount;

    public TreeMap<Long, CloseContactDailyData> getContactCount() {
        return contactCount;
    }

    public ContactAnalyticsResponse() {
        this.contactCount = new TreeMap<>();
    }

}

