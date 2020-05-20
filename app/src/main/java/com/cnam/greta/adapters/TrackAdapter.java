package com.cnam.greta.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cnam.greta.R;
import com.cnam.greta.data.entities.TrackDetails;
import com.cnam.greta.data.entities.WayPoint;
import com.cnam.greta.data.repositories.TrackRepository;
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
        String unit = sharedPreferences.getString(holder.itemView.getContext().getString(R.string.measure_key), holder.itemView.getContext().getString(R.string.measure_default));
        TrackDetails track = mTracks.get(position);
        holder.name.setText(track.getTrack().getTrackName());
        holder.distance.setText(String.format(holder.itemView.getContext().getString(R.string.distance_holder), computeDistance(holder.itemView.getContext(), track.getWayPoints(), unit)));
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

    private long computeDistance(Context context, List<WayPoint> wayPoints, String unit){
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
                if (unit.equals(context.getString(R.string.measure_default))) {
                    dist = dist * 1.609344;
                } else {
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
        private androidx.appcompat.widget.Toolbar toolbar;

        private final Toolbar.OnMenuItemClickListener menuItemClickListener = new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.item_track_map:
                        Intent intent = new Intent(itemView.getContext(), TrackDetailsActivity.class);
                        intent.putExtra(itemView.getContext().getString(R.string.extra_track_id), mTracks.get(getAdapterPosition()).getTrack().getTrackId());
                        itemView.getContext().startActivity(intent);
                        break;

                    case R.id.item_track_edit:
                        editTextDialog(itemView.getContext(), mTracks.get(getAdapterPosition()).getTrack().getTrackName());
                        break;

                    case R.id.item_track_delete:
                        confirmDialog(itemView.getContext());
                        break;
                }
                return false;
            }
        };

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            distance = itemView.findViewById(R.id.distance);
            duration = itemView.findViewById(R.id.duration);
            waypoints = itemView.findViewById(R.id.waypoints);
            toolbar = itemView.findViewById(R.id.item_menu);

            toolbar.inflateMenu(R.menu.menu_item_track);
            toolbar.setOnMenuItemClickListener(menuItemClickListener);
        }

        private void editTextDialog(final Context context, String trackName){
            final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setCancelable(true);
            alertDialog.setTitle(context.getString(R.string.track_name));
            final EditText editText = new EditText(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMarginEnd(48);
            lp.setMarginStart(48);
            editText.setLayoutParams(lp);
            editText.setText(trackName);
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(32);
            editText.setFilters(filterArray);
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TrackDetails track = mTracks.get(getAdapterPosition());
                    track.getTrack().setTrackName(editText.getText().toString());
                    new TrackRepository(context).update(track.getTrack());
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

        private void confirmDialog(final Context context){
            final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setMessage(context.getString(R.string.confirm_delete));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new TrackRepository(context).delete(mTracks.get(getAdapterPosition()).getTrack().getTrackId());
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

    }

}
