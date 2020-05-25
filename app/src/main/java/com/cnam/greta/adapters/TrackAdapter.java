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
import com.cnam.greta.data.repositories.TrackRepository;
import com.cnam.greta.ui.TrackDetailsActivity;
import com.cnam.greta.utils.DistanceFormatter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<TrackDetails> mTracks;
    private boolean isTracking = false;

    public TrackAdapter(List<TrackDetails> tracks) {
        this.mTracks = tracks;
    }

    public void setTracks(List<TrackDetails> mTracks) {
        this.mTracks = mTracks;
        notifyDataSetChanged();
    }

    public void setTracking(boolean tracking) {
        isTracking = tracking;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        if (isTracking && position == 0){
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        } else {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(holder.itemView.getContext());
            String unit = sharedPreferences.getString(holder.itemView.getContext().getString(R.string.measure_key), holder.itemView.getContext().getString(R.string.measure_default));
            TrackDetails track = mTracks.get(position);

            String formattedDistance = DistanceFormatter.format((int) track.computeDistance(holder.itemView.getContext(), unit), false, unit);
            if(formattedDistance == null || formattedDistance.equals("")){
                formattedDistance = "0";
            }
            long time = track.computeTime();
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(time),
                    TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
            );

            holder.name.setText(track.getTrack().getTrackName());
            holder.distance.setText(String.format(holder.itemView.getContext().getString(R.string.distance_holder), formattedDistance));
            holder.duration.setText(String.format(holder.itemView.getContext().getString(R.string.duration_holder), formattedTime));
            holder.waypoints.setText(String.format(holder.itemView.getContext().getString(R.string.waypoints_hodler), track.getWayPoints() != null ? track.getWayPoints().size() : 0));
        }
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
                    new TrackRepository(context).delete(mTracks.get(getAdapterPosition()).getTrack());
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
