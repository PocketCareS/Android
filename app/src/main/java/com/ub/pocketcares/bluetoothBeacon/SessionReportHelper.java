package com.ub.pocketcares.bluetoothBeacon;

import android.util.Pair;

import com.ub.pocketcares.utility.Utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SessionReportHelper {
    public static String buildReportForSession(long sessionStart, long sessionEnd, ArrayList<SessionManager.GPSLog> gpsLogs) throws Exception {
        long fiveMins = 5 * 60 * 1000;
        String info = "";

        Pair<ArrayList<SessionManager.GPSLog>, String> filteredData = SessionReportHelper.getFormattedGPSTags(sessionStart, sessionEnd, fiveMins, gpsLogs);
        ArrayList<SessionManager.GPSLog> filteredValues = filteredData.first;
        String exception = filteredData.second;
        if (exception != null){
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");
            info += "\nException buffer-5 min: "+exception+"\n";
            info += "Values: ";
            for (SessionManager.GPSLog log: filteredValues){
                info += dateFormat.format(new Date(log.logTime))+", ";
            }
            filteredData = SessionReportHelper.getFormattedGPSTags(sessionStart, sessionEnd, fiveMins*2, gpsLogs);
            filteredValues = filteredData.first;
            exception = filteredData.second;
            if(exception != null){
                info += "\nException buffer-10 min: "+exception+"\n";
                info += "Values: ";
                for (SessionManager.GPSLog log: filteredValues){
                    info += dateFormat.format(new Date(log.logTime))+", ";
                }
            }
        }

        ArrayList<SessionManager.TaggedEncounterSession> joinedRegions
            = SessionReportHelper.buildRegions(filteredValues, sessionStart, sessionEnd);

        Pair<ArrayList<SessionManager.TaggedEncounterSession>, Boolean> splitSessionData = splitSessions(sessionStart, sessionEnd,
                joinedRegions);

        ArrayList<SessionManager.TaggedEncounterSession> splitSessions = splitSessionData.first;
        boolean didMainSessionEndOnCampus = splitSessionData.second;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        String start = dateFormat.format(new Date(sessionStart));
        String end = dateFormat.format(new Date(sessionEnd));

        info += "Start: "+start+"\n";
        info += "End: "+end+"\n";
        info += "Overall duration: "+(Utility.toMinutes(sessionEnd-sessionStart)+1)+"\n";
        String filteredValuesStr = "GPS Values used(time, onCampus): ";
        for (SessionManager.GPSLog log: filteredValues){
            filteredValuesStr += "["+dateFormat.format(new Date(log.logTime))+", "+log.campusName+"], ";
        }
        info += filteredValuesStr+"\n";
        long otherOnCampus = 0, otherOffCampus = 0;
        info += "Split sessions:\n";
        int i = 1;
        for (SessionManager.TaggedEncounterSession taggedSession: splitSessions){
            info += "\t\t"+i+") "+dateFormat.format(new Date(taggedSession.start)) +" to "+
                    dateFormat.format(new Date(taggedSession.end))+", onCampus: "+taggedSession.isOnCampus+"\n";
            if (taggedSession.isOnCampus){
                otherOnCampus += taggedSession.end - taggedSession.start;
            }else{
                otherOffCampus += taggedSession.end - taggedSession.start;
            }
            i++;
        }
        Pair<Long, Long> onCampusAsMinAndSec = Utility.toMinAndSeconds(otherOnCampus);
        Pair<Long, Long> offCampusAsMinAndSec = Utility.toMinAndSeconds(otherOffCampus);
        otherOnCampus = onCampusAsMinAndSec.first;
        otherOffCampus = offCampusAsMinAndSec.first;
        if(didMainSessionEndOnCampus){
            otherOnCampus += 1;
        }else{
            otherOffCampus += 1;
        }
        info += "Other on-campus: "+otherOnCampus+":"+onCampusAsMinAndSec.second+"\n";
        info += "Other off-campus: " + otherOffCampus+":"+offCampusAsMinAndSec.second+"\n";
        return info;
    }

    private static Pair<ArrayList<SessionManager.TaggedEncounterSession>, Boolean>
        splitSessions(long sessionStart, long sessionEnd,
                      ArrayList<SessionManager.TaggedEncounterSession> joinedRegions) {
        ArrayList<SessionManager.TaggedEncounterSession> splitSessions = new ArrayList<>();
        boolean didMainSessionEndOnCampus = false;
        if (joinedRegions.size() == 0){
            // if there is no reading, just assume that the encounter happened off-campus
            didMainSessionEndOnCampus = false;
            splitSessions.add(new SessionManager.TaggedEncounterSession(
                    sessionStart, sessionEnd, false
            ));
            return new Pair<>(splitSessions, didMainSessionEndOnCampus);
        }

        boolean isStartBehind = false;
        SessionManager.TaggedEncounterSession previousSession = null;
        for (SessionManager.TaggedEncounterSession region: joinedRegions)
        {
            if (!isStartBehind){
                if (sessionStart >= region.end){
                    continue;
                }else{
                    isStartBehind = true;
                    if (sessionEnd <= region.end){
                        splitSessions.add(new SessionManager.TaggedEncounterSession(sessionStart, sessionEnd,
                                region.isOnCampus));
                        didMainSessionEndOnCampus = region.isOnCampus;
                        break;
                    }else{
                        previousSession = new SessionManager.TaggedEncounterSession(sessionStart, sessionEnd,
                                region.isOnCampus);
                    }
                }
            }else{
                if (sessionEnd > region.end){
                    if (previousSession.isOnCampus == region.isOnCampus){
                        previousSession.end = region.end;
                    }else{
                        splitSessions.add(previousSession);
                        previousSession = new SessionManager.TaggedEncounterSession(region.start, region.end, region.isOnCampus);
                    }
                }else{
                    // now we have fixed the end of the session
                    if (previousSession.isOnCampus == region.isOnCampus){
                        previousSession.end = sessionEnd;
                        splitSessions.add(previousSession);
                    }else{
                        // report previous session
                        splitSessions.add(previousSession);

                        // start a new session
                        previousSession = new SessionManager.TaggedEncounterSession(region.start, sessionEnd, region.isOnCampus);
                        splitSessions.add(previousSession);
                    }
                    didMainSessionEndOnCampus = region.isOnCampus;
                    break;
                }
            }
        }
        return new Pair<>(splitSessions, didMainSessionEndOnCampus);
    }

    private static ArrayList<SessionManager.TaggedEncounterSession> buildRegions(ArrayList<SessionManager.GPSLog> filteredValues,
                                                                                 long start, long end) {
        ArrayList<SessionManager.TaggedEncounterSession> timeRegions =
                new ArrayList<>();
        if (filteredValues.size() == 0){
            return timeRegions;
        }
        // this can be any large value(plays the role of infinity). In this case it is 2 days.
        long buffer = 2*60*60*1000;

        if(filteredValues.size() == 1){
            timeRegions.add(
                    new SessionManager.TaggedEncounterSession(start-buffer, end+buffer,
                            filteredValues.get(0).isOnCampus));
            return timeRegions;
        }

        SessionManager.TaggedEncounterSession prevRegion = null;
        for (int i = 0; i < filteredValues.size(); i++){
            SessionManager.GPSLog gpsLog = filteredValues.get(i);
            if(i == 0){
                // this is the 1st gps tag, so we assume that the region exists from -infinity to gpsLog.logtime
                prevRegion = new SessionManager.TaggedEncounterSession(
                        gpsLog.logTime - buffer, gpsLog.logTime, gpsLog.isOnCampus);
                timeRegions.add(prevRegion);
            }else if (i == filteredValues.size()-1){
                // this is the last gps tag, so we assume that the region exists from gpsLog.logtime to infinity
                timeRegions.add(new SessionManager.TaggedEncounterSession(
                        gpsLog.logTime, gpsLog.logTime+buffer, gpsLog.isOnCampus));
            }

            // do this except for the last reading
            if (i != filteredValues.size() -1 ){
                // of the current value is not the last value
                SessionManager.GPSLog nextLog = filteredValues.get(i+1);
                if(gpsLog.isOnCampus == nextLog.isOnCampus){
                    timeRegions.add(new SessionManager.TaggedEncounterSession(
                            gpsLog.logTime, nextLog.logTime, gpsLog.isOnCampus
                    ));
                }else{
                    long tranDuration = nextLog.logTime - gpsLog.logTime;
                    long mid = gpsLog.logTime + tranDuration/2;
                    timeRegions.add(new SessionManager.TaggedEncounterSession(
                            gpsLog.logTime, mid, gpsLog.isOnCampus
                    ));
                    timeRegions.add(new SessionManager.TaggedEncounterSession(
                            mid, nextLog.logTime, nextLog.isOnCampus
                    ));
                }
            }
        }
        return timeRegions;
    }

    public static String EXCEPTION_EMPTY_MIDDLE = "No GPS reading between start and end";
    public static String EXCEPTION_EMPTY_END = "No GPS reading after session end";

    private static Pair<ArrayList<SessionManager.GPSLog>, String> getFormattedGPSTags(long sessionStart, long sessionEnd,
                                                                        long slackTime,
                                                                        ArrayList<SessionManager.GPSLog> gpsLogs) throws Exception {
        ArrayList<SessionManager.GPSLog> filteredValues = new ArrayList<>();
        /*SessionManager.GPSLog closestLeft = null, rightmostMid = null, closestRight = null;
        boolean closestLeftAdded = false;*/
        boolean isReadingInMiddle = false, isReadingInEnd = false;
        for (SessionManager.GPSLog gpsLog: gpsLogs){
            if(gpsLog.logTime >= sessionStart - slackTime && gpsLog.logTime <= sessionEnd + slackTime){
                /*if (gpsLog.logTime < sessionStart){
                    // we are to the left of sessionStart
                    closestLeft = gpsLog;
                }else if(gpsLog.logTime <= sessionEnd){
                    // we are in the middle
                    if (closestLeft == null){
                        // there was no reading before sessionStart
                        closestLeft = new SessionManager.GPSLog(sessionStart, gpsLog.campusName, gpsLog.isOnCampus);
                        filteredValues.add(closestLeft);
                        closestLeftAdded = true;
                    }else{
                        if(!closestLeftAdded) {
                            filteredValues.add(closestLeft);
                            closestLeftAdded = true;
                        }
                    }
                    rightmostMid = gpsLog;
                    filteredValues.add(gpsLog);
                }else{
                    // we are in the end
                    /*if (rightmostMid == null){
                        throw new Exception(EXCEPTION_EMPTY_MIDDLE);
                    }
                    if (closestLeft != null && !closestLeftAdded){
                        // takes care of the case when there is a value smaller than start, but there is no value
                        // in the middle
                        filteredValues.add(closestLeft);
                        closestLeftAdded = true;
                    }
                    closestRight = gpsLog;
                    // we have found the closest right item, so we break
                    filteredValues.add(gpsLog);
                    break;
                }*/

                filteredValues.add(gpsLog);
                if (gpsLog.logTime >= sessionStart && gpsLog.logTime <= sessionEnd){
                    isReadingInMiddle = true;
                }

                if (gpsLog.logTime >= sessionEnd){
                    isReadingInEnd = true;
                }
            }
        }

        /*if(rightmostMid != null && rightmostMid.logTime == sessionEnd && closestRight == null){
            closestRight = rightmostMid;
            filteredValues.add(closestRight);
        }*/
        String exceptionCase = null;
        if (!isReadingInMiddle){
            exceptionCase = EXCEPTION_EMPTY_MIDDLE;
        }
        if(!isReadingInEnd){
            exceptionCase = EXCEPTION_EMPTY_END;
        }
        return new Pair<>(filteredValues, exceptionCase);
    }

}
