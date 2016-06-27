package com.example.shubhamsingh.cabsmaplocation.Objects;

import java.util.Arrays;

/**
 * Created by shubhamsingh on 25/06/16.
 */
public class SnapToRoadsObject {
    private SnappedPointObject[] snappedPoints;

    @Override
    public String toString() {
        return "SnapToRoadsObject{" +
                "snappedPoints=" + Arrays.toString(snappedPoints) +
                '}';
    }

    public SnappedPointObject[] getSnappedPoints() {
        return snappedPoints;
    }

    public void setSnappedPoints(SnappedPointObject[] snappedPoints) {
        this.snappedPoints = snappedPoints;
    }
}
