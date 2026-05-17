package tn.esprit.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import tn.esprit.entities.Categorie;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.Reponse;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour générer des PDFs des réclamations et leurs réponses
 * ✅ Utilise iText 5
 * ✅ Côté Admin uniquement
 */
public class Pdfservice {

    // Couleurs
    private static final BaseColor VERT_FONCE = new BaseColor(45, 106, 79);   // #2D6A4F
    private static final BaseColor VERT_CLAIR = new BaseColor(210, 234, 219); // #D2EADB
    private static final BaseColor GRIS_CLAIR = new BaseColor(249, 250, 251); // #F9FAFB
    private static final BaseColor GRIS_TEXTE = new BaseColor(90, 100, 117);  // #5A6475
    private static final BaseColor BLANC      = BaseColor.WHITE;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // ---------------------------------------------------------------
    // CACHE CATÉGORIES (chargé une seule fois par appel)
    // ---------------------------------------------------------------

    /** Construit un cache id→nom à partir du CategorieService. */
    private static Map<Integer, String> buildCategorieCache() {
        Map<Integer, String> cache = new HashMap<>();
        try {
            CategorieService categorieService = new CategorieService();
            List<Categorie> categories = categorieService.afficherList();
            if (categories != null) {
                for (Categorie cat : categories) {
                    cache.put(cat.getIdCategorie(), cat.getNomCategorie());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement catégories pour PDF : " + e.getMessage());
        }
        return cache;
    }

    private static String getNomCategorie(Map<Integer, String> cache, int id) {
        return cache.getOrDefault(id, "AUTRE");
    }

    // ---------------------------------------------------------------
    // PDF UNE RÉCLAMATION
    // ---------------------------------------------------------------

    /**
     * Générer un PDF pour UNE réclamation avec ses réponses.
     */
    public static void genererPdfReclamation(Reclamation reclamation,
                                             List<Reponse> reponses,
                                             String cheminFichier) throws Exception {

        // ✅ CORRECTION : charger le cache catégories une seule fois
        Map<Integer, String> categorieCache = buildCategorieCache();

        Document document = new Document(PageSize.A4, 40, 40, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        writer.setPageEvent(new HeaderFooterEvent());
        document.open();

        // ─── TITRE PRINCIPAL ───────────────────────────────────────────
        Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BLANC);
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("Détail de la Réclamation", fontTitre));
        headerCell.setBackgroundColor(VERT_FONCE);
        headerCell.setPadding(15);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(Chunk.NEWLINE);

        // ─── INFORMATIONS DE LA RÉCLAMATION ────────────────────────────
        ajouterSectionTitre(document, "Informations de la Réclamation");

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1, 2});

        ajouterLigneInfo(infoTable, "Sujet",     reclamation.getSujetReclamation());
        // ✅ CORRECTION : getNomCategorie(cache, getCategorieId()) au lieu de getCategorieReclamation()
        ajouterLigneInfo(infoTable, "Catégorie", getNomCategorie(categorieCache, reclamation.getCategorieId()));
        ajouterLigneInfo(infoTable, "Statut",    reclamation.getStatutReclamation());
        ajouterLigneInfo(infoTable, "Date",      reclamation.getDateCreationReclamation() != null
                ? SDF.format(reclamation.getDateCreationReclamation()) : "N/A");

        document.add(infoTable);
        document.add(Chunk.NEWLINE);

        // ─── DESCRIPTION ───────────────────────────────────────────────
        ajouterSectionTitre(document, "Description");

        PdfPTable descTable = new PdfPTable(1);
        descTable.setWidthPercentage(100);

        PdfPCell descCell = new PdfPCell();
        descCell.setBackgroundColor(GRIS_CLAIR);
        descCell.setPadding(12);
        descCell.setBorderColor(VERT_CLAIR);
        descCell.setBorderWidth(1);

        Font fontDesc = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
        descCell.addElement(new Paragraph(
                reclamation.getDescriptionReclamation() != null
                        ? reclamation.getDescriptionReclamation() : "Aucune description", fontDesc));
        descTable.addCell(descCell);
        document.add(descTable);
        document.add(Chunk.NEWLINE);

        // ─── RÉPONSES ──────────────────────────────────────────────────
        ajouterSectionTitre(document, "Réponses (" + reponses.size() + ")");

        if (reponses.isEmpty()) {
            Font fontVide = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, GRIS_TEXTE);
            document.add(new Paragraph("Aucune réponse pour cette réclamation.", fontVide));
        } else {
            for (int i = 0; i < reponses.size(); i++) {
                Reponse r = reponses.get(i);

                PdfPTable reponseTable = new PdfPTable(1);
                reponseTable.setWidthPercentage(100);
                reponseTable.setSpacingBefore(6);

                Font fontRepHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLANC);
                // ✅ getRateReponse() retourne double — formatage avec %.1f
                String headerText = "Réponse #" + (i + 1)
                        + String.format("   |   Note: %.1f/5", r.getRateReponse())
                        + (r.getDateReponse() != null ? "   |   " + SDF.format(r.getDateReponse()) : "");

                PdfPCell repHeaderCell = new PdfPCell(new Phrase(headerText, fontRepHeader));
                repHeaderCell.setBackgroundColor(VERT_FONCE);
                repHeaderCell.setPadding(8);
                repHeaderCell.setBorder(Rectangle.NO_BORDER);
                reponseTable.addCell(repHeaderCell);

                Font fontRepContenu = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.DARK_GRAY);
                PdfPCell repContenuCell = new PdfPCell();
                repContenuCell.setBackgroundColor(GRIS_CLAIR);
                repContenuCell.setPadding(10);
                repContenuCell.setBorderColor(VERT_CLAIR);
                repContenuCell.setBorderWidth(1);
                repContenuCell.addElement(new Paragraph(
                        r.getContenuReponse() != null ? r.getContenuReponse() : "", fontRepContenu));
                reponseTable.addCell(repContenuCell);

                document.add(reponseTable);
            }
        }

        document.close();
        System.out.println("✅ PDF généré: " + cheminFichier);
    }

    // ---------------------------------------------------------------
    // PDF TOUTES LES RÉCLAMATIONS
    // ---------------------------------------------------------------

    /**
     * Générer un PDF pour TOUTES les réclamations (rapport global).
     */
    public static void genererPdfToutesReclamations(List<Reclamation> reclamations,
                                                    String cheminFichier) throws Exception {

        // ✅ CORRECTION : charger le cache catégories une seule fois
        Map<Integer, String> categorieCache = buildCategorieCache();

        Document document = new Document(PageSize.A4.rotate(), 30, 30, 60, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        writer.setPageEvent(new HeaderFooterEvent());
        document.open();

        // ─── TITRE ─────────────────────────────────────────────────────
        Font fontTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BLANC);
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell(new Phrase("Rapport des Réclamations", fontTitre));
        headerCell.setBackgroundColor(VERT_FONCE);
        headerCell.setPadding(15);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA, 10, GRIS_TEXTE);
        Paragraph sousTitre = new Paragraph(
                "Généré le " + new SimpleDateFormat("dd/MM/yyyy à HH:mm").format(new java.util.Date())
                        + " | Total: " + reclamations.size() + " réclamation(s)", fontSousTitre);
        sousTitre.setAlignment(Element.ALIGN_CENTER);
        sousTitre.setSpacingBefore(6);
        sousTitre.setSpacingAfter(15);
        document.add(sousTitre);

        // ─── TABLEAU ───────────────────────────────────────────────────
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 2f, 1.2f, 1.2f});

        String[] headers = {"#", "Sujet", "Description", "Catégorie", "Statut"};
        for (String h : headers) {
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLANC);
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(VERT_FONCE);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        for (int i = 0; i < reclamations.size(); i++) {
            Reclamation r = reclamations.get(i);
            BaseColor bgColor = (i % 2 == 0) ? BLANC : GRIS_CLAIR;

            ajouterCelluleTableau(table, String.valueOf(i + 1), fontData, bgColor, Element.ALIGN_CENTER);
            ajouterCelluleTableau(table, r.getSujetReclamation(), fontData, bgColor, Element.ALIGN_LEFT);

            String desc = r.getDescriptionReclamation() != null ? r.getDescriptionReclamation() : "";
            if (desc.length() > 80) desc = desc.substring(0, 80) + "...";
            ajouterCelluleTableau(table, desc, fontData, bgColor, Element.ALIGN_LEFT);

            // ✅ CORRECTION : getNomCategorie(cache, getCategorieId()) au lieu de getCategorieReclamation()
            ajouterCelluleTableau(table, getNomCategorie(categorieCache, r.getCategorieId()),
                    fontData, bgColor, Element.ALIGN_CENTER);
            ajouterCelluleTableau(table, r.getStatutReclamation(), fontData, bgColor, Element.ALIGN_CENTER);
        }

        document.add(table);
        document.close();
        System.out.println("✅ PDF rapport généré: " + cheminFichier);
    }

    // ---------------------------------------------------------------
    // MÉTHODES UTILITAIRES
    // ---------------------------------------------------------------

    private static void ajouterSectionTitre(Document doc, String titre) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, VERT_FONCE);
        Paragraph p = new Paragraph(titre, font);
        p.setSpacingBefore(8);
        p.setSpacingAfter(6);
        doc.add(p);

        PdfPTable ligne = new PdfPTable(1);
        ligne.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(VERT_FONCE);
        cell.setFixedHeight(2);
        cell.setBorder(Rectangle.NO_BORDER);
        ligne.addCell(cell);
        ligne.setSpacingAfter(8);
        doc.add(ligne);
    }

    private static void ajouterLigneInfo(PdfPTable table, String cle, String valeur) {
        Font fontCle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GRIS_TEXTE);
        Font fontVal = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);

        PdfPCell cleCell = new PdfPCell(new Phrase(cle, fontCle));
        cleCell.setBackgroundColor(VERT_CLAIR);
        cleCell.setPadding(7);
        cleCell.setBorderColor(BaseColor.LIGHT_GRAY);

        PdfPCell valCell = new PdfPCell(new Phrase(valeur != null ? valeur : "N/A", fontVal));
        valCell.setBackgroundColor(BLANC);
        valCell.setPadding(7);
        valCell.setBorderColor(BaseColor.LIGHT_GRAY);

        table.addCell(cleCell);
        table.addCell(valCell);
    }

    private static void ajouterCelluleTableau(PdfPTable table, String texte, Font font,
                                              BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texte != null ? texte : "", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        table.addCell(cell);
    }

    /**
     * Header et Footer sur chaque page.
     */
    static class HeaderFooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_TEXTE);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Système de Gestion des Réclamations  |  Page "
                            + writer.getPageNumber(), fontFooter),
                    (document.right() + document.left()) / 2,
                    document.bottom() - 15, 0);
        }
    }
}