package tn.esprit.utils;

import tn.esprit.entities.Commentaires;
import tn.esprit.services.CommentairesServices;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public class MainC {
    public static void main(String[] args) {

        try (Connection conx = MyDataBase.getInstance().getConx()) {

            // Création du service
            CommentairesServices service = new CommentairesServices(conx);

            // Création d'un nouveau commentaire
            Commentaires com = new Commentaires();
            com.setId_Article(43); // ID de l'article auquel le commentaire appartient
            com.setId_User(2);    // ID de l'utilisateur qui poste le commentaire
            com.setUser_name("minyar");
            com.setContenu("Ceci est un commentaire de test !");
            com.setDate(new Date()); // date actuelle
            com.setLikes(5);
            com.setReponse(1);

            // ou ARCHIVE / SUPPRIME

            // Ajout dans la base
            service.add(com);

            System.out.println("Commentaire ajouté avec l'ID : " + com.getId_commentaires());
            System.out.println(service.afficherList());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
