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
import java.util.concurrent.ThreadLocalRandom;

public class Customer {

    private int freeMoney, necessaryMoney;

    private static final int maxFreeMoney = 1000;

    /**
     * товары для покупки
     *
     * */
    private ArrayList<Product> toBuy;


    private boolean [] bought;

    /**
     * Номер текущего продукта для покупки
     *
     * */
    private int currentProductToBuy;

    /**
     * "бесполезные товары"
     *
     * */
    private ArrayList<Product> useless;


    private static final int maxProductsToBuy = 3;


    private static final byte willpower = 32;


    private Takeable takeable;


    /**
     *
     * Координаты покупателя
     *
     * */
    private int customerRow, customerColumn;



    /**
     *
     * Специфика отрисовки
     *
     * */

    private ImageView imageView;

    /**
     *
     * контейнер для отрисовки
     *
     * */
    private Pane pane;

    /**
     *
     * Класс анимации пути
     *
     * */
    private PathTransition path;

    private TextArea textArea;

    public Customer(Takeable takeable, Pane pane, Image customerImage, TextArea textArea) {

        this.takeable = takeable;

        int productCount = ThreadLocalRandom.current().nextInt(1, maxProductsToBuy + 1);
        if (productCount > Map.getInstance().getProductsAssortment())
            productCount = Map.getInstance().getProductsAssortment();
        currentProductToBuy = 0;
        toBuy = new ArrayList<>();

        Byte [] productsID = Map.getInstance().getShuffleProductsID(productCount);

        for (int i = 0; i < productCount; i++) {
            Product toAdd = Map.getInstance().getProductByID(productsID[i]);
            necessaryMoney += toAdd.getCost();
            toBuy.add(toAdd);
        }

        bought = new boolean[productCount];

        freeMoney = ThreadLocalRandom.current().nextInt(0, maxFreeMoney);
        useless = new ArrayList<>();

        justPaid = false;
        isMove = false;
        isNear = false;
        isGoingToLeave = false;

        customerRow = Gate.getRow();
        customerColumn = Gate.getColumn();

        this.pane = pane;

        this.textArea = textArea;

        imageView = new ImageView(customerImage);
        imageView.setCache(true);
        imageView.setCacheHint(CacheHint.SPEED);
        imageView.setY(customerRow * Map.padding);
        imageView.setX(customerColumn * Map.padding);
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (paused) {
                    int check = 0;
                    StringBuilder info = new StringBuilder("Необходимые деньги: " + necessaryMoney);
                    info.append("\nЛишние деньги: ").append(freeMoney).append("\nСписок:\n");
                    for (int i = 0; i < toBuy.size(); i++) {
                        info.append(toBuy.get(i).getName()).append(" - ");
                        if (bought[i]) {
                            info.append("куплено\n");
                            check += toBuy.get(i).getCost();
                        }
                        else
                            info.append("не куплено\n");
                    }
                    info.append("\nБесполезные продукты:\n");
                    if (useless.size() > 0) {
                        for (Product uselessProduct : useless) {
                            info.append(uselessProduct.getName()).append("\n");
                            check += uselessProduct.getCost();
                        }
                    }
                    else
                        info.append("нет");
                    info.append("\n\nСумма покупки: ").append(check);
                    textArea.setText(info.toString());
                }
            }
        });


        pane.getChildren().add(imageView);

        emptyCounters = new ArrayList<>();

    }


    private int objectRow, objectColumn;


    private boolean paused;


    /**
     *
     * двигается ли покупатель в данный момент
     *
     * */

    private boolean isMove;

    /**
     *
     * расплатился ли покупатель
     *
     * */
    private boolean justPaid;

    /**
     *
     * рядом ли покупатель с пнуктом назначения
     *
     * */
    private boolean isNear;

    /**
     *
     * Собирается ли покупатель выйти из магазина
     *
     * */
    private boolean isGoingToLeave;

    public boolean isGoingToLeave() {
        return isGoingToLeave;
    }

    public boolean isMove() {
        return isMove;
    }


    /**
     *
     * взять товар по координатам
     * false, если товара там больше нет
     *
     * */
    private boolean takeProduct() {
        return takeable.takeProduct(objectRow, objectColumn);
    }

    /**
     *
     *  если покупателя привлек бесполезный продукт, возвращает продукт
     *  иначе - null
     *
     * */
    private Product takeUselessProduct() {
        Pair<Integer, Integer> coords = Map.getInstance().getNearestMostAttractiveProduct(customerRow, customerColumn);

        if (coords != null) {
            if (Map.getInstance().getProductCount(coords.getKey(), coords.getValue())  > 0) {
                byte ID = Map.getInstance().getProductID(coords.getKey(), coords.getValue());
                Product uselessProduct = Map.getInstance().getProductByID(ID);
                if (Math.abs(ID) > ThreadLocalRandom.current().nextInt(0, willpower + 1)
                        && uselessProduct.getCost() < freeMoney) {
                    freeMoney -= uselessProduct.getCost();
                    takeable.takeProduct(coords.getKey(), coords.getValue());
                    return uselessProduct;
                }
            }
        }
        return null;
    }


    /**
     *
     * Шаг обработки покупателя
     *
     * */
    public void timeStep() {
        if (currentProductToBuy < toBuy.size()) {
            if (noMore) {
                currentProductToBuy++;
                emptyCounters.clear();
                if (currentProductToBuy == toBuy.size()) {
                    boolean toPay = false;
                    for (int i = 0; i < bought.length; i++) {
                        if (bought[i]) {
                            toPay = true;
                            break;
                        }
                    }
                    if (!toPay)
                        justPaid = true;
                }
                noMore = false;
                return;
            }
            if (!isNear) {
                path = findPath(customerRow, customerColumn, toBuy.get(currentProductToBuy).getID());
                if (path != null) {
                    isMove = true;
                    path.setNode(imageView);
                    path.play();
                }
            }
            else {
                path = null;
                if (takeProduct()) {
                    necessaryMoney -= toBuy.get(currentProductToBuy).getCost();
                    Product uselessProduct = takeUselessProduct();
                    if (uselessProduct != null)
                        useless.add(uselessProduct);
                    bought[currentProductToBuy++] = true;
                    isNear = false;
                }
                else {
                    emptyCounters.add(new Pair<>(objectRow, objectColumn));
                    isNear = false;
                }
            }
        }
        else if (!justPaid) {
            if (!isNear) {
                path = findPath(customerRow, customerColumn, Map.cashboxID);
                if (path != null) {
                    isMove = true;
                    path.setNode(imageView);
                    path.play();
                }
            }
            else {
                path = null;
                int sum = 0, uselessSum = 0;
                for (int i = 0; i < toBuy.size(); i++) {
                    if (bought[i]) {
                        sum += toBuy.get(i).getCost();
                    }
                }

                for (int i = 0; i < useless.size(); i++) {
                    if (bought[i]) {
                        uselessSum += useless.get(i).getCost();
                    }
                }

                Cashbox.getInstance().pay(sum, uselessSum);
                isNear = false;
                justPaid = true;
            }
        }
        else {
            if (!isGoingToLeave) {
                if (!isNear) {
                    path = findPath(customerRow, customerColumn, Map.gateID);
                    if (path != null) {
                        isMove = true;
                        path.setNode(imageView);
                        path.play();
                    }
                } else {
                    path = null;
                    isGoingToLeave = true;
                }
            }
        }
    }


    /**
     *
     * Покупатель уходит из магазина
     *
     * */
    public void leave() {
        pane.getChildren().remove(imageView);
    }



    /**
     *
     * Приостановка и запук анимации
     * (используется во время остановки симуляции)
     *
     * */
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



    /**
     *
     * Пустые прилавки, которые запоминает покупатель
     * список координат
     *
     * */
    private ArrayList<Pair<Integer, Integer>> emptyCounters;


    /**
     *
     * больше нет ни одного прилавка с наличием нужного товара
     *
     * */
    private boolean noMore;

    /**
     *
     * Ищет путь до чего-либо
     *
     * возвращает null, если путь до объекта имеет длину 1, то есть
     * объект находится рядом и идти никуда не нужно (если noMore = false)
     *
     * если noMore = true, то объекта нет на карте
     *
     *
     *
     * */

    private PathTransition findPath(int row, int column, byte objectID) {

        byte [][] cMatrix = Map.getInstance().getCopyOfMatrix();



        /**
         *
         *  проходимсся по всем местам, где товар закончился,
         *  и проверяем его наличие заново
         *
         * */

        if (emptyCounters.size() > 0) {

            for (int i = 0; i < emptyCounters.size(); i++) {
                int emptyRow = emptyCounters.get(i).getKey();
                int emptyColumn = emptyCounters.get(i).getValue();
                if (Map.getInstance().getProductCount(emptyRow, emptyColumn) == 0)
                    cMatrix[emptyRow][emptyColumn] = -1;
                else
                    emptyCounters.remove(i--);
            }


            boolean hasProduct = false;

            for (int i = 0; i < 15; i++) {
                for (int j = 0; j < 15; j++) {
                    if (cMatrix[i][j] == objectID) {
                        hasProduct = true;
                        break;
                    }
                }
            }

            /**
             *
             * товара нет - выходим
             *
             * */

            if (!hasProduct) {
                noMore = true;
                return null;
            }
        }


        /**
         *
         * Распространение волны
         *
         *
         * */


        byte step = 1;

        cMatrix[row][column] = step;

        int destX = -1, destY = -1;

        boolean isFinished = false;

        while (step < Byte.MAX_VALUE) {

            for (int i = 0; i < cMatrix.length; i++) {

                for (int j = 0; j < cMatrix.length; j++) {

                    if (cMatrix[i][j] == step) {

                        for (int iOffset = -1; iOffset < 2; iOffset++) {

                            if (i + iOffset < 0 || i + iOffset > cMatrix.length - 1)
                                continue;

                            for (int jOffset = -1; jOffset < 2; jOffset++) {

                                if (j + jOffset < 0 || j + jOffset > cMatrix.length - 1)
                                    continue;

                                if (cMatrix[i + iOffset][j + jOffset] == 0)
                                    cMatrix[i + iOffset][j + jOffset] = (byte) (step + 1);
                                else if (cMatrix[i + iOffset][j + jOffset] == objectID) {
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
         * координаты найденного товара или объекта
         *
         * */

        objectRow = destY;
        objectColumn = destX;


        /**
         *
         * Восстановление пути
         *
         * */
        ArrayList<Pair<Integer, Integer>> path = new ArrayList<>();


        PathTransition p;


        double [] arr;

        if (step > 1) {

            if (objectID == Map.gateID)
                path.add(new Pair<>(Gate.getRow(), Gate.getColumn()));

            do {
                for (int iOffset = -1; iOffset < 2; iOffset++) {
                    if (destY + iOffset < 0 || destY + iOffset > cMatrix.length - 1)
                        continue;
                    for (int jOffset = -1; jOffset < 2; jOffset++) {
                        if (destX + jOffset < 0 || destX + jOffset > cMatrix.length - 1)
                            continue;
                        if (cMatrix[destY + iOffset][destX + jOffset] == step) {
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
            customerRow = path.get(0).getKey();
            customerColumn = path.get(0).getValue();

            for (int i = arr.length - 2; i >= 0; i -= 2) {
                arr[i] = path.get(index).getValue() * Map.padding + Map.halfPadding;
                arr[i + 1] = path.get(index).getKey() * Map.padding + Map.halfPadding;
                index++;
            }

            p = new PathTransition();

            p.setPath(new Polyline(arr));

            p.setDuration(Duration.seconds(arr.length / 2.5));

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
