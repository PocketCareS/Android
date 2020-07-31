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

import android.util.Log;

import org.altbeacon.beacon.service.RssiFilter;

public class AverageRSSIFilter implements RssiFilter {
    private int count;
    private int sum;
    private boolean resetAverage;

    public AverageRSSIFilter() {
        this.count = 0;
        this.sum = 0;
        this.resetAverage = false;
    }

    @Override
    public void addMeasurement(Integer rssiValue) {
        if (this.resetAverage) {
            this.sum = rssiValue;
            this.count = 1;
            this.resetAverage = false;
        } else {
            Log.v("AverageRSSI", "RSSI values: " + rssiValue);
            this.count += 1;
            this.sum += rssiValue;
        }

    }

    @Override
    public boolean noMeasurementsAvailable() {
        return false;
    }

    @Override
    public double calculateRssi() {
        this.resetAverage = true;
        if (this.count != 0) {
            double average = (double) this.sum / this.count;
            Log.v("AverageRSSI", "RSSI average: " + average);
            return average;
        }
        return -1;
    }

    @Override
    public int getMeasurementCount() {
        return count;
    }
}
