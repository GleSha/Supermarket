package form;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import model.Cashbox;
import model.Map;

public class Controller {


    /**
     *
     * Ссылки на контролы из form.fxml
     *
     * */

    @FXML
    private Pane pane, mapPane;

    @FXML
    private Button mapChooseButton, startButton, pauseButton, loadMapButton, resetButton;

    @FXML
    private Label productProceedsLabel, uselessProductProceedsLabel, commonProceedsLabel, currentMapNameLabel;

    @FXML
    private TextArea textField;

    private Timeline timeline;

    private String currentMapName = Map.defaultMapName;


    public void loadMapButtonClick(ActionEvent actionEvent) {
        Map.initialization(currentMapName, mapPane, textField);
        if (Map.getInstance().isReady()) {
            resetButton.setDisable(false);
            startButton.setDisable(false);
            mapChooseButton.setDisable(true);
            loadMapButton.setDisable(true);
        }
    }

    private boolean running;


    public void startButtonClick(ActionEvent actionEvent) {
        if (Map.getInstance().isReady()) {
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            if (timeline == null) {
                timeline = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        running = true;
                        Map.getInstance().timeStep();
                        int productProceeds = Cashbox.getInstance().getProductProceeds();
                        int uselessProductProceeds = Cashbox.getInstance().getUselessProductProceeds();
                        productProceedsLabel.setText(String.valueOf(productProceeds));
                        uselessProductProceedsLabel.setText(String.valueOf(uselessProductProceeds));
                        commonProceedsLabel.setText(String.valueOf(productProceeds + uselessProductProceeds));
                        running = false;
                    }
                }));
                timeline.setCycleCount(Animation.INDEFINITE);
                timeline.play();
            }
        }



    }



    public void pauseButtonClick(ActionEvent actionEvent) {
        if (timeline != null) {
            if (timeline.getStatus() == Animation.Status.RUNNING) {
                timeline.pause();
                if (!running) {
                    Map.getInstance().pause();
                }
                pauseButton.setText("Далее");
            }
            else {
                Map.getInstance().play();
                timeline.play();
                pauseButton.setText("Пауза");
            }

        }
    }



    public void resetButtonClick(ActionEvent actionEvent) {
    }
}
