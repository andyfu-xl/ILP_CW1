package uk.ac.ed.inf;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Line2D;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DatabaseTest {
    @Test
    public void testCreateTable() throws SQLException {
        DatabaseConnection dc = new DatabaseConnection("localhost", "9876");
        dc.connectJDBC(Const.DATABASE);
        dc.getStatement().execute("create table students(" +
                "name varchar(48), " +
                "matric char(8), " +
                "edinyear int)");
    }

    @Test
    public void testDeleteTable() throws SQLException {
        DatabaseConnection dc = new DatabaseConnection("localhost", "9876");
        dc.connectJDBC(Const.DATABASE);
        DatabaseMetaData databaseMetadata = dc.getConn().getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables(null, null, "STUDENTS", null);
        if (resultSet.next()) {
            dc.getStatement().execute("drop table students");
        }
    }

    @Test
    public void testReadData() throws SQLException {
        DatabaseConnection dc = new DatabaseConnection("localhost", "9876");
        dc.connectJDBC(Const.DATABASE);
        final String coursesQuery =
                "select * from ORDERS where DELIVERYDATE=(?)";
        PreparedStatement psCourseQuery =
                dc.getConn().prepareStatement(coursesQuery);
        psCourseQuery.setString(1, "2022-1-2");
        ArrayList<String> locList = new ArrayList<>();
        ResultSet rs = psCourseQuery.executeQuery();
        while (rs.next()) {
            String loc = rs.getString("DELIVERTO");
            System.out.println(loc);
            locList.add(loc);
        }
        System.out.println(locList.size());
    }

    @Test
    public void andy() {
        DatabaseConnection database = new DatabaseConnection("localhost", "9876");
        DataParser parser = new DataParser("localhost", "9898");
        Drone drone = new Drone("2023-10-17", parser, database);
//        drone.getNoFlyZones().get(0).coordinates().get(0);
//        double lng = drone.getNoFlyZones().get(0).coordinates().get(0).size();
//        System.out.println(drone.getNoFlyZones().get(0).coordinates().get(0));
        for (int i = 0; i < drone.getNoFlyZones().size(); i++) {
            List<Point> building = drone.getNoFlyZones().get(i).coordinates().get(0);
            for (int j = 0; j < building.size()-1; j++) {
                double x1 = building.get(j).longitude();
                double y1 = building.get(j).latitude();
                double x2 = building.get(j + 1).longitude();
                double y2 = building.get(j + 1).latitude();
                Line2D line = new Line2D.Double();
                line.setLine(x1, y1, x2, y2);
            }

        }
        Line2D l1 = new Line2D.Double();
        l1.setLine(0, 0, 1, 1);
        Line2D l2 = new Line2D.Double();
        l2.setLine(0, 0, 1,1);
        System.out.println(l1.intersectsLine(l2));
    }

    @Test
    public void t() {
        ArrayList<Integer[]> cost = new ArrayList<>();
        cost.add(new Integer[]{12, 1});
        cost.add(new Integer[]{13, 2});
        cost.add(new Integer[]{11, 3});
        cost.add(new Integer[]{10, 4});
        cost.add(new Integer[]{100, 5});
        cost.add(new Integer[]{1, 6});
        cost.add(new Integer[]{19, 7});
        cost.add(new Integer[]{19, 8});
        cost.add(new Integer[]{7, 9});

        cost.sort(new Comparator<Integer[]>() {
            @Override
            public int compare(Integer[] o1, Integer[] o2) {
                return o1[0].compareTo(o2[0]);
            }
        });
        Collections.reverse(cost);
        for (Integer[] i : cost) {
            System.out.println(i[1]);
        }

    }
}
//e3dde4f9|2023-10-17|s2314239|surely.native.foal
//select * from orders where CUSTOMER='s2314239' , DELIVERTO='surely.native.foal';
//ORDERNO |DELIVERYDATE|CUSTOMER|DELIVERTO