package model;

public class ObjectOnMap {
    protected int objectOnMapRow, objectOnMapColumn;

    public int getRow() {
        return objectOnMapRow;
    }

    public int getColumn() {
        return objectOnMapColumn;
    }

    public void liveStep() {}

    public boolean left() {
        return false;
    }
}
