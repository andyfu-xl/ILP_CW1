package uk.ac.ed.inf;

import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class AppTest {
    @Test
    public void testReadData() throws SQLException {
        DatabaseConnection dc = new DatabaseConnection("localhost", "9876");
        dc.connectJDBC(Const.DATABASE);
        final String coursesQuery =
                "select * from ORDERS where DELIVERYDATE=(?)";
        PreparedStatement psCourseQuery =
                dc.getConn().prepareStatement(coursesQuery);
        psCourseQuery.setString(1, "2022-01-01");
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
    public void finalTest() {
        App.main(new String[]{"01", "01", "2022", "9898", "9876"});
    }
}