package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Etat;
import tn.esprit.enums.Objectif;
import tn.esprit.enums.Role;
import tn.esprit.enums.TypeTraitement;
import tn.esprit.services.traitement_service;
import tn.esprit.services.utilisateurs_service;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur dédié à l'AJOUT d'un traitement.
 *
 * CORRECTION : NullPointerException sur errDateFin (et autres labels @FXML)
 * quand le label n'existe pas dans le FXML correspondant.
 * → Tous les accès aux labels sont protégés par un helper setLabel() null-safe.
 */
public class AjouterTraitementController implements Initializable, DashboardController.DashboardAware {

    @FXML private ComboBox<String> fieldType;
    @FXML private ComboBox<String> fieldObjectif;
    @FXML private ComboBox<String> fieldEtat;
    @FXML private DatePicker       fieldDateDebut;
    @FXML private DatePicker       fieldDateFin;
    @FXML private ComboBox<String> fieldUtilisateur;
    @FXML private ComboBox<String> fieldCoach;
    @FXML private TextArea         fieldDescription;

    // Ces labels peuvent être null si absents du FXML → on utilise setLabel() partout
    @FXML private Label errType;
    @FXML private Label errObjectif;
    @FXML private Label errEtat;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;   // ← peut être null si absent du FXML
    @FXML private Label errGlobal;

    /** Map "Prénom Nom" → id_utilisateur (id jamais affiché) */
    private final Map<String, String> nameToId = new HashMap<>();

    private DashboardController dashboardController;
    private final traitement_service   traitService = new traitement_service();
    private final utilisateurs_service userService  = new utilisateurs_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
        fieldEtat.setValue(Etat.EN_COURS.getLibelle());

        // Date de début : bloquer les dates passées
        fieldDateDebut.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // Date de fin : doit être après la date de début
        fieldDateFin.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                LocalDate dateDebut = fieldDateDebut.getValue();
                if (dateDebut != null) {
                    setDisable(empty || date.isBefore(dateDebut) || date.isEqual(dateDebut));
                } else {
                    setDisable(empty || date.isBefore(LocalDate.now()));
                }
            }
        });

        // Réinitialiser la date de fin si elle devient invalide
        fieldDateDebut.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null && fieldDateFin.getValue() != null) {
                if (fieldDateFin.getValue().isBefore(nv) || fieldDateFin.getValue().isEqual(nv)) {
                    fieldDateFin.setValue(null);
                }
            }
        });

        loadUsers();
    }

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    // =========================================================================
    //  CHARGEMENT UTILISATEURS
    // =========================================================================

    private void loadUsers() {
        try {
            List<utilisateurs> users = userService.afficherList();
            nameToId.clear();

            Map<String, String> patientMap = new HashMap<>();
            Map<String, String> coachMap   = new HashMap<>();

            for (utilisateurs u : users) {
                String display = u.getPrenom_utilisateur() + " " + u.getNom_utilisateur();
                Role role = u.getRole_utilisateur();

                if (role == Role.ROLE_PATIENT || role == Role.ROLE_USER)
                    patientMap.put(display, u.getId_utilisateur());

                if (role == Role.ROLE_COACH)
                    coachMap.put(display, u.getId_utilisateur());

                nameToId.put(display, u.getId_utilisateur());
            }

            fieldUtilisateur.setItems(FXCollections.observableArrayList(patientMap.keySet()));
            fieldCoach.setItems(FXCollections.observableArrayList(coachMap.keySet()));

        } catch (SQLException e) {
            setLabel(errGlobal, "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    // =========================================================================
    //  VALIDATION
    // =========================================================================

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;

        if (fieldType.getValue() == null) {
            setLabel(errType, "Type obligatoire.");
            valid = false;
        }
        if (fieldObjectif.getValue() == null) {
            setLabel(errObjectif, "Objectif obligatoire.");
            valid = false;
        }
        if (fieldEtat.getValue() == null) {
            setLabel(errEtat, "État obligatoire.");
            valid = false;
        }

        // Date de début : obligatoire et >= aujourd'hui
        if (fieldDateDebut.getValue() == null) {
            setLabel(errDateDebut, "Date de début obligatoire.");
            valid = false;
        } else if (fieldDateDebut.getValue().isBefore(LocalDate.now())) {
            setLabel(errDateDebut, "La date de début doit être >= aujourd'hui.");
            valid = false;
        }

        // Date de fin : si renseignée, doit être > date de début
        if (fieldDateFin.getValue() != null && fieldDateDebut.getValue() != null) {
            if (fieldDateFin.getValue().isBefore(fieldDateDebut.getValue())
                    || fieldDateFin.getValue().isEqual(fieldDateDebut.getValue())) {
                // errDateFin peut être null → on redirige vers errGlobal dans ce cas
                if (errDateFin != null) {
                    errDateFin.setText("La date de fin doit être après la date de début.");
                } else {
                    setLabel(errGlobal, "La date de fin doit être après la date de début.");
                }
                valid = false;
            }
        }

        return valid;
    }

    // =========================================================================
    //  SAVE
    // =========================================================================

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        traitement t = new traitement();

        for (TypeTraitement tt : TypeTraitement.values())
            if (tt.getLibelle().equals(fieldType.getValue())) { t.setType_traitement(tt); break; }
        for (Objectif o : Objectif.values())
            if (o.getLibelle().equals(fieldObjectif.getValue())) { t.setObjectif_traitement(o); break; }
        for (Etat e : Etat.values())
            if (e.getLibelle().equals(fieldEtat.getValue())) { t.setEtat_traitement(e); break; }

        t.setDate_debut_traitement(
                Date.from(fieldDateDebut.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        t.setDate_fin_traitement(fieldDateFin.getValue() != null
                ? Date.from(fieldDateFin.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null);
        t.setDescription_traitement(
                fieldDescription.getText().trim().isEmpty() ? null : fieldDescription.getText().trim());

        String selUser  = fieldUtilisateur.getValue();
        String selCoach = fieldCoach.getValue();
        t.setId_utilisateur(selUser  != null ? nameToId.get(selUser)  : null);
        t.setId_coach       (selCoach != null ? nameToId.get(selCoach) : null);

        try {
            traitService.addMeth2(t);
            showSuccess("Traitement créé avec succès !");
            if (dashboardController != null)
                dashboardController.navigateTo("ListeTraitements.fxml",
                        "Gestion des Traitements", "Programmes thérapeutiques");
        } catch (SQLException e) {
            setLabel(errGlobal, "Erreur BDD : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        if (dashboardController != null)
            dashboardController.navigateTo("ListeTraitements.fxml",
                    "Gestion des Traitements", "Programmes thérapeutiques");
    }

    // =========================================================================
    //  HELPERS — null-safe
    // =========================================================================

    /**
     * Définit le texte d'un Label de façon null-safe.
     * Si le Label est null (absent du FXML), l'appel est ignoré silencieusement.
     */
    private void setLabel(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private void clearErrors() {
        setLabel(errType,      "");
        setLabel(errObjectif,  "");
        setLabel(errEtat,      "");
        setLabel(errDateDebut, "");
        setLabel(errDateFin,   "");   // null-safe → pas de NullPointerException
        setLabel(errGlobal,    "");
        if (errGlobal != null)
            errGlobal.setStyle("-fx-text-fill:#E74C3C; -fx-font-weight:bold;");
    }

    private void showSuccess(String msg) {
        if (errGlobal != null) {
            errGlobal.setStyle("-fx-text-fill:#2E8B57; -fx-font-weight:bold;");
            errGlobal.setText(msg);
        }
    }
}