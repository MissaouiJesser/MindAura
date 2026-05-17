package tn.esprit.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import static javafx.application.Application.launch;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/AccueilRessource.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Accueil");
        stage.setMaximized(true);

        stage.setScene(scene);
        stage.show();
    }}



/*

   @Override
    public void start(Stage primaryStage) {
        try {
            // Charger FXML
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherRe.fxml"));

            // Charger CSS global
            root.getStylesheets().add(getClass().getResource("/css/modifier.css").toExternalForm());

            Scene scene = new Scene(root);

            primaryStage.setTitle("MindAura - Gestion des Ressources");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur lors du chargement de l'interface : " + e.getMessage());
        }
    }
}*//*

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VideoPlayer.fxml"));
            Parent root = loader.load();

            // Récupérer le controller
            Controllers.VideoPlayerController controller = loader.getController();

            // Créer une ressource exemple
            tn.esprit.entities.Ressources res = new tn.esprit.entities.Ressources();
            res.setTitre("\n" +
                    "Comment devenir une nouvelle version de toi-même");
            res.setUrl("src/main/resources/video/Comment devenir une nouvelle version de toi-même.mp4"); // Chemin relatif vers ton fichier
            res.setResume("Résumé de la vidéo");
            res.setImageUrl("src/main/resources/ChatGPT Image 14 févr. 2026, 11_43_26.png");


            tn.esprit.entities.Ressources res2 = new tn.esprit.entities.Ressources();
            res2.setTitre("Comment l'intelligence émotionnelle peut transformer votre vie Avec Christophe Haag");
            res2.setUrl("src/main/resources/video/Comment l'intelligence émotionnelle peut transformer votre vie Avec Christophe Haag.mp4");
            res2.setResume("Comment l'intelligence émotionnelle peut transformer votre vie Avec Christophe Haag");
            res2.setImageUrl("src/main/resources/intelligence émotionnelle .png");
            // Injecter la ressource et le stage
            controller.setRessource(res, primaryStage);
            controller.setRessource(res2, primaryStage);
            Scene scene = new Scene(root);
            primaryStage.setTitle("MindAura - Ressources pour votre bien-être mental");
            primaryStage.setMaximized(true);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}
*//*
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ajouter.fxml"));

            Scene scene = new Scene(root);
            primaryStage.setTitle("Ajouter Ressources - Développement Personnel");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/