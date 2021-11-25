package uk.ac.ed.inf;

import java.awt.geom.Line2D;
import java.beans.beancontext.BeanContextChild;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Drone {

    private int battery;
    private ArrayList<Order> orders;
    private Order currentOrder;
    private String date;
    private DataParser parser;
    private DatabaseConnection database;
    public Map map;
    private ArrayList<Path> flightPathBack;
    private List<Point> flightLineBack;

    public Drone(String date, DataParser parser, DatabaseConnection database) {
        this.date = date;
        this.parser = parser;
        this.database = database;
        this.battery = Const.MAX_POWER;
        readDrone();
    }

    public ArrayList<Order> getOrders() {
        return orders;
    }

    public Order getCurrentOrder() {
        return currentOrder;
    }

    public int getBattery() {
        return battery;
    }

    private void readDrone() {
        this.orders = this.database.readOrders(this.date);
        ArrayList<Polygon> noFlyZones = this.parser.readNoFlyZones();
        LongLat dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        this.map = new Map(dronePosition, noFlyZones);
        selectOrderByUtility();
    }

    public void selectOrderByUtility() {
        for (int i = 0; i < orders.size(); i++) {
            orders.get(i).estimateUtility(map.dronePosition, this.parser);
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

    public boolean backAPT(LongLat pos) {
        LongLat apt = new LongLat(Const.APT_LONG, Const.APT_LAT);
        if (pos.closeTo(apt) & battery >= 0) {
            return true;
        }
        Map.Node routeBack = map.aStarSearch(pos, apt);
        ArrayList<Point> pathFrame = new ArrayList<>();
        int moveNumber = 0;
        while (routeBack.parent != null) {
            pathFrame.add(Point.fromLngLat(routeBack.position.longitude, routeBack.position.latitude));
            routeBack = routeBack.parent;
        }
        pathFrame.add(Point.fromLngLat(routeBack.position.longitude, routeBack.position.latitude));
        Collections.reverse(pathFrame);
        moveNumber = map.pathBackFromFrame(pathFrame);
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
                routeToNext = map.aStarSearch(map.dronePosition, loc);
                allRoutes.add(routeToNext);
//                Map.Node hoveringStep = new Map.Node(0, 0, loc, -999);
//                allRoutes.add(hoveringStep);
                map.dronePosition = routeToNext.position;
                //System.out.println("HHHHHHHHHHHHHH");
            }
            //System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAA");
            //System.out.println(initialState.position.longitude + "," + initialState.position.latitude);
            //System.out.println(deliverTo.longitude + "," + deliverTo.latitude);
            //goTo = aStarSearch(initialState, deliverTo);
            routeToNext = map.aStarSearch(map.dronePosition, deliverTo);
            allRoutes.add(routeToNext);
//            Map.Node hoveringStep = new Map.Node(0, 0, deliverTo, -999);
//            allRoutes.add(hoveringStep);
            //System.out.println("goTo is NUll: " + (goTo == null));
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
//        flightPath = new ArrayList<>();
        ArrayList<Point> pathFrame = new ArrayList<>();
        int moveNumber = 0;
        for (Map.Node n : allRoutes) {
//            ArrayList<Path> tempPath = new ArrayList<>();
            ArrayList<Point> tempPoints = new ArrayList<>();
//            if (n.parent != null) {
//                tempPoints.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
//            }
            while (n.parent != null) {
//                Path path = new Path(currentOrder.getOrderNo(), n.position.longitude,
//                        n.position.latitude, n.angle, n.parent.position.longitude,
//                        n.parent.position.latitude);
//                numberOfMoves++;
//                tempPath.add(path);
                tempPoints.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
                n = n.parent;
            }
            tempPoints.add(Point.fromLngLat(n.position.longitude, n.position.latitude));
            Collections.reverse(tempPoints);
//            Collections.reverse(tempPath);
            pathFrame.addAll(tempPoints);
//            flightPath.addAll(tempPath);
        }
        System.out.println("hhhhhhhh");
        for (Point p : pathFrame) {
            System.out.println(p.longitude() + ",,,,," + p.latitude());
        }
        moveNumber = map.pathFromFrame(pathFrame, currentOrder);
        battery -= moveNumber;
        System.out.println("[" + map.dronePosition.longitude + "," + map.dronePosition.latitude + "],");
        if (!backAPT(map.dronePosition)) {
            map.dronePosition = initialPosition;
            return false;
        }
        return true;
    }
}
