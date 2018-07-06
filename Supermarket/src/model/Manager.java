package model;

import javafx.animation.Animation;
import javafx.animation.PathTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.CacheHint;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polyline;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;

public class Manager {

    private static Manager ourInstance = new Manager();

    public static Manager getInstance() {
        return ourInstance;
    }


    private Putable putable;


    private Manager() {
        inactivity = true;
    }

    /**
     *
     * Координаты менеджера
     *
     * */
    private int managerRow, managerColumn;


    /**
     *
     * Отрисовка
     *
     *
     * */
    private ImageView imageView;

    private Pane pane;

    private PathTransition path;

    private Product refill;

    private TextArea textArea;


    public void init(Putable putable, Pane pane, Image managerImage, TextArea textArea) {
        this.putable = putable;
        this.pane = pane;
        this.textArea = textArea;
        imageView = new ImageView(managerImage);
        imageView.setCache(true);
        imageView.setCacheHint(CacheHint.SPEED);

        imageView.setY(Storage.getRow() * Map.padding);
        imageView.setX(Storage.getColumn() * Map.padding);
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (paused) {
                    if (refill != null)
                        textArea.setText("Несу " + refill.getName() + " к пустому прилавку...");
                    else {
                        if (inactivity)
                            textArea.setText("Жду пустого прилавка...");
                        else if (goToStorage)
                            textArea.setText("Иду на склад...");
                    }
                }
            }
        });

        pane.getChildren().add(imageView);
    }

    private void putProduct() {
        putable.putProduct(emptyCounterRow, getEmptyCounterColumn);
        refill = null;
        goToStorage = true;
        isNear = false;
        emptyCounterRow = -1;
        getEmptyCounterColumn = -1;
    }



    public void timeStep() {
        if (inactivity) {
            path = findPathToEmptyCounter();
            if (path != null) {
                inactivity = false;
                isMove = true;
                path.setNode(imageView);
                path.play();
            }
            else if (isNear) {
                /**
                 *
                 * мы рядом с пустой полкой
                 * сразу кладем в нее товар
                 *
                 * */
                putProduct();
            }
        }
        else {
            if (!goToStorage) {
                /**
                 *
                 * пополняем полку
                 *
                 * */
                putProduct();

            }
            else {
                /**
                 *
                 * идем обратно на склад
                 *
                 *
                 * */
                if (path != null) {
                    path.setRate(-1);
                    isMove = true;
                    path.setOnFinished(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            isMove = false;
                            isNear = false;
                            inactivity = true;
                            goToStorage = false;
                        }
                    });
                    path.play();
                }
                else {
                    inactivity = true;
                    isNear = false;
                    isMove = false;
                    goToStorage = false;
                }
            }
        }
    }

    private boolean paused;

    public void pause() {
        paused = true;
        if (path != null)
            path.pause();
    }

    public void play() {
        paused = false;
        if (path != null) {
            if (path.getStatus() == Animation.Status.PAUSED) {
                path.play();
            }
        }
    }


    public boolean isMove() {
        return isMove;
    }


    private int emptyCounterRow, getEmptyCounterColumn;

    private boolean isMove;

    private boolean isNear;

    private boolean goToStorage;

    private boolean inactivity;


    private PathTransition findPathToEmptyCounter() {

        byte [][] matrix = Map.getInstance().getCopyOfMatrix();


        boolean isEmpty = false;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (matrix[i][j] < Map.storageID && Map.getInstance().getProductCount(i, j) == 0) {
                    isEmpty = true;
                    break;
                }
            }
            if (isEmpty)
                break;
        }

        if (!isEmpty)
            return null;



        /**
         *
         * Распространение волны
         *
         *
         * */

        int row = Storage.getRow();
        int column = Storage.getColumn();


        byte step = 1;

        matrix[row][column] = step;

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
                                else if (matrix[i + iOffset][j + jOffset] < Map.storageID
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

        /**
         *
         * координаты найденного товара
         *
         * */

        emptyCounterRow = destY;
        getEmptyCounterColumn = destX;



        refill = new Product(Map.getInstance().getProductID(emptyCounterRow, getEmptyCounterColumn),
                Map.getInstance().getProductInfo(emptyCounterRow, getEmptyCounterColumn));


        /**
         *
         * Восстановление пути
         *
         * */
        ArrayList<Pair<Integer, Integer>> path = new ArrayList<>();


        PathTransition p;


        double [] arr;

        if (step > 1) {


            do {
                for (int iOffset = -1; iOffset < 2; iOffset++) {
                    if (destY + iOffset < 0 || destY + iOffset > matrix.length - 1)
                        continue;
                    for (int jOffset = -1; jOffset < 2; jOffset++) {
                        if (destX + jOffset < 0 || destX + jOffset > matrix.length - 1)
                            continue;
                        if (matrix[destY + iOffset][destX + jOffset] == step) {
                            path.add(new Pair<>(destY + iOffset, destX + jOffset));
                            destY += iOffset;
                            destX += jOffset;
                            step--;
                            break;
                        }
                    }
                }
            }
            while (step > 0);


            arr = new double[path.size() * 2];

            int index = 0;


            /**
             *
             * обновляем координаты покупателя
             *
             * */
            managerRow = path.get(0).getKey();
            managerColumn = path.get(0).getValue();

            for (int i = arr.length - 2; i >= 0; i -= 2) {
                arr[i] = path.get(index).getValue() * Map.padding + Map.halfPadding;
                arr[i + 1] = path.get(index).getKey() * Map.padding + Map.halfPadding;
                index++;
            }

            p = new PathTransition();

            p.setPath(new Polyline(arr));

            p.setDuration(Duration.seconds(arr.length / 4.0));

            p.setOnFinished(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    isMove = false;
                    isNear = true;
                }
            });

            p.setCycleCount(1);
        }
        else {
            isNear = true;
            p = null;
        }
        return p;
    }




}
