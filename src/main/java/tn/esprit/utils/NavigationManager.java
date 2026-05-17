package tn.esprit.utils;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Gestionnaire de navigation entre les vues FXML.
 * Applique une transition fade-in/fade-out fluide.
 */
public class NavigationManager {

    private static final String CSS_PATH = "/styles.css";
    private static final String FXML_BASE = "/";

    private NavigationManager() {}

    /**
     * Navigue vers une nouvelle fenêtre (nouveau Stage).
     * @param fxmlFile  nom du fichier FXML (ex: "Login.fxml")
     * @param title     titre de la fenêtre
     * @param oldStage  stage actuel à fermer (peut être null)
     * @param resizable fenêtre redimensionnable
     */
    public static void navigateTo(String fxmlFile, String title, Stage oldStage, boolean resizable) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource(FXML_BASE + fxmlFile)
            );
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    NavigationManager.class.getResource(CSS_PATH).toExternalForm()
            );

            Stage newStage = new Stage();
            newStage.setTitle(title);
            newStage.setScene(scene);

            // Rendre la fenêtre Login redimensionnable avec nouvelle taille
            if (fxmlFile.equals("Login.fxml")) {
                newStage.setResizable(true);
                // Définir une taille minimale pour la fenêtre Login
                newStage.setMinWidth(1100);
                newStage.setMinHeight(700);
            } else {
                newStage.setResizable(resizable);
            }

            newStage.centerOnScreen();

            // Fade-in animation
            root.setOpacity(0);
            newStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // Fermer l'ancien stage
            if (oldStage != null) {
                oldStage.close();
            }

            // Icône
            try {
                // Essayer d'abord avec le chemin /logo.png dans resources
                var logoStream = NavigationManager.class.getResourceAsStream("/logo.png");
                if (logoStream != null) {
                    newStage.getIcons().add(new javafx.scene.image.Image(logoStream));
                } else {
                    // Si le logo n'existe pas, ne pas afficher d'erreur
                    System.out.println("Logo non trouvé : /logo.png");
                }
            } catch (Exception e) {
                System.out.println("Erreur lors du chargement du logo : " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    /**
     * Charge un FXML dans un conteneur (pane) existant avec fade.
     * Utilisé pour changer le contenu du dashboard sans rechanger de fenêtre.
     */
    public static <T> T loadInto(javafx.scene.layout.Pane container, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationManager.class.getResource(FXML_BASE + fxmlFile)
            );
            Node node = loader.load();
            node.setOpacity(0);

            container.getChildren().setAll(node);

            // Remplir le container
            if (node instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(container.widthProperty());
                region.prefHeightProperty().bind(container.heightProperty());
            }

            // Fade in
            FadeTransition ft = new FadeTransition(Duration.millis(300), node);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de charger : " + fxmlFile);
            return null;
        }
    }

    /**
     * Récupère le Stage depuis n'importe quel Node.
     */
    public static Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    private static void showError(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR, msg
        );
        alert.setHeaderText("Erreur de navigation");
        alert.showAndWait();
    }
}
