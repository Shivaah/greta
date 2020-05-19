package com.cnam.greta.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.cnam.greta.database.entities.WayPoint;

import java.util.List;

@Dao
public interface WayPointDao extends BaseDao<WayPoint> {

    @Query("SELECT * FROM WayPoint")
    LiveData<List<WayPoint>> fetchAllWayPoints();

    @Query("SELECT * FROM WayPoint WHERE wayPointId =:id")
    LiveData<WayPoint> getWayPoint(long id);

}
