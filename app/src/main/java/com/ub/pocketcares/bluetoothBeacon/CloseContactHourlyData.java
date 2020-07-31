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
