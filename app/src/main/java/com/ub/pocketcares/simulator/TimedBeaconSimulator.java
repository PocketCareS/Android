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

package com.ub.pocketcares.simulator;

import org.altbeacon.beacon.AltBeacon;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.simulator.BeaconSimulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimedBeaconSimulator implements BeaconSimulator {
    protected static final String TAG = "TimedBeaconSimulator";
    private List<Beacon> beacons;

    /*
     * You may simulate detection of beacons by creating a class like this in your project.
     * This is especially useful for when you are testing in an Emulator or on a device without BluetoothLE capability.
     *
     * Uncomment the lines in BeaconReferenceApplication starting with:
     *     // If you wish to test beacon detection in the Android Emulator, you can use code like this:
     * Then set USE_SIMULATED_BEACONS = true to initialize the sample code in this class.
     * If using a Bluetooth incapable test device (i.e. Emulator), you will want to comment
     * out the verifyBluetooth() in MonitoringActivity.java as well.
     *
     * Any simulated beacons will automatically be ignored when building for production.
     */
    public boolean USE_SIMULATED_BEACONS = false;

    /**
     *  Creates empty beacons ArrayList.
     */
    public TimedBeaconSimulator(){
        beacons = new ArrayList<Beacon>();
    }

    /**
     * Required getter method that is called regularly by the Android Beacon Library.
     * Any beacons returned by this method will appear within your test environment immediately.
     */
    public List<Beacon> getBeacons(){
        return beacons;
    }

    /**
     * Creates simulated beacons all at once.
     */
    public void createBasicSimulatedBeacons(){
        if (USE_SIMULATED_BEACONS) {

            /*beaconManager.getBeaconParsers().add(new BeaconParser().
                    setBeaconLayout(iBeacon));
            beaconManager.getBeaconParsers().add(new BeaconParser("overflowArea").setBeaconLayout(overflowArea));
            region = new Region("backgroundRegion",
                    null, null, null);*/

            Beacon beacon1 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("1").setRssi(-55).setTxPower(-55).build();
            Beacon beacon2 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("2").setRssi(-55).setTxPower(-55).build();
            Beacon beacon3 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("3").setRssi(-55).setTxPower(-55).build();
            Beacon beacon4 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("4").setRssi(-55).setTxPower(-55).build();
            beacons.add(beacon1);
            beacons.add(beacon2);
            beacons.add(beacon3);
            beacons.add(beacon4);


        }
    }


    private ScheduledExecutorService scheduleTaskExecutor;


    /**
     * Simulates a new beacon every 10 seconds until it runs out of new ones to add.
     */
    public void createTimedSimulatedBeacons(){
        if (USE_SIMULATED_BEACONS){
            beacons = new ArrayList<Beacon>();
            Beacon beacon1 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("1").setRssi(-55).setTxPower(-55).build();
            Beacon beacon2 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("2").setRssi(-55).setTxPower(-55).build();
            Beacon beacon3 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("3").setRssi(-55).setTxPower(-55).build();
            Beacon beacon4 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                    .setId2("1").setId3("4").setRssi(-55).setTxPower(-55).build();
            beacons.add(beacon1);
            beacons.add(beacon2);
            beacons.add(beacon3);
            beacons.add(beacon4);

            final List<Beacon> finalBeacons = new ArrayList<Beacon>(beacons);

            //Clearing beacons list to prevent all beacons from appearing immediately.
            //These will be added back into the beacons list from finalBeacons later.
            beacons.clear();

            scheduleTaskExecutor= Executors.newScheduledThreadPool(5);

            // This schedules an beacon to appear every 10 seconds:
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try{
                        //putting a single beacon back into the beacons list.
                        if (finalBeacons.size() > beacons.size())
                            beacons.add(finalBeacons.get(beacons.size()));
                        else
                            scheduleTaskExecutor.shutdown();

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);
        }
    }

}
