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

    private Map map;

    private TextArea textArea;

    private static final int padding = 32;

    private static final double horizLineWidth = 1.5;

    private static final double verticLineWidth = 0.5;

    private byte[][] matrix, productsCount;

    private Label[][] counters;

    private HashMap<Byte, Image> counterImages;

    private Background emptyCounter;

    private ImageView[] customerImageView;

    private ImageView managerImageView;

    private boolean paused;

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Drawer(Map map, Pane pane, TextArea textArea) {
        this.map = map;
        this.textArea = textArea;


        HashMap<Byte, Pair<Integer, String>> productType = map.getProductType();

        try {

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


            customerImageView = new ImageView[Map.MAX_CUSTOMERS];
            for (int i = 0; i < customerImageView.length; i++) {
                customerImageView[i] = new ImageView(customerImage);
                customerImageView[i].setCache(true);
                customerImageView[i].setCacheHint(CacheHint.SPEED);
                customerImageView[i].setVisible(false);
                pane.getChildren().add(customerImageView[i]);
            }


            managerImageView = new ImageView(managerImage);
            managerImageView.setCache(true);
            managerImageView.setCacheHint(CacheHint.SPEED);
            managerImageView.relocate(Storage.getColumn() * padding, Storage.getRow() * padding);
            managerImageView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (paused)
                        textArea.setText(map.getManagerInfo());
                }
            });

            pane.getChildren().add(managerImageView);

            pane.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (paused) {
                        int column = (int) event.getX();
                        column /= padding;
                        int row = (int) event.getY();
                        row /= padding;
                        String text = map.getCustomerInfo(row, column);
                        if (text.length() != 0)
                            textArea.setText(map.getCustomerInfo(row, column));
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



    public void draw() {
        managerImageView.relocate(map.getManagerColumn() * padding, map.getManagerRow() * padding);
        ArrayList<Customer> customers = map.getCustomers();
        int count = 0;
        for (Customer customer : customers) {
            int row = customer.getRow() * padding;
            int column = customer.getColumn() * padding;
            customerImageView[count].setVisible(true);
            customerImageView[count++].relocate(column + ThreadLocalRandom.current().nextInt(-4, 5),
                    row + ThreadLocalRandom.current().nextInt(-4, 5));
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