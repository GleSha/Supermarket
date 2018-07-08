package model;

import javafx.util.Pair;
import java.util.ArrayList;
import java.util.Collections;

public class Manager extends ObjectOnMap {

    private static Manager ourInstance = new Manager();

    public static Manager getInstance() {
        return ourInstance;
    }

    private Manager() {
        currentAction = ActionType.INACTIVITY;
        objectOnMapRow = Storage.getRow();
        objectOnMapColumn = Storage.getColumn();
    }

    /**
     * координаты пустого прилавка
     * */
    private int emptyCounterRow, emptyCounterColumn;

    /**
     * путь до пустого прилавка
     * */
    private ArrayList<Pair<Integer, Integer>> path;

    /**
     * копия матрицы для алгоритма поиска пути
     * */
    private byte [][] matrix;

    /**
     * минимальная длина пути
     * */
    private byte MIN_STEP = 1;

    /**
     * продукт для пополнения
     * */
    private Product refill;

    /**
     * текущее действие менеджера
     * */
    private int currentAction;

    /**
     * текущий шаг покупателя на пути к прилавку или складу
     * */
    private int currentStep;

    /**
     * класс констант-действий менеджера
     * */
    private class ActionType {
        static final int TAKE_PRODUCT = 0;      //взять продукт со склада
        static final int MOVE_TO_COUNTER = 1;   //идти к прилавку
        static final int PUT_PRODUCT = 2;       //пополнить пустой прилавок
        static final int MOVE_TO_STORAGE = 3;   //идти к складу
        static final int INACTIVITY = 4;        //ждать появления пустого прилавка
    }

    private void putProduct() {
        Map.getInstance().putProduct(emptyCounterRow, emptyCounterColumn);
        refill = null;
        emptyCounterRow = -1;
        emptyCounterColumn = -1;
    }

    /**
     * шаг жизненного цикла менеджера
     * */
    @Override
    public void liveStep() {
        action(currentAction);
    }

    /**
     * выполняем действие, параметр - константы действий
     * класса ActionType
     * */
    private void action(int actionType) {
        switch (actionType) {
            case ActionType.INACTIVITY: {
                if (emptyCounterExists()) {
                    findPath();
                    currentAction = ActionType.TAKE_PRODUCT;
                }
            } break;
            case ActionType.TAKE_PRODUCT: {
                byte ID = Map.getInstance().getProductID(emptyCounterRow, emptyCounterColumn);
                refill = new Product(ID, Map.getInstance().getProductInfo(emptyCounterRow, emptyCounterColumn));
                currentStep = 0;
                if (path != null)
                    currentAction = ActionType.MOVE_TO_COUNTER;
                else
                    currentAction = ActionType.PUT_PRODUCT;
            } break;
            case ActionType.PUT_PRODUCT: {
                putProduct();
                if (path != null) {
                    Collections.reverse(path);
                    currentStep = 0;
                    currentAction = ActionType.MOVE_TO_STORAGE;
                }
                else
                    currentAction = ActionType.INACTIVITY;
            } break;
            default: {
                if (currentStep < path.size()) {
                    moveTo(path.get(currentStep));
                    currentStep++;
                }
                else {
                    if (actionType == ActionType.MOVE_TO_COUNTER)
                        currentAction = ActionType.PUT_PRODUCT;
                    else
                        currentAction = ActionType.INACTIVITY;
                }
            } break;
        }
    }

    /**
     * пережвигает менеджера на 1 шаг по карте
     * параметр {@param coords} не должнен быть равен null
     * */
    private void moveTo(Pair<Integer, Integer> coords) {
        objectOnMapRow = coords.getKey();
        objectOnMapColumn = coords.getValue();
    }

    /**
     * проверяет карту на наличие пустых прилавков
     * */
    private boolean emptyCounterExists() {
        matrix = Map.getInstance().getCopyOfMatrix();
        boolean isEmpty = false;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[i][j] < Map.STORAGE_ID && Map.getInstance().getProductCount(i, j) == 0) {
                    isEmpty = true;
                    break;
                }
            }
            if (isEmpty)
                break;
        }
        return isEmpty;
    }

    /**
     * возвращает длину пути
     * если она равна MIN_STEP, то мы рядом, идти никуда не нужно
     * метод может вызываться только при условии, что на карте
     * есть пустые прилавки
     * */
    private void findPath() {
        path = null;
        byte step = startWave();
        if (step > MIN_STEP)
            restorePath(step);
    }

    /**
     * Распространение волны
     * Возвращает длину пути
     * можно вызывать, только если на карте есть пустые прилавки
     * */
    private byte startWave() {
        byte step = 1;
        matrix[Storage.getRow()][Storage.getColumn()] = step;
        int destX = -1, destY = -1;
        boolean isFinished = false;
        while (step < Byte.MAX_VALUE) {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) {
                    if (matrix[i][j] == step) {
                        for (int iOffset = -1; iOffset < 2; iOffset++) {
                            if (i + iOffset < 0 || i + iOffset > matrix.length - 1)
                                continue;
                            for (int jOffset = -1; jOffset < 2; jOffset++) {
                                if (j + jOffset < 0 || j + jOffset > matrix.length - 1)
                                    continue;
                                if (matrix[i + iOffset][j + jOffset] == 0)
                                    matrix[i + iOffset][j + jOffset] = (byte) (step + 1);
                                else if (matrix[i + iOffset][j + jOffset] < Map.STORAGE_ID
                                        && Map.getInstance().getProductCount(i + iOffset, j + jOffset) == 0) {
                                    isFinished = true;
                                    destX = j + jOffset;
                                    destY = i + iOffset;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (isFinished)
                break;
            step++;
        }
        emptyCounterRow = destY;
        emptyCounterColumn = destX;
        return step;
    }

    /**
     * {@param step} должен быть в пределах от MIN_STEP до MAX_STEP
     * */
    private void restorePath(byte step) {
        path = new ArrayList<>();
        int row = emptyCounterRow;
        int column = emptyCounterColumn;
        do {
            for (int iOffset = -1; iOffset < 2; iOffset++) {
                if (row + iOffset < 0 || row + iOffset > matrix.length - 1)
                    continue;
                for (int jOffset = -1; jOffset < 2; jOffset++) {
                    if (column + jOffset < 0 || column + jOffset > matrix.length - 1)
                        continue;
                    if (matrix[row + iOffset][column + jOffset] == step) {
                        path.add(new Pair<>(row + iOffset, column + jOffset));
                        row += iOffset;
                        column += jOffset;
                        step--;
                        break;
                    }
                }
            }
        }
        while (step > 0);
        Collections.reverse(path);
    }

    @Override
    public String getInfo() {
        if (refill != null)
            return  "Несу " + refill.getName() + " к пустому прилавку";
        else if (currentAction == ActionType.MOVE_TO_STORAGE)
            return "Иду на склад...";
        else
            return "Жду пустого прилавка";
    }
}
