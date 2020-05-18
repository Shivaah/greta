package com.cnam.greta.ui.map;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.cnam.greta.R;
import com.cnam.greta.model.LocationModel;
import com.cnam.greta.view.Altimeter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.content.Context.LOCATION_SERVICE;

public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private Altimeter mAltimeter;
    private DatabaseReference usersDatabaseReference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usersDatabaseReference = FirebaseDatabase.getInstance()
                .getReference(getString(R.string.firebase_child_data))
                .child(getString(R.string.firebase_child_users));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = rootView.findViewById(R.id.map);
        mAltimeter = rootView.findViewById(R.id.altimeter);

        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        usersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
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

    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        if (ContextCompat.checkSelfPermission( requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            mGoogleMap.setMyLocationEnabled(true);
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(LOCATION_SERVICE);
            if(locationManager != null){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mAltimeter.setAltitude((float) location.getAltitude());
        usersDatabaseReference.child(Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID))
            .setValue(new LocationModel(
                "Paul",
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude()
            ));
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
}