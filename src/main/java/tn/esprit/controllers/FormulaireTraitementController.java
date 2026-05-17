package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Etat;
import tn.esprit.enums.Objectif;
import tn.esprit.enums.TypeTraitement;
import tn.esprit.services.traitement_service;
import tn.esprit.services.utilisateurs_service;

import java.net.URL;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur du formulaire Ajouter / Modifier traitement.
 * Les utilisateurs sont sélectionnés par nom complet (pas par ID).
 */
public class FormulaireTraitementController implements Initializable {

    @FXML private Label            formTitle;
    @FXML private ComboBox<String> fieldType;
    @FXML private ComboBox<String> fieldObjectif;
    @FXML private ComboBox<String> fieldEtat;
    @FXML private DatePicker       fieldDateDebut;
    @FXML private DatePicker       fieldDateFin;
    @FXML private ComboBox<String> fieldUtilisateur;
    @FXML private ComboBox<String> fieldCoach;
    @FXML private TextArea         fieldDescription;

    // Labels d'erreur
    @FXML private Label errType;
    @FXML private Label errObjectif;
    @FXML private Label errEtat;
    @FXML private Label errDateDebut;
    @FXML private Label errGlobal;

    private traitement traitementToEdit = null;
    private final traitement_service   traitService = new traitement_service();
    private final utilisateurs_service userService  = new utilisateurs_service();

    /**
     * Maps : "Nom Prénom (email)" → id_utilisateur (utilisé en interne seulement)
     * L'ID ne s'affiche jamais dans le ComboBox.
     */
    private final Map<String, String> displayToIdMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Remplir les ComboBox avec libellés lisibles
        fieldType.setItems(FXCollections.observableArrayList(
            TypeTraitement.PSYCHOLOGIQUE.getLibelle(),
            TypeTraitement.COMPORTEMENTAL.getLibelle(),
            TypeTraitement.MIXTE.getLibelle()
        ));

        fieldObjectif.setItems(FXCollections.observableArrayList(
            Objectif.GESTION_DU_STRESS.getLibelle(),
            Objectif.ANXIETE.getLibelle()
        ));

        fieldEtat.setItems(FXCollections.observableArrayList(
            Etat.EN_COURS.getLibelle(),
            Etat.TERMINE.getLibelle(),
            Etat.SUSPENDU.getLibelle()
        ));

        // Charger la liste des utilisateurs par nom (pas ID)
        loadUtilisateursList();
    }

    /**
     * Charge les utilisateurs dans le ComboBox sous format "Prénom Nom"
     * (l'ID reste en mémoire dans la map, jamais affiché).
     */
    private void loadUtilisateursList() {
        try {
            List<utilisateurs> users = userService.afficherList();
            displayToIdMap.clear();

            for (utilisateurs u : users) {
                // Format affiché : "Prénom Nom" – aucun ID visible
                String display = u.getPrenom_utilisateur() + " " + u.getNom_utilisateur();
                displayToIdMap.put(display, u.getId_utilisateur());
            }

            fieldUtilisateur.setItems(
                FXCollections.observableArrayList(displayToIdMap.keySet()));
            fieldCoach.setItems(
                FXCollections.observableArrayList(displayToIdMap.keySet()));

        } catch (SQLException e) {
            errGlobal.setText("Impossible de charger les utilisateurs.");
        }
    }

    /**
     * Pré-remplit le formulaire pour la modification.
     */
    public void setTraitementToEdit(traitement t) {
        this.traitementToEdit = t;
        formTitle.setText("Modifier le traitement");

        // Pré-remplissage (sans afficher l'ID)
        if (t.getType_traitement() != null)
            fieldType.setValue(t.getType_traitement().getLibelle());

        if (t.getObjectif_traitement() != null)
            fieldObjectif.setValue(t.getObjectif_traitement().getLibelle());

        if (t.getEtat_traitement() != null)
            fieldEtat.setValue(t.getEtat_traitement().getLibelle());

        if (t.getDate_debut_traitement() != null)
            fieldDateDebut.setValue(
                t.getDate_debut_traitement().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());

        if (t.getDate_fin_traitement() != null)
            fieldDateFin.setValue(
                t.getDate_fin_traitement().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());

        fieldDescription.setText(t.getDescription_traitement() != null
                ? t.getDescription_traitement() : "");

        // Retrouver le nom affiché à partir de l'ID interne (pour pré-sélectionner)
        if (t.getId_utilisateur() != null) {
            displayToIdMap.entrySet().stream()
                .filter(e -> e.getValue().equals(t.getId_utilisateur()))
                .findFirst()
                .ifPresent(e -> fieldUtilisateur.setValue(e.getKey()));
        }
        if (t.getId_coach() != null) {
            displayToIdMap.entrySet().stream()
                .filter(e -> e.getValue().equals(t.getId_coach()))
                .findFirst()
                .ifPresent(e -> fieldCoach.setValue(e.getKey()));
        }
    }

    /**
     * Valide et sauvegarde le traitement.
     * @return true si succès
     */
    public boolean save() {
        clearErrors();
        boolean valid = true;

        // Validations
        if (fieldType.getValue() == null) {
            errType.setText("Type obligatoire."); valid = false;
        }
        if (fieldObjectif.getValue() == null) {
            errObjectif.setText("Objectif obligatoire."); valid = false;
        }
        if (fieldEtat.getValue() == null) {
            errEtat.setText("État obligatoire."); valid = false;
        }
        if (fieldDateDebut.getValue() == null) {
            errDateDebut.setText("Date de début obligatoire."); valid = false;
        }
        if (!valid) return false;

        // Vérification cohérence dates
        if (fieldDateFin.getValue() != null
                && fieldDateFin.getValue().isBefore(fieldDateDebut.getValue())) {
            errDateDebut.setText("La date de fin doit être après la date de début.");
            return false;
        }

        // Construction de l'objet traitement
        traitement t = (traitementToEdit != null) ? traitementToEdit : new traitement();

        // Convertir libellé → enum
        for (TypeTraitement tt : TypeTraitement.values())
            if (tt.getLibelle().equals(fieldType.getValue())) { t.setType_traitement(tt); break; }

        for (Objectif o : Objectif.values())
            if (o.getLibelle().equals(fieldObjectif.getValue())) { t.setObjectif_traitement(o); break; }

        for (Etat e : Etat.values())
            if (e.getLibelle().equals(fieldEtat.getValue())) { t.setEtat_traitement(e); break; }

        t.setDate_debut_traitement(
            Date.from(fieldDateDebut.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
        );
        t.setDate_fin_traitement(
            fieldDateFin.getValue() != null
                ? Date.from(fieldDateFin.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
                : null
        );

        t.setDescription_traitement(
            fieldDescription.getText().trim().isEmpty() ? null : fieldDescription.getText().trim()
        );

        // Résoudre les IDs internes à partir du nom sélectionné (jamais affiché)
        String selectedUser  = fieldUtilisateur.getValue();
        String selectedCoach = fieldCoach.getValue();
        t.setId_utilisateur(selectedUser  != null ? displayToIdMap.get(selectedUser)  : null);
        t.setId_coach       (selectedCoach != null ? displayToIdMap.get(selectedCoach) : null);

        // Persistance
        try {
            if (traitementToEdit == null) {
                traitService.addMeth2(t);
            } else {
                traitService.modifier(t);
            }
            return true;
        } catch (SQLException e) {
            errGlobal.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void clearErrors() {
        errType.setText("");
        errObjectif.setText("");
        errEtat.setText("");
        errDateDebut.setText("");
        errGlobal.setText("");
    }
}
