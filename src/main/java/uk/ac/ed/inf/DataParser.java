package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Parsing data of orders in Json file,
 * and data of map details in Geojson file.
 */
public class DataParser {
    // server that the class read data from.
    private HttpConnection server;

    /**
     * Constructor of DataParser
     *
     * @param name name (IP) of the server.
     * @param port port number the class will connect with.
     */
    public DataParser(String name, String port) {
        this.server = new HttpConnection(name, port);
    }

    /**
     * Read menus file from the server, store menus as ArrayList of shops.
     *
     * @return List of shops which stores menu details.
     */
    public ArrayList<Shop> readMenus() {
        server.connectHttp(Const.PATH_MENUS);
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = new ArrayList<>();
        shops = new Gson().fromJson(server.getJson(), listType);
        return shops;
    }

    /**
     * Read no-fly-zones file from the server, store no-fly-zone as list of polygons.
     */

    /**
     * Read no-fly-zones file from the server, store no-fly-zones as ArrayList of polygons.
     *
     * @return Each polygon in the ArrayList representing a building (no-fly-zone).
     */
    public ArrayList<Polygon> readNoFlyZones() {
        server.connectHttp(Const.PATH_NO_FLY_ZONES);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        ArrayList<Polygon> buildings = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            buildings.add((Polygon) feature.geometry());
        }
        return buildings;
    }

    /**
     * Read landmarks file from the server, store landmarks as ArrayList of points.
     * Landmarks is never used in my algorithm
     *
     * @return landmarks in an ArrayList.
     */
    // TODO: 2021/11/28  delete it.
    public ArrayList<Point> readLandmarks() {
        server.connectHttp(Const.PATH_LANDMARKS);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        ArrayList<Point> landmarks = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            landmarks.add((Point) feature.geometry());
        }
        return landmarks;
    }

    /**
     * Parse coordinates of the center of deliverTo,
     * from a string of whatThreeWords address in json format.
     *
     * @param json string of whatThreeWords address in json format
     *             , we only need the coordinate of the center
     * @return center coordinate of the address in a LongLat object.
     */
    public LongLat parseCoordinate (String json) {
        json = json.split("},\n  \"words")[0];
        json = json.split("coordinates\": \\{")[1];
        json = json.replaceAll(" ","");
        json = json.replaceAll("\n","");
        json = json.replaceAll("\"lng\":","");
        json = json.replaceAll("\"lat\":","");
        double lng = Double.valueOf(json.split(",")[0]);
        double lat = Double.valueOf(json.split(",")[1]);
        return (new LongLat(lng, lat));
    }

    /**
     * Converting WhatThreeWords addresses into coordinates.
     *
     * @param whatThreeWords WhatThreeWords, three words in one String, each word is
     *           separated with "." from others.
     * @return Coordinate of the center of address represented by whatThreeWords
     */
    public LongLat wordsToLongLat(String whatThreeWords) {
        String[] words = whatThreeWords.split("\\.");
        if (words.length != 3) {
            System.out.println("Error: Invalid WhatThreeWords " + whatThreeWords);
            throw (new IllegalArgumentException());
        }
        String pathJson = Const.PATH_WORDS + "/" + words[0] + "/" + words[1] + "/" +
                words[2] + Const.DETAIL;
        this.server.connectHttp(pathJson);
        String json = this.server.getJson();
        return parseCoordinate(json);
    }

}
