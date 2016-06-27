package com.example.shubhamsingh.cabsmaplocation.Objects;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by shubhamsingh on 27/06/16.
 */
public class AnimationObject {

    private LatLng latLng;
    private long duration;

    public AnimationObject(LatLng latLngObject, long duration) {
        this.latLng = latLngObject;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "AnimationObject{" +
                "latLng=" + latLng +
                ", duration=" + duration +
                '}';
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
