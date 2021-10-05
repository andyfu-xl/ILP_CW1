package uk.ac.ed.inf;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Set up connection with the server.
 * Accessing Menus data stored in the server.
 */
public class Menus {

    /* Just have one HttpClient, use it multiple times. We have one
       client so it's declared as static, it won't update (final) */
    private static final HttpClient client = HttpClient.newHttpClient();

    private String name;
    private String port;
    private String json;
    private ArrayList<Shop> shops;

    /**
     * Nested class for parsing json.
     * Represent one Shop, contains information of its name,
     * location, and menu
     */
    private static class Shop {
        String name;
        String location;
        ArrayList<Item> menu;          // List of food items on the menu.
    }

    /**
     * Nested class for parsing json.
     * Represent one food item, stores information of
     * the item's name and the price in pence
     */
    private static class Item {
        String item;                   // name of the item.
        Integer pence;
    }

    /**
     * Constructor of Menus Class.
     *
     * @param name name of the machine.
     * @param port port number.
     */
    public Menus(String name, String port) {
        this.name = name;
        this.port = port;
    }

    // Getters
    public String getName() {
        return this.name;
    }

    public String getPort() {
        return this.port;
    }

    public String getJson() {
        return this.json;
    }

    public ArrayList<Shop> getShops() {
        return this.shops;
    }

    /**
     * Access the server given a URL, and store the information in json format.
     *
     * @param urlString the URL of the server file
     */
    public void connectUrl(String urlString) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString)).build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                this.json = response.body();
            } else {
                System.out.println("Fatal error. Response code: "
                        + response.statusCode() + ". URL:" + urlString + ".");
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
     * Read menus file from the server, store menus as list of shops.
     */
    public void readMenus() {
        connectUrl("http://" + this.name + ":" + this.port + Const.URL_MENUS);
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        this.shops = new Gson().fromJson(this.json, listType);
    }

    /**
     * Calculate the cost in pence of all items delivered by drone.
     *
     * @param args items to be delivered by drone.
     * @return cost in pence of delivery service.
     */
    public int getDeliveryCost(String... args) {
        int foodCost = 0;
        readMenus();

        /* for each items, we need to loop through all shops and their
           menus to find the desired fooditem, then sum up the cost */
        for (String foodItem : args) {
            boolean found = false;      // Once the item is found, the loop will break.
            for (Shop s : this.shops) {
                for (Item i : s.menu) {
                    if (i.item.equals(foodItem)) {
                        found = true;
                        foodCost += i.pence;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        return (foodCost + Const.DELIVERY_CHARGE);
    }

}
