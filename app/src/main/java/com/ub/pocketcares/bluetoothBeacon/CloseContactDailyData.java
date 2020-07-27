package com.ub.pocketcares.bluetoothBeacon;

import java.util.TreeMap;

public class CloseContactDailyData {
    TreeMap<Long, CloseContactHourlyData> closeContactCount;
    Integer duration;
    Integer totalCount;

    public CloseContactDailyData() {
        this.closeContactCount = new TreeMap<>();
        this.duration = 0;
        this.totalCount = 0;
    }

    public CloseContactDailyData(TreeMap<Long, CloseContactHourlyData> closeContactCount, Integer duration, Integer totalCount) {
        this.closeContactCount = closeContactCount;
        this.duration = duration;
        this.totalCount = totalCount;
    }

    public TreeMap<Long, CloseContactHourlyData> getHourlyData() {
        return closeContactCount;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setTotalCount(int count) {
        this.totalCount = count;
    }

    public int getDailyTotalDuration() {
        return duration;
    }

    public int getDailyTotalCount() {
        return totalCount;
    }

    @Override
    public String toString() {
        return "CloseContactDailyData{" +
                "closeContactCount=" + closeContactCount +
                ", duration=" + duration +
                ", totalCount=" + totalCount +
                '}';
    }
}
