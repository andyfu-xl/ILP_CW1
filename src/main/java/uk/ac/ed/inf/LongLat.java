package uk.ac.ed.inf;

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
     * Calculate the next position of drone given direction of its next move.
     *
     * @param angle the direction of next move, 0 is East, 90 is North, 180 is West
     *              270 is South, the angle must be multiples of ten.
     *              -999 means hovering (position doesn't change)
     * @return the next position.
     */
    public LongLat nextPosition(int angle) {
        LongLat nextPos = new LongLat(this.longitude, this.latitude);
        if (angle == Const.HOVERING) {
            return (nextPos);
        } else if (angle < 0 | angle > 350 | angle % 10 != 0) {
            System.err.println("Angle must be multiples of ten " +
                    "and between 0 and 350, or -999 for hovering");
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
