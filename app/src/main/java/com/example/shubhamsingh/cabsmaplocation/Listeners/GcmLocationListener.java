package com.example.shubhamsingh.cabsmaplocation.Listeners;

import com.example.shubhamsingh.cabsmaplocation.Objects.SimpleLatLngObject;

/**
 * Created by shubhamsingh on 23/06/16.
 */
public interface GcmLocationListener {
    void onLocationReceived(SimpleLatLngObject location);
}
