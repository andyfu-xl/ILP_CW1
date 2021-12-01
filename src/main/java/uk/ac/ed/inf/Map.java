package uk.ac.ed.inf;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;
import com.mapbox.turf.TurfJoins.*;

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
    }

    /**
     * Constructor of the map
     *
     * @param dronePosition position of the drone
     * @param noFlyZones    buildings that drones cannot fly through
     */
    public Map(LongLat dronePosition, ArrayList<Polygon> noFlyZones) {
        this.dronePosition = dronePosition;
        this.noFlyZones = noFlyZones;
        initializePaths(true);
        initializePaths(false);
    }

    // getters
    public ArrayList<Path> getFlightPath() {
        return flightPath;
    }

    public List<Point> getFlightLine() {
        return flightLine;
    }

    public ArrayList<Path> getFlightPathBack() {
        return flightPathBack;
    }

    public List<Point> getFlightLineBack() {
        return flightLineBack;
    }

    public ArrayList<Polygon> getNoFlyZones() {
        return noFlyZones;
    }

    /**
     * initialize variables for storing the flight path.
     *
     * @param back where initialize the flight path back to the appleton tower
     */
    public void initializePaths(boolean back) {
        if (back) {
            flightPathBack = new ArrayList<>();
            flightLineBack = new ArrayList<>();
        } else {
            flightPath = new ArrayList<>();
            flightLine = new ArrayList<>();
        }
    }

    /**
     * Generate the path given an ArrayList of turningPoints
     * The flights will fly roughly straight unless it passes a turningPoint
     *
     * @param turningPoints position where the drone is about to turn
     * @param orderNumber   order number which will be written into the database
     * @return number of move of the path
     */
    public int turningPointsToPath(ArrayList<LongLat> turningPoints, String orderNumber, Boolean back) {
        int moveNumber = 0;
        if (turningPoints.size() <= 1) {
            return moveNumber;
        }
        LongLat initialPos = turningPoints.get(0);
        initializePaths(back);

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
            /*
              <n> is a linked list of positions, the head is close to the target (turning point)
              the linked list is the path from initialPos to target, <n>'s parent is closer to
              initialPos, the drone will move from its parent to it.
             */
            Node n = straightRoute(initialPos, target);
            /*
              the drone will be at n's position before it travels to the next target (turning point).
             */
            initialPos = n.position;
            // The linked list is in reserved order of the flight, so I flip it
            ArrayList<Path> reversedPaths = new ArrayList<>();
            ArrayList<Point> reversedPoints = new ArrayList<>();
            while (n.parent != null) {
                Path path = new Path(orderNumber, n.parent.position.longitude,
                        n.parent.position.latitude, n.angle, n.position.longitude,
                        n.position.latitude);
                moveNumber++;
                reversedPaths.add(path);
                reversedPoints.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
                n = n.parent;
            }
            Collections.reverse(reversedPoints);
            Collections.reverse(reversedPaths);
            // The drone fly back to the appleton tower
            if (back) {
                this.flightPathBack.addAll(reversedPaths);
                this.flightLineBack.addAll(reversedPoints);
            }
            // The drone deliver an order
            else {
                this.flightPath.addAll(reversedPaths);
                this.flightLine.addAll(reversedPoints);
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
        // Line2D representing a move
        Line2D move = new Line2D.Double();
        if (pseudoLandmarks) {
            // move both end of the line slightly so they are not on corners of no-fly-zones.
            double lengthOfMove = currentPosition.distanceTo(nextPosition);
            double x1 = currentPosition.longitude + Const.DISTANCE_MOVE * 0.01 *
                    (nextPosition.longitude - currentPosition.longitude) * (1 / lengthOfMove);
            double y1 = currentPosition.latitude + Const.DISTANCE_MOVE * 0.01 *
                    (nextPosition.latitude - currentPosition.latitude) * (1 / lengthOfMove);
            double x2 = nextPosition.longitude + Const.DISTANCE_MOVE * 0.01 *
                    (currentPosition.longitude - nextPosition.longitude) * (1 / lengthOfMove);
            double y2 = nextPosition.latitude + Const.DISTANCE_MOVE * 0.01 *
                    (currentPosition.latitude - nextPosition.latitude) * (1 / lengthOfMove);
            move.setLine(x1, y1, x2, y2);
        }
        else {
            move.setLine(currentPosition.longitude, currentPosition.latitude,
                    nextPosition.longitude, nextPosition.latitude);
        }
        for (Polygon p : noFlyZones) {
            // the starts and end of the move should not in any building (no-fly-zones polygon)
            Point p1 = Point.fromLngLat(move.getX1(), move.getY1());
            Point p2 = Point.fromLngLat(move.getX2(), move.getY2());
            if (TurfJoins.inside(p1, p) | TurfJoins.inside(p2, p)) {
                return false;
            }
            List<Point> building = p.coordinates().get(0);
            for (int i = 0; i < building.size() - 1; i++) {
                double x1 = building.get(i).longitude();
                double y1 = building.get(i).latitude();
                double x2 = building.get(i + 1).longitude();
                double y2 = building.get(i + 1).latitude();
                Line2D line = new Line2D.Double();
                line.setLine(x1, y1, x2, y2);
                /*
                    if the move intersects with one edge, the move is illegal.
                 */
                if (line.intersectsLine(move)) {
                    return false;
                }
                if (pseudoLandmarks) {
                    Line2D from = new Line2D.Double();
                    from.setLine(currentPosition.longitude, currentPosition.latitude,
                            currentPosition.longitude, currentPosition.latitude);
                    Line2D to = new Line2D.Double();
                    to.setLine(nextPosition.longitude, nextPosition.latitude,
                            nextPosition.longitude, nextPosition.latitude);
                    if (line.intersectsLine(from) & line.intersectsLine(to)) {
                        return true;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Apply A* search finding the optimal route to destination,
     * assuming the drone only turn at the nodes of no-fly-zones, and it can
     * fly in any direction.
     *
     * @param start       the initial position of the route
     * @param destination the destination where the route ends
     * @return the turning points to the destination
     */
    public Node turningPoints(LongLat start, LongLat destination) {
        // the heuristic is the straight distance to the destination (ignoring no-fly-zones)
        double heuristic = start.distanceTo(destination);
        // initial state
        Node current = new Node(0, heuristic, start, -1);
        ArrayList<String> explored = new ArrayList<>();
        // The algorithm do not reconsider any points, for lower complexity
        explored.add(current.position.longitude + "," + current.position.latitude);
        ArrayList<Node> fronts = new ArrayList<>();
        while (true) {
            // termination condition, the destination is reached
            if (isValidMove(current.position, destination, true)) {
                Node n = new Node(0, 0, destination, 0);
                n.parent = current;
                return n;
            }
            // A-star search, record unvisited expanded nodes (fronts)
            for (Polygon p : noFlyZones) {
                List<Point> building = p.coordinates().get(0);
                for (int i = 0; i < building.size(); i++) {
                    double lng = building.get(i).longitude();
                    double lat = building.get(i).latitude();
                    LongLat pointOnBuilding = new LongLat(lng, lat);
                    String pointToString = lng + "," + lat;
                    /*
                      the drone is allowed to cut the edge of no-fly-zone for simplicity,
                      an unvisited node will not be recorded if the drone goes inside no-fly-zones.
                     */
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
            // if the there is no way to go next, the algorithm failed to find a path
            if (fronts.size() == 0) {
                break;
            }
            // pick the node with minimum cost+heuristic for next iteration.
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
     * Apply A* search finding the optimal route to destination,
     * the current positionc and destination should be roughly on a straight line
     * that won't cutting through no-fly-zone
     *
     * @param start       the initial position of the route
     * @param destination the destination where the route ends
     * @return the optimal route from current state to the destination
     */
    public Node straightRoute(LongLat start, LongLat destination) {
        // the heuristic is the straight distance to the destination (ignoring no-fly-zones)
        double heuristic = start.distanceTo(destination);
        // initial state
        Node current = new Node(0, heuristic, start, -1);
        ArrayList<String> explored = new ArrayList<>();
        // The algorithm do not reconsider any points, for lower complexity
        explored.add(current.position.longitude + "," + current.position.latitude);
        ArrayList<Node> fronts = new ArrayList<>();
        while (true) {
            // termination condition, the destination is reached
            if (current.position.closeTo(destination)) {
                return current;
            }
            /*
               Since we are searching for path with no big change of direction,
               We assume the drone will often move towards the target, so we cut
               half of directions to speed up the algorithm.
             */
            double latDiff = destination.latitude - current.position.latitude;
            double lngDiff = destination.longitude - current.position.longitude;
            int direction = (int) (Math.toDegrees(Math.atan2(latDiff, lngDiff)) / 10) * 10;
            ArrayList<Node> frontsFromPoint = new ArrayList<>();
            // A-star search, record unvisited expanded nodes (fronts)
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
            // in case the drone reaches a dead end, the algorithm will consider the route going backwards.
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
            // if the there is no way to go next, the algorithm failed to find a path
            if (fronts.size() == 0) {
                break;
            }
            // pick the node with minimum cost+heuristic for next iteration.
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
