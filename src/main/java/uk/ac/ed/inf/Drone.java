package uk.ac.ed.inf;

import java.awt.geom.Line2D;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import org.w3c.dom.Node;

public class Drone {

    public LongLat dronePosition;
    private int battery;
    private ArrayList<Order> orders;
    private Order currentOrder;
    private ArrayList<Polygon> noFlyZones;
    private ArrayList<Point> landmarks;
    private String date;
    private DataParser parser;
    private DatabaseConnection database;
    private ArrayList<Path> flightPath;
    private List<Point> flightLine;

    public class Node {
        double cost;
        int moved;
        double heuristic;
        LongLat position;
        Node parent;
        int angle;

        public Node(int moved, double heuristic, LongLat position, int angle) {
            this.cost = moved * Const.DISTANCE_MOVE + heuristic;
            this.moved = moved;
            this.heuristic = heuristic;
            this.position = position;
            this.angle = angle;
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
    public Order getCurrentOrder() { return currentOrder; }
    public ArrayList<Polygon> getNoFlyZones() { return noFlyZones; }
    public ArrayList<Path> getFlightPath() { return flightPath; }
    public List<Point> getFlightLine() { return flightLine; }

    private void readDrone() {
        this.orders = this.database.readOrders(this.date);
        selectOrderByUtility();
        this.noFlyZones = this.parser.readNoFlyZones();
        this.landmarks = this.parser.readLandmarks();
    }

    public void selectOrderByUtility() {
        for (int i = 0; i < orders.size(); i++) {
            orders.get(i).estimateUtility(this.dronePosition, this.parser);
        }
        this.currentOrder = Collections.max(this.orders, new Comparator<>() {
            @Override
            public int compare(Order o1, Order o2) {
                return Double.valueOf(o1.getUtility()).compareTo(
                        Double.valueOf(o2.getUtility()));
            }
        });
        this.orders.remove(currentOrder);
    }

    public boolean pathForOrder() {
        LongLat deliverTo = parser.wordsToLongLat(currentOrder.getDeliverTo());
        int heuristic = (int)(dronePosition.distanceTo(deliverTo) / Const.DISTANCE_MOVE);
        // angle of the first node are set to -1 as there is no last move
        Node initialState = new Node(0, heuristic, dronePosition, -1);
        ArrayList<Node> linkedNodes = new ArrayList<>();
        Node goTo;
        try {
            for (LongLat loc : currentOrder.getShopCoordinate()) {
                ArrayList<Node> fronts = new ArrayList<Node>();
                goTo = aStarSearch(initialState, loc, fronts);
                linkedNodes.add(goTo);
                dronePosition = goTo.position;
                initialState = new Node(0, heuristic, dronePosition, -1);
                System.out.println("HHHHHHHHHHHHHH");
            }
            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAA");
            System.out.println(initialState.position.longitude + "," + initialState.position.latitude);
            System.out.println(deliverTo.longitude + "," + deliverTo.latitude);
            ArrayList<Node> fronts = new ArrayList<Node>();
            goTo = aStarSearch(initialState, deliverTo, fronts);
            System.out.println("fffffffffffffffffffffff");
            linkedNodes.add(goTo);
            dronePosition = goTo.position;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (linkedNodes == null) {
            return false;
        }
        for (Node n : linkedNodes) {
            if (n == null) {
                return false;
            }
        }
        flightPath = new ArrayList<>();
        flightLine = new ArrayList<>();
        int numberOfMoves = 0;
        for (Node n : linkedNodes) {
            ArrayList<Path> tempPath = new ArrayList<>();
            ArrayList<Point> tempPoints = new ArrayList<>();
            while (n.parent != null) {
                Path path = new Path(currentOrder.getOrderNo(), n.position.longitude,
                        n.position.latitude, n.angle, n.parent.position.longitude,
                        n.parent.position.latitude);
                numberOfMoves++;
                tempPath.add(path);
                tempPoints.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
                n = n.parent;
            }
            Collections.reverse(tempPoints);
            Collections.reverse(tempPath);
            flightLine.addAll(tempPoints);
            flightPath.addAll(tempPath);
        }
        LongLat apt = new LongLat(Const.APT_LONG, Const.APT_LAT);
        int powerBack = (int) (apt.distanceTo(dronePosition) / Const.DISTANCE_MOVE);
        battery -= numberOfMoves;
        if (battery < powerBack) {
            return false;
        }
        return true;
    }

    /**
     * Check whether the move can move to the next position without entering
     * no-fly-zones or getting out of the confinement area.
     *
     * @param nextPosition the next position which the drone will move to
     * @return whether the move is valid.
     */
    private boolean isValidMove(LongLat currentPosition, LongLat nextPosition) {
        if (!dronePosition.isConfined() | !nextPosition.isConfined()) {
            return false;
        }
        Line2D move = new Line2D.Double();
        move.setLine(currentPosition.longitude, currentPosition.latitude,
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

    /**
     * Apply A* search finding the optimal route to destination
     *
     * @param current current position and moved distance
     * @param destination the destination where the route ends
     * @param fronts number of node can be explored in next recursion
     * @return the optimal route from current state to the destination
     * @throws Exception when there is no path found
     */
    public Node aStarSearch(Node current, LongLat destination,
                                        ArrayList<Node> fronts) throws Exception {
        ArrayList<String> explored = new ArrayList<>();
        explored.add(current.position.longitude + "," + current.position.latitude);
        while (true) {
            if (current.position.closeTo(destination)) {
                return current;
            }
            // TODO: 2021/10/29 only 180 degrees required
//            double latDiff = destination.latitude - current.position.latitude;
//            double lngDiff = destination.longitude - current.position.longitude;
//            int direction = (int)(Math.toDegrees(Math.atan2(latDiff, lngDiff)) / 10) * 10;
//            for (int i = -90; i <= 100; i += 10) {
//                direction = (direction + i + 360) % 360;
//                LongLat nextPosition = current.position.nextPosition(direction);
//                String toString = nextPosition.longitude + "," + nextPosition.latitude;
//                if (!isValidMove(current.position, nextPosition) | explored.contains(toString)) {
//                    continue;
//                }
//                explored.add(toString);
//                double heuristic = destination.distanceTo(nextPosition);
//                Node front = new Node(current.moved + 1, heuristic, nextPosition, direction);
//                front.parent = current;
//                fronts.add(front);
//            }
            for (int i = 0; i < 360; i += 10) {
                LongLat nextPosition = current.position.nextPosition(i);
                String toString = nextPosition.longitude + "," + nextPosition.latitude;
                if (!isValidMove(current.position, nextPosition) | explored.contains(toString)) {
                    continue;
                }
                explored.add(toString);
                double heuristic = (int) (destination.distanceTo(nextPosition)
                        / Const.DISTANCE_MOVE);
                Node front = new Node(current.moved + 1, heuristic, nextPosition, i);
                front.parent = current;
                fronts.add(front);
            }
            LongLat apt = new LongLat(Const.APT_LONG, Const.APT_LAT);
            int powerBack = (int) (apt.distanceTo(current.position) / Const.DISTANCE_MOVE);
            if (fronts.size() == 0 | battery < powerBack) {
                break;
            }
            Node nextNode = Collections.min(fronts, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return ((Double)o1.cost).compareTo((Double) o2.cost);
                }
            });
            fronts.remove(nextNode);
            //System.out.println("[" + nextNode.position.longitude + "," + nextNode.position.latitude + "],");
            current = nextNode;
        }
        return null;
    }
}
