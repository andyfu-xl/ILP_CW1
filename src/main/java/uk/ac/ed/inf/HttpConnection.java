package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

/**
 * Connect with Http server which stores Json and Geojson files.
 * Parsing data of orders in Json file, and data of map details in Geojson file.
 */
public class HttpConnection {
    /* Just have one HttpClient, use it multiple times. We have one
   client so it's declared as static, it won't update (final) */
    private static final HttpClient client = HttpClient.newHttpClient();
    private String json;
    private String name;
    private String port;

    /**
     * Constructor of DataParser
     *
     * @param name name (IP) of the server.
     * @param port port number the class will connect with.
     */
    public HttpConnection(String name, String port) {
        this.name = name;
        this.port = port;
    }


    /**
     * Access file in the server, and store the information in json format.
     * The protocol should be HTTP
     *
     * @param file the URL of the server file
     */
    public void connectHttp(String file) {
        String urlString = ("http://" + this.name + ":" + this.port + file);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString)).build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                this.json = response.body();
            } else {
                System.out.println("Fatal error. Response code: "
                        + response.statusCode() + ". URL:" + file + ".");
                System.exit(1);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Fatal error: Unable to connect to " + this.name
                    + " at port " + this.port + ".");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Read menus file from the server, store menus as ArrayList of shops.
     *
     * @return List of shops which stores menu details.
     */
    public ArrayList<Shop> readMenus() {
        connectHttp(Const.PATH_MENUS);
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = new ArrayList<>();
        shops = new Gson().fromJson(json, listType);
        return shops;
    }

    /**
     * Read no-fly-zones file from the server, store no-fly-zones as ArrayList of polygons.
     *
     * @return Each polygon in the ArrayList representing a building (no-fly-zone).
     */
    public ArrayList<Polygon> readNoFlyZones() {
        connectHttp(Const.PATH_NO_FLY_ZONES);
        FeatureCollection featureCollection = FeatureCollection.fromJson(json);
        ArrayList<Polygon> buildings = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            buildings.add((Polygon) feature.geometry());
        }
        return buildings;
    }

    /**
     * Parse coordinates of the center of deliverTo,
     * from a string of whatThreeWords address in json format.
     *
     * @param address string of whatThreeWords address in json format
     *             , we only need the coordinate of the center
     * @return center coordinate of the address in a LongLat object.
     */
    public LongLat parseCoordinate (String address) {
        address = address.split("},\n  \"words")[0];
        address = address.split("coordinates\": \\{")[1];
        address = address.replaceAll(" ","");
        address = address.replaceAll("\n","");
        address = address.replaceAll("\"lng\":","");
        address = address.replaceAll("\"lat\":","");
        double lng = Double.valueOf(address.split(",")[0]);
        double lat = Double.valueOf(address.split(",")[1]);
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
        this.connectHttp(pathJson);
        return parseCoordinate(json);
    }

}
