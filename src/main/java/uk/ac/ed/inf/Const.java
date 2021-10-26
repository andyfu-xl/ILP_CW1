package uk.ac.ed.inf;

/**
 *  This class contains only constants will be used in the coursework.
 */
public class Const {
    
    // North-End of confinement area has latitude of 55.946233.
    public static final Double MAX_LAT = 55.946233;

    // South-End of confinement area has latitude of 55.842617.
    public static final Double MIN_LAT = 55.942617;

    // East-End of confinement area has longitude of -3.184319.
    public static final Double MAX_LONG = -3.184319;

    // West-End of confinement area has longitude of -3.192473.
    public static final Double MIN_LONG = -3.192473;

    /* Distance Tolerance of 0.00015 degree, one point is close to another
     if the distance between is strictly less than 0.00015 degree */
    public static final Double DISTANCE_TOLERANCE = 0.00015;

    // Distance of each move of drone in degrees
    public static final Double DISTANCE_MOVE = 0.00015;

    // Directory of menus.json in the server
    public static final String PATH_MENUS = "/menus/menus.json";

    // Directory of no-fly-zones.geojson in the server
    public static final String PATH_NO_FLY_ZONES = "/buildings/no-fly-zones.geojson";

    // Directory of landmarks.geojson in the server
    public static final String PATH_LANDMARKS = "/buildings/landmarks.geojson";

    // Directory of words folder in the server
    public static final String PATH_WORDS = "/words/";

    // File name of delivery destination detail
    public static final String DETAIL = "/details.json";

    // Directory of landmarks.geojson in the server
    public static final String DATABASE = "/derbyDB";

    // Delivery Charge of 50p
    public static final int DELIVERY_CHARGE = 50;

    // Angle value when hovering
    public static final int HOVERING = -999;

    // IP of localhost
    public static final String IP = "localhost";
}
