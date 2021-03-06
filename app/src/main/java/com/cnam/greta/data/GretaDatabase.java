package com.cnam.greta.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.cnam.greta.data.dao.TrackDao;
import com.cnam.greta.data.dao.WayPointDao;
import com.cnam.greta.data.entities.Track;
import com.cnam.greta.data.entities.WayPoint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local database
 */
@Database(entities = {Track.class, WayPoint.class}, version = 2, exportSchema = false)
public abstract class GretaDatabase extends RoomDatabase {

    public abstract TrackDao trackDao();
    public abstract WayPointDao wayPointDao();

    private static volatile GretaDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    private static final String DATABASE_NAME = "greta_database";
    private static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static GretaDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (GretaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), GretaDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration() //Allow destructive migration
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static ExecutorService getDatabaseWriteExecutor() {
        return databaseWriteExecutor;
    }
}
