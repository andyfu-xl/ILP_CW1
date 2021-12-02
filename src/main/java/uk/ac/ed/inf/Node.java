package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A linked node stores a set of points in sequence.
 * The node is mainly used for the A-star searching algorithm.
 */
public class Node {
    // variables
    double cost;
    double moved;
    double heuristic;
    LongLat position;
    Node parent;
    int angle;

    /**
     * constructor of the node
     *
     * @param moved     the cost takes to reach the current state, from the beginning position
     *                  of the A-star searching algorithm
     * @param heuristic the estimated cost form current state to the destination of the A-star
     *                  searching algorithm.
     * @param position  coordinates of the current node
     * @param angle     angle of last move to reach current state.
     */
    public Node(double moved, double heuristic, LongLat position, int angle) {
        this.cost = moved + heuristic;
        this.moved = moved;
        this.heuristic = heuristic;
        this.position = position;
        this.position = position;
        this.angle = angle;
    }

    public ArrayList<Path> toPaths(String orderNumber) {
        ArrayList<Path> paths = new ArrayList<>();
        if (parent != null) {
            Path path = new Path(orderNumber, parent.position.longitude,
                    parent.position.latitude, angle, position.longitude,
                    position.latitude);
            paths = (parent.toPaths(orderNumber));
            paths.add(path);
        }
        else {
            return paths;
        }
        return paths;
    }

    public ArrayList<LongLat> toLongLats() {
        ArrayList<LongLat> longLats = new ArrayList<>();
        if (parent != null) {
            longLats = (parent.toLongLats());
            longLats.add(position);
        }
        else {
            longLats.add(position);
            return longLats;
        }
        return longLats;
    }


    public ArrayList<Point> toPoints() {
        ArrayList<Point> points = new ArrayList<>();
        if (parent != null) {
            points = (parent.toPoints());
            points.add(Point.fromLngLat(position.longitude, position.latitude));
        }
        else {
            return points;
        }
        return points;
    }

}
