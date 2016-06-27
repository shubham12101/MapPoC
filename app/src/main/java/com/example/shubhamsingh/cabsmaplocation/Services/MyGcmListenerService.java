package com.example.shubhamsingh.cabsmaplocation.Services;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.example.shubhamsingh.cabsmaplocation.Commons.Constants;
import com.google.android.gms.gcm.GcmListenerService;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = MyGcmListenerService.class.getSimpleName();


    @Override
    public void onMessageReceived(String from, Bundle data) {
        String location = data.getString("location");
//        SimpleLatLngObject locationObject = CommonUtils.getGsonFromString(location,
//                SimpleLatLngObject.class);
//        Log.d(TAG, "From: " + from);
//        Log.d(TAG, "Message: " + locationObject.toString());

        Intent intent = new Intent(Constants.GCM_LOCATION_EVENT);
        // You can also include some extra data.
        intent.putExtra("location", location);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);


        /*TODO: Notification payload:
        https://developers.google.com/cloud-messaging/downstream#sample-receive*/
    }
}
