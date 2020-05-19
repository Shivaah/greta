package com.cnam.greta.database.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cnam.greta.database.GretaDatabase;
import com.cnam.greta.database.entities.WayPoint;

import java.util.List;


public class WayPointRepository implements BaseRepository<WayPoint>{

    private GretaDatabase gretaDatabase;

    public WayPointRepository(Context context) {
        gretaDatabase = GretaDatabase.getDatabase(context);
    }

    @Override
    public LiveData<Long> insert(final WayPoint wayPoint) {
        final MutableLiveData<Long> result = new MutableLiveData<>();
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                result.postValue(gretaDatabase.wayPointDao().insert(wayPoint));
            }
        });
        return result;
    }

    @Override
    public LiveData<Long[]> insert(final List<WayPoint> wayPoints) {
        final MutableLiveData<Long[]> result = new MutableLiveData<>();
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                result.postValue(gretaDatabase.wayPointDao().insert(wayPoints));
            }
        });
        return result;
    }


    @Override
    public LiveData<WayPoint> get(long id) {
        return gretaDatabase.wayPointDao().getWayPoint(id);
    }

    @Override
    public LiveData<List<WayPoint>> get() {
        return gretaDatabase.wayPointDao().fetchAllWayPoints();
    }

    @Override
    public void update(final WayPoint wayPoint) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.wayPointDao().update(wayPoint);
            }
        });
    }

    @Override
    public void update(final List<WayPoint> wayPoints) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.wayPointDao().update(wayPoints);
            }
        });
    }

    @Override
    public void delete(long id) {
        final LiveData<WayPoint> location = get(id);
        delete(location.getValue());
    }

    @Override
    public void delete(final WayPoint wayPoint) {
        if (wayPoint != null) {
            GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    gretaDatabase.wayPointDao().delete(wayPoint);
                }
            });
        }
    }

    @Override
    public void delete(final List<WayPoint> wayPoints) {
        GretaDatabase.getDatabaseWriteExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gretaDatabase.wayPointDao().delete(wayPoints);
            }
        });
    }

}