package com.cnam.greta.database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;


public class LocationRepository {
    private String DB_NAME = "location_db";
    private LocationDatabase myDataBase;

    public LocationRepository(Context context) {
        myDataBase = LocationDatabase.getDatabase(context);
    }


    public void insertLocation(final LocationEntity location) {
        LocationDatabase.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                myDataBase.locationDao().insertLocation(location);
            }
        });
    }

    public void updateLocation(final LocationEntity location) {
        LocationDatabase.databaseWriteExecutor.execute(new Runnable() {
            @Override
            public void run() {
                myDataBase.locationDao().updateLocation(location);
            }
        });
    }

    public void deleteLocation(final int id) {
        final LiveData<LocationEntity> location = getLocation(id);
        deleteLocation(location.getValue());
    }

    public void deleteLocation(final LocationEntity location) {
        if (location != null) {
            LocationDatabase.databaseWriteExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    myDataBase.locationDao().deleteLocation(location);
                }
            });
        }
    }

    public LiveData<LocationEntity> getLocation(int id) {
        return myDataBase.locationDao().getLocation(id);
    }

    public LiveData<List<LocationEntity>> getLocations() {
        return myDataBase.locationDao().fetchAllLocation();
    }
}