package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Point;

import java.awt.*;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static String date;

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new IllegalArgumentException("Error: Illegal argument, should be [dd mm " +
                    "year port1 port2]");
        }
        date = args[2] + "-" + args[1] + "-" + args[0];
        DataParser parser = new DataParser(Const.IP, args[3]);
        DatabaseConnection database = new DatabaseConnection(Const.IP, args[4]);
        Drone drone = new Drone(date, parser, database);
        drone.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        List<Point> pl = new ArrayList<>();
        pl.add(Point.fromLngLat(Const.APT_LONG, Const.APT_LAT));
        while (drone.pathForOrder()) {
            pl.addAll(drone.getFlightLine());

            if (drone.getOrders().size() == 0) {
                break;
            }
            drone.selectOrderByUtility();
            //System.out.println(drone.getOrders().size() + "ggggggggggggggggggg");
        }
        LineString lineString = LineString.fromLngLats(pl);
        Geometry geometry = lineString;
        Feature f = Feature.fromGeometry(geometry);
        FeatureCollection fc = FeatureCollection.fromFeature(f);
        System.out.println(fc.toJson());
        System.out.println(drone.getBattery());
    }
}
