package uk.ac.ed.inf;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The class process path-finding algorithm
 */
public class Map {

    // variables
    public LongLat dronePosition;
    private ArrayList<Path> flightPath;
    private List<Point> flightLine;
    private ArrayList<Path> flightPathBack;
    private List<Point> flightLineBack;
    private ArrayList<Polygon> noFlyZones;

    /**
     * A linked node stores a set of points in sequence.
     * The node is mainly used for the A-star searching algorithm.
     */
    public static class Node {
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
         * @param moved the cost takes to reach the current state, from the beginning position
         *              of the A-star searching algorithm
         * @param heuristic the estimated cost form current state to the destination of the A-star
         *                  searching algorithm.
         * @param position coordinates of the current node
         * @param angle angle of last move to reach current state.
         */
        public Node(double moved, double heuristic, LongLat position, int angle) {
            this.cost = moved + heuristic;
            this.moved = moved;
            this.heuristic = heuristic;
            this.position = position;
            this.position = position;
            this.angle = angle;
        }
    }

    /**
     * Constructor of the map
     *
     * @param dronePosition position of the drone
     * @param noFlyZones buildings that drones cannot fly through
     */
    public Map(LongLat dronePosition, ArrayList<Polygon> noFlyZones) {
        this.dronePosition = dronePosition;
        this.noFlyZones = noFlyZones;
    }

    // getters
    public ArrayList<Path> getFlightPath() { return flightPath; }
    public List<Point> getFlightLine() { return flightLine; }
    public ArrayList<Path> getFlightPathBack() { return flightPathBack; }
    public List<Point> getFlightLineBack() { return flightLineBack; }
    public ArrayList<Polygon> getNoFlyZones() { return noFlyZones; }

    /**
     * Generate the path back to the Appleton Tower given an ArrayList of turningPoints
     * The flights will fly roughly straight unless it passes a turningPoint
     *
     * @param turningPoints position where the drone is about to turn
     * @return number of move of the path
     */
    public int turningPointsToPathBack(ArrayList<LongLat> turningPoints) {
        int moveNumber = 0;
        if (turningPoints.size() <= 1) {
            return moveNumber;
        }
        LongLat initialPos = turningPoints.get(0);
        flightPathBack = new ArrayList<>();
        flightLineBack = new ArrayList<>();
        for (int i = 1; i < turningPoints.size(); i++) {
            LongLat target = turningPoints.get(i);
            Node n = straightRoute(initialPos, target);
            initialPos = n.position;
            ArrayList<Path> tempPath = new ArrayList<>();
            ArrayList<Point> tempLine = new ArrayList<>();
            while (n.parent != null) {
                Path path = new Path("0", n.position.longitude,
                        n.position.latitude, n.angle, n.parent.position.longitude,
                        n.parent.position.latitude);
                moveNumber++;
                tempPath.add(path);
                tempLine.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
                n = n.parent;
            }
            Collections.reverse(tempLine);
            Collections.reverse(tempPath);
            this.flightPathBack.addAll(tempPath);
            this.flightLineBack.addAll(tempLine);
        }
        return moveNumber;
    }

    public void initializePaths() {
        flightPath = new ArrayList<>();
        flightLine = new ArrayList<>();
        flightPathBack = new ArrayList<>();
        flightLineBack = new ArrayList<>();
    }

    /**
     * Generate the path given an ArrayList of turningPoints
     * The flights will fly roughly straight unless it passes a turningPoint
     *
     * @param turningPoints position where the drone is about to turn
     * @return number of move of the path
     *
     * @param turningPoints position where the drone is about to turn
     * @param orderNumber order number which will be written into the database
     * @return number of move of the path
     */
    public int turningPointsToPathOrder(ArrayList<LongLat> turningPoints, String orderNumber) {
        int moveNumber = 0;
        if (turningPoints.size() <= 1) {
            return moveNumber;
        }
        LongLat initialPos = turningPoints.get(0);
        initializePaths();

        for (int i = 1; i < turningPoints.size(); i++) {
            LongLat target = turningPoints.get(i);
            // if the target location is the same as current location, the drone hovers
            if ((target.latitude == turningPoints.get(i - 1).latitude) &
                    (target.longitude == turningPoints.get(i - 1).longitude)) {
                Path previousPath = this.flightPath.get(this.flightPath.size() - 1);
                Path path = new Path(orderNumber, previousPath.toLongitude,
                        previousPath.toLatitude, -999, previousPath.toLongitude,
                        previousPath.toLatitude);
                Point point = this.flightLine.get(this.flightLine.size() - 1);
                moveNumber++;
                this.flightPath.add(path);
                this.flightLine.add(point);
                continue;
            }
            Node n = straightRoute(initialPos, target);
            initialPos = n.position;
            ArrayList<Path> tempPath = new ArrayList<>();
            ArrayList<Point> tempLine = new ArrayList<>();
            while (n.parent != null) {
                Path path = new Path(orderNumber, n.parent.position.longitude,
                        n.parent.position.latitude, n.angle, n.position.longitude,
                        n.position.latitude);
                moveNumber++;
                tempPath.add(path);
                tempLine.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
                n = n.parent;
            }
            Collections.reverse(tempLine);
            Collections.reverse(tempPath);
            if (orderNumber.equals("0")) {
                this.flightPathBack.addAll(tempPath);
                this.flightLineBack.addAll(tempLine);
            }
            else {
                this.flightPath.addAll(tempPath);
                this.flightLine.addAll(tempLine);
            }
        }
        return moveNumber;
    }

    /**
     * Check whether the drone can move to the next position without entering
     * no-fly-zones or getting out of the confinement area.
     *
     * @param currentPosition the current position which the drone will move to.
     * @param nextPosition    the next position which the drone will move to.
     * @param pseudoLandmarks whether we treat points of buildings as pseudo-landmarks.
     *                        true if one move can start or end with one node on any building
     *                        without passing through the building. This is set to true when
     *                        searching for the frame of the shortest path.
     * @return whether the move is valid.
     */
    public boolean isValidMove(LongLat currentPosition, LongLat nextPosition, Boolean pseudoLandmarks) {
        if (!dronePosition.isConfined() | !nextPosition.isConfined()) {
            return false;
        }
        Line2D move = new Line2D.Double();
        move.setLine(currentPosition.longitude, currentPosition.latitude,
                nextPosition.longitude, nextPosition.latitude);
        int cornerIntersect = 0;
        for (Polygon p : noFlyZones) {
            List<Point> building = p.coordinates().get(0);
            for (int i = 0; i < building.size() - 1; i++) {
                double x1 = building.get(i).longitude();
                double y1 = building.get(i).latitude();
                double x2 = building.get(i + 1).longitude();
                double y2 = building.get(i + 1).latitude();
                Line2D line = new Line2D.Double();
                line.setLine(x1, y1, x2, y2);
                if (pseudoLandmarks) {
                    int coincideBetweenTwoLines = 0;
                    if (x1 == currentPosition.longitude & y1 == currentPosition.latitude) {
                        coincideBetweenTwoLines++;
                    }
                    if (x1 == nextPosition.longitude & y1 == nextPosition.latitude) {
                        coincideBetweenTwoLines++;
                    }
                    if (x2 == currentPosition.longitude & y2 == currentPosition.latitude) {
                        coincideBetweenTwoLines++;
                    }
                    if (x2 == nextPosition.longitude & y2 == nextPosition.latitude) {
                        coincideBetweenTwoLines++;
                    }
                    if (coincideBetweenTwoLines == 2) {
                        return true;
                    } else if (coincideBetweenTwoLines == 1) {
                        cornerIntersect++;
                        continue;
                    } else if (coincideBetweenTwoLines > 2) {
                        System.out.println(coincideBetweenTwoLines + " coincide between 4 points.");
                        return false;
                    }
                }
                if (line.intersectsLine(move)) {
                    return false;
                }
            }
        }
        if (pseudoLandmarks & cornerIntersect > 2) {
            return false;
        }
        return true;
    }

    public Node turningPoints(LongLat start, LongLat destination) {
        double heuristic = start.distanceTo(destination);
        Node current = new Node(0, heuristic, start, -1);
        ArrayList<String> explored = new ArrayList<>();
        explored.add(current.position.longitude + "," + current.position.latitude);
        ArrayList<Node> fronts = new ArrayList<>();
        while (true) {
            if (isValidMove(current.position, destination, true)) {
                Node n = new Node(0, 0, destination, 0);
                n.parent = current;
                return n;
            }
            for (Polygon p : noFlyZones) {
                List<Point> building = p.coordinates().get(0);
                for (int i = 0; i < building.size(); i++) {
                    double lng = building.get(i).longitude();
                    double lat = building.get(i).latitude();
                    LongLat pointOnBuilding = new LongLat(lng, lat);
                    String pointToString = lng + "," + lat;
                    if (!isValidMove(current.position, pointOnBuilding, true) |
                            explored.contains(pointToString)) {
                        continue;
                    }
                    explored.add(pointToString);
                    heuristic = pointOnBuilding.distanceTo(destination);
                    double moved = pointOnBuilding.distanceTo(current.position);
                    Node front = new Node(moved + current.moved, heuristic, pointOnBuilding, -1);
                    front.parent = current;
                    fronts.add(front);
                }
            }
            if (fronts.size() == 0) {
                break;
            }
            Node nextNode = Collections.min(fronts, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return ((Double) o1.cost).compareTo((Double) o2.cost);
                }
            });
            fronts.remove(nextNode);
            current = nextNode;
        }
        return null;
    }

    /**
     * Apply A* search finding the optimal route to destination, the current position
     * and destination should be roughly on a straight line that is not cutting through
     * no-fly-zone, otherwise the time for computation would be too long.
     *
     * @param start       the initial position of the route
     * @param destination the destination where the route ends
     * @return the optimal route from current state to the destination
     */
    public Node straightRoute(LongLat start, LongLat destination) {
        double heuristic = start.distanceTo(destination);
        Node current = new Node(0, heuristic, start, -1);
        ArrayList<String> explored = new ArrayList<>();
        explored.add(current.position.longitude + "," + current.position.latitude);
        ArrayList<Node> fronts = new ArrayList<>();
        while (true) {
            if (current.position.closeTo(destination)) {
                return current;
            }
            double latDiff = destination.latitude - current.position.latitude;
            double lngDiff = destination.longitude - current.position.longitude;
            int direction = (int) (Math.toDegrees(Math.atan2(latDiff, lngDiff)) / 10) * 10;
            ArrayList<Node> frontsFromPoint = new ArrayList<>();
            for (int i = -90; i <= 100; i += 10) {
                direction = (direction + i + 360) % 360;
                LongLat nextPosition = current.position.nextPosition(direction);
                String toString = nextPosition.longitude + "," + nextPosition.latitude;
                if (!isValidMove(current.position, nextPosition, false) | explored.contains(toString)) {
                    continue;
                }
                explored.add(toString);
                heuristic = destination.distanceTo(nextPosition);
                Node front = new Node(current.moved + Const.DISTANCE_MOVE, heuristic, nextPosition, direction);
                front.parent = current;
                frontsFromPoint.add(front);
            }
            fronts.addAll(frontsFromPoint);
            if (frontsFromPoint.size() == 0) {
                for (int i = 0; i < 360; i += 10) {
                    LongLat nextPosition = current.position.nextPosition(i);
                    String toString = nextPosition.longitude + "," + nextPosition.latitude;
                    if (!isValidMove(current.position, nextPosition, false) | explored.contains(toString)) {
                        continue;
                    }
                    explored.add(toString);
                    heuristic = destination.distanceTo(nextPosition);
                    Node front = new Node(current.moved + Const.DISTANCE_MOVE, heuristic, nextPosition, i);
                    front.parent = current;
                    fronts.add(front);
                }
            }
            if (fronts.size() == 0) {
                break;
            }
            Node nextNode = Collections.min(fronts, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return ((Double) o1.cost).compareTo((Double) o2.cost);
                }
            });
            fronts.remove(nextNode);
            current = nextNode;
        }
        return null;
    }
}
