package com.ub.pocketcares.geofencing;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GeofenceUtils {

    public static boolean isOnCampus() {
        return true;
    }

    public static String identifyIfOnCampus(double latitude, double longitude) {

        HashMap<String, GeofenceCampus> campusesMap = GeofenceUtils.fetchCampuses();

        ArrayList<GeofenceCampus> campuses = new ArrayList<>(campusesMap.values());

        String campusName = null;
        for (GeofenceCampus campus: campuses){
            if(PolyUtils.containsLocation(latitude, longitude, campus.getPointsInLatLng(), false)){
                campusName = campus.getCampusName();
                break;
            }
        }

        return (campusName);
    }

    public static HashMap<String, GeofenceCampus> fetchCampuses() {
        HashMap<String, GeofenceCampus> campuses = new HashMap<>();
        campuses.put("Home",new GeofenceCampus("Home", new ArrayList<LatLng>(Arrays.asList(
                new LatLng(42.95530742256826, -78.82886169719141),
                new LatLng(42.95608086720053, -78.82816968726557),
                new LatLng(42.955397723711826, -78.82696269320887),
                new LatLng(42.954836284017844, -78.82857738303584),
                new LatLng(42.95530742256826, -78.82886169719141)))));

        /*campuses.add(new GeofenceCampus("Home-right", new ArrayList<LatLng>(Arrays.asList(
                new LatLng(42.95423950334259, -78.82684467601221),
                new LatLng(42.954875545701476, -78.8268607692663),
                new LatLng(42.95515430293503, -78.82607756423396),
                new LatLng(42.95440833004168, -78.82562158870142),
                new LatLng(42.95423950334259, -78.82684467601221)))));

        campuses.add(new GeofenceCampus("Home-right", new ArrayList<LatLng>(Arrays.asList(
                new LatLng(42.954659606131514, -78.82849155234736),
                new LatLng(42.954274839201645, -78.82766543197077),
                new LatLng(42.953795840274395, -78.82857738303584),
                new LatLng(42.954659606131514, -78.82849155234736)))));*/

        campuses.put("UB-South", new GeofenceCampus("UB-South", new ArrayList<LatLng>(Arrays.asList(
                new LatLng(42.951819902178094, -78.82504823639472),
                new LatLng(42.94958968812546, -78.82105710938056),
                new LatLng(42.94943262775192, -78.81371858551582),
                new LatLng(42.95970353219272, -78.81384733154853),
                new LatLng(42.95772486740483, -78.81865385010322),
                new LatLng(42.951819902178094, -78.82504823639472)))));

        campuses.put("UB-North", new GeofenceCampus("UB-North", new ArrayList<LatLng>(Arrays.asList(
                new LatLng(43.01268931028516, -78.79429973781717),
                new LatLng(43.006915153269084, -78.7942139071287),
                new LatLng(43.006726856401315, -78.7989345949949),
                new LatLng(42.9986923190239, -78.7989345949949),
                new LatLng(42.990970640795325, -78.8040844363035),
                new LatLng(42.99153567454042, -78.78803409755838),
                new LatLng(42.99674629644974, -78.78571666896951),
                new LatLng(42.99762515298582, -78.77610363186014),
                new LatLng(43.00314910624702, -78.7722412508787),
                new LatLng(43.006475793013124, -78.77275623500955),
                new LatLng(43.01312862609674, -78.78477253139627),
                new LatLng(43.01268931028516, -78.79429973781717)))));
        return campuses;
    }

}
