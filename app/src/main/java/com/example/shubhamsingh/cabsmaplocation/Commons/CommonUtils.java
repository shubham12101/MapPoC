package com.example.shubhamsingh.cabsmaplocation.Commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by shubhamsingh on 17/06/16.
 */
public class CommonUtils {

    private static String TAG = CommonUtils.class.getSimpleName();

    public static <T> T getGsonFromString (String jsonStr, Class<T> tClass){
        Gson gson = new Gson();
        T t = gson.fromJson(jsonStr, tClass);
        return t;
    }

    public static void writeToSharePref(Context context, String key, String value){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context);
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String readFromSharePref(Context context, String key){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context);
        return sharedPreferences.getString(key, "");
    }

    public static String formSnapRoadUrl(LatLng[] latLngs, boolean interpolate,
                                         String apiKey){
        StringBuilder pathBuilder = new StringBuilder();
        for (LatLng latLng: latLngs) {
            pathBuilder.append(Double.toString(latLng.latitude)+","+Double.toString(latLng
                    .longitude));
            pathBuilder.append("|");
        }
        pathBuilder.deleteCharAt(pathBuilder.length()-1);
        String path = pathBuilder.toString();
        Log.d(TAG, path);

        StringBuilder urlBuilder = new StringBuilder(Constants.SNAP_ROADS_API_URL);
        urlBuilder.append("path="+path);
        if(interpolate){
            urlBuilder.append("&interpolate=true");
        }
        urlBuilder.append("&key="+apiKey);
        return urlBuilder.toString();
    }
}
