package com.example.shubhamsingh.cabsmaplocation.Commons;

/**
 * Created by shubhamsingh on 16/06/16.
 */
public class Constants {

    public static String SERVER_URL = "http://172.16.1.158:33003/";
    public static String START_RIDE_URL = SERVER_URL+"start_ride";
    public static String CAB_LOCATION_URL = SERVER_URL+"get_current_location/";
    public static int LOCATION_PERMISSION_CODE = 1;
    public static int CAB_LOCATION_REQUEST_TIME_INTERVAL = 1000 * 5;
    public static String GCM_TOKEN_KEY = "appGcmToken";
    public static String GCM_LOCATION_EVENT = "gcmLocationReceived";
    public static String SNAP_ROADS_API_URL = "https://roads.googleapis.com/v1/snapToRoads?";
}
