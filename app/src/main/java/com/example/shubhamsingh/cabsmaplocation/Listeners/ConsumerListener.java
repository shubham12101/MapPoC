package com.example.shubhamsingh.cabsmaplocation.Listeners;

import com.example.shubhamsingh.cabsmaplocation.Objects.SimpleLatLngObject;

/**
 * Created by shubhamsingh on 30/06/16.
 */
public interface ConsumerListener {
    void onTakenFromQueue(SimpleLatLngObject latLngObject);
}
