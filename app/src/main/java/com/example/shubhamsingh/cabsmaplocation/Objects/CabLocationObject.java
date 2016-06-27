package com.example.shubhamsingh.cabsmaplocation.Objects;

/**
 * Created by shubhamsingh on 16/06/16.
 */
public class CabLocationObject {

    private String id;
    private boolean ride_ended;
    private SimpleLatLngObject current_location;

    public CabLocationObject(String id, boolean ride_ended, SimpleLatLngObject current_location) {
        this.id = id;
        this.ride_ended = ride_ended;
        this.current_location = current_location;
    }

    @Override
    public String toString() {
        return "CabLocationObject{" +
                "id='" + id + '\'' +
                ", ride_ended=" + ride_ended +
                ", current_location=" + current_location +
                '}';
    }

    public SimpleLatLngObject getCurrent_location() {
        return current_location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isRide_ended() {
        return ride_ended;
    }

    public void setRide_ended(boolean ride_ended) {
        this.ride_ended = ride_ended;
    }

    public SimpleLatLngObject getCabCurrentLocationObject() {
        return current_location;
    }

    public void setCabCurrentLocationObject(SimpleLatLngObject simpleLatLngObject) {
        this.current_location = simpleLatLngObject;
    }
}
