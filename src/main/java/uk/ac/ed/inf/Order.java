package uk.ac.ed.inf;

import java.util.ArrayList;

public class Order {
    private String orderNo;
    private String deliveryDate;
    private String customer;
    private String deliverTo;
    private ArrayList<String> items;
    private int cost;

    public Order(String orderNo, String deliveryDate, String customer,
                 String deliverTo, ArrayList<String> items) {
        this.orderNo = orderNo;
        this.deliveryDate = deliveryDate;
        this.customer = customer;
        this.deliverTo = deliverTo;
        this.items = items;
    }

    // getters
    public String getOrderNo() { return orderNo; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getCustomer() { return customer; }
    public String getDeliverTo() { return deliverTo; }
    public ArrayList<String> getItem() { return items; }
    public int getCost() { return cost; }

    /**
     *Calculate the cost in pence of all items delivered by drone.
     *
     * @param name name/IP of the server to connect with
     * @param port port number of the server to connect with
     */
    public void calculateDeliveryCost(String name, String port) {
        int foodCost = 0;
        DataParser parser = new DataParser(name, port);
        parser.readMenus();

        /* for each items, we need to loop through all shops and their
           menus to find the desired food item, then sum up the cost */
        for (String foodItem : this.items) {
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
        this.cost = foodCost + Const.DELIVERY_CHARGE;
    }


}
