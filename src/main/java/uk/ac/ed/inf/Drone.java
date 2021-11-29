package uk.ac.ed.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * The class for the drone
 */
public class Drone {

    // private variables
    private int battery;
    private ArrayList<Order> orders;
    private Order currentOrder;
    private String date;
    private DataParser parser;
    private DatabaseConnection database;
    public Map map;
    private ArrayList<Path> flightPathBack;
    private List<Point> flightLineBack;
    private int batteryBack;

    /**
     * Constructor of the drone.
     *
     * @param date date of flight
     * @param parser providing data from json and geojson files in the server.
     * @param database providing data from the database in the server.
     */
    public Drone(String date, DataParser parser, DatabaseConnection database) {
        this.date = date;
        this.parser = parser;
        this.database = database;
        // the drone has full battery as the beginning
        this.battery = Const.MAX_POWER;
        readDrone();
    }

    // getters
    public ArrayList<Order> getOrders() {
        return orders;
    }
    public Order getCurrentOrder() {
        return currentOrder;
    }
    public int getBattery() {
        return battery;
    }
    public int getBatteryBack() {
        return batteryBack;
    }

    /**
     * initialize the drone using information in the server
     */
    private void readDrone() {
        this.orders = this.database.readOrders(this.date);
        ArrayList<Polygon> noFlyZones = this.parser.readNoFlyZones();
        // the drone launches from the Appleton Tower
        LongLat dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        this.map = new Map(dronePosition, noFlyZones);
        /*
          select the first order to be delivered and remove it from the
          ArrayList of all orders
         */
        selectOrderByUtility();
    }

    /**
     * select the order with highest utility and remove it from orders,
     * the utility is calculated as monetary value divided by the estimated distance.
     */
    public void selectOrderByUtility() {
        for (int i = 0; i < orders.size(); i++) {
            orders.get(i).estimateUtility(map.dronePosition, this.parser);
        }
        // search for the order with maximum estimated utility
        this.currentOrder = Collections.max(this.orders, new Comparator<>() {
            @Override
            public int compare(Order o1, Order o2) {
                return Double.valueOf(o1.getUtility()).compareTo(
                        Double.valueOf(o2.getUtility()));
            }
        });
        this.orders.remove(currentOrder);
    }

    /**
     * Compute the route of the drone back to the appleton tower from a given position
     * The computed route will be stored in the map
     *
     * @param pos the position which the drone starts
     * @return ture if the drone has enough battery to go back
     */
    public boolean backAPT(LongLat pos) {
        LongLat apt = new LongLat(Const.APT_LONG, Const.APT_LAT);
        // the drone is already close to the Appleton Tower
        if (pos.closeTo(apt) & battery >= 0) {
            return true;
        }
        // convert linked list to arrayList
        Map.Node turningPointsLinked = map.turningPoints(pos, apt);
        ArrayList<LongLat> turningPointsList = new ArrayList<>();
        while (turningPointsLinked.parent != null) {
            turningPointsList.add(turningPointsLinked.position);
            turningPointsLinked = turningPointsLinked.parent;
        }
        turningPointsList.add(turningPointsLinked.position);
        Collections.reverse(turningPointsList);
        // no order to deliver, the orderNumber is set to 0.
        int moveNumber = map.turningPointsToPath(turningPointsList, "0", true);
        batteryBack = moveNumber;
        if (battery >= moveNumber) {
            return true;
        }
        return false;
    }

    /**
     * Compute path for delivering the current order, store the path in map
     *
     * @return ture if the drone has enough power going back to the Appleton tower
     *          after the current order is deliver, otherwise the order will not be delivered.
     */
    public boolean pathForOrder() {
        LongLat initialPosition = map.dronePosition;
        LongLat deliverTo = parser.wordsToLongLat(currentOrder.getDeliverTo());
        // angle of the first node are set to -1 as there is no last move
        ArrayList<Map.Node> allTurningPoint = new ArrayList<>();
        Map.Node turningPoints;
        try {
            // for each shop (there can be 2 shops)
            for (LongLat loc : currentOrder.getShopCoordinate()) {
                // record turning points for each shop.
                turningPoints = map.turningPoints(map.dronePosition, loc);
                allTurningPoint.add(turningPoints);
                map.dronePosition = turningPoints.position;
            }
            // record turning points for deliver the food
            turningPoints = map.turningPoints(map.dronePosition, deliverTo);
            allTurningPoint.add(turningPoints);
            map.dronePosition = turningPoints.position;
        } catch (Exception e) {
            e.printStackTrace();
            map.dronePosition = initialPosition;
            return false;
        }
        // Error, they should not be reached if the program runs as expected.
        if (allTurningPoint == null) {
            map.dronePosition = initialPosition;
            return false;
        }
        for (Map.Node n : allTurningPoint) {
            if (n == null) {
                map.dronePosition = initialPosition;
                return false;
            }
        }

        // convert linked list to array list
        ArrayList<LongLat> turningPointsList = new ArrayList<>();
        int moveNumber;
        for (Map.Node n : allTurningPoint) {
            ArrayList<LongLat> reversedPoints = new ArrayList<>();
            while (n.parent != null) {
                reversedPoints.add(n.position);
                n = n.parent;
            }
            reversedPoints.add(n.position);
            Collections.reverse(reversedPoints);
            turningPointsList.addAll(reversedPoints);
        }
        moveNumber = map.turningPointsToPath(turningPointsList, currentOrder.getOrderNo(), false);
        // subtract the battery before letting the drone back to the appleton tower
        battery -= moveNumber;
        if (!backAPT(map.dronePosition)) {
            // reset the drone status to before delivering this order.
            map.dronePosition = initialPosition;
            battery += moveNumber;
            return false;
        }
        return true;
    }
}
