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

    public DataParser(String name, String port) {
        this.server = new ServerConnector(name, port);
    }

    public ArrayList<Shop> getShops() {
        return this.shops;
    }

    /**
     * Read menus file from the server, store menus as list of shops.
     */
    public void readMenus() {
        server.connectHttp(Const.URL_MENUS);
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        this.shops = new Gson().fromJson(server.getJson(), listType);
    }

    /**
     * Read no-fly-zones file from the server, store no-fly-zone as list of polygons.
     */
    public void readNoFlyZones() {
        server.connectHttp(Const.URL_NO_FLY_ZONES);
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
        server.connectHttp(Const.URL_LANDMARKS);
        FeatureCollection featureCollection = FeatureCollection.fromJson(server.getJson());
        this.landmarks = new ArrayList<>();
        for (Feature feature : featureCollection.features()) {
            this.landmarks.add((Point) feature.geometry());
        }
    }

    public void readOrders() {
        server.connectJDBC(Const.DATABASE);
        ServerConnector sc = new ServerConnector("localhost", "9876");
        sc.connectJDBC(Const.DATABASE);
        final String coursesQuery =
                "select * from ORDERS where DELIVERYDATE=(?)";
        ArrayList<String> locList = new ArrayList<>();
        try {
            PreparedStatement psCourseQuery = sc.getConn().prepareStatement(coursesQuery);
            psCourseQuery.setString(1, "2022-1-1");
            ResultSet rs = psCourseQuery.executeQuery();
            while (rs.next()) {
                String loc = rs.getString("DELIVERTO");
                System.out.println(loc);
                locList.add(loc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
