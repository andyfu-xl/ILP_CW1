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
            System.out.println("Flightpath GeoJson file created.");
        } catch (IOException e) {
            System.out.println("Error: Failed to create flightpath geojson file.");
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
        String dd = args[0];
        String mm = args[1];
        String year = args[2];
        date = year + "-" + mm + "-" + dd;

        // TODO: 2021/11/15 add hovering step
        DataParser parser = new DataParser(Const.IP, portHttp);
        DatabaseConnection database = new DatabaseConnection(Const.IP, portDatabase);
        Drone drone = new Drone(date, parser, database);
        drone.map.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        List<Point> points = new ArrayList<>();
        points.add(Point.fromLngLat(Const.APT_LONG, Const.APT_LAT));
        List<Path> paths = new ArrayList<>();
        while (drone.pathForOrder()) {
            points.addAll(drone.map.getFlightLine());
            paths.addAll(drone.map.getFlightPath());
            //System.out.println(drone.getBattery());
            if (drone.getOrders().size() == 0) {
                break;
            }
            drone.selectOrderByUtility();
            //System.out.println(drone.getOrders().size() + "ggggggggggggggggggg");
        }
        if (drone.backAPT(drone.map.dronePosition)) {
            points.addAll(drone.map.getFlightLineBack());
            paths.addAll(drone.map.getFlightPathBack());
        }
        else {
            System.out.println("Error: The drone cannot back to the APT.");
            return;
        }
        database.writePaths(paths);
        LineString lineString = LineString.fromLngLats(points);
        Feature f = Feature.fromGeometry(lineString);
        FeatureCollection fc = FeatureCollection.fromFeature(f);
        date = dd + "-" + mm + "-" + year;
        writeFile(fc.toJson(), date);
        System.out.println(fc.toJson());
        System.out.println(drone.getBatteryBack());
        System.out.println(1500 - drone.getBattery() + drone.getBatteryBack());
    }
}
