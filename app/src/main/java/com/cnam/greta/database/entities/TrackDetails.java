package com.cnam.greta.database.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class TrackDetails{

    @Embedded
    private Track track;
    @Relation(parentColumn = "trackId", entityColumn = "trackId", entity = WayPoint.class)
    private List<WayPoint> wayPoints;

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<WayPoint> wayPoint) {
        this.wayPoints = wayPoint;
    }

}
