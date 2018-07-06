package model;

public class Storage {

    private static int storageRow, storageColumn;

    public static void setPosition(int row, int column) {
        storageRow = row;
        storageColumn = column;
    }

    public static int getRow() {
        return storageRow;
    }

    public static int getColumn() {
        return storageColumn;
    }
}
