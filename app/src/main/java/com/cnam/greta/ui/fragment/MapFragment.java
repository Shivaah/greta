package com.cnam.greta.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.cnam.greta.R;
import com.cnam.greta.database.entities.Track;
import com.cnam.greta.database.entities.TrackDetails;
import com.cnam.greta.database.entities.WayPoint;
import com.cnam.greta.database.repositories.TrackRepository;
import com.cnam.greta.services.LocationService;
import com.cnam.greta.ui.CameraActivity;
import com.cnam.greta.views.AltimeterView;
import com.cnam.greta.views.richmaps.RichLayer;
import com.cnam.greta.views.richmaps.RichPoint;
import com.cnam.greta.views.richmaps.RichPolylineOptions;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {

    //Google Map
    private GoogleMap mGoogleMap;

    //Firebase
    private DatabaseReference usersDatabaseReference;

    //Views
    private RichLayer richLayer;
    private View rootView;
    private MapView mMapView;
    private AltimeterView mAltimeterView;
    private View trackButton;
    private View cameraButton;

    //Service
    private LocationService locationService;

    //Data
    private HashMap<String, Marker> markers = new HashMap<>();
    private LiveData<TrackDetails> currentTrackDetails;

    private final Observer<TrackDetails> trackDetailsObserver = new Observer<TrackDetails>() {
        @Override
        public void onChanged(TrackDetails trackDetails) {
            //Update the UI
            if(trackDetails.getWayPoints().size() != 0){
                List<WayPoint> wayPoints = trackDetails.getWayPoints();
                RichPolylineOptions polylineOpts = new RichPolylineOptions(null)
                        .zIndex(3)
                        .strokeWidth(15)
                        .strokeColor(Color.GRAY)
                        .linearGradient(true);
                for (WayPoint wayPoint : wayPoints){
                    float altitudeRatio = (float) (wayPoint.getAltitude() / 4000);
                    int color = Color.HSVToColor(255, new float[]{altitudeRatio * 360, 1.0f, 0.8f});
                    polylineOpts.add(new RichPoint(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude())).color(color));
                }
                WayPoint lastWayPoint = trackDetails.getWayPoints().get(trackDetails.getWayPoints().size() - 1);
                richLayer.addShape(polylineOpts.build());
                if(mGoogleMap.getUiSettings().isMyLocationButtonEnabled()){
                    CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(lastWayPoint.getLatitude(), lastWayPoint.getLongitude()));
                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
                    mGoogleMap.moveCamera(center);
                    mGoogleMap.animateCamera(zoom);
                }
                mAltimeterView.setAltitude((float) lastWayPoint.getAltitude());
            }
        }
    };

    /**
     * Callback for Firebase data changes on "users" child
     */
    private final ValueEventListener usersListener = new ValueEventListener() {
        @SuppressLint("HardwareIds")
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if(dataSnapshot.getValue() != null){
                HashMap<String, HashMap<String, Object>> users = (HashMap<String, HashMap<String, Object>>) dataSnapshot.getValue();
                for (Map.Entry<String, HashMap<String, Object>> user : users.entrySet()){
                    if(!user.getKey().equals(Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID))){
                        LatLng position = new LatLng((double) user.getValue().get("latitude"), (double) user.getValue().get("longitude"));
                        String username = (String) user.getValue().get("username");
                        Marker marker = markers.get(user.getKey());
                        if(marker == null){
                            marker = mGoogleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(username)
                            );
                        } else {
                            marker.setPosition(position);
                            marker.setTitle(username);
                        }
                        markers.put(user.getKey(), marker);
                    }
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
            mGoogleMap.setOnCameraIdleListener(mOnCameraChangeListener);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            richLayer = new RichLayer.Builder(mMapView, mGoogleMap).build();
        }
    };

    private final LocationService.TrackListener trackListener = new LocationService.TrackListener() {
        @Override
        public void onTrackReady(Track track) {
            currentTrackDetails = new TrackRepository(requireContext()).getTrackDetails(track.getTrackId());
            currentTrackDetails.observe(getViewLifecycleOwner(), trackDetailsObserver);
        }
    };

    private final GoogleMap.OnCameraIdleListener mOnCameraChangeListener = new GoogleMap.OnCameraIdleListener() {
        @Override
        public void onCameraIdle() {
            if(richLayer != null){
                richLayer.refresh();
            }
        }
    };

    private View.OnClickListener trackButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(locationService != null){
                if(locationService.isTracking()){
                    locationService.stopTracking();
                } else {
                    locationService.startTracking();
                }
                updateTrackingButton(locationService.isTracking());
            }
        }
    };

    private View.OnClickListener cameraButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), CameraActivity.class);
            startActivity(intent);
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
            locationService.setTrackListener(trackListener);
            updateTrackingButton(locationService.isTracking());
        }

        public void onServiceDisconnected(ComponentName className) {
            currentTrackDetails.removeObserver(trackDetailsObserver);
            locationService.removeTrackListener();
            updateTrackingButton(locationService.isTracking());
            locationService = null;
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
            mAltimeterView = rootView.findViewById(R.id.altimeter);

            mMapView.onCreate(savedInstanceState);
            mMapView.getMapAsync(mapReadyCallback);

            trackButton = rootView.findViewById(R.id.track_buttton);
            trackButton.setOnClickListener(trackButtonClickListener);
            cameraButton = rootView.findViewById(R.id.camera_button);
            cameraButton.setOnClickListener(cameraButtonClickListener);

            if(locationService != null){
                updateTrackingButton(locationService.isTracking());
            }
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        usersDatabaseReference.addValueEventListener(usersListener);
        Intent intent = new Intent(requireActivity(), LocationService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        usersDatabaseReference.removeEventListener(usersListener);
        requireActivity().unbindService(serviceConnection);
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

    private void updateTrackingButton(boolean isTracking){
        if(isTracking){
            ((ImageView) rootView.findViewById(R.id.track_buttton_image)).setImageDrawable(requireContext().getDrawable(R.drawable.ic_stop_black_24dp));
            ((TextView) rootView.findViewById(R.id.track_buttton_text)).setText(requireContext().getString(R.string.stop_tracking));
        } else {
            ((ImageView) rootView.findViewById(R.id.track_buttton_image)).setImageDrawable(requireContext().getDrawable(R.drawable.ic_my_location_black_24dp));
            ((TextView) rootView.findViewById(R.id.track_buttton_text)).setText(requireContext().getString(R.string.start_tracking));
        }
    }
}