package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

/**
 * Path object has details of each move,
 * The details will be written into the database.
 */
public class Path {
    public String orderNo;
    public double fromLongitude;
    public double fromLatitude;
    public int angle;
    public double toLongitude;
    public double toLatitude;

    /**
     * Constructor of the path
     *
     * @param orderNo order number
     * @param fromLongitude longitude of the position before this move
     * @param fromLatitude latitude of the position before this move
     * @param angle angle of this move
     * @param toLongitude longitude of the position after this move
     * @param toLatitude latitude of the position after this moves
     */
    public Path(String orderNo, double fromLongitude, double fromLatitude,
                int angle, double toLongitude, double toLatitude) {
        this.orderNo = orderNo;
        this.fromLongitude = fromLongitude;
        this.fromLatitude = fromLatitude;
        this.angle = angle;
        this.toLongitude = toLongitude;
        this.toLatitude = toLatitude;
    }

    /**
     * Point of from coordinate
     *
     * @return Point of from coordinate
     */
    public Point pointFrom() {
        return Point.fromLngLat(fromLongitude, fromLatitude);
    }

    /**
     * Point of to coordinate
     *
     * @return Point of to coordinate
     */
    public Point pointTo() {
        return Point.fromLngLat(toLongitude, toLatitude);
    }

}
