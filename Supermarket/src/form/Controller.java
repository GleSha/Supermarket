package form;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import model.Cashbox;
import model.Map;

import java.io.File;

public class Controller {
    /**
     * Ссылки на контролы из form.fxml
     * */
    @FXML
    private Pane pane, mapPane;

    /**
     * Сслыки на все кнопки формы
     * */
    @FXML
    private Button mapChooseButton, startButton, pauseButton, loadMapButton, resetButton;

    /**
     * Ссылки на все лейблы формы
     * */
    @FXML
    private Label productProceedsLabel, uselessProductProceedsLabel, commonProceedsLabel, currentMapNameLabel;

    /**
     * Ссылка на текстовое поле с информацией
     * */
    @FXML
    private TextArea textField;

    /**
     * Объект класса FileChooser для выбора файла в диалоговым окне
     * */
    private static FileChooser fileChooser = new FileChooser();

    /**
     * Класс TimeLine - таймер программы
     * */
    private Timeline timeline;

    /**
     * имя файла текущей карты
     * */
    private String currentMapName = Map.DEFAULT_MAP_NAME;

    /**
     * сообщение на поле textField
     * */
    private static String message;

    /**
     * Ссылка на класс, отвечающий за отрисовку
     * */
    private Drawer drawer;

    /**
     * Обработчик события нажатия на кнопку "Загрузить карту"
     * */
    public void loadMapButtonClick(ActionEvent actionEvent) {
        message = textField.getText();
        Map.initialization(currentMapName);
        if (Map.getInstance().isReady()) {
            resetButton.setDisable(false);
            startButton.setDisable(false);
            mapChooseButton.setDisable(true);
            loadMapButton.setDisable(true);
            drawer = new Drawer(Map.getInstance(), mapPane, textField);
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Сообщение");
            alert.setHeaderText("Ошибка загрузки карты");
            alert.setContentText(Map.getErrorMessage());
            alert.show();
        }
    }

    /**
     * Обработчик события нажатия на кнопку "Загрузить карту"
     * */
    public void startButtonClick(ActionEvent actionEvent) {
        if (Map.getInstance().isReady()) {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            if (timeline == null) {
                timeline = new Timeline(new KeyFrame(Duration.millis(750), new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        Map.getInstance().timeStep();
                        int productProceeds = Cashbox.getInstance().getProductProceeds();
                        int uselessProductProceeds = Cashbox.getInstance().getUselessProductProceeds();
                        productProceedsLabel.setText(String.valueOf(productProceeds));
                        uselessProductProceedsLabel.setText(String.valueOf(uselessProductProceeds));
                        commonProceedsLabel.setText(String.valueOf(productProceeds + uselessProductProceeds));
                        drawer.draw();
                    }
                }));
                timeline.setCycleCount(Animation.INDEFINITE);
                timeline.play();
            }
        }
    }

    /**
     * Обработчик события нажатия на кнопку "Сброс"
     * */
    public void resetButtonClick(ActionEvent actionEvent) {
        startButton.setDisable(true);
        pauseButton.setDisable(true);
        resetButton.setDisable(true);
        mapChooseButton.setDisable(false);
        loadMapButton.setDisable(false);
        textField.setText(message);
        if (timeline != null) {
            if (timeline.getStatus() == Animation.Status.RUNNING)
                timeline.stop();
            timeline = null;
        }
        drawer = null;
        mapPane.getChildren().removeAll(mapPane.getChildren());
    }

    /**
     * Обработчик события нажатия на кнопку "Пауза/Далее"
     * */
    public void pauseButtonClick(ActionEvent actionEvent) {
        if (timeline != null) {
            if (timeline.getStatus() == Animation.Status.RUNNING) {
                timeline.pause();
                pauseButton.setText("Далее");
                drawer.setPaused(true);
            }
            else {
                textField.setText(message);
                drawer.setPaused(false);
                pauseButton.setText("Пауза");
                timeline.play();
            }
        }
    }

    /**
     * Обработчик события нажатия на кнопку "Выбрать карту"
     * */
    public void mapChooseButtonClick(ActionEvent actionEvent) {
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Файлы карт", "*.map"));
        File mapFile = fileChooser.showOpenDialog(pane.getScene().getWindow());
        if (mapFile != null) {
            currentMapName = mapFile.getAbsolutePath();
            currentMapNameLabel.setText(currentMapName);
        }
    }
}
