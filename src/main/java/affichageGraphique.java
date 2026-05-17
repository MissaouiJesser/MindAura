/*import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.local_psychiatrie;

public class AffichageLocal extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Créer un local pour test
        local_psychiatrie local = new local_psychiatrie(
                1,
                "Centre Serenity",
                "123 Rue de la Paix",
                "Tunis",
                "Centre moderne pour soins psychiatriques",
                "50",
                "Hôpital",
                71234567,
                "contact@serenity.tn",
                "Disponible",
                "/images/sereneityskin.jpg"
        );

        // Charger et afficher l'image
        Image image = new Image(getClass().getResourceAsStream(local.getImageURL()));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(400);
        imageView.setPreserveRatio(true);

        // Créer les labels pour les informations
        Label nomLabel = new Label("Nom: " + local.getNom_local());
        Label adresseLabel = new Label("Adresse: " + local.getAdresse_local());
        Label villeLabel = new Label("Ville: " + local.getVille_local());

        // Layout
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(imageView, nomLabel, adresseLabel, villeLabel);

        Scene scene = new Scene(vbox, 500, 600);
        primaryStage.setTitle("Local Psychiatrie");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}*/