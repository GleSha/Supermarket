package model;

import javafx.util.Pair;
import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;

public class Customer extends ObjectOnMap {

    /**
     * необходимые и лишние деньги покупателя
     * */
    private int freeMoney, necessaryMoney;

    private static final int MAX_FREE_MONEY = 1000;

    /**
     * товары для покупки
     * */
    private ArrayList<Product> toBuy;

    /**
     * цели покупателя - ID всех продуктов по списку, кассы и выхода
     * */
    private byte [] targets;

    /**
     * Номер текущей цели (товара, кассы или выхода)
     * */
    private int currentTarget;

    /**
     * массив флагов, кплен товар или нет
     * */
    private boolean [] bought;

    /**
     * "бесполезные товары"
     * */
    private ArrayList<Product> useless;

    /**
     * максисмальное число продуктов, которое может купить покупатель
     * */
    private static final int MAX_PRODUCTS_TO_BUY = 3;

    /**
     * Сила сопротивляемости покупателя бесполезным товарам
     * */
    private static final byte WILLPOWER = 32;

    /**
     * координаты текущей цели покупателя - прилавка с товаром, кассы или выхода
     * */
    private int targetRow, targetColumn;

    /**
     * Пустые прилавки, которые запоминает покупатель
     * (список координат)
     * */
    private ArrayList<Pair<Integer, Integer>> emptyCounters;

    /**
     * вышел покупатель из магазина или нет
     * */
    private boolean left;

    /**
     * копия матрицы карты (нужна для алгоритма поиска пути)
     * */
    private byte [][] matrix;

    /**
     * Путь покупателя (список координат)
     * */
    private Stack<Pair<Integer, Integer>> path;

    /**
     * максимальная и минимальная длина пути
     * */
    private final byte MAX_STEP = Byte.MAX_VALUE;

    private final byte MIN_STEP = 1;

    /**
     * конструктор
     * */
    public Customer() {
        int productCount = ThreadLocalRandom.current().nextInt(1, MAX_PRODUCTS_TO_BUY + 1);
        if (productCount > Map.getInstance().getProductsAssortment())
            productCount = Map.getInstance().getProductsAssortment();
        targets = new byte[productCount + 2];
        currentTarget = 0;
        toBuy = new ArrayList<>();
        Byte [] productsID = Map.getInstance().getShuffleProductsID(productCount);
        for (int i = 0; i < productCount; i++) {
            Product toAdd = Map.getInstance().getProductByID(productsID[i]);
            necessaryMoney += toAdd.getCost();
            toBuy.add(toAdd);
            targets[i] = productsID[i];
        }
        targets[productCount] = Map.CASHBOX_ID;
        targets[productCount + 1] = Map.GATE_ID;
        bought = new boolean[productCount];
        freeMoney = ThreadLocalRandom.current().nextInt(0, MAX_FREE_MONEY);
        useless = new ArrayList<>();
        objectOnMapRow = Gate.getRow();
        objectOnMapColumn = Gate.getColumn();
        emptyCounters = new ArrayList<>();
    }

    /**
     * взять товар по координатам
     * false, если товара там больше нет
     * */
    private boolean takeProduct() {
        if (Map.getInstance().takeProduct(targetRow, targetColumn)) {
            necessaryMoney -= toBuy.get(currentTarget).getCost();
            return true;
        }
        return false;
    }

    /**
     *  если покупателя привлек бесполезный продукт, возвращает продукт
     *  иначе - null
     * */
    private Product takeUselessProduct() {
        Pair<Integer, Integer> coords = Map.getInstance().getNearestMostAttractiveProduct(objectOnMapRow, objectOnMapColumn);
        if (coords != null) {
            if (Map.getInstance().getProductCount(coords.getKey(), coords.getValue())  > 0) {
                byte ID = Map.getInstance().getProductID(coords.getKey(), coords.getValue());
                Product uselessProduct = Map.getInstance().getProductByID(ID);
                if (Math.abs(ID) > ThreadLocalRandom.current().nextInt(0, WILLPOWER + 1)
                        && uselessProduct.getCost() < freeMoney) {
                    freeMoney -= uselessProduct.getCost();
                    Map.getInstance().takeProduct(coords.getKey(), coords.getValue());
                    return uselessProduct;
                }
            }
        }
        return null;
    }

    /**
     * Шаг обработки покупателя
     * */
    public void liveStep() {
        /**
         * 1    найти путь
         * 2    передвинуться
         * 3    если путь завершен - действовать (взять товар/расплатиться)
         * */
        if (currentTarget < targets.length) {
            if (path == null) {
                byte pathLength = findPath(objectOnMapRow, objectOnMapColumn, targets[currentTarget]);

                if (pathLength == 1) {
                    /**
                     * Берем товар, платим или выходим
                     * */
                    action(currentTarget);
                }
                if (pathLength == MAX_STEP) {
                    /**
                     * товара нет
                     * */
                    path = null;
                    currentTarget++;
                    if (currentTarget == targets.length - 2) {
                        boolean goHome = true;
                        for (boolean b : bought) {
                            if (b) {
                                goHome = false;
                                break;
                            }
                        }
                        if (goHome)
                            currentTarget = targets.length - 1;
                    }
                    emptyCounters.clear();
                }
            }
            else {
                /**
                 * если есть путь, идем по пути
                 * */
                if (!path.isEmpty())
                    moveTo(path.pop());
                else {
                    /**
                     * иначе - мы у цели, действуем
                     * */
                    path = null;
                    action(currentTarget);
                }
            }
        }
    }

    /**
     * действие покупателя (покупка, оплата, выход)
     * параметр - номер текущей цели
     * */
    private void action(int targetIndex) {
        if (targetIndex < toBuy.size()) {
            /**
             * если товар есть на полке, то берем его, переходим к следующей цели
             * если товара нет, запоминаем координаты пустого прилавка
             * */
            if (takeProduct()) {
                bought[currentTarget] = true;
                currentTarget++;
                /**
                 * Проверяем, возьмет ли пользователь бесполезный продукт
                 * */
                Product uselessProduct = takeUselessProduct();
                if (uselessProduct != null)
                    useless.add(uselessProduct);
            }
            else
                emptyCounters.add(new Pair<>(targetRow, targetColumn));
        }
        else if (targets[targetIndex] == Map.CASHBOX_ID) {
            /**
             * покупаем товары
             * */
            int sum = 0, uselessSum = 0;
            for (int i = 0; i < toBuy.size(); i++)
                if (bought[i])
                    sum += toBuy.get(i).getCost();
            for (Product uselessProduct : useless)
                uselessSum += uselessProduct.getCost();
            Cashbox.getInstance().pay(sum, uselessSum);
            currentTarget++;
        }
        else {
            /**
             * мы у выхода - покидаем карту
             * */
            left = true;
            currentTarget++;
        }
    }

    /**
     * метод нужен для удаления картой покупателя
     * */
    @Override
    public boolean left() {
       return left;
    }


    /**
     * пережвигает покупателя на 1 шаг по карте
     * параметр {@param coords} не должнен быть равен null
     * */
    private void moveTo(Pair<Integer, Integer> coords) {
        objectOnMapRow = coords.getKey();
        objectOnMapColumn = coords.getValue();
    }

    /**
     * возвращает длину пути
     * если она равна MIN_STEP, то мы рядом, идти никуда не нужно
     * если MAX_STEP, то товара больше нет, идем за слудющим
     * если иное, то путь есть, идем по нему
     * */
    private byte findPath(int row, int column, byte objectID) {
        matrix = Map.getInstance().getCopyOfMatrix();
        if (objectID < Map.STORAGE_ID)
            if (noMoreProduct(objectID))
                return MAX_STEP;
        if (objectID == Map.GATE_ID)
            if (row == Gate.getRow() && column == Gate.getColumn())
                return MIN_STEP;
        byte step = startWave(row, column, objectID);
        if (step > MIN_STEP && step < MAX_STEP) {
            restorePath(step, objectID);
            return step;
        }
        return step;
    }


    /**
     *  проходимся по всем местам, где товар закончился,
     *  и проверяем его наличие заново
     * */
    private boolean noMoreProduct(byte productID) {
        if (emptyCounters.size() > 0) {
            for (int i = 0; i < emptyCounters.size(); i++) {
                int emptyRow = emptyCounters.get(i).getKey();
                int emptyColumn = emptyCounters.get(i).getValue();
                if (Map.getInstance().getProductCount(emptyRow, emptyColumn) == 0)
                    matrix[emptyRow][emptyColumn] = -1;
                else
                    emptyCounters.remove(i--);
            }

            boolean hasProduct = false;

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) {
                    if (matrix[i][j] == productID) {
                        hasProduct = true;
                        break;
                    }
                }
            }
            /**
             * товара нет - выходим
             * */
            return !hasProduct;
        }
        return false;
    }

    /**
     * Распространение волны
     * Возвращает длину пути
     * метод вернет 127, если цель вне досягяаемости
     * */
    private byte startWave(int row, int column, byte objectID) {
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
                                else if (matrix[i + iOffset][j + jOffset] == objectID) {
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
        if (step < Byte.MAX_VALUE) {
            /**
             * координаты найденного товара или объекта
             * */
            targetRow = destY;
            targetColumn = destX;
        }
        return step;
    }

    /**
     * {@param step} должен быть в пределах от MIN_STEP до MAX_STEP
     * */
    private void restorePath(byte step, byte objectID) {
        path = new Stack<>();
        if (objectID == Map.GATE_ID)
            path.push(new Pair<>(Gate.getRow(), Gate.getColumn()));
        do {
            for (int iOffset = -1; iOffset < 2; iOffset++) {
                if (targetRow + iOffset < 0 || targetRow + iOffset > matrix.length - 1)
                    continue;
                for (int jOffset = -1; jOffset < 2; jOffset++) {
                    if (targetColumn + jOffset < 0 || targetColumn + jOffset > matrix.length - 1)
                        continue;
                    if (matrix[targetRow + iOffset][targetColumn + jOffset] == step) {
                        path.push(new Pair<>(targetRow + iOffset, targetColumn + jOffset));
                        targetRow += iOffset;
                        targetColumn += jOffset;
                        step--;
                        break;
                    }
                }
            }
        }
        while (step > 0);
    }

    /**
     * Возвращает строку с текущей информацией о покупателе
     * */
    public String getInfo() {
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
        return info.toString();
    }
}