package model;


public class Gate {

    private static int gateRow, gateColumn;

    public static void setPosition(int row, int column) {
        gateRow = row;
        gateColumn = column;
    }


    public static int getRow() {
        return gateRow;
    }

    public static int getColumn() {
        return gateColumn;
    }

}
