package uk.ac.ed.inf;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import org.w3c.dom.Node;

public class Drone {

    private LongLat dronePosition;
    private int battery;
    private ArrayList<Order> orders;
    private ArrayList<Polygon> noFlyZones;
    private ArrayList<Point> landmarks;
    private String date;
    private DataParser parser;
    private DatabaseConnection database;

    public class Node {
        int cost;
        int moved;
        int heuristic;
        LongLat position;

        public Node(int moved, int heuristic, LongLat position) {
            this.cost = moved + heuristic;
            this.moved = moved;
            this.heuristic = heuristic;
            this.position = position;
        }
    }

    public Drone (String date, DataParser parser, DatabaseConnection database) {
        this.date = date;
        this.parser = parser;
        this.database = database;
        this.battery = Const.MAX_POWER;
        this.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        readDrone();
    }

    public ArrayList<Order> getOrders() { return orders; }
    public ArrayList<Polygon> getNoFlyZones() { return  noFlyZones; }

    private void readDrone() {
        this.orders = this.database.readOrders(this.date);
        sortOrdersByUtility(this.orders);
        this.noFlyZones = this.parser.readNoFlyZones();
        this.landmarks = this.parser.readLandmarks();
    }

    private void sortOrdersByUtility(ArrayList<Order> orders1) {
        for (int i = 0; i < orders1.size(); i++) {
            orders1.get(i).estimateUtility(this.dronePosition, this.parser);
        }
        Collections.sort(this.orders, new Comparator<>() {
            @Override
            public int compare(Order o1, Order o2) {
                return Double.valueOf(o1.getUtility()).compareTo(
                        Double.valueOf(o2.getUtility()));
            }
        });
        Collections.reverse(this.orders);
    }

    /**
     * Check whether the move can move to the next position without entering
     * no-fly-zones or getting out of the confinement area.
     *
     * @param nextPosition the next position which the drone will move to
     * @return whether the move is valid.
     */
    private boolean isValidMove(LongLat currentPostion, LongLat nextPosition) {
        if (!dronePosition.isConfined() | !nextPosition.isConfined()) {
            return false;
        }
        Line2D move = new Line2D.Double();
        move.setLine(currentPostion.longitude, currentPostion.latitude,
                nextPosition.longitude, nextPosition.latitude);
        for (Polygon p : noFlyZones) {
            List<Point> building = p.coordinates().get(0);
            for (int i = 0; i < building.size()-1; i++) {
                double x1 = building.get(i).longitude();
                double y1 = building.get(i).latitude();
                double x2 = building.get(i + 1).longitude();
                double y2 = building.get(i + 1).latitude();
                Line2D line = new Line2D.Double();
                line.setLine(x1, y1, x2, y2);
                if (line.intersectsLine(move)) {
                    return false;
                }
            }
        }
        return true;
    }

    public ArrayList<LongLat> aStarSearch(Node current, LongLat destination,
                                       int movedDistance, ArrayList<Node> fronts) throws Exception {
        LongLat apt = new LongLat(Const.APT_LONG, Const.APT_LAT);
        int powerBack = (int) (apt.distanceTo(current.position) / Const.DISTANCE_MOVE);
        if (current.position.closeTo(destination) & battery >= powerBack) {
            ArrayList<LongLat> paths = new ArrayList<>();
            return paths;
        }
        if (fronts.size() == 0) {
            System.out.println("Error: There is no path.");
            return null;
        }
        for (int i = 0; i < 360; i += 10) {
            LongLat nextPosition = current.position.nextPosition(i);
            if (!isValidMove(current.position, nextPosition)) {
                continue;
            }
            int heuristic = (int) (destination.distanceTo(nextPosition)
                    / Const.DISTANCE_MOVE);
            Node front = new Node(movedDistance + 1, heuristic, nextPosition);
            fronts.add(front);
        }
        Node nextNode = Collections.max(fronts, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return ((Integer)o1.cost).compareTo((Integer)o2.cost);
            }
        });
        ArrayList<LongLat> path= aStarSearch(nextNode, destination, nextNode.moved, fronts);
        if (path == null) {
            return null;
        }
        path.add(current.position);
        return path;
    }
}
