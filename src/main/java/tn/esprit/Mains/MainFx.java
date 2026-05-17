package tn.esprit.Mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principale optimisée - Responsive Design
 */
public class MainFx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Accueil.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768); // Dimension initiale minimale

            scene.getStylesheets().add("/app.css");

            primaryStage.setScene(scene);
            primaryStage.setTitle("Réclamations & Réponses");

            // Permet le redimensionnement complet
            primaryStage.setWidth(1024);
            primaryStage.setHeight(768);
            primaryStage.setResizable(true); // Important!

            // Dimension minimale pour éviter un affichage cassé
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);

            primaryStage.show();

        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}