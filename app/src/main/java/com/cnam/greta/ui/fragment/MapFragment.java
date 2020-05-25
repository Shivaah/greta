package com.cnam.greta.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.cnam.greta.R;
import com.cnam.greta.data.FirebaseConstants;
import com.cnam.greta.models.SharedWayPoint;
import com.cnam.greta.data.entities.Track;
import com.cnam.greta.data.entities.TrackDetails;
import com.cnam.greta.data.entities.WayPoint;
import com.cnam.greta.services.LocationService;
import com.cnam.greta.ui.CameraActivity;
import com.cnam.greta.views.AltimeterView;
import com.cnam.greta.views.richmaps.RichLayer;
import com.cnam.greta.views.richmaps.RichPoint;
import com.cnam.greta.views.richmaps.RichPolylineOptions;
import com.cnam.greta.views.richmaps.RichShape;
import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapFragment extends Fragment {

    //Google Map
    private GoogleMap mGoogleMap;

    //Firebase
    private DatabaseReference usersDatabaseReference;
    private DatabaseReference wayPointDatabaseReference;

    //Views
    private RichLayer richLayer;
    private View rootView;
    private MapView mMapView;
    private AltimeterView mAltimeterView;
    private MapScaleView scaleView;
    private View trackButton;
    private View cameraButton;
    private View deleteMarkerButton;

    //Service
    private LocationService locationService;

    //Data
    private HashMap<String, Marker> markers = new HashMap<>();
    private HashMap<Integer, Marker> sharedWayPoints = new HashMap<>();
    private HashMap<String, TrackDetails> usersTrackDetailList = new HashMap<>();
    private HashMap<String, RichShape> usersPolylines = new HashMap<>();
    private Marker selectedMarker;
    private String hardwareId;

    /**
     * Callback for Firebase data changes on "users" childs
     */
    private final ChildEventListener usersListener = new ChildEventListener() {
        @SuppressLint("HardwareIds")
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey() != null){
                //Create new user track
                TrackDetails trackDetails = new TrackDetails();
                trackDetails.setTrack(new Track());
                trackDetails.setWayPoints(new ArrayList<WayPoint>());
                usersTrackDetailList.put(dataSnapshot.getKey(), trackDetails);
                //Attach listener to child
                usersDatabaseReference.child(dataSnapshot.getKey())
                        .addChildEventListener(singleUserChildEventListener);
                //Create Polyline for drawing user path
                richLayer.addShape(usersPolylines.put(dataSnapshot.getKey(),
                        new RichPolylineOptions(null)
                            .zIndex(3)
                            .strokeWidth(15)
                            .strokeColor(Color.BLACK)
                            .linearGradient(true)
                            .build()
                ));
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            //Ignore
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            if(dataSnapshot.getKey() != null){
                usersTrackDetailList.remove(dataSnapshot.getKey());
                usersDatabaseReference.child(dataSnapshot.getKey()).removeEventListener(singleUserChildEventListener);
                richLayer.removeShape(usersPolylines.remove(dataSnapshot.getKey()));
                richLayer.refresh();
                if(markers.get(dataSnapshot.getKey()) != null){
                    Objects.requireNonNull(markers.get(dataSnapshot.getKey())).remove();
                    markers.remove(dataSnapshot.getKey());
                }
            }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            //Ignore
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w(this.getClass().getSimpleName(), databaseError.getMessage());
        }
    };

    private final ChildEventListener singleUserChildEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if (dataSnapshot.getKey() != null){
                String id = Objects.requireNonNull(dataSnapshot.getRef().getParent()).getKey();
                TrackDetails trackDetails = usersTrackDetailList.get(id);
                switch (dataSnapshot.getKey()){

                    case FirebaseConstants.USERNAME:
                        Objects.requireNonNull(trackDetails).getTrack().setOwnerName((String) dataSnapshot.getValue());
                        if(markers.get(id) != null){
                            Objects.requireNonNull(markers.get(id)).setTitle((String) dataSnapshot.getValue());
                        }
                        break;

                    default:
                        WayPoint wayPoint = dataSnapshot.getValue(WayPoint.class);

                        Objects.requireNonNull(trackDetails).getWayPoints().add(Integer.parseInt(dataSnapshot.getKey()), wayPoint);

                        float altitudeRatio = (float) (wayPoint.getAltitude() / 4000);
                        int color = Color.HSVToColor(255, new float[]{altitudeRatio * 360, 1.0f, 0.8f});
                        Objects.requireNonNull(usersPolylines.get(id)).add(Integer.parseInt(dataSnapshot.getKey()), new RichPoint(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude())).color(color));
                        richLayer.addShape(Objects.requireNonNull(usersPolylines.get(id)));
                        richLayer.refresh();
                        //If last known position, update the marker
                        if(!id.equals(hardwareId) &&  trackDetails.getWayPoints().size() - 1 == Integer.parseInt(dataSnapshot.getKey())){
                            if(markers.get(id) != null){
                                Objects.requireNonNull(markers.get(id)).setPosition(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude()));
                            } else {
                                markers.put(id, mGoogleMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude()))
                                        .title(trackDetails.getTrack().getOwnerName())
                                ));
                            }
                        }

                        break;
                }
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if (dataSnapshot.getKey() != null && dataSnapshot.getKey().equals(FirebaseConstants.USERNAME)) {
                String id = Objects.requireNonNull(dataSnapshot.getRef().getParent()).getKey();
                TrackDetails trackDetails = usersTrackDetailList.get(id);
                Objects.requireNonNull(trackDetails).getTrack().setOwnerName((String) dataSnapshot.getValue());
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            //Ignore
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            //Ignore
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.w(this.getClass().getSimpleName(), databaseError.getMessage());
        }
    };

    /**
     * Callback for Firebase data changes on "users" childs
     */
    private final ChildEventListener sharedWaypointsListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey() != null){
                HashMap<String, Object> sharedWaypoint = (HashMap<String, Object>) dataSnapshot.getValue();
                if(sharedWaypoint != null){
                    sharedWayPoints.put(Integer.valueOf(dataSnapshot.getKey()), mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(
                                    (double) sharedWaypoint.get(FirebaseConstants.LATITUDE),
                                    (double) sharedWaypoint.get(FirebaseConstants.LONGITUDE)
                            ))
                            .title((String) sharedWaypoint.get(FirebaseConstants.NAME))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    ));
                }
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Marker marker = sharedWayPoints.get(dataSnapshot.getKey());
            HashMap<String, Object> sharedWayPoint = (HashMap<String, Object>) dataSnapshot.getValue();
            if(sharedWayPoint != null && marker != null){
                marker.setTitle((String) sharedWayPoint.get(FirebaseConstants.USERNAME));
                marker.setPosition(new LatLng(
                        (double) sharedWayPoint.get(FirebaseConstants.LATITUDE),
                        (double) sharedWayPoint.get(FirebaseConstants.LONGITUDE)
                ));
                sharedWayPoints.put(Integer.valueOf(dataSnapshot.getKey()), marker);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            Marker marker = sharedWayPoints.get(Integer.valueOf(dataSnapshot.getKey()));
            if(marker != null){
                marker.remove();
                sharedWayPoints.remove(Integer.valueOf(dataSnapshot.getKey()));
            }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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
            mGoogleMap.setOnMapLongClickListener(mapLongClickListener);
            mGoogleMap.setOnCameraIdleListener(mOnCameraChangeListener);
            mGoogleMap.setOnCameraMoveListener(mOnCameraMoveListener);
            mGoogleMap.setOnMarkerClickListener(markerClickListener);
            mGoogleMap.setOnInfoWindowCloseListener(onInfoWindowCloseListener);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

            usersDatabaseReference.addChildEventListener(usersListener);
            wayPointDatabaseReference.addChildEventListener(sharedWaypointsListener);

            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            richLayer = new RichLayer.Builder(mMapView, mGoogleMap).build();
        }
    };

    /**
     * On track fully initialized
     */
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(mGoogleMap != null && mGoogleMap.getUiSettings().isMyLocationButtonEnabled()){
                CameraUpdate center= CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
                mGoogleMap.moveCamera(center);
                mGoogleMap.animateCamera(zoom);
            }
            mAltimeterView.setAltitude((float) location.getAltitude());
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

    /**
     * Google Maps camera not moving
     */
    private final GoogleMap.OnCameraIdleListener mOnCameraChangeListener = new GoogleMap.OnCameraIdleListener() {
        @Override
        public void onCameraIdle() {
            CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
            scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
            if(richLayer != null){
                richLayer.refresh();
            }
        }
    };

    /**
     * Google Maps camera is moving
     */
    private final GoogleMap.OnCameraMoveListener mOnCameraMoveListener = new GoogleMap.OnCameraMoveListener() {
        @Override
        public void onCameraMove() {
            CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
            scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
        }
    };

    /**
     * Start / stop tracking button
     */
    private final View.OnClickListener trackButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(locationService != null){
                if(locationService.isTracking()){
                    locationService.stopTracking();
                } else {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    String userName = sharedPreferences.getString(getString(R.string.username_key), "");
                    if(userName.isEmpty()){
                        userNameDialog(getContext(), userName);
                    } else {
                        locationService.startTracking();
                    }
                }
                updateTrackingButton(locationService.isTracking());
            }
        }
    };

    /**
     * Camera button
     */
    private final View.OnClickListener cameraButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getContext(), CameraActivity.class);
            startActivity(intent);
        }
    };

    /**
     * Delete marker button
     */
    private final View.OnClickListener deleteMarkerButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(selectedMarker != null){
                int index = getMarkerIndex(selectedMarker);
                if(index != -1){
                    confirmDeleteMarker(getContext(), getMarkerIndex(selectedMarker));
                }
            }
        }
    };

    /**
     * Google Maps long click
     */
    private final GoogleMap.OnMapLongClickListener mapLongClickListener = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            markerDialog(getContext(), latLng);
        }
    };

    /**
     * Google Maps Marker click listener
     */
    private final GoogleMap.OnMarkerClickListener markerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            selectedMarker = marker;
            int index = getMarkerIndex(marker);
            if(index != -1){
                deleteMarkerButton.setVisibility(View.VISIBLE);
            }
            return false;
        }
    };

    /**
     * Marker's window is closed
     */
    private final GoogleMap.OnInfoWindowCloseListener onInfoWindowCloseListener = new GoogleMap.OnInfoWindowCloseListener() {
        @Override
        public void onInfoWindowClose(Marker marker) {
            int index = getMarkerIndex(marker);
            if(index != -1){
                deleteMarkerButton.setVisibility(View.GONE);
            }
            selectedMarker = null;
        }
    };

    /**
     * Service connection callback
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
            locationService.addLocationListener(locationListener);
            updateTrackingButton(locationService.isTracking());
        }

        public void onServiceDisconnected(ComponentName className) {
            locationService.removeLocationListener(locationListener);
            updateTrackingButton(locationService.isTracking());
            locationService = null;
        }
    };

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.initialize(requireContext().getApplicationContext());
        usersDatabaseReference = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.DATA)
                .child(FirebaseConstants.USERS);

        wayPointDatabaseReference = FirebaseDatabase.getInstance()
                .getReference(FirebaseConstants.DATA)
                .child(FirebaseConstants.SHARED_WAYPOINTS);

        hardwareId = Settings.Secure.getString(requireActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
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

            deleteMarkerButton = rootView.findViewById(R.id.delete_marker_button);
            deleteMarkerButton.setOnClickListener(deleteMarkerButtonClickListener);

            scaleView = rootView.findViewById(R.id.scaleView);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String unit = sharedPreferences.getString(getString(R.string.measure_key), getString(R.string.measure_default));
            if(unit.equals(getString(R.string.measure_default))){
                scaleView.metersOnly();
            } else {
                scaleView.milesOnly();
            }

            if(locationService != null){
                updateTrackingButton(locationService.isTracking());
            }
        }
        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMapView != null){
            mMapView.onResume();
        }
        Intent intent = new Intent(requireActivity(), LocationService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mMapView != null){
            mMapView.onPause();
        }
        usersDatabaseReference.removeEventListener(usersListener);
        usersDatabaseReference.removeEventListener(sharedWaypointsListener);
        requireActivity().unbindService(serviceConnection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mMapView != null){
            mMapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if(mMapView != null){
            mMapView.onLowMemory();
        }
    }

    private void updateTrackingButton(boolean isTracking){
        if(isTracking){
            ((TextView) rootView.findViewById(R.id.track_buttton_text)).setText(requireContext().getString(R.string.stop_tracking));
            cameraButton.setVisibility(View.VISIBLE);
        } else {
            ((TextView) rootView.findViewById(R.id.track_buttton_text)).setText(requireContext().getString(R.string.start_tracking));
            cameraButton.setVisibility(View.GONE);
        }
    }

    private void userNameDialog(final Context context, String text){
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCancelable(true);
        alertDialog.setTitle(getString(R.string.enter_username));
        final EditText editText = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd(48);
        lp.setMarginStart(48);
        editText.setLayoutParams(lp);
        editText.setText(text);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(16);
        editText.setFilters(filterArray);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!editText.getText().toString().isEmpty()){
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(getString(R.string.username_key), editText.getText().toString());
                    editor.apply();
                    locationService.startTracking();
                    updateTrackingButton(locationService.isTracking());
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.error)
                            .setMessage(R.string.invalid_username_error_message)
                            .show();
                }
            }
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        FrameLayout container = new FrameLayout(context);
        container.addView(editText);
        alertDialog.setView(container);
        alertDialog.show();
    }

    private void markerDialog(final Context context, final LatLng latLng){
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setCancelable(true);
        alertDialog.setTitle(getString(R.string.set_marker_name));
        final EditText editText = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd(48);
        lp.setMarginStart(48);
        editText.setLayoutParams(lp);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(32);
        editText.setFilters(filterArray);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!editText.getText().toString().isEmpty()){
                    int key = 0;
                    if(!sharedWayPoints.isEmpty()){
                        for (Map.Entry<Integer, Marker> entry : sharedWayPoints.entrySet()) {
                            if(key < entry.getKey()){
                                key = entry.getKey();
                            }
                        }
                        key++;
                    }
                    wayPointDatabaseReference.child(String.valueOf(key))
                            .setValue(new SharedWayPoint(
                                    editText.getText().toString(),
                                    latLng.latitude,
                                    latLng.longitude
                            ));
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.error)
                            .setMessage(R.string.invalid_marker_name)
                            .show();
                }
            }
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        FrameLayout container = new FrameLayout(context);
        container.addView(editText);
        alertDialog.setView(container);
        alertDialog.show();
    }

    private void confirmDeleteMarker(final Context context, final int sharedWaypointId){
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setMessage(context.getString(R.string.confirm_delete));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                wayPointDatabaseReference.child(String.valueOf(sharedWaypointId)).removeValue();
            }
        });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.setCancelable(true);
        alertDialog.show();
    }

    private int getMarkerIndex(Marker marker){
        for (Map.Entry<Integer, Marker> sharedWaypoint : sharedWayPoints.entrySet()){
            if(marker.getId().equals(sharedWaypoint.getValue().getId())){
                return sharedWaypoint.getKey();
            }
        }
        return -1;
    }
}