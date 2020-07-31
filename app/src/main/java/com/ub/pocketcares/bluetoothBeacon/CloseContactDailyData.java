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
