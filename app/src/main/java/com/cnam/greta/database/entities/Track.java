package com.cnam.greta.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Track{

    @PrimaryKey(autoGenerate = true)
    private long trackId;
    private String trackName;

    public long getTrackId() {
        return trackId;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

}
