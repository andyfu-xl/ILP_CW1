package uk.ac.ed.inf;

import java.net.http.HttpClient;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseConnection {

    /* Just have one HttpClient, use it multiple times. We have one
       client so it's declared as static, it won't update (final) */
    private static final HttpClient client = HttpClient.newHttpClient();

    private String name;
    private String port;
    private Statement statement;
    private Connection conn;

    /**
     * Constructor of ServerConnector Class.
     *
     * @param name name of the machine.
     * @param port port number.
     */
    public DatabaseConnection(String name, String port) {
        this.name = name;
        this.port = port;
    }

    /**
     * Connect with database in the server
     * The protocol should be jdbc:derby
     *
     * @param database the name of database
     */
    public void connectJDBC(String database) {
        String jdbcString = ("jdbc:derby://" + this.name + ":" + this.port + database);
        try {
            this.conn = DriverManager.getConnection(jdbcString);
            this.statement = this.conn.createStatement();
        } catch (SQLException e) {
            System.out.println("Fatal error: Unable to connect to " + this.name
                    + " at port " + this.port + ".");
            e.printStackTrace();
        }
    }

    public ArrayList<String> getItemNames(String orderNo) {
        final String coursesQuery =
                "select * from orderDetails where orderNo=(?)";
        ArrayList<String> itemNames = new ArrayList<>();
        try {
            PreparedStatement psCourseQuery = this.conn.prepareStatement(coursesQuery);
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
     * @return
     */
    public ArrayList<Order> readOrders(String date) {
        final String coursesQuery =
                "select * from orders where deliveryDate=(?)";
        ArrayList<Order> orderList = new ArrayList<>();
        try {
            PreparedStatement psCourseQuery = this.conn.prepareStatement(coursesQuery);
            psCourseQuery.setString(1, date);
            ResultSet rs = psCourseQuery.executeQuery();
            while (rs.next()) {
                String orderNo = rs.getString("orderNo");
                String customer = rs.getString("customer");
                String deliverTo = rs.getString("deliverTo");
                ArrayList<String> itemNames = getItemNames(orderNo);
                Order order = new Order(orderNo, date, customer, deliverTo, itemNames);
                orderList.add(order);
            }
        } catch (SQLException e) {
            System.out.println("Error: Failed to retrieve orders from data base.");
            e.printStackTrace();
        }
        return orderList;
    }
}
