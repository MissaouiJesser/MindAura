package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.utils.MyDataBase;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        MyDataBase.getInstance();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("MindAura – Psychologie & Développement Personnel");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception ignored) {}
        stage.show();
    }

    @Override
    public void stop() {
        try {
            if (MyDataBase.getInstance().getConx() != null
                    && !MyDataBase.getInstance().getConx().isClosed()) {
                MyDataBase.getInstance().getConx().close();
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}
