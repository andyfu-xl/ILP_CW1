package uk.ac.ed.inf;

import java.net.http.HttpClient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {

    /* Just have one HttpClient, use it multiple times. We have one
       client so it's declared as static, it won't update (final) */
    private static final HttpClient client = HttpClient.newHttpClient();

    private String name;
    private String port;
    private Statement statement;
    private Connection conn;

    public Statement getStatement() { return statement; }
    public Connection getConn() { return conn; }

    /**
     * Constructor of ServerConnector Class.
     *
     * @param name name of the machine.
     * @param port port number.
     */
    public DatabaseConnection(String name, String port) {
        this.name = name;
        this.port = port;
        connectJDBC(Const.DATABASE);
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
     * @return a list of order objects
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

    /**
     * create table called flightpath to store the flightpath data, drop the table if
     * there is already a table called flightpath
     */
    public void createTable() {
        try {
            DatabaseMetaData databaseMetadata = this.conn.getMetaData();
            ResultSet resultSet =
                    databaseMetadata.getTables(null, null, "FLIGHTPATH", null);
            if (resultSet.next()) {
                this.statement.execute("drop table flightpath");
            }
            this.statement.execute("create table flightpath(" +
                    "        orderNo char(8)," +
                    "        fromLongitude double," +
                    "        fromLatitude double," +
                    "        angle integer," +
                    "        toLongitude double," +
                    "        toLatitude double)");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.err.println("Error: Failed to create new table.");
        }
        System.out.println("New table created successfully.");
    }

    /**
     *
     */
    public void writePaths(List<Path> paths) {
        createTable();
        try {
            PreparedStatement psPath = conn.prepareStatement(
                    "insert into flightpath values (?, ?, ?, ?, ?, ?)"
            );
            for (Path path : paths) {
                psPath.setString(1, path.orderNo);
                psPath.setDouble(2, path.fromLongitude);
                psPath.setDouble(3, path.fromLatitude);
                psPath.setInt(4, path.angle);
                psPath.setDouble(5, path.toLongitude);
                psPath.setDouble(6, path.toLatitude);
                psPath.execute();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.err.println("Error: Failed to write paths into database.");
        }
        System.out.println("Path writen into database.");
    }

}
