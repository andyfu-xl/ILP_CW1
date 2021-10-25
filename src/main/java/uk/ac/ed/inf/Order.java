package uk.ac.ed.inf;

import java.util.ArrayList;

public class Order {
    private String orderNo;
    private String deliveryDate;
    private String customer;
    private String deliverTo;
    private ArrayList<String> items;

    public Order(String orderNo, String deliveryDate, String customer,
                 String deliverTo, ArrayList<String> items) {
        this.orderNo = orderNo;
        this.deliveryDate = deliveryDate;
        this.customer = customer;
        this.deliverTo = deliverTo;
        this.items = items;
    }

    public String getOrderNo() { return orderNo; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getCustomer() { return customer; }
    public String getDeliverTo() { return deliverTo; }
    public ArrayList<String> getItem() { return items; }
}
