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

package com.ub.pocketcares.geofencing;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class GeofenceCampus {
    String campusName;
    ArrayList<Point> pointsInUTM;
    ArrayList<LatLng> pointsInLatLng;

    public String getCampusName() {
        return campusName;
    }

    public ArrayList<LatLng> getPointsInLatLng() {
        return pointsInLatLng;
    }

    public GeofenceCampus(String campusName, ArrayList<LatLng> pointsInLatLng){
        this.campusName = campusName;
        this.pointsInLatLng = pointsInLatLng;
        this.pointsInUTM = new ArrayList<>();
        for (LatLng latLng : this.pointsInLatLng){
            this.pointsInUTM.add(UTMPoint.getUTMCoordinate(latLng.latitude, latLng.longitude).toNorthingEastingPoint());
        }
        UTMPoint anyPoint = UTMPoint.getUTMCoordinate(pointsInLatLng.get(0).latitude, pointsInLatLng.get(0).longitude);
    }
}

