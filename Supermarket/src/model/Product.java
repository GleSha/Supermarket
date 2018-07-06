package model;


import javafx.util.Pair;

public class Product {

    private String name;

    private byte ID;

    private int cost;

    public Product(byte ID, Pair<Integer, String> pair) {
        this.ID = ID;
        cost = pair.getKey();
        name = pair.getValue();
    }


    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public byte getID() {
        return ID;
    }
}
