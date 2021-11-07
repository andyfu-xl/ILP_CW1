package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Point;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class App {

    private static String date;

    /**
     * Output json file.
     *
     * @param json json of the flightpath of the drone
     * @param date the date of the flightpath
     */
    private static void writeFile(String json, String date) {
        try {
            FileWriter writer = new FileWriter(
                    "drone-" + date + ".geojson");
            writer.write(json);
            writer.close();
            System.out.println("flightpath GeoJson successfully created!");
        } catch (IOException e) {
            System.out.println("Fatal error: flightpath GeoJson wasn't created.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 5) {
            throw new IllegalArgumentException("Error: Illegal argument, should be [dd mm " +
                    "year port1 port2]");
        }
        String portDatabase = args[4];
        String portHttp = args[3];
        String dd = args[2];
        String mm = args[1];
        String year = args[0];
        date = dd + "-" + mm + "-" + year;

        DataParser parser = new DataParser(Const.IP, portHttp);
        DatabaseConnection database = new DatabaseConnection(Const.IP, portDatabase);
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
        writeFile(fc.toJson(), date);
        System.out.println(drone.getBattery());
    }
}
