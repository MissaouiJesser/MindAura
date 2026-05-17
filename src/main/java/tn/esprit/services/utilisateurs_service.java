package tn.esprit.services;

import tn.esprit.entities.utilisateurs;
import tn.esprit.utils.MyDataBase;
import tn.esprit.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour les utilisateurs.
 *
 * SÉCURITÉ — Hachage BCrypt :
 *   • addMeth2() et add() : le mot de passe est haché avant insertion en BDD.
 *   • modifier()          : le mot de passe n'est mis à jour QUE s'il a changé
 *                           (on re-hache uniquement si ce n'est pas déjà un hash BCrypt).
 *   • afficherList()      : retourne le hash tel quel (jamais le mot de passe en clair).
 *   • La vérification au login se fait avec PasswordUtils.verify() dans LoginController.
 */
public class utilisateurs_service implements ICRUD<utilisateurs> {

    private Connection connection;
    private Statement stm;
    private PreparedStatement pstm;

    public utilisateurs_service() {
        this.connection = MyDataBase.getInstance().getConx();
    }

    // =========================================================================
    //  ADD — Méthode 1 (concaténation — gardée pour compatibilité)
    // =========================================================================

    @Override
    public void add(utilisateurs user) throws SQLException {
        // Hacher le mot de passe avant insertion
        String hashedPwd = PasswordUtils.isHashed(user.getMdp_utilisateur())
                ? user.getMdp_utilisateur()
                : PasswordUtils.hash(user.getMdp_utilisateur());

        String req = "INSERT INTO utilisateur(nom_utilisateur, prenom_utilisateur, email_utilisateur, " +
                "mdp_utilisateur, telephone_utilisateur, date_naissance_utilisateur, role_utilisateur, " +
                "photo_profil_utilisateur, est_actif_utilisateur, bio_utilisateur) " +
                "VALUES ('" + user.getNom_utilisateur() + "','" + user.getPrenom_utilisateur() + "','" +
                user.getEmail_utilisateur() + "','" + hashedPwd + "','" +
                (user.getTelephone_utilisateur() != null ? user.getTelephone_utilisateur() : "") + "','" +
                new java.sql.Date(user.getDate_naissance_utilisateur().getTime()) + "','" +
                user.getRole_utilisateur().name() + "','" +
                (user.getPhoto_profil_utilisateur() != null ? user.getPhoto_profil_utilisateur() : "") + "'," +
                (user.isEst_actif_utilisateur() ? 1 : 0) + ",'" +
                (user.getBio_utilisateur() != null ? user.getBio_utilisateur() : "") + "')";

        this.connection = MyDataBase.getInstance().getConx();
        this.stm = this.connection.createStatement();
        this.stm.executeUpdate(req);
        System.out.println("Utilisateur ajouté avec succès (Méthode 1)");
    }

    // =========================================================================
    //  ADD — Méthode 2 (PreparedStatement — recommandée)
    // =========================================================================

    @Override
    public void addMeth2(utilisateurs user) throws SQLException {
        // Hacher le mot de passe avant insertion (si pas déjà haché)
        String hashedPwd = PasswordUtils.isHashed(user.getMdp_utilisateur())
                ? user.getMdp_utilisateur()
                : PasswordUtils.hash(user.getMdp_utilisateur());

        String req = "INSERT INTO utilisateur(nom_utilisateur, prenom_utilisateur, email_utilisateur, " +
                "mdp_utilisateur, telephone_utilisateur, date_naissance_utilisateur, role_utilisateur, " +
                "photo_profil_utilisateur, est_actif_utilisateur, bio_utilisateur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);
        this.pstm.setString(1, user.getNom_utilisateur());
        this.pstm.setString(2, user.getPrenom_utilisateur());
        this.pstm.setString(3, user.getEmail_utilisateur());
        this.pstm.setString(4, hashedPwd);                          // ← hash BCrypt

        if (user.getTelephone_utilisateur() != null)
            this.pstm.setString(5, user.getTelephone_utilisateur());
        else
            this.pstm.setNull(5, Types.VARCHAR);

        this.pstm.setDate(6, new java.sql.Date(user.getDate_naissance_utilisateur().getTime()));
        this.pstm.setString(7, user.getRole_utilisateur().name());

        if (user.getPhoto_profil_utilisateur() != null)
            this.pstm.setString(8, user.getPhoto_profil_utilisateur());
        else
            this.pstm.setNull(8, Types.VARCHAR);

        this.pstm.setBoolean(9, user.isEst_actif_utilisateur());

        if (user.getBio_utilisateur() != null)
            this.pstm.setString(10, user.getBio_utilisateur());
        else
            this.pstm.setNull(10, Types.VARCHAR);

        this.pstm.executeUpdate();
        System.out.println("Utilisateur ajouté avec succès (Méthode 2 - PreparedStatement)");
    }

    // =========================================================================
    //  MODIFIER
    // =========================================================================

    @Override
    public void modifier(utilisateurs user) throws SQLException {
        // Re-hacher uniquement si le mot de passe n'est pas déjà un hash BCrypt.
        // Cas d'usage :
        //   • NouveauMotDePasseController passe un mot de passe en clair → on hache
        //   • ModifierUtilisateurController sans changement de mdp → champ vide ou déjà haché → on garde
        String pwd = user.getMdp_utilisateur();
        String hashedPwd;
        if (pwd == null || pwd.isEmpty()) {
            // Pas de changement de mot de passe → récupérer le hash actuel depuis la BDD
            hashedPwd = getHashedPasswordFromDb(user.getId_utilisateur());
        } else if (PasswordUtils.isHashed(pwd)) {
            hashedPwd = pwd;   // déjà un hash BCrypt (ex: rechargé depuis la BDD)
        } else {
            hashedPwd = PasswordUtils.hash(pwd);  // nouveau mot de passe en clair → hacher
        }

        String req = "UPDATE utilisateur SET nom_utilisateur=?, prenom_utilisateur=?, " +
                "email_utilisateur=?, mdp_utilisateur=?, telephone_utilisateur=?, " +
                "date_naissance_utilisateur=?, role_utilisateur=?, photo_profil_utilisateur=?, " +
                "est_actif_utilisateur=?, bio_utilisateur=? WHERE id_utilisateur=?";

        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);
        this.pstm.setString(1, user.getNom_utilisateur());
        this.pstm.setString(2, user.getPrenom_utilisateur());
        this.pstm.setString(3, user.getEmail_utilisateur());
        this.pstm.setString(4, hashedPwd);                          // ← hash BCrypt

        if (user.getTelephone_utilisateur() != null)
            this.pstm.setString(5, user.getTelephone_utilisateur());
        else
            this.pstm.setNull(5, Types.VARCHAR);

        this.pstm.setDate(6, new java.sql.Date(user.getDate_naissance_utilisateur().getTime()));
        this.pstm.setString(7, user.getRole_utilisateur().name());

        if (user.getPhoto_profil_utilisateur() != null)
            this.pstm.setString(8, user.getPhoto_profil_utilisateur());
        else
            this.pstm.setNull(8, Types.VARCHAR);

        this.pstm.setBoolean(9, user.isEst_actif_utilisateur());

        if (user.getBio_utilisateur() != null)
            this.pstm.setString(10, user.getBio_utilisateur());
        else
            this.pstm.setNull(10, Types.VARCHAR);

        this.pstm.setLong(11, Long.parseLong(user.getId_utilisateur()));

        int rowsAffected = this.pstm.executeUpdate();
        System.out.println(rowsAffected + " utilisateur(s) modifié(s) avec succès");
    }

    // =========================================================================
    //  DELETE
    // =========================================================================

    @Override
    public void delete(utilisateurs user) throws SQLException {
        String req = "DELETE FROM utilisateur WHERE id_utilisateur=?";
        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);
        this.pstm.setLong(1, Long.parseLong(user.getId_utilisateur()));
        int rowsAffected = this.pstm.executeUpdate();
        System.out.println(rowsAffected + " utilisateur(s) supprimé(s) avec succès");
    }

    // =========================================================================
    //  AFFICHER LIST
    // =========================================================================

    @Override
    public List<utilisateurs> afficherList() throws SQLException {
        List<utilisateurs> users = new ArrayList<>();
        String req = "SELECT * FROM utilisateur";

        this.connection = MyDataBase.getInstance().getConx();
        this.stm = this.connection.createStatement();
        ResultSet res = this.stm.executeQuery(req);

        while (res.next()) {
            utilisateurs user = new utilisateurs();
            user.setId_utilisateur(String.valueOf(res.getLong("id_utilisateur")));
            user.setNom_utilisateur(res.getString("nom_utilisateur"));
            user.setPrenom_utilisateur(res.getString("prenom_utilisateur"));
            user.setEmail_utilisateur(res.getString("email_utilisateur"));
            user.setMdp_utilisateur(res.getString("mdp_utilisateur")); // hash BCrypt
            user.setTelephone_utilisateur(res.getString("telephone_utilisateur"));
            user.setDate_naissance_utilisateur(res.getDate("date_naissance_utilisateur"));
            user.setRole_utilisateur(res.getString("role_utilisateur"));
            user.setPhoto_profil_utilisateur(res.getString("photo_profil_utilisateur"));
            user.setDate_inscription_utilisateur(res.getTimestamp("date_inscription_utilisateur"));
            user.setEst_actif_utilisateur(res.getBoolean("est_actif_utilisateur"));
            user.setBio_utilisateur(res.getString("bio_utilisateur"));
            users.add(user);
        }

        return users;
    }

    // =========================================================================
    //  RECHERCHER
    // =========================================================================

    public List<utilisateurs> rechercher(String keyword) throws SQLException {
        List<utilisateurs> users = new ArrayList<>();
        String req = "SELECT * FROM utilisateur WHERE " +
                "nom_utilisateur LIKE ? OR prenom_utilisateur LIKE ? OR " +
                "email_utilisateur LIKE ? OR CONCAT(prenom_utilisateur, ' ', nom_utilisateur) LIKE ? " +
                "ORDER BY id_utilisateur DESC";

        connection = MyDataBase.getInstance().getConx();
        pstm = connection.prepareStatement(req);
        String searchPattern = "%" + keyword + "%";
        pstm.setString(1, searchPattern);
        pstm.setString(2, searchPattern);
        pstm.setString(3, searchPattern);
        pstm.setString(4, searchPattern);

        ResultSet res = pstm.executeQuery();

        while (res.next()) {
            utilisateurs user = new utilisateurs();
            user.setId_utilisateur(res.getString("id_utilisateur"));
            user.setNom_utilisateur(res.getString("nom_utilisateur"));
            user.setPrenom_utilisateur(res.getString("prenom_utilisateur"));
            user.setEmail_utilisateur(res.getString("email_utilisateur"));
            user.setMdp_utilisateur(res.getString("mdp_utilisateur"));
            user.setTelephone_utilisateur(res.getString("telephone_utilisateur"));
            user.setDate_naissance_utilisateur(res.getDate("date_naissance_utilisateur"));
            user.setRole_utilisateur(res.getString("role_utilisateur"));
            user.setPhoto_profil_utilisateur(res.getString("photo_profil_utilisateur"));
            user.setDate_inscription_utilisateur(res.getTimestamp("date_inscription_utilisateur"));
            user.setEst_actif_utilisateur(res.getBoolean("est_actif_utilisateur"));
            user.setBio_utilisateur(res.getString("bio_utilisateur"));
            users.add(user);
        }
        return users;
    }

    // =========================================================================
    //  HELPER PRIVÉ — Récupère le hash actuel depuis la BDD (pour modifier())
    // =========================================================================

    // =========================================================================
    //  GET BY ID / NOM COMPLET (pour afficher auteur réclamation / réponse)
    // =========================================================================

    /**
     * Retourne le nom complet (prénom + nom) d'un utilisateur à partir de son id.
     * Utilisé pour afficher l'auteur à côté des réclamations et des réponses.
     *
     * @param idUtilisateur id de l'utilisateur (String ou numérique)
     * @return "Prénom Nom" ou "Utilisateur #id" si non trouvé, jamais null
     */
    public String getNomCompletById(String idUtilisateur) {
        if (idUtilisateur == null || idUtilisateur.trim().isEmpty()) return "Anonyme";
        try {
            String req = "SELECT prenom_utilisateur, nom_utilisateur FROM utilisateur WHERE id_utilisateur = ?";
            connection = MyDataBase.getInstance().getConx();
            pstm = connection.prepareStatement(req);
            try {
                pstm.setLong(1, Long.parseLong(idUtilisateur.trim()));
            } catch (NumberFormatException e) {
                pstm.setString(1, idUtilisateur.trim());
            }
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                String prenom = rs.getString("prenom_utilisateur");
                String nom = rs.getString("nom_utilisateur");
                prenom = prenom != null ? prenom.trim() : "";
                nom = nom != null ? nom.trim() : "";
                return (prenom + " " + nom).trim().isEmpty() ? ("Utilisateur #" + idUtilisateur) : (prenom + " " + nom);
            }
        } catch (SQLException e) {
            System.err.println("[utilisateurs_service] getNomCompletById: " + e.getMessage());
        }
        return "Utilisateur #" + idUtilisateur;
    }

    /**
     * Retourne true si l'utilisateur donné est un administrateur (role ROLE_ADMIN).
     * Utilisé pour afficher "(admin)" à côté du nom sur les réponses.
     */
    public boolean isAdminById(String idUtilisateur) {
        if (idUtilisateur == null || idUtilisateur.trim().isEmpty()) return false;
        try {
            String req = "SELECT role_utilisateur FROM utilisateur WHERE id_utilisateur = ?";
            connection = MyDataBase.getInstance().getConx();
            pstm = connection.prepareStatement(req);
            try {
                pstm.setLong(1, Long.parseLong(idUtilisateur.trim()));
            } catch (NumberFormatException e) {
                pstm.setString(1, idUtilisateur.trim());
            }
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role_utilisateur");
                return "ROLE_ADMIN".equals(role);
            }
        } catch (SQLException e) {
            System.err.println("[utilisateurs_service] isAdminById: " + e.getMessage());
        }
        return false;
    }

    /**
     * Récupère le hash BCrypt stocké en BDD pour un utilisateur donné.
     * Utilisé dans modifier() quand le champ mdp est vide (pas de changement).
     *
     * @param idUtilisateur  id de l'utilisateur
     * @return               hash BCrypt actuel, ou chaîne vide si non trouvé
     */
    private String getHashedPasswordFromDb(String idUtilisateur) {
        try {
            String req = "SELECT mdp_utilisateur FROM utilisateur WHERE id_utilisateur=?";
            Connection con = MyDataBase.getInstance().getConx();
            PreparedStatement ps = con.prepareStatement(req);
            ps.setLong(1, Long.parseLong(idUtilisateur));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("mdp_utilisateur");
        } catch (SQLException | NumberFormatException e) {
            System.err.println("[PasswordUtils] Impossible de récupérer le hash : " + e.getMessage());
        }
        return "";
    }
}