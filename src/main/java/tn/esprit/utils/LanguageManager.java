package tn.esprit.utils;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tn.esprit.services.TranslationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de langue global (Singleton).
 * Notifie tous les contrôleurs enregistrés lors d'un changement de langue.
 *
 * Usage :
 *   LanguageManager.getInstance().setLanguage("en");
 *   LanguageManager.getInstance().addListener(this::onLanguageChanged);
 */
public class LanguageManager {

    // ── Singleton ────────────────────────────────────────────────────────────────
    private static LanguageManager instance;

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    // ── État ──────────────────────────────────────────────────────────────────────
    private final StringProperty currentLanguage =
            new SimpleStringProperty(TranslationService.LANG_FR);

    private final List<LanguageChangeListener> listeners = new ArrayList<>();

    private LanguageManager() {}

    // ── API publique ─────────────────────────────────────────────────────────────

    /**
     * Change la langue courante et notifie tous les listeners enregistrés.
     * Vide aussi le cache de traduction pour la nouvelle langue.
     *
     * @param langCode Code langue : "fr", "en", "ar", "es"
     */
    public void setLanguage(String langCode) {
        if (!langCode.equals(currentLanguage.get())) {
            // Vider le cache uniquement si on change vraiment de langue
            TranslationService.getInstance().viderCache();

            currentLanguage.set(langCode);
            TranslationService.getInstance().setCurrentLanguage(langCode);

            System.out.println("🌐 Langue changée : " + TranslationService.getNomLangue(langCode));

            // Notifier tous les listeners
            for (LanguageChangeListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onLanguageChanged(langCode);
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur listener langue: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Retourne la langue courante.
     */
    public String getCurrentLanguage() {
        return currentLanguage.get();
    }

    /**
     * Propriété JavaFX observable de la langue courante.
     * Permet de binder directement sur la langue.
     */
    public StringProperty currentLanguageProperty() {
        return currentLanguage;
    }

    /**
     * Enregistre un listener qui sera appelé à chaque changement de langue.
     */
    public void addListener(LanguageChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Supprime un listener.
     */
    public void removeListener(LanguageChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Vérifie si la langue courante est l'arabe (RTL).
     */
    public boolean isRTL() {
        return TranslationService.LANG_AR.equals(currentLanguage.get());
    }

    // ── Interface listener ────────────────────────────────────────────────────────
    @FunctionalInterface
    public interface LanguageChangeListener {
        void onLanguageChanged(String langCode);
    }
}
