package uk.ac.ed.inf;

import java.net.http.HttpClient;


/**
 * Set up connection with the server.
 * Accessing Menus data stored in the server.
 */
public class Menus {

    private String name;
    private String port;

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

//    /**
//     * Read menus file from the server, store menus as list of shops.
//     */
//    public void readMenus() {
//        connectMenus("http://" + this.name + ":" + this.port + Const.URL_MENUS);
//        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
//        this.shops = new Gson().fromJson(this.json, listType);
//    }

    /**
     * Calculate the cost in pence of all items delivered by drone.
     *
     * @param args items to be delivered by drone.
     * @return cost in pence of delivery service.
     */
    public int getDeliveryCost(String... args) {
        int foodCost = 0;
        DataParser parser = new DataParser(this.name, this.port);
        parser.readMenus();

        /* for each items, we need to loop through all shops and their
           menus to find the desired fooditem, then sum up the cost */
        for (String foodItem : args) {
            boolean found = false;      // Once the item is found, the loop will break.
            for (Shop s : parser.getShops()) {
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
