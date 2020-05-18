package com.cnam.greta.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    Long insertLocation(LocationEntity locationEntity);


    @Query("SELECT * FROM LocationEntity")
    LiveData<List<LocationEntity>> fetchAllLocation();


    @Query("SELECT * FROM LocationEntity WHERE id =:id")
    LiveData<LocationEntity> getLocation(int id);


    @Update
    void updateLocation(LocationEntity locationEntity);


    @Delete
    void deleteLocation(LocationEntity locationEntity);
}