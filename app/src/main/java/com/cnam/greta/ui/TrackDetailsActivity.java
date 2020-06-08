package com.cnam.greta.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.cnam.greta.R;
import com.cnam.greta.data.entities.TrackDetails;
import com.cnam.greta.data.entities.WayPoint;
import com.cnam.greta.data.repositories.TrackRepository;
import com.cnam.greta.views.richmaps.RichLayer;
import com.cnam.greta.views.richmaps.RichPoint;
import com.cnam.greta.views.richmaps.RichPolylineOptions;
import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Objects;

/**
 * Activité de suivi d'une piste existante dans notre historique.
 */
public class TrackDetailsActivity extends FragmentActivity{

    private GoogleMap mGoogleMap;
    private RichLayer richLayer;
    private MapScaleView scaleView;

    private LiveData<TrackDetails> trackDetails;

    private final Observer<TrackDetails> trackDetailsObserver = new Observer<TrackDetails>() {
        @Override
        public void onChanged(TrackDetails trackDetails) {
            //Update the UI
            if(trackDetails.getTrack() != null){
                Objects.requireNonNull(getActionBar()).setTitle(trackDetails.getTrack().getTrackName());
            }
            if(trackDetails.getWayPoints().size() != 0){
                mGoogleMap.clear();
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
                WayPoint start = trackDetails.getWayPoints().get(0);
                LatLng startPosition = new LatLng(start.getLatitude(), start.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions()
                        .position(startPosition)
                        .title(getString(R.string.start))
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                );
                WayPoint end = trackDetails.getWayPoints().get(trackDetails.getWayPoints().size() - 1);
                LatLng endPosition = new LatLng(end.getLatitude(), end.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions()
                        .position(endPosition)
                        .title(getString(R.string.end))
                        .icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                );
                richLayer.addShape(polylineOpts.build());
                CameraUpdate center= CameraUpdateFactory.newLatLng(endPosition);
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
                mGoogleMap.moveCamera(center);
                mGoogleMap.animateCamera(zoom);
            }
        }
    };

    private final OnMapReadyCallback mapReadyCallback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap map) {
            mGoogleMap = map;
            mGoogleMap.setOnCameraIdleListener(mOnCameraChangeListener);
            mGoogleMap.setOnCameraMoveListener(mOnCameraMoveListener);
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            richLayer = new RichLayer.Builder(findViewById(R.id.map), mGoogleMap).build();
        }
    };

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

    private final GoogleMap.OnCameraMoveListener mOnCameraMoveListener = new GoogleMap.OnCameraMoveListener() {
        @Override
        public void onCameraMove() {
            CameraPosition cameraPosition = mGoogleMap.getCameraPosition();
            scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_details);

        //Set back button
        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);

        //Init map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(mapReadyCallback);

        //Init data
        long trackId = Objects.requireNonNull(getIntent().getExtras()).getLong(getString(R.string.extra_track_id));
        trackDetails = new TrackRepository(this).getTrackDetails(trackId);

        scaleView = findViewById(R.id.scaleView);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String unit = sharedPreferences.getString(getString(R.string.measure_key), getString(R.string.measure_default));
        if(unit.equals(getString(R.string.measure_default))){
            scaleView.metersOnly();
        } else {
            scaleView.milesOnly();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        trackDetails.observe(this, trackDetailsObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        trackDetails.removeObserver(trackDetailsObserver);
    }
}
