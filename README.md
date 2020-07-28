# PocketCare S - Android

<img src="logo.png" width="300">

Call for code submission for COVID-19 track. 
Description will come here. 

## Contents 
1. [Demo Video](#demo-video) 
2. [Getting Started](#getting-started)
3. [How does PocketCare S Work?](#how-does-pocketcare-s-work)
4. [The Architecture](#the-architecture) 
5. [Built With](#built-with)
6. [Project RoadMap](#project-roadmap)
7. [Further Reading](#further-reading)
8. [License](#license)
9. [Acknowledgments](#acknowledgements)

## Demo Video 

[![Demo](http://img.youtube.com/vi/JnOWwagUgxQ/0.jpg)](http://www.youtube.com/watch?v=JnOWwagUgxQ "PocketCare S Demo")
 

## Getting Started 

**Due to emulators not supporting bluetooth, close encounter detection will not work on emulators.**

### Prerequisites

Before you begin, make sure you satisfy the following requirements:

1. You are running this on a physical android device.
2. The device should at least be running on [android sdk version 21](https://developer.android.com/studio/releases/platforms#5.0) (Android L)
3. The device should have Bluetooth LE support. More details about this requirement can be found here. 

As long as you run this on any modern android device it should work, you can check the Bluetooth LE compatibility of your device [here](https://altbeacon.github.io/android-beacon-library/beacon-transmitter-devices.html). 


### Running PocketCare S using Android Studio

1. Open the project in Android Studio.
2. Wait for Gradle build to finish.
3. Connect your android device to your computer and make sure you have USB debugging turned on. You can follow this [article](https://developer.android.com/studio/debug/dev-options#enable) to enable usb debugging.  
4. The application is already configured with the IBM server URLs. If you want to run server on your local machine follow the PocketCareS-Server setup documentation and replace the **serverHost** variable with your own link. 
5. Android Studio should automatically detect the configurations, after the gradle build is finished click on the play button on top to run PocketCare S. 

### Running PocketCare S using an APK 

1. On your android device, make sure you have enabled. You can follow this [article](https://www.androidcentral.com/unknown-sources) to enable it. 
2. Download the APK from here. 
3. After the APK is downloaded, tap to install it and run PocketCare S.

Once the application starts, follow the on-boarding process and read how P works below. 

## How does PocketCare S Work?

You can read more about how the server works here.

1. PocketCare S uses Bluetooth LE technology to scan and send packets called beacons. 
2. PocketCare S uses the iBeacon layout for the beacon format in order to work with the iOS version.
3. As soon as PocketCare S starts with the required permissions, it starts scanning for iBeacons in a regular interval (every minute) and transmitting iBeacons continuously.
4. When PocketCare S detects a beacon nearby, it starts a session.    


For a more detailed description, refer to [further reading](#further-reading) section. 


## The Architecture

## Built With 

## Project RoadMap 

## Further Reading

## License 

## Acknowledgements
