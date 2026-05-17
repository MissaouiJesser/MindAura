package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.services.TranslationService;
import tn.esprit.utils.LanguageManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur de base que tous les contrôleurs étendent.
 * Fournit la traduction automatique via LanguageManager.
 */
public abstract class TranslatableController implements Initializable {

    protected final TranslationService ts = TranslationService.getInstance();
    protected final LanguageManager lm   = LanguageManager.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // ✅ IMPORTANT: FXML injection is complete here
        // Now we can safely initialize components
        initComponents(url, resourceBundle);

        // S'enregistrer auprès du LanguageManager
        lm.addListener(this::traduireUI);

        // Traduire immédiatement si la langue n'est pas le français
        String lang = lm.getCurrentLanguage();
        if (!TranslationService.LANG_FR.equals(lang)) {
            traduireUI(lang);
        }
    }

    /**
     * À implémenter dans chaque contrôleur enfant.
     * Contient toute la logique d'initialisation.
     */
    protected abstract void initComponents(URL url, ResourceBundle resourceBundle);

    /**
     * À implémenter dans chaque contrôleur enfant.
     * Appelé automatiquement à chaque changement de langue.
     */
    protected abstract void traduireUI(String langCode);

    // ─── Helpers de traduction ────────────────────────────────────────────────────

    /** Traduit un Label */
    protected void tr(Label label, String texteFr) {
        if (label == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            label.setText(texteFr);
        } else {
            ts.traduireAsync(texteFr, label::setText);
        }
    }

    /** Traduit un Button */
    protected void tr(Button btn, String texteFr) {
        if (btn == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            btn.setText(texteFr);
        } else {
            ts.traduireAsync(texteFr, btn::setText);
        }
    }

    /** Traduit un en-tête de TableColumn */
    protected <S, T> void tr(TableColumn<S, T> col, String texteFr) {
        if (col == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            col.setText(texteFr);
        } else {
            ts.traduireAsync(texteFr, col::setText);
        }
    }

    /** Traduit le placeholder d'un TextField */
    protected void trPrompt(TextField field, String texteFr) {
        if (field == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            field.setPromptText(texteFr);
        } else {
            ts.traduireAsync(texteFr, field::setPromptText);
        }
    }

    /** Traduit le texte d'un Tab */
    protected void tr(Tab tab, String texteFr) {
        if (tab == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            tab.setText(texteFr);
        } else {
            ts.traduireAsync(texteFr, tab::setText);
        }
    }

    /** Traduit le texte d'un MenuItem */
    protected void tr(MenuItem item, String texteFr) {
        if (item == null) return;
        if (TranslationService.LANG_FR.equals(lm.getCurrentLanguage())) {
            item.setText(texteFr);
        } else {
            ts.traduireAsync(texteFr, item::setText);
        }
    }
}