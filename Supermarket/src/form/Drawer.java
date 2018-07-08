package form;

import javafx.event.EventHandler;
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
import model.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Drawer {

    /**
     * Ссылка на карту
     * */
    private Map map;

    /**
     * Ссылка на текстовое поле для вывода информации
     * */
    private TextArea textArea;

    /**
     * Ширина одной клетки карты в пикселях
     * */
    private static final int padding = 32;

    /**
     * Толщина горизонтальных и вертикальных линий для отображения карты
     * */
    private static final double horizLineWidth = 1.5;

    private static final double verticLineWidth = 0.5;

    /**
     * копии массивов matrix и productsCount карты
     * */
    private byte[][] matrix, productsCount;

    /**
     * Массив лейблов, каждый элемент массива изображает прилавок с товаром
     * */
    private Label[][] counters;

    /**
     * Хеш-таблица изображений прилавков
     * */
    private HashMap<Byte, Image> counterImages;

    /**
     * Изображение пустого прилавка
     * */
    private Background emptyCounter;

    /**
     * массив ImageView, предназначеных для отрисовки изображений покупателей
     * */
    private ImageView[] customerImageView;

    /**
     * ImageView, предназначеный для отрисовки изображения менеджера
     * */
    private ImageView managerImageView;

    /**
     * Флаг - остановлена симуляция или нет
     * */
    private boolean paused;

    public Drawer(Map map, Pane pane, TextArea textArea) {
        this.map = map;
        this.textArea = textArea;
        HashMap<Byte, Pair<Integer, String>> productType = map.getProductType();
        try {
            /**
             * Загрузка всех изображений
             * */
            Image customerImage = new Image(new FileInputStream("sources/images/customer.png"));
            Image managerImage = new Image(new FileInputStream("sources/images/manager.png"));
            Image emptyCounterImage = new Image(new FileInputStream("sources/images/counters/emptyCounter.png"));
            Image storageImage = new Image(new FileInputStream("sources/images/storageImage.png"));
            Image cashboxImage = new Image(new FileInputStream("sources/images/cashboxImage.png"));
            Image gateImage = new Image(new FileInputStream("sources/images/gateImage.png"));

            counterImages = new HashMap<>();

            for (Byte productID : productType.keySet()) {
                Image image = new Image(new FileInputStream("sources/images/counters/" + productID + ".png"));
                counterImages.put(productID, image);
            }

            /**
             * Отрисовка сетки
             * */
            int matrixSize = Map.MATRIX_SIZE;
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
             * Отрисовка прилавков с товарами
             * */
            matrix = map.getCopyOfMatrix();
            productsCount = map.getCopyOfProductsCountMatrix();
            counters = new Label[matrixSize][matrixSize];
            emptyCounter = new Background(new BackgroundImage(emptyCounterImage,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, BackgroundSize.DEFAULT));
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
                            pane.getChildren().add(counters[i][j]);
                        }
                    }
                }
            }

            /**
             * Создание и добавление изображений кассы, входа и склада
             * */
            ImageView iv = new ImageView(storageImage);
            iv.setCache(true);
            iv.setCacheHint(CacheHint.SPEED);
            iv.setY(Storage.getRow() * padding);
            iv.setX(Storage.getColumn() * padding);
            pane.getChildren().add(iv);

            iv = new ImageView(cashboxImage);
            iv.setCache(true);
            iv.setCacheHint(CacheHint.SPEED);
            iv.setY(Cashbox.getInstance().getRow() * padding);
            iv.setX(Cashbox.getInstance().getColumn() * padding);
            pane.getChildren().add(iv);

            iv = new ImageView(gateImage);
            iv.setCache(true);
            iv.setCacheHint(CacheHint.SPEED);
            iv.setY(Gate.getRow() * padding);
            iv.setX(Gate.getColumn() * padding);
            pane.getChildren().add(iv);

            /**
             * Создание и добавдение всех изображений покупателей
             * */
            customerImageView = new ImageView[Map.MAX_CUSTOMERS];
            for (int i = 0; i < customerImageView.length; i++) {
                customerImageView[i] = new ImageView(customerImage);
                customerImageView[i].setCache(true);
                customerImageView[i].setCacheHint(CacheHint.SPEED);
                customerImageView[i].setVisible(false);
                pane.getChildren().add(customerImageView[i]);
            }

            /**
             * создание и добавление изображения менеджера
             * */
            managerImageView = new ImageView(managerImage);
            managerImageView.setCache(true);
            managerImageView.setCacheHint(CacheHint.SPEED);
            managerImageView.relocate(Storage.getColumn() * padding, Storage.getRow() * padding);
            pane.getChildren().add(managerImageView);

            /**
             * добавление слушателя на событие "клик мышью по объекту mapPane", который
             * выводит информацию о покупателе в данном месте
             * */
            pane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (paused) {
                        int column = (int) event.getX();
                        column /= padding;
                        int row = (int) event.getY();
                        row /= padding;
                        String text = map.getObjectInfo(row, column);
                        if (text.length() != 0)
                            textArea.setText(text);
                    }
                }
            });
        } catch (IOException | NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Сообщение");
            alert.setHeaderText("Ошибка загрузки изображений");
            alert.setContentText(ex.toString());
            alert.show();
        }
    }

    /**
     * Установка флага paused
     * */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Перерисовка карты
     * */
    public void draw() {
        ArrayList<ObjectOnMap> objects = map.getObjects();
        int count = 0;
        for (ObjectOnMap object : objects) {
            if (object instanceof Customer) {
                int row = object.getRow() * padding;
                int column = object.getColumn() * padding;
                customerImageView[count].setVisible(true);
                customerImageView[count++].relocate(column + ThreadLocalRandom.current().nextInt(-4, 5),
                        row + ThreadLocalRandom.current().nextInt(-4, 5));
            }
            else
                managerImageView.relocate(object.getColumn() * padding, object.getRow() * padding);
        }
        for (int j = count; j < Map.MAX_CUSTOMERS; j++) {
            customerImageView[j].setVisible(false);
        }
        byte[][] copy = map.getCopyOfProductsCountMatrix();
        for (int i = 0; i < productsCount.length; i++) {
            for (int j = 0; j < productsCount.length; j++) {
                if (matrix[i][j] < Map.STORAGE_ID) {
                    if (productsCount[i][j] != copy[i][j]) {
                        counters[i][j].setText(String.valueOf(copy[i][j]));
                        if (copy[i][j] == 0)
                            counters[i][j].setBackground(emptyCounter);
                        else if (productsCount[i][j] == 0)
                            counters[i][j].setBackground(new Background(new BackgroundImage(counterImages.get(matrix[i][j]),
                                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
                    }
                }
            }
        }
        productsCount = copy;
    }
}