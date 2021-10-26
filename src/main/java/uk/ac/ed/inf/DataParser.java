package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class DataParser {

    private HttpConnection server;

    public DataParser(String name, String port) {
        this.server = new HttpConnection(name, port);
    }

    /**
     * Read menus file from the server, store menus as list of shops.
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
     * Read landmarks file from the server, store landmarks as list of points.
     */
    public ArrayList<Point> readLandmarks() {
        server.connectHttp(Const.PATH_LANDMARKS);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        ArrayList<Point> landmarks = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            landmarks.add((Point) feature.geometry());
        }
        return landmarks;
    }

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
     * @param whatThreeWords WhatThreeWords, three words in one String, each word is
     *              separated with "." from others.
     */
    // TODO: 2021/10/26  catch this exception when calling this function.
    public LongLat wordsToLongLat(String whatThreeWords) {
        String[] words = whatThreeWords.split(".");
        if (words.length != 3) {
            throw (new IllegalArgumentException());
        }
        String pathJson = Const.PATH_WORDS + "/" + words[0] + "/" + words[1] + "/" +
                words[2] + Const.DETAIL;
        this.server.connectHttp(pathJson);
        String json = this.server.getJson();
        return parseCoordinate(json);
    }

}
