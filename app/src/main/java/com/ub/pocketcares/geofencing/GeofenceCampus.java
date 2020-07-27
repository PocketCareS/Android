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

