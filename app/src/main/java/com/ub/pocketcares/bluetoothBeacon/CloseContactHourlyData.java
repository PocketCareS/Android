package com.ub.pocketcares.bluetoothBeacon;

public class CloseContactHourlyData {
    Integer numberOfContacts;
    Integer duration;

    public CloseContactHourlyData(int duration, int numberOfContacts) {
        this.numberOfContacts = numberOfContacts;
        this.duration = duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setNumberOfContacts(int numberOfContacts) {
        this.numberOfContacts = numberOfContacts;
    }

    public int getCloseContactCount() {
        return numberOfContacts;
    }

    public int getCloseContactDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "CloseContactHourlyData{" +
                "numberOfContacts=" + numberOfContacts +
                ", duration=" + duration +
                '}';
    }
}
