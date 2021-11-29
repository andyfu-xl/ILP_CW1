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
        if (pos.closeTo(apt) & battery >= 0) {
            return true;
        }
        Map.Node routeBack = map.turningPoints(pos, apt);
        ArrayList<LongLat> pathFrame = new ArrayList<>();
        while (routeBack.parent != null) {
            pathFrame.add(routeBack.position);
            routeBack = routeBack.parent;
        }
        pathFrame.add(routeBack.position);
        Collections.reverse(pathFrame);
        int moveNumber = map.turningPointsToPathOrder(pathFrame, "0");
        batteryBack = moveNumber;
        if (battery >= moveNumber) {
            return true;
        }
        return false;
    }


    public boolean pathForOrder() {
        LongLat initialPosition = map.dronePosition;
        LongLat deliverTo = parser.wordsToLongLat(currentOrder.getDeliverTo());
        // angle of the first node are set to -1 as there is no last move
        ArrayList<Map.Node> allRoutes = new ArrayList<>();
        Map.Node routeToNext;
        try {
            for (LongLat loc : currentOrder.getShopCoordinate()) {
                routeToNext = map.turningPoints(map.dronePosition, loc);
                allRoutes.add(routeToNext);
                map.dronePosition = routeToNext.position;
            }
            routeToNext = map.turningPoints(map.dronePosition, deliverTo);
            allRoutes.add(routeToNext);
            map.dronePosition = routeToNext.position;
        } catch (Exception e) {
            e.printStackTrace();
            map.dronePosition = initialPosition;
            return false;
        }
        if (allRoutes == null) {
            map.dronePosition = initialPosition;
            return false;
        }
        for (Map.Node n : allRoutes) {
            if (n == null) {
                map.dronePosition = initialPosition;
                return false;
            }
        }
        ArrayList<LongLat> pathFrame = new ArrayList<>();
        int moveNumber = 0;
        for (Map.Node n : allRoutes) {
            ArrayList<LongLat> tempPoints = new ArrayList<>();
            while (n.parent != null) {
                tempPoints.add(n.position);
                n = n.parent;
            }
            tempPoints.add(n.position);
            Collections.reverse(tempPoints);
            pathFrame.addAll(tempPoints);
        }
        moveNumber = map.turningPointsToPathOrder(pathFrame, currentOrder.getOrderNo());
        battery -= moveNumber;
        System.out.println("[" + map.dronePosition.longitude + "," + map.dronePosition.latitude + "],");
        if (!backAPT(map.dronePosition)) {
            map.dronePosition = initialPosition;
            battery += moveNumber;
            return false;
        }
        return true;
    }
}
