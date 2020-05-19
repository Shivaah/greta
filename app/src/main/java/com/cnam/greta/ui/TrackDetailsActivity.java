package com.cnam.greta.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.cnam.greta.R;
import com.cnam.greta.database.entities.TrackDetails;
import com.cnam.greta.database.entities.WayPoint;
import com.cnam.greta.database.repositories.TrackRepository;
import com.cnam.greta.views.richmaps.RichLayer;
import com.cnam.greta.views.richmaps.RichPoint;
import com.cnam.greta.views.richmaps.RichPolylineOptions;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Objects;

public class TrackDetailsActivity extends FragmentActivity{

    private GoogleMap mGoogleMap;
    private RichLayer richLayer;

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
                WayPoint lastWayPoint = trackDetails.getWayPoints().get(trackDetails.getWayPoints().size() - 1);
                LatLng lastPosition = new LatLng(lastWayPoint.getLatitude(), lastWayPoint.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions()
                        .position(lastPosition)
                        .title(trackDetails.getTrack().getTrackName())
                );
                richLayer.addShape(polylineOpts.build());
                CameraUpdate center= CameraUpdateFactory.newLatLng(lastPosition);
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
            mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            richLayer = new RichLayer.Builder(findViewById(R.id.map), mGoogleMap).build();
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
