package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Point;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * App for creating flight path
 */
public class App {
    /**
     * Output json file.
     *
     * @param json json of the flightpath of the drone
     * @param date the date of the flightpath, in the format of dd-mm-year
     */
    private static void writeFile(String json, String date) {
        try {
            FileWriter writer = new FileWriter(
                    "drone-" + date + ".geojson");
            writer.write(json);
            writer.close();
            System.out.println("Flightpath GeoJson file created.");
        } catch (IOException e) {
            System.err.println("Error: Failed to create flightpath geojson file.");
            e.printStackTrace();
        }
    }

    /**
     * @param args args[0], args[1], args[2] is day, month, year of flight;
     *             args[3] is the port number of the server;
     *             args[4] is the port number of the database.
     */
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
        String date = year + "-" + mm + "-" + dd;

        DataParser parser = new DataParser(Const.IP, portHttp);
        DatabaseConnection database = new DatabaseConnection(Const.IP, portDatabase);
        // Initialize the drone and map.
        Drone drone = new Drone(date, parser, database);
        drone.map.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        // List of points storing the data to be written in the geojson file.
        List<Point> points = new ArrayList<>();
        points.add(Point.fromLngLat(Const.APT_LONG, Const.APT_LAT));
        // List of paths storing the data to be written in the database.
        List<Path> paths = new ArrayList<>();

        // The path before the drone back to Appleton Tower (before running out battery)
        while (drone.pathForOrder()) {
            points.addAll(drone.map.getFlightLine());
            paths.addAll(drone.map.getFlightPath());
            if (drone.getOrders().size() == 0) {
                break;
            }
            drone.selectOrderByUtility();
        }
        // The drone cannot back to Appleton Tower if it deliver for the next order.
        // Adding the path of the drone back to Appleton Tower.
        if (drone.backAPT(drone.map.dronePosition)) {
            points.addAll(drone.map.getFlightLineBack());
            paths.addAll(drone.map.getFlightPathBack());
        }
        else {
            // This will never be reached unless there are bugs.
            System.err.println("Error: The drone cannot back to the APT.");
            return;
        }
        database.writePaths(paths);

        // Convert list of points to FeatureCollection and write into a file.
        LineString lineString = LineString.fromLngLats(points);
        Feature f = Feature.fromGeometry(lineString);
        FeatureCollection fc = FeatureCollection.fromFeature(f);
        date = dd + "-" + mm + "-" + year;
        writeFile(fc.toJson(), date);
        System.out.println(fc.toJson());

        System.out.println("total number of moves: "
                + (1500 - drone.getBattery() + drone.getBatteryBack()));
    }
}
