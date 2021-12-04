package uk.ac.ed.inf;

import java.net.http.HttpClient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Connect with the database, read and write data.
 */
public class DatabaseConnection {

    private String name;
    private String port;
    private Statement statement;
    private Connection conn;

    // getters
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
            System.err.println("Fatal error: Unable to connect to " + this.name
                    + " at port " + this.port + ".");
            e.printStackTrace();
        }
    }

    /**
     * Read all item names of a given order
     *
     * @param orderNo order number of the order
     * @return All item names as Strings in a Arraylist
     */
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
            System.err.println("Error: Failed to retrieve items of order from date base.");
            e.printStackTrace();
        }
        return itemNames;
    }

    /**
     * Read order details from the server, store orders as list of order objects.
     *
     * @param date date of the flight
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
                // read each order and store into orderList
                String orderNo = rs.getString("orderNo");
                String customer = rs.getString("customer");
                String deliverTo = rs.getString("deliverTo");
                ArrayList<String> itemNames = getItemNames(orderNo);
                Order order = new Order(orderNo, date, customer, deliverTo, itemNames);
                orderList.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Error: Failed to retrieve orders from data base.");
            e.printStackTrace();
        }
        return orderList;
    }

    /**
     * create table called deliveries to store the delivered orders,
     * create table called flightpath to store the flightpath data,
     * drop the table if there is already a table with the same name.
     */
    public void createTable() {
        try {
            DatabaseMetaData databaseMetadata = this.conn.getMetaData();
            ResultSet resultSet =
                    databaseMetadata.getTables(null, null, "DELIVERIES", null);
            // delete the table if it exists already.
            if (resultSet.next()) {
                this.statement.execute("drop table deliveries");
            }
            this.statement.execute("create table deliveries(" +
                    "        orderNo char(8)," +
                    "        deliveredTo varchar(19)," +
                    "        costInPence int)");
            resultSet = databaseMetadata.getTables(null, null, "FLIGHTPATH", null);
            // delete the table if it exists already.
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
            return;
        }
        System.out.println("New table created successfully.");
    }

    /**
     * write delivered orders into the database
     *
     * @param orders delivered orders
     */
    public void writeOrders(ArrayList<Order> orders) {
        try {
            PreparedStatement psPath = conn.prepareStatement(
                    "insert into deliveries values (?, ?, ?)"
            );
            // write each order
            for (Order order : orders) {
                psPath.setString(1, order.getOrderNo());
                psPath.setString(2, order.getDeliverTo());
                psPath.setInt(3, order.getCost());
                psPath.execute();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.err.println("Error: Failed to write Orders into database.");
        }
        System.out.println("Orders writen into database.");
    }

    /**
     * write flight path into the database
     *
     * @param paths paths of the flight
     */
    public void writePaths(List<Path> paths) {
        try {
            PreparedStatement psPath = conn.prepareStatement(
                    "insert into flightpath values (?, ?, ?, ?, ?, ?)"
            );
            // write each path
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
