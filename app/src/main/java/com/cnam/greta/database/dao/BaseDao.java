package com.cnam.greta.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Update;

import java.util.List;

/**
 * Base for app Daos
 * @param <T> Entity object
 */

@Dao
public interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long insert(T object);

    @Insert
    Long[] insert(List<T> tracks);

    @Update
    void update(T object);

    @Update
    void update(List<T> tracks);

    @Delete
    void delete(T object);

    @Delete
    void delete(List<T> tracks);
}
