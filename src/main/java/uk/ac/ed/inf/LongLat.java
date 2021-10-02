package uk.ac.ed.inf;
import java.lang.Math;

/**
 * representation of a point, by its longitude and latitude.
 */
public class LongLat {
    public Double longitude;
    public Double latitude;

    /**
     * Location constructor
     *
     * @param lat - Latitude of point on map
     * @param lng - Longitude of point on map
     */
    public LongLat(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * check whether the point is within the drone confinement area.
     * @return true if the point is in the confinement area.
     */
    public boolean isConfined() {
        boolean validLong = (this.longitude < Const.MAXLONG) & (this.longitude > Const.MINLONG);
        boolean validLat = (this.latitude < Const.MAXLAT) & (this.latitude > Const.MINLAT);
        return (validLong & validLat);
    }

    /**
     * Apply Pythagorean to calculate distance to another point.
     * @param point, the target point we want compute its distance from this LongLat.
     * @return a degree (for distance) in double precision.
     */
    public double distanceTo(LongLat point) {
        double longDifference = this.longitude - point.longitude;
        double latDifference = this.latitude - point.latitude;
        double longDifSqure = Math.pow(longDifference,2);
        double latDifSqure = Math.pow(latDifference,2);
        return (Math.sqrt(longDifSqure + latDifSqure));
    }

    /**
     * Check if this point is close to another point on map.
     * @param point another point on map.
     * @return true if the distance between is strictly less than 0.00015 degree.
     */
    public boolean closeTo(LongLat point) {
        return (distanceTo(point) < Const.DTOLERANCE);
    }

    /**
     * Calculate the next position of drone given direction of its next move.
     * @param angle the direction of next move, 0 is East, 90 is North, 180 is West
     *              270 is South, the angle must be multiples of ten.
     * @return the next position.
     */
    public LongLat nextPosition(int angle) {
        double newLong = this.longitude + 0.00015 * Math.cos(Math.toRadians(angle));
        double newLat = this.latitude + 0.00015 * Math.sin(Math.toRadians(angle));
        LongLat nextPos = new LongLat(newLong, newLat);
        return nextPos;
    }
}
