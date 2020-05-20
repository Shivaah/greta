package com.cnam.greta.data.entities;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity
public class WayPoint{

    @PrimaryKey(autoGenerate = true)
    private int wayPointId;
    @ForeignKey(entity = Track.class, parentColumns = "trackId", childColumns = "wayPointId", onDelete = CASCADE)
    private long trackId;
    private double latitude;
    private double longitude;
    private double altitude;
    private long time;

    public int getWayPointId() {
        return wayPointId;
    }

    public void setWayPointId(int wayPointId) {
        this.wayPointId = wayPointId;
    }

    public long getTrackId() {
        return trackId;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

}