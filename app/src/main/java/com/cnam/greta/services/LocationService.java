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
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@SuppressLint("HardwareIds")
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

    private List<LocationListener> locationListeners = new ArrayList<>();
    private List<TrackListener> trackListeners = new ArrayList<>();

    private int wayPointIndex;

    private final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(final Location location) {

            WayPoint wayPoint = new WayPoint();
            wayPoint.setAltitude(location.getAltitude());
            wayPoint.setLatitude(location.getLatitude());
            wayPoint.setLongitude(location.getLongitude());
            wayPoint.setTime(System.currentTimeMillis());
            wayPoint.setTrackId(mTrack.getTrackId());

            usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                    .child(String.valueOf(wayPointIndex))
                    .setValue(wayPoint);

            wayPointIndex++;

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

            for (LocationListener locationListener : locationListeners){
                if(locationListener != null){
                    locationListener.onLocationChanged(location);
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            for (LocationListener locationListener : locationListeners){
                if(locationListener != null){
                    locationListener.onStatusChanged(provider, status, extras);
                }
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            for (LocationListener locationListener : locationListeners){
                if(locationListener != null){
                    locationListener.onProviderEnabled(provider);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            for (LocationListener locationListener : locationListeners){
                if(locationListener != null){
                    locationListener.onProviderDisabled(provider);
                }
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if(key.equals(getString(R.string.username_key))){
                setUsername();
            } else {
                setLocationManagerListener();
            }
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
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopTracking();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
                usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                        .removeValue();
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
        mTrack.setOwnerName(sharedPreferences.getString(getString(R.string.username_key), getString(R.string.username_title)));
        final LiveData<Long> trackId = trackRepository.insert(mTrack);
        trackId.observeForever(new Observer<Long>() {
            @Override
            public void onChanged(Long id) {
                mTrack.setTrackId(id);
                for (TrackListener trackListener : trackListeners){
                    if(trackListener != null){
                        trackListener.onStartTracking(id);
                    }
                }
                trackId.removeObserver(this);
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

    private void setUsername(){
        usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))
                .child(getString(R.string.username_key))
                .setValue(sharedPreferences.getString(getString(R.string.username_key), getString(R.string.username_key)));
    }

    public void startTracking() {
        try {
            initializeLocationManager();
            setLocationManagerListener();
            initializeTrack();
            isTracking = true;
            wayPointIndex = 0;
            setUsername();
        } catch (Exception e){
          e.printStackTrace();
        }
    }

    public void stopTracking() {
        isTracking = false;
        usersDatabaseReference.child(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)).removeValue();
        mLocationManager = null;
        for (TrackListener trackListener : trackListeners){
            if(trackListener != null && mTrack != null){
                trackListener.onStopTracking(mTrack.getTrackId());
            }
        }
        onDestroy();
    }

    public boolean isTracking() {
        return isTracking;
    }

    public void addLocationListener(LocationListener listener){
        locationListeners.add(listener);
    }

    public void removeLocationListener(LocationListener listener) {
        locationListeners.remove(listener);
    }

    public void addTrackListener(TrackListener listener){
        trackListeners.add(listener);
        if (isTracking){
            listener.onStartTracking(mTrack.getTrackId());
        }
    }

    public void removeTrackListener(TrackListener listener) {
        trackListeners.remove(listener);
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    public interface TrackListener{
        void onStartTracking(long trackId);
        void onStopTracking(long trackId);
    }

}