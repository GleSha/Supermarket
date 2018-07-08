package model;


import javafx.scene.control.Alert;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Map {

    public static final int MATRIX_SIZE = 15;

    /**
     * Максимальное количество товаров на прилавке
     * */
    private static final byte MAX_PRODUCTS_IN_CELL = 5;

    /**
     * Имя стандартной карты
     * */
    public static final String DEFAULT_MAP_NAME = "sources/map.map";

    /**
     * Имя конфигурационного файла с описанием товаров
     * */
    private static final String DEFAULT_PRODUCTS_FILE_NAME = "sources/config/products.txt";

    /**
     * Карта в виде двумерного массива
     * */
    private byte [][] matrix;

    /**
     * ID в массиве для входа, кассы и склада
     * */
    public static final byte GATE_ID = -2;

    public static final byte CASHBOX_ID = -3;

    public static final byte STORAGE_ID = -4;

    /**
     * Карта с количеством товара в каждом прилавке
     * */
    private byte [][] productsCount;

    /**
     * Все виды товаров из списка products.txt
     * */
    private HashMap<Byte, Pair<Integer, String>> productType;

    /**
     * Список посетителей
     * */
    private ArrayList<Customer> customers;


    private ArrayList<ObjectOnMap> objects;

    /**
     * Максимальное количество посетителей
     * */
    public static final int MAX_CUSTOMERS = 5;

    private static String errorMessage;

    private static Map ourInstance = null;

    public static Map getInstance() {
        return ourInstance;
    }

    private Map(String fileName) {
        try {
            /**
             * Загрузка карты
             * */
            if (mapLoad(fileName)) {
                objects = new ArrayList<>();
                objects.add(Manager.getInstance());

                customers = new ArrayList<>();
                isReady = true;
            }
        }
        catch (IOException | NumberFormatException ex) {
            if (ex instanceof IOException) {
                errorMessage = "Ошибка чтения файла карты\n" + ex.getMessage();
            }
            else {
                errorMessage = "Формат карты не верен\n" + ex.getMessage();
            }
        }
    }

    /**
     * Загружает карту и формирует список продаваемых на этой карте товаров
     * */
    private boolean mapLoad(String fileName) throws IOException, NumberFormatException {
        HashMap<Byte, Pair<Integer, String>> allProductsTypes = allProductsTypesLoad();
        productType = new HashMap<>();
        String[] lines = Files.readAllLines(Paths.get(fileName)).toArray(new String[0]);
        if (lines.length != MATRIX_SIZE) {
            errorMessage = "\nФормат файла карты не верен!\nНужная размерность матрицы -  15 * 15.";
            return false;
        }
        matrix = new byte[MATRIX_SIZE][MATRIX_SIZE];
        productsCount = new byte[MATRIX_SIZE][MATRIX_SIZE];
        String [] row;
        boolean storage = false, cashbox = false, gate = false;
        for (int i = 0; i < MATRIX_SIZE; i++) {
            row = lines[i].split(" ");
            if (row.length != MATRIX_SIZE) {
                errorMessage = "Формат файла карты не верен!\nНужная размерность матрицы -  15 * 15.";
                return false;
            }
            for (int j = 0; j < MATRIX_SIZE; j++) {
                matrix[i][j] = Byte.valueOf(row[j]);
                if (matrix[i][j] < 0) {
                    if (matrix[i][j] < STORAGE_ID) {
                        productsCount[i][j] = MAX_PRODUCTS_IN_CELL;
                        if (!productType.containsKey(matrix[i][j]))
                            productType.put(matrix[i][j], allProductsTypes.get(matrix[i][j]));
                    }
                    if (matrix[i][j] == STORAGE_ID) {
                        Storage.setPosition(i, j);
                        storage = true;
                    }
                    if (matrix[i][j] == CASHBOX_ID) {
                        Cashbox.setPosition(i, j);
                        cashbox = true;
                    }
                    if (matrix[i][j] == GATE_ID) {
                        Gate.setPosition(i, j);
                        gate = true;
                    }
                }
                else if (matrix[i][j] > 0) {
                    errorMessage = "Формат карты не верен:\nне должно быть положительных чисел!";
                    return false;
                }
            }
        }
        if (productType.size() == 0) {
            errorMessage = "На карте должен быть товар!";
            return false;
        }
        if (!storage) {
            errorMessage = "На карте нет склада!";
            return false;
        }
        if (!gate) {
            errorMessage = "На карте нет входа!";
            return false;
        }
        if (!cashbox) {
            errorMessage = "На карте нет кассы!";
            return false;
        }
        return true;
    }

    /**
     * Загружает файл products.txt
     * */
    private HashMap<Byte, Pair<Integer, String>> allProductsTypesLoad() throws IOException, NumberFormatException {
        String[] lines = Files.readAllLines(Paths.get(DEFAULT_PRODUCTS_FILE_NAME)).toArray(new String[0]);
        HashMap<Byte, Pair<Integer, String>> allProductsTypes = new HashMap<>();
        String[] row;
        for (String line : lines) {
            row = line.split(":");
            byte ID = Byte.parseByte(row[2]);
            allProductsTypes.put(ID, new Pair<>(Integer.parseInt(row[1]), row[0]));
        }
        return allProductsTypes;
    }

    /**
     * готова ли карта к работе
     * */
    private boolean isReady;


    public boolean isReady() {
        return isReady;
    }

    /**
     * инициализирует карту, считывая ее из файла filename
     * */
    public static void initialization(String filename) {
        ourInstance = new Map(filename);
    }


    public boolean takeProduct(int row, int column) {
        if (productsCount[row][column] > 0) {
            productsCount[row][column]--;
            return true;
        }
        return false;
    }

    public void putProduct(int row, int column) {
        productsCount[row][column] = MAX_PRODUCTS_IN_CELL;
    }

    private boolean customerHasEntered() {
        return (ThreadLocalRandom.current().nextInt(0, MAX_CUSTOMERS)) == 0;
    }

    /**
     * один шаг жизненного цикла системы
     * */
    public void timeStep() {
        if (isReady) {
            if (objects.size() - 1 < MAX_CUSTOMERS)
                if (customerHasEntered())
                    objects.add(new Customer());

            /**
             * Цикл обработки посетителей
             * */
            for (ObjectOnMap object : objects)
                object.liveStep();

            for (int i = 0; i < objects.size(); i++) {
                if (objects.get(i).left()) {
                    objects.remove(i);
                    i--;
                }
            }
        }
    }

    public Pair<Integer, Integer> getNearestMostAttractiveProduct(int row, int column) {
        byte min = STORAGE_ID - 1;
        int pRow = -1, pColumn = -1;

        for (int i = -1; i < 2; i++) {
            if (row + i < 0 || row + i == matrix.length)
                continue;
            for (int j = -1; j < 2; j++) {
                if (column + j < 0 || column + j == matrix.length)
                    continue;
                if (matrix[row + i][column + j] < min) {
                    min = matrix[row + i][column + j];
                    pRow = row + i;
                    pColumn = column + j;
                }
            }
        }
        if (pRow == -1)
            return null;
        return new Pair<>(pRow, pColumn);
    }

    public byte [][] getCopyOfMatrix() {
        byte [][] copy = new byte[MATRIX_SIZE][MATRIX_SIZE];
        for (int i = 0; i < MATRIX_SIZE; i++)
            System.arraycopy(matrix[i], 0, copy[i], 0, MATRIX_SIZE);
        return copy;
    }

    public byte getProductCount(int row, int column) {
        return productsCount[row][column];
    }

    /**
     * Возвращает инфу о прилавке с товаром по индексам его расположения
     * */
    public Pair<Integer, String> getProductInfo(int row, int column) {
        return productType.get(matrix[row][column]);
    }

    public byte getProductID(int row, int column) {
        return matrix[row][column];
    }

    public Product getProductByID(byte ID) {
        return new Product(ID, productType.get(ID));
    }

    public int getProductsAssortment() {
        return productType.keySet().size();
    }

    public Byte [] getShuffleProductsID(int length) {
        Byte [] ID = productType.keySet().toArray(new Byte[0]);
        Collections.shuffle(Arrays.asList(ID));
        return Arrays.copyOf(ID, length);
    }

    public byte [][] getCopyOfProductsCountMatrix() {
        byte [][] copy = new byte[MATRIX_SIZE][MATRIX_SIZE];
        for (int i = 0; i < MATRIX_SIZE; i++)
            System.arraycopy(productsCount[i], 0, copy[i], 0, MATRIX_SIZE);
        return copy;
    }

    public HashMap<Byte, Pair<Integer, String>> getProductType() {
        HashMap<Byte, Pair<Integer, String>> copy = new HashMap<>();
        Pair<Integer, String> info;
        for (Byte ID : productType.keySet()) {
            info = new Pair<>(getProductByID(ID).getCost(), getProductByID(ID).getName());
            copy.put(ID, info);
        }
        return copy;
    }

    public String getCustomerInfo(int row, int column) {
        for (Customer customer : customers) {
            if (customer.getRow() == row && customer.getColumn() == column) {
                return customer.getInfo();
            }
        }
        return "";
    }

    public String getManagerInfo() {
        return Manager.getInstance().getInfo();
    }

    public ArrayList<ObjectOnMap> getObjects() {
        return new ArrayList<>(objects);
    }

    public static String getErrorMessage() {
        return errorMessage;
    }
}
