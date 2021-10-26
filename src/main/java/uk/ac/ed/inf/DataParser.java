package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DataParser {

    private ServerConnector server;

    private ArrayList<Shop> shops;
    private ArrayList<Polygon> buildings;
    private ArrayList<Point> landmarks;
    private ArrayList<Order> orders;
    private LongLat deliverTo;

    public DataParser(String name, String port) {
        this.server = new ServerConnector(name, port);
    }

    public ArrayList<Shop> getShops() {
        return this.shops;
    }
    public ArrayList<Polygon> getBuildings() {
        return this.buildings;
    }
    public ArrayList<Point> getLandmarks() {
        return this.landmarks;
    }
    public ArrayList<Order> getOrders() {
        return this.orders;
    }
    public LongLat getDeliverTo() {
        return this.deliverTo;
    }


    /**
     * Read menus file from the server, store menus as list of shops.
     */
    public void readMenus() {
        server.connectHttp(Const.PATH_MENUS);
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        this.shops = new Gson().fromJson(server.getJson(), listType);
    }

    /**
     * Read no-fly-zones file from the server, store no-fly-zone as list of polygons.
     */
    public void readNoFlyZones() {
        server.connectHttp(Const.PATH_NO_FLY_ZONES);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        this.buildings = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            this.buildings.add((Polygon) feature.geometry());
        }
    }

    /**
     * Read landmarks file from the server, store landmarks as list of points.
     */
    public void readLandmarks() {
        server.connectHttp(Const.PATH_LANDMARKS);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        this.landmarks = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            this.landmarks.add((Point) feature.geometry());
        }
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
    public void wordsToLongLat(String whatThreeWords) {
        String[] words = whatThreeWords.split(".");
        if (words.length != 3) {
            throw (new IllegalArgumentException());
        }
        String pathJson = Const.PATH_WORDS + "/" + words[0] + "/" + words[1] + "/" +
                words[2] + Const.DETAIL;
        server.connectHttp(pathJson);
        String json = server.getJson();
        this.deliverTo = parseCoordinate(json);
    }

    public ArrayList<String> getItemNames(ServerConnector sc, String orderNo) {
        final String coursesQuery =
                "select * from orderDetails where orderNo=(?)";
        ArrayList<String> itemNames = new ArrayList<>();
        try {
            PreparedStatement psCourseQuery = sc.getConn().prepareStatement(coursesQuery);
            psCourseQuery.setString(1, orderNo);
            ResultSet rs = psCourseQuery.executeQuery();
            while (rs.next()) {
                itemNames.add(rs.getString("item"));
            }
        } catch (SQLException e) {
            System.out.println("Error: Failed to retrieve items of order from date base.");
            e.printStackTrace();
        }
        return itemNames;
    }

    /**
     * Read order details from the server, store orders as list of order objects.
     */
    public void readOrders(String date) {
        server.connectJDBC(Const.DATABASE);
        ServerConnector sc = new ServerConnector("localhost", "9876");
        sc.connectJDBC(Const.DATABASE);
        final String coursesQuery =
                "select * from orders where deliveryDate=(?)";
        ArrayList<Order> orderList = new ArrayList<>();
        try {
            PreparedStatement psCourseQuery = sc.getConn().prepareStatement(coursesQuery);
            psCourseQuery.setString(1, date);
            ResultSet rs = psCourseQuery.executeQuery();
            while (rs.next()) {
                String orderNo = rs.getString("orderNo");
                String customer = rs.getString("customer");
                String deliverTo = rs.getString("deliverTo");
                ArrayList<String> itemNames = getItemNames(sc, orderNo);
                Order order = new Order(orderNo, date, customer, deliverTo, itemNames);
                orderList.add(order);
            }
        } catch (SQLException e) {
            System.out.println("Error: Failed to retrieve orders from data base.");
            e.printStackTrace();
        }
        this.orders = orderList;
    }

}
