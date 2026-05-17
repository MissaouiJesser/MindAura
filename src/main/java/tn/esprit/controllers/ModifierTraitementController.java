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
 * Contrôleur dédié à la MODIFICATION d'un traitement.
 *
 * CORRECTION : NullPointerException sur errDateFin (et autres labels @FXML)
 * quand le label n'existe pas dans ModifierTraitement.fxml.
 * → Tous les accès aux labels passent par setLabel() null-safe.
 */
public class ModifierTraitementController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label            headerInfo;
    @FXML private Label            badgeEtat;

    @FXML private ComboBox<String> fieldType;
    @FXML private ComboBox<String> fieldObjectif;
    @FXML private ComboBox<String> fieldEtat;
    @FXML private DatePicker       fieldDateDebut;
    @FXML private DatePicker       fieldDateFin;
    @FXML private ComboBox<String> fieldUtilisateur;
    @FXML private ComboBox<String> fieldCoach;
    @FXML private TextArea         fieldDescription;

    // Ces labels peuvent être null si absents du FXML → setLabel() null-safe partout
    @FXML private Label errType;
    @FXML private Label errObjectif;
    @FXML private Label errEtat;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;   // ← null si absent du FXML
    @FXML private Label errGlobal;

    /** Map "Prénom Nom" → id_utilisateur (jamais affiché) */
    private final Map<String, String> nameToId = new HashMap<>();

    private traitement traitementToEdit;
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

        fieldEtat.valueProperty().addListener((o, ov, nv) -> updateBadge(nv));

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
    //  PRÉ-REMPLISSAGE
    // =========================================================================

    public void setTraitementToEdit(traitement t) {
        this.traitementToEdit = t;

        String typeStr = t.getType_traitement() != null ? t.getType_traitement().getLibelle() : "?";
        String objStr  = t.getObjectif_traitement() != null ? t.getObjectif_traitement().getLibelle() : "?";
        if (headerInfo != null) headerInfo.setText(typeStr + " - " + objStr);

        if (t.getType_traitement() != null)     fieldType.setValue(t.getType_traitement().getLibelle());
        if (t.getObjectif_traitement() != null) fieldObjectif.setValue(t.getObjectif_traitement().getLibelle());
        if (t.getEtat_traitement() != null)     fieldEtat.setValue(t.getEtat_traitement().getLibelle());

        if (t.getDate_debut_traitement() != null) {
            Date utilDate = new Date(t.getDate_debut_traitement().getTime());
            fieldDateDebut.setValue(utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        if (t.getDate_fin_traitement() != null) {
            Date utilDate = new Date(t.getDate_fin_traitement().getTime());
            fieldDateFin.setValue(utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        fieldDescription.setText(t.getDescription_traitement() != null ? t.getDescription_traitement() : "");

        if (t.getEtat_traitement() != null) updateBadge(t.getEtat_traitement().getLibelle());

        javafx.application.Platform.runLater(() -> {
            preselectUser(t.getId_utilisateur(), fieldUtilisateur);
            preselectUser(t.getId_coach(), fieldCoach);
        });
    }

    private void preselectUser(String id, ComboBox<String> combo) {
        if (id == null) return;
        nameToId.entrySet().stream()
                .filter(e -> e.getValue().equals(id))
                .findFirst()
                .ifPresent(e -> combo.setValue(e.getKey()));
    }

    private void updateBadge(String etatLib) {
        if (badgeEtat == null) return;
        if (etatLib == null) { badgeEtat.setText(""); return; }
        badgeEtat.setText(etatLib);
        if (Etat.EN_COURS.getLibelle().equals(etatLib))
            badgeEtat.setStyle("-fx-background-color:rgba(52,152,219,0.12); -fx-text-fill:#2471A3; -fx-background-radius:20; -fx-padding:5 14 5 14; -fx-font-weight:bold;");
        else if (Etat.TERMINE.getLibelle().equals(etatLib))
            badgeEtat.setStyle("-fx-background-color:rgba(46,139,87,0.12); -fx-text-fill:#1E7A45; -fx-background-radius:20; -fx-padding:5 14 5 14; -fx-font-weight:bold;");
        else
            badgeEtat.setStyle("-fx-background-color:rgba(192,57,43,0.10); -fx-text-fill:#A93226; -fx-background-radius:20; -fx-padding:5 14 5 14; -fx-font-weight:bold;");
    }

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
            setLabel(errGlobal, "Impossible de charger les utilisateurs.");
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
                // errDateFin peut être null → fallback sur errGlobal
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

        for (TypeTraitement tt : TypeTraitement.values())
            if (tt.getLibelle().equals(fieldType.getValue())) { traitementToEdit.setType_traitement(tt); break; }
        for (Objectif o : Objectif.values())
            if (o.getLibelle().equals(fieldObjectif.getValue())) { traitementToEdit.setObjectif_traitement(o); break; }
        for (Etat e : Etat.values())
            if (e.getLibelle().equals(fieldEtat.getValue())) { traitementToEdit.setEtat_traitement(e); break; }

        traitementToEdit.setDate_debut_traitement(
                Date.from(fieldDateDebut.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        traitementToEdit.setDate_fin_traitement(fieldDateFin.getValue() != null
                ? Date.from(fieldDateFin.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null);
        traitementToEdit.setDescription_traitement(
                fieldDescription.getText().trim().isEmpty() ? null : fieldDescription.getText().trim());

        String selUser  = fieldUtilisateur.getValue();
        String selCoach = fieldCoach.getValue();
        traitementToEdit.setId_utilisateur(selUser  != null ? nameToId.get(selUser)  : null);
        traitementToEdit.setId_coach       (selCoach != null ? nameToId.get(selCoach) : null);

        try {
            traitService.modifier(traitementToEdit);
            showSuccess("Traitement modifié avec succès !");
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