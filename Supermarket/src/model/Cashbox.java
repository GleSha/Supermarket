package model;


public class Cashbox {

    private static Cashbox ourInstance = new Cashbox();

    public static Cashbox getInstance() {
        return ourInstance;
    }

    private Cashbox() {
        productProceeds = 0;
        uselessProductProceeds = 0;
    }


    private static int cashboxRow, cashboxColumn;

    private int productProceeds;

    private int uselessProductProceeds;



    public void pay(int money, int uselessProductMoney) {
        productProceeds += money;
        uselessProductProceeds += uselessProductMoney;
    }


    public int getUselessProductProceeds() {
        return uselessProductProceeds;
    }

    public int getProductProceeds() {
        return productProceeds;
    }



    public static void setPosition(int row, int column) {
        cashboxRow = row;
        cashboxColumn = column;
    }

    public int getRow() {
        return cashboxRow;
    }

    public int getColumn() {
        return cashboxColumn;
    }




}
