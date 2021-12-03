package uk.ac.ed.inf;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 * representation of a point, by its longitude and latitude.
 */
public class LongLat {
    public Double longitude;
    public Double latitude;

    /**
     * Constructor of LongLat class.
     *
     * @param latitude  - Latitude of point on map
     * @param longitude - Longitude of point on map
     */
    public LongLat(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Check whether the point is within the drone confinement area.
     * The confinement area is a rectangle.
     *
     * @return true if the point is in the confinement area.
     */
    public boolean isConfined() {
        boolean validLong = (this.longitude < Const.MAX_LONG) & (this.longitude > Const.MIN_LONG);
        boolean validLat = (this.latitude < Const.MAX_LAT) & (this.latitude > Const.MIN_LAT);
        return (validLong & validLat);
    }

    /**
     * get the angle of move from this LongLat to destination.
     *
     * @param destination where the move ends.
     * @return angle in degrees.
     */
    public double bearing(LongLat destination) {
        double latDiff = destination.latitude - this.latitude;
        double lngDiff = destination.longitude - this.longitude;
        return Math.toDegrees(Math.atan2(latDiff, lngDiff));
    }

    /**
     * Apply Pythagorean to calculate distance to another point.
     *
     * @param point, the target point we want compute its distance from this LongLat.
     * @return a degree (for distance) in double precision.
     */
    public double distanceTo(LongLat point) {
        double longDifference = this.longitude - point.longitude;
        double latDifference = this.latitude - point.latitude;
        double longDifSqure = Math.pow(longDifference, 2);
        double latDifSqure = Math.pow(latDifference, 2);
        return (Math.sqrt(longDifSqure + latDifSqure));
    }

    /**
     * Check if this point is close to another point on map.
     *
     * @param point another point on map.
     * @return true if the distance between is strictly less than 0.00015 degree.
     */
    public boolean closeTo(LongLat point) {
        return (distanceTo(point) < Const.DISTANCE_TOLERANCE);
    }

    /**
     * Check if two point has the same coordinates.
     *
     * @param obj another point
     * @return true if this LongLat has the same coordinate as point
     */
    public boolean equals(LongLat obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LongLat)) {
            return false;
        }
        LongLat point = obj;
        return (latitude.equals(point.latitude) & longitude.equals(point.longitude));
    }

    /**
     * convert geojson.Point objects to LongLat objects
     *
     * @param point geojson.point to be converted
     * @return a LongLat with same coordinate
     */
    public static LongLat fromPoint(com.mapbox.geojson.Point point) {
        return new LongLat(point.longitude(), point.latitude());
    }

    /**
     * Two points forms a Line2D object
     *
     * @param p1 the first point
     * @param p2 the second point
     * @return Line2D between two points
     */
    public static Line2D formLine(LongLat p1, LongLat p2) {
        Line2D line = new Line2D.Double();
        line.setLine(p1.longitude, p1.latitude, p2.longitude, p2.latitude);
        return line;
    }

    /**
     * Calculate the next position of drone given direction of its next move.
     *
     * @param angle the direction of next move, 0 is East, 90 is North, 180 is West
     *              270 is South
     *              -999 means hovering (position doesn't change)
     * @return the next position.
     */
    public LongLat nextPosition(double angle) {
        LongLat nextPos = new LongLat(this.longitude, this.latitude);
        if (angle == Const.HOVERING) {
            return (nextPos);
        } else if (angle < 0 | angle > 360) {
            // the function is also used for estimation of flight path, so any
            // double between 0 and 360 is allowed
            System.err.println("Angle must between 0 and 360, or -999 for hovering");
            return (nextPos);
        }
        // Cosine of angle to calculate vector of displacement parallel to longitude.
        nextPos.longitude = this.longitude
                + Const.DISTANCE_MOVE * Math.cos(Math.toRadians(angle));
        // Sine of angle to calculate vector of displacement parallel to latitude.
        nextPos.latitude = this.latitude
                + Const.DISTANCE_MOVE * Math.sin(Math.toRadians(angle));
        return nextPos;
    }

}
