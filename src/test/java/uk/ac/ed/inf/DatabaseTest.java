package uk.ac.ed.inf;

import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class DatabaseTest {
    @Test
    public void testCreateTable() throws SQLException {
        ServerConnector sc = new ServerConnector("localhost", "9876");
        sc.connectJDBC(Const.DATABASE);
        sc.getStatement().execute("create table students(" +
                "name varchar(48), " +
                "matric char(8), " +
                "edinyear int)");
    }

    @Test
    public void testDeleteTable() throws SQLException {
        ServerConnector sc = new ServerConnector("localhost", "9876");
        sc.connectJDBC(Const.DATABASE);
        DatabaseMetaData databaseMetadata = sc.getConn().getMetaData();
        ResultSet resultSet =
                databaseMetadata.getTables(null, null, "STUDENTS", null);
        if (resultSet.next()) {
            sc.getStatement().execute("drop table students");
        }
    }

    @Test
    public void testReadData() throws SQLException {
        ServerConnector sc = new ServerConnector("localhost", "9876");
        sc.connectJDBC(Const.DATABASE);
        final String coursesQuery =
                "select * from ORDERS where DELIVERYDATE=(?)";
        PreparedStatement psCourseQuery =
                sc.getConn().prepareStatement(coursesQuery);
        psCourseQuery.setString(1, "2022-1-1");
        ArrayList<String> locList = new ArrayList<>();
        ResultSet rs = psCourseQuery.executeQuery();
        while (rs.next()) {
            String loc = rs.getString("DELIVERTO");
            System.out.println(loc);
            locList.add(loc);
        }
        System.out.println(locList.size());
    }
}
//e3dde4f9|2023-10-17|s2314239|surely.native.foal
//select * from orders where CUSTOMER='s2314239' , DELIVERTO='surely.native.foal';
//ORDERNO |DELIVERYDATE|CUSTOMER|DELIVERTO