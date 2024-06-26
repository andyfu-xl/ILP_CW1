package uk.ac.ed.inf;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Details of orders
 */
public class Order {
    private String orderNo;
    private String deliveryDate;
    private String customer;
    private String deliverTo;
    private ArrayList<String> items;
    private int cost;
    private ArrayList<String> shopAddress;
    private ArrayList<LongLat> shopCoordinate;
    private double utility;

    /**
     * Constructor of Order class
     *
     * @param orderNo order number
     * @param deliveryDate delivery date
     * @param customer name of customer
     * @param deliverTo whatThreeWords address, where the drone deliver food to the customer
     * @param items food items
     */
    public Order(String orderNo, String deliveryDate, String customer,
                 String deliverTo, ArrayList<String> items) {
        this.orderNo = orderNo;
        this.deliveryDate = deliveryDate;
        this.customer = customer;
        this.deliverTo = deliverTo;
        this.items = items;
    }

    // getters, some of them are only used for testing
    public String getOrderNo() { return orderNo; }
    public String getDeliveryDate() { return deliveryDate; }
    public String getCustomer() { return customer; }
    public String getDeliverTo() { return deliverTo; }
    public ArrayList<String> getItem() { return items; }
    public int getCost() { return this.cost; }
    public double getUtility() { return utility; }
    public ArrayList<LongLat> getShopCoordinate() { return shopCoordinate; }

    /**
     * Estimating moves required for the drone to deliver the order
     * The estimation ignores the best
     *
     * @param dronePosition position of the drone, a LongLat object
     * @param parser that can parse data from the server
     * @return estimated number of moves to deliver this order
     */
    public int estimateMoves(LongLat dronePosition, HttpConnection parser) {
        ArrayList<LongLat> coordinates = new ArrayList<>();
        for (String address : this.shopAddress) {
            // convert WhatThreeWord address into LongLat object.
            coordinates.add(parser.wordsToLongLat(address));
        }
        LongLat destination = parser.wordsToLongLat(this.deliverTo);
        if (coordinates.size() == 1) {
            double distance = coordinates.get(0).distanceTo(destination);
            distance += coordinates.get(0).distanceTo(dronePosition);
            this.shopCoordinate = coordinates;
            return (int)(distance / Const.DISTANCE_MOVE);
        }else if (coordinates.size() == 2) {
            double distance = coordinates.get(0).distanceTo(coordinates.get(1));
            double path1 = coordinates.get(1).distanceTo(destination) +
                    coordinates.get(0).distanceTo(dronePosition);
            double path2 = coordinates.get(0).distanceTo(destination) +
                    coordinates.get(1).distanceTo(dronePosition);
            if (path1 <= path2) {
                distance += path1;
                this.shopCoordinate = coordinates;
            }else {
                distance += path2;
                // if path2 is shorter, the drone would visit the second shop first
                this.shopCoordinate = new ArrayList<>();
                this.shopCoordinate.add(coordinates.get(1));
                this.shopCoordinate.add(coordinates.get(0));
            }
            return (int)(distance / Const.DISTANCE_MOVE);
        }else {
            /* the number of shop to visit is not 1 or 2. The order is invalid,
               give the order 1501 moves, which the drone will not deliver.*/
            System.out.println("Error: Invalid shop number (should only select food from" +
                    " one or two shops).");
            return 1501;
        }
    }

    /**
     * The goodness of the algorithm is depends on monetary value, a simple way to estimate
     * the utility of an order is delivery cost divided by estimated moves.
     *
     * @param dronePosition position of the drone, a LongLat object.
     * @param parser that can parse data from the server
     * @return deliveryCost divided by estimated moves
     */
    public double estimateUtility (LongLat dronePosition, HttpConnection parser) {
        calCostAndGetShops(parser);
        int moves = estimateMoves(dronePosition, parser);
        this.utility = this.cost / moves;
        return this.cost / moves;
    }

    /**
     *Calculate the cost in pence of all items delivered by drone.
     *
     * @param parser that can parse data from the server
     */
    public void calCostAndGetShops(HttpConnection parser) {
        int foodCost = 0;
        ArrayList<String> shopLocations = new ArrayList<>();
        int numberOfItems = 0;
        // store location and cost of each item in HashMap
        HashMap<String, String> locationMapper = new HashMap<String, String>();
        HashMap<String, Integer> costMapper = new HashMap<String, Integer>();
        for (Shop s : parser.readMenus()) {
            for (Item i : s.menu) {
                locationMapper.put(i.item, s.location);
                costMapper.put(i.item, i.pence);
            }
        }
        // Search for the item in HashMap to get location and cost
        for (String foodItem : this.items) {
            if (!shopLocations.contains(locationMapper.get(foodItem))) {
                shopLocations.add(locationMapper.get(foodItem));
            }
            foodCost += costMapper.get(foodItem);
            numberOfItems += 1;
        }
        /* The cost are set to -1 if order is illegal, therefore such
           order will not be considered by the algorithm */
        if (shopLocations.size() > 2 | shopLocations.size() < 1) {
            System.out.println("Error: Illegal order. Can only select items from 1 " +
                    "or 2 restaurants.");
            this.cost = -1;
        }else if (numberOfItems > 4 | numberOfItems < 0) {
            System.out.println("Error: Illegal order. At least 1 item should" +
                    "be selected, and cannot select more than 4 items.");
            this.cost = -1;
        }else {
            this.shopAddress = shopLocations;
            this.cost = foodCost + Const.DELIVERY_CHARGE;
        }
    }

}
