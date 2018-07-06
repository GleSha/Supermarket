package model;


import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.Pair;

import java.beans.EventHandler;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Map {


    public static final int matrixSize = 15;

    /**
     *
     * Максимальное количество товаров на прилавке
     *
     * */
    private static final byte maxProductsInCell = 5;

    /**
     *
     *
     *
     * */
    public static final String defaultMapName = "sources/map.txt";

    private static final String defaultProductsFileName = "sources/config/products.txt";

    /**
     *
     * Карта в виде двумерного массива
     *
     * */
    private byte [][] matrix;

    /**
     *
     * ID в массиве для входа, кассы и склада
     *
     * */

    public static final byte gateID = -2;

    public static final byte cashboxID = -3;

    public static final byte storageID = -4;


    /**
     *
     * Карта с количеством товара в каждом прилавке
     *
     * */
    private byte [][] productsCount;


    /**
     *
     * Все виды товаров из списка products.txt
     *
     * */
    private static HashMap<Byte, Pair<Integer, String>> productType;


    /**
     *
     * Список посетителей
     *
     * */
    private ArrayList<Customer> customers;

    /**
     *
     * Максимальное количество посетителей
     *
     * */
    private static final int maxCustomers = 5;


    private String errorMessage;

    /**
     *
     * Отрисовка:
     *
     * отступ для определения координат в пикселях
     *
     *
     * набор изображений всех объектов на карте
     *
     *
     * */

    public static final int padding = 32;

    public static final int halfPadding = padding / 2;

    private static final double horizLineWidth = 1.5;

    private static final double verticLineWidth = 0.5;


    private Image emptyCounterImage;

    private Image customerImage;

    private Image managerImage;

    private Image gateImage;

    private Image cashboxImage;

    private Image storageImage;

    private Label [][] counters;

    private HashMap<Byte, Image> counterImages;


    /**
     *
     * Ссылка на Pane для отрисовки
     *
     * */
    private Pane mapPane;


    /**
     *
     * Ифнормция о покупателе или или менеджере
     *
     * */
    private TextArea textArea;

    private String textAreaMessage;




    private static Map ourInstance = null;

    public static Map getInstance() {
        return ourInstance;
    }

    private Map(String fileName, Pane pane, TextArea textArea) {
        try {


            mapPane = pane;

            this.textArea = textArea;

            textAreaMessage = textArea.getText();

            /**
             *
             * Загрузка карты
             *
             * */

            if (mapLoad(fileName)) {


                /**
                 *
                 * Загрузка всех изображений
                 *
                 * */


                customerImage = new Image(new FileInputStream("sources/images/customer.png"));

                managerImage = new Image(new FileInputStream("sources/images/manager.png"));

                emptyCounterImage = new Image(new FileInputStream("sources/images/counters/emptyCounter.png"));

                storageImage = new Image(new FileInputStream("sources/images/storageImage.png"));

                cashboxImage = new Image(new FileInputStream("sources/images/cashboxImage.png"));

                gateImage = new Image(new FileInputStream("sources/images/gateImage.png"));

                counterImages = new HashMap<>();


                for (Byte productID : productType.keySet()) {
                    Image image = new Image(new FileInputStream("sources/images/counters/" + productID + ".png"));
                    counterImages.put(productID, image);
                }


                /**
                 *
                 * Загрузка всех изображений
                 *
                 * */

                /**
                 *
                 * Рисуем сетку на карте
                 *
                 * */

                for (int i = 0; i < matrixSize + 1; i++) {
                    Line line = new Line(i * padding, 0, i * padding, padding * matrixSize);
                    line.setStroke(Color.BLUE);
                    line.setStrokeWidth(verticLineWidth);
                    line.setCache(true);
                    line.setCacheHint(CacheHint.SPEED);
                    pane.getChildren().add(line);
                    line = new Line(0, i * padding, padding * matrixSize, i * padding);
                    line.setStroke(Color.BLUE);
                    line.setStrokeWidth(horizLineWidth);
                    line.setCache(true);
                    line.setCacheHint(CacheHint.SPEED);
                    pane.getChildren().add(line);
                }


                /**
                 *
                 * Рисуем изображения прилавков с товарами, кассу, склад и вход
                 *
                 * */


                counters = new Label[matrixSize][matrixSize];

                for (int i = 0; i < matrixSize; i++) {
                    for (int j = 0; j < matrixSize; j++) {
                        if (matrix[i][j] < 0) {
                            if (counterImages.containsKey(matrix[i][j])) {
                                counters[i][j] = new Label();
                                Background back = new Background(new BackgroundImage(counterImages.get(matrix[i][j]),
                                        BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                        BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
                                Tooltip tooltip = new Tooltip(productType.get(matrix[i][j]).getValue() +
                                        "\nцена: " + productType.get(matrix[i][j]).getKey() +
                                        "\nпривлекательность: " + Math.abs(matrix[i][j]));
                                tooltip.setFont(Font.font(12));
                                tooltip.setShowDelay(Duration.ZERO);
                                tooltip.setHideDelay(Duration.ZERO);
                                counters[i][j].setBackground(back);
                                counters[i][j].setPrefSize(padding, padding);
                                counters[i][j].setTooltip(tooltip);
                                counters[i][j].setLayoutY(i * padding);
                                counters[i][j].setLayoutX(j * padding);
                                counters[i][j].setAlignment(Pos.BOTTOM_RIGHT);
                                counters[i][j].setFont(Font.font(12));
                                counters[i][j].setTextFill(Paint.valueOf("000000"));
                                counters[i][j].setText(String.valueOf(productsCount[i][j]));
                                counters[i][j].setCache(true);
                                counters[i][j].setCacheHint(CacheHint.SPEED);
                                mapPane.getChildren().add(counters[i][j]);
                            }
                        }
                    }
                }


                ImageView iv = new ImageView(storageImage);
                iv.setCache(true);
                iv.setCacheHint(CacheHint.SPEED);
                iv.setY(Storage.getRow() * padding);
                iv.setX(Storage.getColumn() * padding);
                mapPane.getChildren().add(iv);

                iv = new ImageView(cashboxImage);
                iv.setCache(true);
                iv.setCacheHint(CacheHint.SPEED);
                iv.setY(Cashbox.getInstance().getRow() * padding);
                iv.setX(Cashbox.getInstance().getColumn() * padding);
                mapPane.getChildren().add(iv);

                iv = new ImageView(gateImage);
                iv.setCache(true);
                iv.setCacheHint(CacheHint.SPEED);
                iv.setY(Gate.getRow() * padding);
                iv.setX(Gate.getColumn() * padding);
                mapPane.getChildren().add(iv);

                customers = new ArrayList<>();

                Manager.getInstance().init(new Put(), mapPane, managerImage, textArea);

                isReady = true;
            }
            else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Сообщение");
                alert.setHeaderText("Ошибка загрузки карты");
                alert.setContentText(errorMessage);
                errorMessage = "";
                alert.show();
            }
        }
        catch (IOException | NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Сообщение");
            alert.setHeaderText("Ошибка загрузки карты");
            alert.setContentText(ex.toString());
            alert.show();
        }
    }



    /**
     *
     * Загружает карту и формирует список продаваемых на этой карте товаров
     *
     * */
    private boolean mapLoad(String fileName) throws IOException, NumberFormatException {
        HashMap<Byte, Pair<Integer, String>> allProductsTypes = allProductsTypesLoad();


        productType = new HashMap<>();

        String[] lines = Files.readAllLines(Paths.get(fileName)).toArray(new String[0]);
        if (lines.length != matrixSize) {
            errorMessage = "\nФормат файла карты не верен!\nНужная размерность матрицы -  15 * 15.";
            return false;
        }
        matrix = new byte[matrixSize][matrixSize];
        productsCount = new byte[matrixSize][matrixSize];

        String [] row;

        boolean storage = false, cashbox = false, gate = false;

        for (int i = 0; i < matrixSize; i++) {
            row = lines[i].split(" ");
            if (row.length != matrixSize) {
                errorMessage = "Формат файла карты не верен!\nНужная размерность матрицы -  15 * 15.";
                return false;
            }
            for (int j = 0; j < matrixSize; j++) {
                matrix[i][j] = Byte.valueOf(row[j]);
                if (matrix[i][j] < 0) {
                    if (matrix[i][j] < storageID) {
                        productsCount[i][j] = maxProductsInCell;
                        if (!productType.containsKey(matrix[i][j]))
                            productType.put(matrix[i][j], allProductsTypes.get(matrix[i][j]));
                    }
                    if (matrix[i][j] == storageID) {
                        Storage.setPosition(i, j);
                        storage = true;
                    }
                    if (matrix[i][j] == cashboxID) {
                        Cashbox.setPosition(i, j);
                        cashbox = true;
                    }
                    if (matrix[i][j] == gateID) {
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
     *
     * Загружает файл products.txt
     *
     * */
    private HashMap<Byte, Pair<Integer, String>> allProductsTypesLoad() throws IOException, NumberFormatException {
        String[] lines = Files.readAllLines(Paths.get(defaultProductsFileName)).toArray(new String[0]);
        HashMap<Byte, Pair<Integer, String>> allProductsTypes = new HashMap<>();

        String[] row;

        for (int i = 0; i < lines.length; i++) {
            row = lines[i].split(":");
            byte ID = Byte.parseByte(row[2]);
            allProductsTypes.put(ID, new Pair<>(Integer.parseInt(row[1]), row[0]));
        }
        return allProductsTypes;
    }



    /**
     *
     * готова ли карта к работе
     *
     *
     * */
    private boolean isReady;


    public boolean isReady() {
        return isReady;
    }

    public static void initialization(String filename, Pane pane, TextArea textArea) {
        ourInstance = new Map(filename, pane, textArea);
    }



    private class Take implements Takeable {
        @Override
        public boolean takeProduct(int row, int column) {
            if (productsCount[row][column] > 0) {
                productsCount[row][column]--;
                counters[row][column].setText(String.valueOf(productsCount[row][column]));
                if (productsCount[row][column] == 0) {
                    Background back = new Background(new BackgroundImage(emptyCounterImage,
                            BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
                    counters[row][column].setBackground(back);
                }
                return true;
            }
            return false;
        }
    }


    class Put implements Putable {
        @Override
        public void putProduct(int row, int column) {
            productsCount[row][column] = maxProductsInCell;
            Background back = new Background(new BackgroundImage(counterImages.get(matrix[row][column]),
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
            counters[row][column].setBackground(back);
            counters[row][column].setText(String.valueOf(productsCount[row][column]));
        }
    }


    private boolean customerHasEntered() {
        return (ThreadLocalRandom.current().nextInt(0, maxCustomers)) == 0;
    }

    /**
     *
     * один шаг жизненного цикла карты
     *
     * */
    public void timeStep() {
        if (isReady) {

            if (customers.size() < maxCustomers)
                if (customerHasEntered())
                    customers.add(new Customer(new Take(), mapPane, customerImage, textArea));
            /**
             *
             * Цикл обработки посетителей
             *
             * */
            for (int i = 0; i < customers.size(); i++) {
                if (!customers.get(i).isMove())
                    customers.get(i).timeStep();
            }


            for (int i = 0; i < customers.size(); i++) {
                if (!customers.get(i).isMove()) {
                    if (customers.get(i).isGoingToLeave()) {
                        customers.get(i).leave();
                        customers.remove(i);
                        i--;
                    }
                }
            }

            if (!Manager.getInstance().isMove())
                Manager.getInstance().timeStep();
        }
    }


    public void pause() {
        for (Customer customer : customers)
            customer.pause();
        Manager.getInstance().pause();
    }

    public void play() {
        textArea.setText(textAreaMessage);
        for (Customer customer : customers)
            customer.play();
        Manager.getInstance().play();
    }




    public Pair<Integer, Integer> getNearestMostAttractiveProduct(int row, int column) {
        byte min = storageID - 1;
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
        byte [][] copy = new byte[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++)
            System.arraycopy(matrix[i], 0, copy[i], 0, matrixSize);
        return copy;
    }

    public byte getProductCount(int row, int column) {
        return productsCount[row][column];
    }

    /**
     *
     * Возвращает инфу о прилавке с товаром по индексам его расположения
     *
     * */
    public Pair<Integer, String> getProductInfo(int row, int column) {
        return productType.get(matrix[row][column]);
    }


    public Pair<Integer, String> getProductInfo(byte ID) {
        return productType.get(ID);
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
}
