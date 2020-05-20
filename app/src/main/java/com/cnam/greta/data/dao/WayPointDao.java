package com.cnam.greta.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.cnam.greta.data.entities.WayPoint;

import java.util.List;

@Dao
public interface WayPointDao extends BaseDao<WayPoint> {

    @Query("SELECT * FROM WayPoint")
    LiveData<List<WayPoint>> fetchAllWayPoints();

    @Query("SELECT * FROM WayPoint WHERE wayPointId =:id")
    LiveData<WayPoint> getWayPoint(long id);

}
