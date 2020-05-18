package com.cnam.greta.ui.map;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.cnam.greta.R;
import com.cnam.greta.model.LocationModel;
import com.cnam.greta.view.Altimeter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment{

    private View rootView;

    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private Altimeter mAltimeter;
    private DatabaseReference usersDatabaseReference;

    /**
     * Callback for Firebase data changes on "users" child
     */
    private final ValueEventListener usersListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() != null){
                mGoogleMap.clear();
                HashMap<String, HashMap<String, Object>> users = (HashMap<String, HashMap<String, Object>>) dataSnapshot.getValue();
                for (Map.Entry<String, HashMap<String, Object>> user : users.entrySet()){
                    LatLng userPosition = new LatLng((Double) user.getValue().get("latitude"), (Double) user.getValue().get("longitude"));
                    mGoogleMap.addMarker(new MarkerOptions().position(userPosition).title((String) user.getValue().get("username")));
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w(this.getClass().getSimpleName(), databaseError.getMessage());
        }
    };

    /**
     * Callback for Google Map initialization
     */
    private final OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap map) {
            mGoogleMap = map;
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    };

    /**
     * Callback for location changes
     */
    private final LocationListener locationListener = new LocationListener() {
        @SuppressLint("HardwareIds")
        @Override
        public void onLocationChanged(Location location) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            usersDatabaseReference.child(Settings.Secure.getString(requireActivity().getContentResolver(), Settings.Secure.ANDROID_ID))
                    .setValue(new LocationModel(
                            sharedPreferences.getString(requireContext().getString(R.string.username_key), getString(R.string.unknown_user)),
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAltitude()
                    ));

            CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(10);
            mAltimeter.setAltitude((float) location.getAltitude());
            mGoogleMap.moveCamera(center);
            mGoogleMap.animateCamera(zoom);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Deprecated
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(this.getClass().getSimpleName(), "Provider enabled : " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(this.getClass().getSimpleName(), "Provider disabled : " + provider);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersDatabaseReference = FirebaseDatabase.getInstance()
                .getReference(getString(R.string.firebase_child_data))
                .child(getString(R.string.firebase_child_users));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_map, container, false);

            mMapView = rootView.findViewById(R.id.map);
            mAltimeter = rootView.findViewById(R.id.altimeter);

            mMapView.onCreate(savedInstanceState);
            mMapView.getMapAsync(mapReadyCallback);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(LOCATION_SERVICE);
        if (locationManager != null && ContextCompat.checkSelfPermission( requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            int minMillis = Integer.parseInt(sharedPreferences.getString(
                    requireContext().getString(R.string.localisation_update_time_frequency_key),
                    requireContext().getString(R.string.localisation_update_time_frequency_default)
            ));
            int minDistance = Integer.parseInt(sharedPreferences.getString(
                    requireContext().getString(R.string.localisation_update_distance_frequency_key),
                    requireContext().getString(R.string.localisation_update_distance_frequency_default)
            ));
            String provider = sharedPreferences.getString(
                    requireContext().getString(R.string.localisation_provider_key),
                    requireContext().getString(R.string.localisation_providers_default)
            );
            locationManager.requestLocationUpdates(provider, minMillis, minDistance, locationListener);
        }
        usersDatabaseReference.addValueEventListener(usersListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(LOCATION_SERVICE);
        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
        usersDatabaseReference.removeEventListener(usersListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}