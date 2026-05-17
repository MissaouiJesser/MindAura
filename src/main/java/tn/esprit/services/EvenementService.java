package tn.esprit.services;

import tn.esprit.entities.Evenements;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.utils.MyDataBase;
import tn.esprit.utils.FileStorageUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EvenementService implements ICRUD<Evenements> {

    private Connection conx;

    public EvenementService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // ── ICRUD ─────────────────────────────────────────────────────────────────

    @Override
    public void add(Evenements ev) throws SQLException {
        // Vérification titre unique (optionnelle)
        String checkReq = "SELECT COUNT(*) AS cnt FROM evenement WHERE titre_evenement = ?";
        try (PreparedStatement cs = conx.prepareStatement(checkReq)) {
            cs.setString(1, ev.getTitreEvenement());
            ResultSet rs = cs.executeQuery();
            if (rs.next() && rs.getInt("cnt") > 0) {
                throw new SQLException("Un événement avec ce titre existe déjà !");
            }
        }

        // Résolution FK type_evenement_id
        if (ev.getTypeEvenementId() <= 0) {
            int resolvedId = resolveTypeId(ev.getTypeEvenement());
            if (resolvedId <= 0) {
                throw new SQLException("Type d'événement introuvable : '" + ev.getTypeEvenement() + "'.");
            }
            ev.setTypeEvenementId(resolvedId);
        }

        if (ev.getIdentifiantEvenemnt() == null || ev.getIdentifiantEvenemnt().isEmpty()) {
            ev.setIdentifiantEvenemnt(UUID.randomUUID().toString());
        }

        String req = "INSERT INTO evenement " +
                "(identifiant_evenemnt, titre_evenement, description_evenement, " +
                "datedebut_evenemnt, datefin_evenemnt, lieu_evenement, " +
                "capacite_evenement, max_liste_attente, statut_evenemnt, " +
                "image, type_evenement_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstm = conx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pstm.setString(1, ev.getIdentifiantEvenemnt());
            pstm.setString(2, ev.getTitreEvenement());
            pstm.setString(3, ev.getDescriptionEvenement());
            pstm.setDate(4, ev.getDatedebutEvenemnt() != null
                    ? new java.sql.Date(ev.getDatedebutEvenemnt().getTime()) : null);
            pstm.setDate(5, ev.getDatefinEvenemnt() != null
                    ? new java.sql.Date(ev.getDatefinEvenemnt().getTime()) : null);
            pstm.setString(6, ev.getLieuEvenement());
            pstm.setInt(7, ev.getCapaciteEvenement());
            if (ev.getMaxListeAttente() != null) {
                pstm.setInt(8, ev.getMaxListeAttente());
            } else {
                pstm.setNull(8, Types.INTEGER);
            }
            pstm.setString(9, ev.getStatutEvenemnt());
            pstm.setString(10, ev.getImage());  // stocke le chemin absolu copié
            pstm.setInt(11, ev.getTypeEvenementId());
            pstm.executeUpdate();

            ResultSet keys = pstm.getGeneratedKeys();
            if (keys.next()) ev.setId(keys.getInt(1));
        }
    }

    @Override
    public void addMeth2(Evenements ev) throws SQLException {
        add(ev);
    }

    @Override
    public void modifier(Evenements ev) throws SQLException {
        if (ev.getTypeEvenementId() <= 0) {
            int resolvedId = resolveTypeId(ev.getTypeEvenement());
            if (resolvedId > 0) ev.setTypeEvenementId(resolvedId);
        }

        String req = "UPDATE evenement SET " +
                "titre_evenement=?, description_evenement=?, " +
                "datedebut_evenemnt=?, datefin_evenemnt=?, " +
                "lieu_evenement=?, capacite_evenement=?, max_liste_attente=?, " +
                "statut_evenemnt=?, image=?, type_evenement_id=? " +
                "WHERE id=?";

        try (PreparedStatement pstm = conx.prepareStatement(req)) {
            pstm.setString(1, ev.getTitreEvenement());
            pstm.setString(2, ev.getDescriptionEvenement());
            pstm.setDate(3, ev.getDatedebutEvenemnt() != null
                    ? new java.sql.Date(ev.getDatedebutEvenemnt().getTime()) : null);
            pstm.setDate(4, ev.getDatefinEvenemnt() != null
                    ? new java.sql.Date(ev.getDatefinEvenemnt().getTime()) : null);
            pstm.setString(5, ev.getLieuEvenement());
            pstm.setInt(6, ev.getCapaciteEvenement());
            if (ev.getMaxListeAttente() != null) {
                pstm.setInt(7, ev.getMaxListeAttente());
            } else {
                pstm.setNull(7, Types.INTEGER);
            }
            pstm.setString(8, ev.getStatutEvenemnt());
            pstm.setString(9, ev.getImage());
            pstm.setInt(10, ev.getTypeEvenementId());
            pstm.setInt(11, ev.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void delete(Evenements ev) throws SQLException {
        // Supprimer l'image associée avant de supprimer l'enregistrement
        if (ev.getImage() != null && !ev.getImage().isEmpty()) {
            FileStorageUtil.deleteImage(ev.getImage());
        }
        String req = "DELETE FROM evenement WHERE id=?";
        try (PreparedStatement pstm = conx.prepareStatement(req)) {
            pstm.setInt(1, ev.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public List<Evenements> afficherList() throws SQLException {
        List<Evenements> list = new ArrayList<>();
        String req = "SELECT e.*, t.libelle, t.modalite, t.categorie, t.est_gratuit " +
                "FROM evenement e " +
                "JOIN type_evenement t ON e.type_evenement_id = t.id " +
                "ORDER BY e.datedebut_evenemnt DESC";
        try (PreparedStatement pstm = conx.prepareStatement(req);
             ResultSet res = pstm.executeQuery()) {
            while (res.next()) list.add(mapRow(res));
        }
        return list;
    }

    // ── Méthodes utilitaires ──────────────────────────────────────────────────

    public List<Evenements> getAllEvenements() throws SQLException { return afficherList(); }
    public void ajouterEvenement(Evenements ev) throws SQLException { add(ev); }
    public void modifierEvenement(Evenements ev) throws SQLException { modifier(ev); }

    public void supprimerEvenement(int id) throws SQLException {
        Evenements ev = findById(id);
        if (ev != null) delete(ev);
    }

    public Evenements findById(int id) throws SQLException {
        String req = "SELECT e.*, t.libelle, t.modalite, t.categorie, t.est_gratuit " +
                "FROM evenement e " +
                "JOIN type_evenement t ON e.type_evenement_id = t.id " +
                "WHERE e.id = ?";
        try (PreparedStatement pstm = conx.prepareStatement(req)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<Evenements> getEvenementsAVenir() throws SQLException {
        List<Evenements> list = new ArrayList<>();
        String req = "SELECT e.*, t.libelle, t.modalite, t.categorie, t.est_gratuit " +
                "FROM evenement e " +
                "JOIN type_evenement t ON e.type_evenement_id = t.id " +
                "WHERE e.statut_evenemnt = 'a_venir' " +
                "ORDER BY e.datedebut_evenemnt ASC";
        try (PreparedStatement pstm = conx.prepareStatement(req);
             ResultSet res = pstm.executeQuery()) {
            while (res.next()) list.add(mapRow(res));
        }
        return list;
    }

    public List<TypeEvenement> getAllTypes() throws SQLException {
        List<TypeEvenement> list = new ArrayList<>();
        String sql = "SELECT * FROM type_evenement ORDER BY libelle";
        try (PreparedStatement ps = conx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new TypeEvenement(
                        rs.getInt("id"),
                        rs.getString("libelle"),
                        rs.getString("modalite"),
                        rs.getString("categorie"),
                        rs.getBoolean("est_gratuit")
                ));
            }
        }
        return list;
    }

    // ── Résolution libellé → ID ───────────────────────────────────────────────

    private int resolveTypeId(String libelle) throws SQLException {
        if (libelle == null || libelle.isEmpty()) return 0;
        String normalized = normalize(libelle);
        String sql = "SELECT id, libelle FROM type_evenement";
        try (PreparedStatement ps = conx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (normalize(rs.getString("libelle")).equals(normalized)) {
                    return rs.getInt("id");
                }
            }
        }
        return 0;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a").replace("ä", "a")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ö", "o")
                .replace("ù", "u").replace("û", "u").replace("ü", "u")
                .replace("ç", "c");
    }

    // ── Mapping ResultSet → Entité ────────────────────────────────────────────

    private Evenements mapRow(ResultSet rs) throws SQLException {
        TypeEvenement type = new TypeEvenement(
                rs.getInt("type_evenement_id"),
                rs.getString("libelle"),
                rs.getString("modalite"),
                rs.getString("categorie"),
                rs.getBoolean("est_gratuit")
        );

        Evenements ev = new Evenements(
                rs.getInt("id"),
                rs.getString("identifiant_evenemnt"),
                rs.getString("titre_evenement"),
                rs.getString("description_evenement"),
                rs.getDate("datedebut_evenemnt"),
                rs.getDate("datefin_evenemnt"),
                rs.getString("lieu_evenement"),
                rs.getInt("capacite_evenement"),
                (Integer) rs.getObject("max_liste_attente"),
                rs.getString("statut_evenemnt"),
                rs.getString("image"),
                rs.getInt("type_evenement_id")
        );
        ev.setTypeEvenementObj(type);
        return ev;
    }
}