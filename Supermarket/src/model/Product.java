package model;


import javafx.util.Pair;

public class Product {

    /**
     * Название продукта
     * */
    private String name;

    /**
     * уникальный номер продукта
     * */
    private byte ID;

    /**
     * цена продукта
     * */
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
