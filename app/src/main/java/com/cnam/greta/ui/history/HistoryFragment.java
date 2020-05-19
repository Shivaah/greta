package com.cnam.greta.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cnam.greta.R;
import com.cnam.greta.adapters.TrackAdapter;
import com.cnam.greta.database.entities.TrackDetails;
import com.cnam.greta.database.repositories.TrackRepository;

import java.util.List;

public class HistoryFragment extends Fragment {

    private View rootView;
    private TrackRepository trackRepository;
    private TrackAdapter trackAdapter;
    private RecyclerView trackRecyclerView;

    private LiveData<List<TrackDetails>> trackDetails;

    private Observer<List<TrackDetails>> trackDetailsObserver = new Observer<List<TrackDetails>>() {
        @Override
        public void onChanged(List<TrackDetails> trackDetails) {
            trackAdapter = new TrackAdapter(trackDetails);
            trackRecyclerView.setAdapter(trackAdapter);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackRepository = new TrackRepository(requireContext());
        trackDetails = trackRepository.getAllTrackDetails();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(rootView == null){
            rootView = inflater.inflate(R.layout.fragment_history, container, false);
            trackRecyclerView = rootView.findViewById(R.id.history_recyclerview);
            trackRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        trackDetails.observe(getViewLifecycleOwner(), trackDetailsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        trackDetails.removeObserver(trackDetailsObserver);
    }
}
