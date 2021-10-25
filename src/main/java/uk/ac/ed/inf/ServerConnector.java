package uk.ac.ed.inf;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class ServerConnector {

    /* Just have one HttpClient, use it multiple times. We have one
       client so it's declared as static, it won't update (final) */
    private static final HttpClient client = HttpClient.newHttpClient();

    private String name;
    private String port;
    private String json;
    private Statement statement;
    private Connection conn;

    // getters
    public String getJson() {
        return this.json;
    }
    public Statement getStatement() { return this.statement; }
    public Connection getConn() {return this.conn; }

    /**
     * Constructor of ServerConnector Class.
     *
     * @param name name of the machine.
     * @param port port number.
     */
    public ServerConnector(String name, String port) {
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

}
