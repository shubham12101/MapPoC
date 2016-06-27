package com.example.shubhamsingh.cabsmaplocation.Objects;

/**
 * Created by shubhamsingh on 17/06/16.
 */
public class SimpleLatLngObject {

    private double latitude;
    private double longitude;

    public SimpleLatLngObject(long latitude, long longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "SimpleLatLngObject{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }
}
