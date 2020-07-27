package com.ub.pocketcares.bluetoothBeacon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.altbeacon.beacon.Beacon;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;


public class BeaconStat implements Serializable {
    private Beacon beacon;
    private double totalDistance;
    private double twoDistance;
    private double tenDistance;
    private int totalCount;
    private int twoCount;
    private int tenCount;
    private String regionName;
    private int postalCode;
    private long timeStamp;
    private int major;
    private int minor;
    private long twoSessionDuration;

    public Beacon getBeacon() {
        return beacon;
    }

    public void setBeacon(Beacon beacon) {
        this.beacon = beacon;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setTimeStamp(long time) {
        this.timeStamp = time;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public double getTwoDistance() {
        return twoDistance;
    }

    public double getTenDistance() {
        return tenDistance;
    }

    public int getTwoCount() {
        return twoCount;
    }

    public int getTenCount() {
        return tenCount;
    }

    public int getPostalCode() {
        return this.postalCode;
    }

    public void setPostalCode(int postalCode) {
        this.postalCode = postalCode;
    }

    public void setTwoDistance(double twoDistance) {
        this.twoDistance = twoDistance;
        this.totalDistance = twoDistance + this.tenDistance;
    }

    public void setTenDistance(double tenDistance) {
        this.tenDistance = tenDistance;
        this.totalDistance = tenDistance + this.twoDistance;
    }

    public void setTwoCount(int twoCount) {
        this.twoCount = twoCount;
        this.totalCount = twoCount + this.tenCount;
    }

    public void setTenCount(int tenCount) {
        this.tenCount = tenCount;
        this.totalCount = tenCount + this.twoCount;
    }

    public int getMinor() {
        return this.minor;
    }

    public int getMajor() {
        return this.major;
    }

    public String getVbt() {
        return this.major + Integer.toString(this.minor);
    }

    public void setTwoDuration(long beaconSessionDuration) {
        this.twoSessionDuration = beaconSessionDuration;
    }

    public long getTwoSessionDuration() {
        return this.twoSessionDuration;
    }

    public int getTwoSessionDurationMinutes() {
        return (int) (this.getTwoSessionDuration() / (1000 * 60));
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getRegionName() {
        return this.regionName;
    }

    public BeaconStat(Beacon beacon, long time) {
        this.beacon = beacon;
        this.totalDistance = 0;
        this.totalCount = 0;
        this.twoDistance = 0;
        this.twoCount = 0;
        this.tenDistance = 0;
        this.tenCount = 0;
        this.timeStamp = time;
        this.regionName = null;
        this.postalCode = 0;
        this.major = beacon.getId2().toInt();
        this.minor = beacon.getId3().toInt();
    }

    public BeaconStat(Beacon beacon, long time, String regionName) {
        this.beacon = beacon;
        this.totalDistance = 0;
        this.totalCount = 0;
        this.twoDistance = 0;
        this.twoCount = 0;
        this.tenDistance = 0;
        this.tenCount = 0;
        this.timeStamp = time;
        this.regionName = regionName;
        this.major = beacon.getId2().toInt();
        this.minor = beacon.getId3().toInt();
    }

    public BeaconStat(Beacon beacon, long time, int postalCode, String regionName, double twoDistance,
                      int twoCount, long twoSessionDuration, double tenDistance, int tenCount, double totalDistance, int totalCount) {
        this.beacon = beacon;
        this.timeStamp = time;
        this.postalCode = postalCode;
        this.regionName = regionName;
        this.twoDistance = twoDistance;
        this.twoCount = twoCount;
        this.twoSessionDuration = twoSessionDuration;
        this.tenDistance = tenDistance;
        this.tenCount = tenCount;
        this.totalDistance = totalDistance;
        this.totalCount = totalCount;
        this.major = beacon.getId2().toInt();
        this.minor = beacon.getId3().toInt();
    }

    public BeaconStat add(@NonNull BeaconStat other) {
        this.totalDistance += other.getTotalDistance();
        this.totalCount += other.getTotalCount();
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            BeaconStat beaconObj = (BeaconStat) obj;
            return this.beacon.equals(beaconObj.beacon);
        }
    }

    @Override
    public int hashCode() {
        return beacon.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.getTimeStamp());
        return String.format(Locale.getDefault(), "Time: %s,VBT: %s, UUID: %s,Two Distance: %f, Two Count: %d, Two Session duration: %d " +
                        "Ten Distance: %f,Ten Count: %d, Total Distance: %f, Total Count: %d", calendar.getTime().toString(),
                this.getVbt(), this.getBeacon().getId1().toString(), this.twoDistance, this.twoCount, this.twoSessionDuration,
                this.tenDistance, this.tenCount, this.totalDistance, this.totalCount);
    }
}
