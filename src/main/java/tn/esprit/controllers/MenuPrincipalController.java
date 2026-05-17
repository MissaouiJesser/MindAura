package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.io.IOException;

public class MenuPrincipalController {

    @FXML
    void accederPatient(ActionEvent event) {
        try {
            // Ouvrir l'interface de choix de test pour le patient
            Parent root = FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"));
            ((Button)event.getSource()).getScene().setRoot(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur lors de l'ouverture : " + e.getMessage());
            alert.show();
            e.printStackTrace();
        }
    }

    @FXML
    void accederAdmin(ActionEvent event) {
        try {
            // Ouvrir l'interface de gestion des tests pour l'admin
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
            ((Button)event.getSource()).getScene().setRoot(root);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur lors de l'ouverture : " + e.getMessage());
            alert.show();
            e.printStackTrace();
        }
    }
}