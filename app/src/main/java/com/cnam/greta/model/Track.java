package com.cnam.greta.model;

public class Track {

    private String name;
    private float distance;
    private long duration;
    private long waypoints;

    public Track(String name, float distance, long duration, long waypoints) {
        this.name = name;
        this.distance = distance;
        this.duration = duration;
        this.waypoints = waypoints;
    }

    public String getName() {
        return name;
    }

    public float getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public long getWaypoints() {
        return waypoints;
    }
}
