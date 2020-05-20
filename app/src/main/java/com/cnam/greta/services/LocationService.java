package com.cnam.greta.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.cnam.greta.R;
import com.cnam.greta.data.FirebaseConstants;
import com.cnam.greta.data.entities.Track;
import com.cnam.greta.data.entities.WayPoint;
import com.cnam.greta.data.repositories.TrackRepository;
import com.cnam.greta.data.repositories.WayPointRepository;
import com.cnam.greta.data.entities.UserPosition;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationService extends Service {

    private final LocationServiceBinder binder = new LocationServiceBinder();

    private LocationManager mLocationManager;
    private WayPointRepository wayPointRepository;
    private TrackRepository trackRepository;
    private Track mTrack;
    private boolean isTracking = false;

    private DatabaseReference usersDatabaseReference;

    private SharedPreferences sharedPreferences;
    private boolean pedestrianMode;

    private TrackListener trackListener;

    private final LocationListener mLocationListener = new LocationListener() {

        @SuppressLint("HardwareIds")
        @Override
        public void onLocationChanged(Location location) {

            Location test = new Location(location);
            test.setLatitude(43.1237889);
            test.setLongitude(5.9552382);

            location.bearingTo(test);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                .setValue(new UserPosition(
                        sharedPreferences.getString(getString(R.string.username_key), getString(R.string.unknown_user)),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAltitude()
                ));

            WayPoint wayPoint = new WayPoint();
            wayPoint.setAltitude(location.getAltitude());
            wayPoint.setLatitude(location.getLatitude());
            wayPoint.setLongitude(location.getLongitude());
            wayPoint.setTime(System.currentTimeMillis());
            wayPoint.setTrackId(mTrack.getTrackId());
            wayPointRepository.insert(wayPoint);

            //Above 10kmh
            if(location.getSpeed() > 2.77f){
                if(pedestrianMode){
                    pedestrianMode = false;
                    setLocationManagerListener();
                }
            } else {
                if(!pedestrianMode){
                    pedestrianMode = true;
                    setLocationManagerListener();
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setLocationManagerListener();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        wayPointRepository = new WayPointRepository(getApplicationContext());
        usersDatabaseReference = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.DATA)
                .child(FirebaseConstants.USERS);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        pedestrianMode = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
                usersDatabaseReference = null;
                trackRepository = null;
                wayPointRepository = null;
            } catch (Exception ex) {
                Log.i(this.getClass().getSimpleName(), "fail to remove location listeners, ignore", ex);
            }
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void initializeTrack(){
        if(trackRepository == null){
            trackRepository = new TrackRepository(getApplicationContext());
        }
        mTrack = new Track();
        mTrack.setTrackName("Track : " + new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis())));
        final LiveData<Long> trackId = trackRepository.insert(mTrack);
        trackId.observeForever(new Observer<Long>() {
            @Override
            public void onChanged(Long id) {
                mTrack.setTrackId(id);
                trackId.removeObserver(this);
                if(trackListener != null){
                    trackListener.onTrackReady(mTrack);
                }
            }
        });
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void setLocationManagerListener(){
        try {
            if (mLocationManager != null && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                String provider;
                int minMillis;
                int minDistance;
                if(sharedPreferences.getBoolean(getString(R.string.localisation_automatic_key), true)){
                    Criteria criteria = new Criteria();
                    criteria.setAltitudeRequired(true);
                    provider = mLocationManager.getBestProvider(criteria, true);
                    minMillis = pedestrianMode ? 5000 : 1000;
                    minDistance = pedestrianMode ? 30 : 10;
                } else {
                    minMillis = Integer.parseInt(sharedPreferences.getString(
                            getString(R.string.localisation_update_time_frequency_key),
                            getString(R.string.localisation_update_time_frequency_default)
                    ));
                    minDistance = Integer.parseInt(sharedPreferences.getString(
                            getString(R.string.localisation_update_distance_frequency_key),
                            getString(R.string.localisation_update_distance_frequency_default)
                    ));
                    provider = sharedPreferences.getString(
                            getString(R.string.localisation_provider_key),
                            getString(R.string.localisation_providers_default)
                    );
                }
                mLocationManager.removeUpdates(mLocationListener);
                mLocationManager.requestLocationUpdates(provider == null ? LocationManager.NETWORK_PROVIDER : provider, minMillis, minDistance, mLocationListener);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startTracking() {
        try {
            initializeLocationManager();
            setLocationManagerListener();
            initializeTrack();
            isTracking = true;
        } catch (Exception e){
          e.printStackTrace();
        }
    }

    @SuppressLint("HardwareIds")
    public void stopTracking() {
        isTracking = false;
        usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)).removeValue();
        mLocationManager = null;
        this.onDestroy();
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void removeTrackListener() {
        trackListener = null;
    }

    public void setTrackListener(TrackListener trackListener) {
        this.trackListener = trackListener;
        if(mTrack != null && mTrack.getTrackId() != 0){
            trackListener.onTrackReady(mTrack);
        }
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    public interface TrackListener{
        void onTrackReady(Track track);
    }
}