package com.cnam.greta.models;

public class UserPosition {

    private String username;
    private double latitude;
    private double longitude;
    private double altitude;

    public UserPosition(String username, double latitude, double longitude, double altitude) {
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public String getUsername() {
        return username;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }
}
