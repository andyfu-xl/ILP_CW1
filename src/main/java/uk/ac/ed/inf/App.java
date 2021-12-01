package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import com.mapbox.geojson.Point;

import java.io.FileWriter;
import java.io.IOException;
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

        HttpConnection parser = new HttpConnection(Const.IP, portHttp);
        DatabaseConnection database = new DatabaseConnection(Const.IP, portDatabase);
        // Initialize the drone and map.
        Drone drone = new Drone(date, parser, database);
        drone.map.dronePosition = new LongLat(Const.APT_LONG, Const.APT_LAT);
        drone.pathForDate();
        // List of points storing the data to be written in the geojson file.
        List<Point> outputPoints = drone.getOutputPoints();
        // List of paths storing the data to be written in the database.
        List<Path> outputPaths = drone.getOutputPaths();

        database.writePaths(outputPaths);
        // Convert list of points to FeatureCollection and write into a file.
        LineString lineString = LineString.fromLngLats(outputPoints);
        Feature f = Feature.fromGeometry(lineString);
        FeatureCollection fc = FeatureCollection.fromFeature(f);
        date = dd + "-" + mm + "-" + year;
        writeFile(fc.toJson(), date);
        //System.out.println(fc.toJson()); // uncomment for print the json data

        System.out.println("total number of moves: "
                + (Const.MAX_POWER - drone.getBattery() + drone.getEnergyBack()));
    }
}
