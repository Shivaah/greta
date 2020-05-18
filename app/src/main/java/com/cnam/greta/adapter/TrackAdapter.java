package com.cnam.greta.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cnam.greta.R;
import com.cnam.greta.model.Track;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> mTracks;

    public TrackAdapter(List<Track> mTracks) {
        this.mTracks = mTracks;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = mTracks.get(position);
        holder.name.setText(track.getName());
        holder.distance.setText((int) track.getDistance());
        holder.duration.setText((int) track.getDuration());
        holder.waypoints.setText((int) track.getWaypoints());
    }

    @Override
    public int getItemCount() {
        if (mTracks == null){
            return 0;
        }
        return mTracks.size();
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView distance;
        private TextView duration;
        private TextView waypoints;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            distance = itemView.findViewById(R.id.distance);
            duration = itemView.findViewById(R.id.duration);
            waypoints = itemView.findViewById(R.id.waypoints);
        }
    }

}
