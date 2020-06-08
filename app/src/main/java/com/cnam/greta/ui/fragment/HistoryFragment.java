package com.cnam.greta.ui.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cnam.greta.R;
import com.cnam.greta.adapters.TrackAdapter;
import com.cnam.greta.data.entities.TrackDetails;
import com.cnam.greta.data.repositories.TrackRepository;
import com.cnam.greta.services.LocationService;
import com.cnam.greta.utils.DistanceFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HistoryFragment extends Fragment {

    private LocationService locationService;

    private LiveData<TrackDetails> trackDetails;

    private View rootView;
    private TrackRepository trackRepository;
    private TrackAdapter trackAdapter;
    private RecyclerView trackRecyclerView;
    private View emptyState;
    private View divider;
    private View currentTrackLayout;

    //Current track
    private TextView currentTrackName;
    private TextView currentTrackDistance;
    private TextView currentTrackDuration;
    private TextView currentTrackWaypoints;
    private TextView currentTrackTitleText;

    private LiveData<List<TrackDetails>> trackDetailsList;

    /**
     * Met à jour la section de la piste actuelle.
     */
    private Observer<TrackDetails> trackDetailsObserver = new Observer<TrackDetails>() {
        @Override
        public void onChanged(TrackDetails trackDetails) {
            if(getContext() != null){
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                String unit = sharedPreferences.getString(getContext().getString(R.string.measure_key), getContext().getString(R.string.measure_default));

                String formattedDistance = DistanceFormatter.format((int) trackDetails.computeDistance(getContext(), unit), false, unit);
                if(formattedDistance == null || formattedDistance.equals("")){
                    formattedDistance = "0";
                }
                long time = trackDetails.computeTime();
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(time),
                        TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
                        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                );

                currentTrackName.setText(trackDetails.getTrack().getTrackName());
                currentTrackDistance.setText(String.format(getContext().getString(R.string.distance_holder), formattedDistance));
                currentTrackDuration.setText(String.format(getString(R.string.duration_holder), formattedTime));
                currentTrackWaypoints.setText(String.format(getContext().getString(R.string.waypoints_hodler), trackDetails.getWayPoints() != null ? trackDetails.getWayPoints().size() : 0));
            }
        }
    };

    private Observer<List<TrackDetails>> trackDetailsListObserver = new Observer<List<TrackDetails>>() {
        @Override
        public void onChanged(List<TrackDetails> trackDetails) {
            if(trackDetails != null && trackDetails.size() != 0){
                trackAdapter.setTracks(trackDetails);
                emptyState.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.VISIBLE);
            }
        }
    };

    /**
     * Communication avec le location service.
     */
    private final LocationService.TrackListener trackListener = new LocationService.TrackListener() {
        @Override
        public void onStartTracking(long trackId) {
            setCurrentTrackVisibility(true);
            if (getContext() != null){
                trackDetails = new TrackRepository(requireContext()).getTrackDetails(trackId);
                trackDetails.observe(getViewLifecycleOwner(), trackDetailsObserver);
            }
        }

        @Override
        public void onStopTracking(long trackId) {
            setCurrentTrackVisibility(false);
            if (trackDetails != null){
                trackDetails.removeObserver(trackDetailsObserver);
            }
        }
    };

    /**
     * Service connection callback
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            locationService = ((LocationService.LocationServiceBinder) service).getService();
            locationService.addTrackListener(trackListener);
            if (locationService.isTracking()){
                setCurrentTrackVisibility(true);
            } else {
                setCurrentTrackVisibility(false);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            locationService.removeTrackListener(trackListener);
            if (locationService.isTracking()){
                setCurrentTrackVisibility(true);
            } else {
                setCurrentTrackVisibility(false);
            }
            locationService = null;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackRepository = new TrackRepository(requireContext());
        trackDetailsList = trackRepository.getAllTrackDetails();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_history, container, false);
            trackRecyclerView = rootView.findViewById(R.id.history_recyclerview);
            emptyState = rootView.findViewById(R.id.fragment_history_empty_state);
            divider = rootView.findViewById(R.id.divider);
            currentTrackLayout = rootView.findViewById(R.id.current_track_layout);
            currentTrackLayout = rootView.findViewById(R.id.current_track_layout);
            currentTrackTitleText = rootView.findViewById(R.id.current_track_title);
            trackAdapter = new TrackAdapter(new ArrayList<TrackDetails>());
            trackRecyclerView.setAdapter(trackAdapter);
            trackRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            currentTrackName = currentTrackLayout.findViewById(R.id.name);
            currentTrackDistance = currentTrackLayout.findViewById(R.id.distance);
            currentTrackDuration = currentTrackLayout.findViewById(R.id.duration);
            currentTrackWaypoints = currentTrackLayout.findViewById(R.id.waypoints);
            currentTrackLayout.findViewById(R.id.item_menu).setVisibility(View.GONE);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        trackDetailsList.observe(getViewLifecycleOwner(), trackDetailsListObserver);
        Intent intent = new Intent(requireActivity(), LocationService.class);
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        trackDetailsList.removeObserver(trackDetailsListObserver);
        requireActivity().unbindService(serviceConnection);
    }

    /**
     * Mise à jour de l'UI.
     * @param visible
     */
    private void setCurrentTrackVisibility(boolean visible){
        if(visible){
            currentTrackLayout.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            currentTrackTitleText.setVisibility(View.VISIBLE);
        } else {
            currentTrackLayout.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            currentTrackTitleText.setVisibility(View.GONE);
        }
        if(locationService != null && trackAdapter != null){
            if(locationService.isTracking()){
                trackAdapter.setTracking(visible);
            } else {
                trackAdapter.setTracking(visible);
            }
        }
    }

}
