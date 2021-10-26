package uk.ac.ed.inf;

import java.awt.*;
import java.util.ArrayList;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Drone {

    private LongLat dronePosition;
    private int battery;
    private ArrayList<Order> orders;
    private ArrayList<Polygon> noFlyZones;
    private ArrayList<Point> landmarks;
    private String date;
    private DataParser parser;
    private DatabaseConnection database;

    public Drone (String date, DataParser parser, DatabaseConnection database) {
        this.date = date;
        this.parser = parser;
        this.database = database;
        this.battery = Const.MAX_POWER;
        this.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        readDrone();
    }

    private void readDrone() {
        this.orders = this.database.readOrders(this.date);
        this.noFlyZones = this.parser.readNoFlyZones();
        this.landmarks = this.parser.readLandmarks();
    }
}
