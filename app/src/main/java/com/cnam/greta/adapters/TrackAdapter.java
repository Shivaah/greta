package com.cnam.greta.adapters;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cnam.greta.R;
import com.cnam.greta.database.entities.TrackDetails;
import com.cnam.greta.database.entities.WayPoint;
import com.cnam.greta.ui.TrackDetailsActivity;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<TrackDetails> mTracks;

    public TrackAdapter(List<TrackDetails> tracks) {
        this.mTracks = tracks;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(holder.itemView.getContext());
        String unit = sharedPreferences.getString(holder.itemView.getContext().getString(R.string.measure_key), "kilometers");
        TrackDetails track = mTracks.get(position);
        holder.name.setText(track.getTrack().getTrackName());
        holder.distance.setText(String.format(holder.itemView.getContext().getString(R.string.distance_holder), computeDistance(track.getWayPoints(), unit)));
        holder.duration.setText(String.format(holder.itemView.getContext().getString(R.string.duration_holder), computeTime(track.getWayPoints())));
        holder.waypoints.setText(String.format(holder.itemView.getContext().getString(R.string.waypoints_hodler), track.getWayPoints() != null ? track.getWayPoints().size() : 0));
    }

    @Override
    public int getItemCount() {
        if (mTracks == null){
            return 0;
        }
        return mTracks.size();
    }

    private long computeDistance(List<WayPoint> wayPoints, String unit){
        long total = 0;
        for (int i = 0; i < wayPoints.size() - 1; i++){
            WayPoint point1 = wayPoints.get(i);
            WayPoint point2 = wayPoints.get(i + 1);
            if (!((point1.getLatitude() == point2.getLongitude()) && (point1.getLatitude() == point2.getLongitude()))) {
                double theta = point1.getLatitude() - point2.getLongitude();
                double dist = Math.sin(Math.toRadians(point1.getLatitude())) * Math.sin(Math.toRadians(point2.getLatitude())) + Math.cos(Math.toRadians(point1.getLatitude())) * Math.cos(Math.toRadians(point2.getLatitude())) * Math.cos(Math.toRadians(theta));
                dist = Math.acos(dist);
                dist = Math.toDegrees(dist);
                dist = dist * 60 * 1.1515;
                if (unit.equals("kilometers")) {
                    dist = dist * 1.609344;
                } else if (unit.equals("miles")) {
                    dist = dist * 0.8684;
                }
                total += dist;
            }
        }
        return total;
    }

    private long computeTime(List<WayPoint> wayPoints){
        if(wayPoints.size() == 0 || wayPoints.size() == 1){
            return 0;
        }
        WayPoint point1 = wayPoints.get(0);
        WayPoint point2 = wayPoints.get(wayPoints.size() - 1);
        return point2.getTime() - point1.getTime();
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
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), TrackDetailsActivity.class);
                    intent.putExtra(v.getContext().getString(R.string.extra_track_id), mTracks.get(getAdapterPosition()).getTrack().getTrackId());
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

}
