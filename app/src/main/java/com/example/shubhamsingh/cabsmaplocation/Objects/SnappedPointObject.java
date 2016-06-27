package com.example.shubhamsingh.cabsmaplocation.Objects;

/**
 * Created by shubhamsingh on 24/06/16.
 */
public class SnappedPointObject {

    private SimpleLatLngObject location;
    private Integer originalIndex;
    private String placeId;

    public SimpleLatLngObject getLocation() {
        return location;
    }

    public void setLocation(SimpleLatLngObject location) {
        this.location = location;
    }

    public Integer getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(Integer originalIndex) {
        this.originalIndex = originalIndex;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    @Override
    public String toString() {
        return "SnappedPointObject{" +
                "location=" + location +
                ", originalIndex=" + originalIndex +
                ", placeId='" + placeId + '\'' +
                '}';
    }
}
