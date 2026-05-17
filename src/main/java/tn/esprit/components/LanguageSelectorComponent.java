package tn.esprit.components;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import tn.esprit.services.TranslationService;
import tn.esprit.utils.LanguageManager;

import java.util.Arrays;
import java.util.List;

/**
 * Composant ComboBox réutilisable pour sélectionner la langue.
 * S'intègre dans n'importe quel FXML via fx:id ou en code Java.
 *
 * Utilisation dans un controller :
 *
 *   @FXML private ComboBox<String> comboLangue;
 *   // Dans initialize() :
 *   LanguageSelectorComponent.setup(comboLangue);
 */
public class LanguageSelectorComponent {

    // Langues disponibles (codes)
    private static final List<String> LANGUES = Arrays.asList(
            TranslationService.LANG_FR,
            TranslationService.LANG_EN,
            TranslationService.LANG_AR,
            TranslationService.LANG_ES
    );

    /**
     * Configure un ComboBox existant comme sélecteur de langue.
     * Applique le style MindAura et branche le LanguageManager.
     *
     * @param combo Le ComboBox à configurer (déclaré en @FXML dans le controller)
     */
    public static void setup(ComboBox<String> combo) {
        combo.setItems(FXCollections.observableArrayList(LANGUES));

        // Afficher les noms lisibles (ex: "🇫🇷 Français")
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);
                setText(empty || code == null ? null : TranslationService.getNomLangue(code));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String code, boolean empty) {
                super.updateItem(code, empty);
                setText(empty || code == null ? null : TranslationService.getNomLangue(code));
            }
        });

        // Valeur initiale = langue courante
        combo.setValue(LanguageManager.getInstance().getCurrentLanguage());

        // Style MindAura
        applyStyle(combo);

        // Action : changer la langue globalement
        combo.setOnAction(e -> {
            String selected = combo.getValue();
            if (selected != null) {
                LanguageManager.getInstance().setLanguage(selected);
            }
        });

        // Synchroniser si la langue change depuis ailleurs
        LanguageManager.getInstance().currentLanguageProperty().addListener(
                (obs, old, newLang) -> {
                    if (!newLang.equals(combo.getValue())) {
                        combo.setValue(newLang);
                    }
                }
        );
    }

    /**
     * Applique le style visuel MindAura au sélecteur.
     */
    private static void applyStyle(ComboBox<String> combo) {
        combo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); " +
                "-fx-border-color: rgba(255,255,255,0.4); " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 700; " +
                "-fx-cursor: hand; " +
                "-fx-pref-width: 145px;"
        );

        // Hover effect
        combo.setOnMouseEntered(e -> combo.setStyle(
                "-fx-background-color: rgba(255,255,255,0.25); " +
                "-fx-border-color: rgba(255,255,255,0.7); " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 700; " +
                "-fx-cursor: hand; " +
                "-fx-pref-width: 145px;"
        ));
        combo.setOnMouseExited(e -> applyStyle(combo));
    }
}
