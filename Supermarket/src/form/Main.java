package form;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Manager;
import model.Map;

import java.util.concurrent.ThreadLocalRandom;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        /**
         * Создание формы приложения
         * */
        Parent root = FXMLLoader.load(getClass().getResource("form.fxml"));
        primaryStage.setTitle("Супермаркет");
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();

    }

    public static void main(String[] args) {
        /**
         * Запуск программы
         * */
        launch(args);
    }
}
