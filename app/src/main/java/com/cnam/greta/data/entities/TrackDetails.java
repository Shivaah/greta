package com.cnam.greta.data.entities;

import android.content.Context;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.cnam.greta.R;

import java.util.List;

public class TrackDetails{

    @Embedded
    private Track track;
    @Relation(parentColumn = "trackId", entityColumn = "trackId", entity = WayPoint.class)
    private List<WayPoint> wayPoints;

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public List<WayPoint> getWayPoints() {
        return wayPoints;
    }

    public void setWayPoints(List<WayPoint> wayPoint) {
        this.wayPoints = wayPoint;
    }

    /**
     * Calcule la distance entre le premier et le dernier waypoint.
     *
     * @param context
     * @param unit
     *
     * @return
     */
    public long computeDistance(Context context, String unit){
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

    /**
     * Calcule la différence de temps entre le premier et le dernier waypoint.
     *
     * @return
     */
    public long computeTime(){
        if(wayPoints.size() == 0 || wayPoints.size() == 1){
            return 0;
        }
        WayPoint point1 = wayPoints.get(0);
        WayPoint point2 = wayPoints.get(wayPoints.size() - 1);
        return point2.getTime() - point1.getTime();
    }
}
