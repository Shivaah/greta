package com.cnam.greta.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.cnam.greta.data.entities.Track;
import com.cnam.greta.data.entities.TrackDetails;

import java.util.List;

@Dao
public interface TrackDao extends BaseDao<Track> {

    @Query("SELECT * FROM Track")
    LiveData<List<Track>> fetchAllTracks();

    @Query("SELECT * FROM Track WHERE trackId =:id")
    LiveData<Track> getTrack(long id);

    @Transaction
    @Query("SELECT * FROM Track ORDER BY trackId DESC")
    LiveData<List<TrackDetails>> fetchAllTrackDetails();

    @Transaction
    @Query("SELECT * FROM Track WHERE trackId =:id")
    LiveData<TrackDetails> getTrackDetails(long id);

}