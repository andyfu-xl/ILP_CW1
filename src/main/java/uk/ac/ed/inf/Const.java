package uk.ac.ed.inf;

public class Const {
    
    // North-End of confinement area has latitude of 55.946233.
    public static final Double MAXLAT = 55.946233;

    // South-End of confinement area has latitude of 55.842617.
    public static final Double MINLAT = 55.942617;

    // East-End of confinement area has longitude of -3.184319.
    public static final Double MAXLONG = -3.184319;

    // West-End of confinement area has longitude of -3.192473.
    public static final Double MINLONG = -3.192473;

    /* Distance Tolerance of 0.00015 degree, one point is close to another
     if the distance between is strictly less than 0.00015 degree */
    public static final Double DTOLERANCE = 0.00015;

    // Distance of each move of drone in degrees
    public static final Double DMOVE = 0.00015;
}
