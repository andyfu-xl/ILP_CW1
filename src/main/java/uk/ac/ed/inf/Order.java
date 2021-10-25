package uk.ac.ed.inf;

import java.util.ArrayList;

public class Order {
    private String orderNo;
    private String deliveryDate;
    private String customer;
    private String deliverTo;
    private ArrayList<String> item;

    public String getOrderNo() { return orderNo; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getCustomer() { return customer; }
    public String getDeliverTo() { return deliverTo; }
    public ArrayList<String> getItem() { return item; }
}
