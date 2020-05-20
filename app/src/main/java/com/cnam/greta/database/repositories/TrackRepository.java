package com.cnam.greta.database.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cnam.greta.database.GretaDatabase;
import com.cnam.greta.database.entities.Track;
import com.cnam.greta.database.entities.TrackDetails;

import java.util.List;

public class TrackRepository implements BaseRepository<Track> {

    private GretaDatabase gretaDatabase;

    public TrackRepository(Context context) {
        gretaDatabase = GretaDatabase.getDatabase(context);
    }

    @Override
    public LiveData<Long> insert(final Track track) {
        final MutableLiveData<Long> result = new MutableLiveData<>();
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                result.postValue(gretaDatabase.trackDao().insert(track));
            }
        });
        return result;
    }

    @Override
    public LiveData<Long[]> insert(final List<Track> tracks) {
        final MutableLiveData<Long[]> result = new MutableLiveData<>();
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                result.postValue(gretaDatabase.trackDao().insert(tracks));
            }
        });
        return result;
    }

    @Override
    public void update(final Track track) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.trackDao().update(track);
            }
        });
    }

    @Override
    public void update(final List<Track> tracks) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.trackDao().update(tracks);
            }
        });
    }

    @Override
    public void delete(final long id) {
        final LiveData<Track> track = get(id);
        delete(track.getValue());
    }

    @Override
    public void delete(final Track track) {
        if (track != null) {
            GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    gretaDatabase.trackDao().delete(track);
                }
            });
        }
    }

    @Override
    public void delete(final List<Track> tracks) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.trackDao().delete(tracks);
            }
        });
    }

    @Override
    public LiveData<Track> get(long id) {
        return gretaDatabase.trackDao().getTrack(id);
    }

    @Override
    public LiveData<List<Track>> get() {
        return gretaDatabase.trackDao().fetchAllTracks();
    }

    public LiveData<TrackDetails> getTrackDetails(long id) {
        return gretaDatabase.trackDao().getTrackDetails(id);
    }

    public LiveData<List<TrackDetails>> getAllTrackDetails() {
        return gretaDatabase.trackDao().fetchAllTrackDetails();
    }

}
