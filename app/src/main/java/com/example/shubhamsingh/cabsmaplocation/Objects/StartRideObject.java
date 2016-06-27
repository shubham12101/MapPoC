package com.example.shubhamsingh.cabsmaplocation.Objects;

/**
 * Created by shubhamsingh on 17/06/16.
 */
public class StartRideObject {

    private String status;
    private String ride_id;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRide_id() {
        return ride_id;
    }

    @Override
    public String toString() {
        return "StartRideObject{" +
                "status='" + status + '\'' +
                ", ride_id='" + ride_id + '\'' +
                '}';
    }

    public void setRide_id(String ride_id) {
        this.ride_id = ride_id;
    }
}
