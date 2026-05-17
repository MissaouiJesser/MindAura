package tn.esprit.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;

/**
 * ImageManager : partage les images avec Symfony 6.4.
 *
 *  - Dossier salles : <SYMFONY>/public/uploads/salles/
 *  - Dossier locaux : <SYMFONY>/public/uploads/locaux/
 *  - BDD : on stocke UNIQUEMENT le nom du fichier (ex: "salle-680f3abc.webp")
 *
 * Pourquoi ImageIO et pas directement new Image(URI) ?
 * → JavaFX ne décode PAS le WebP nativement. Symfony génère beaucoup de .webp
 *   (`guessExtension()`), donc il faut décoder via javax.imageio.ImageIO et la
 *   dépendance webp-imageio dans le pom.xml :
 *
 *     <dependency>
 *         <groupId>org.sejda.imageio</groupId>
 *         <artifactId>webp-imageio</artifactId>
 *         <version>0.1.6</version>
 *     </dependency>
 */
public class ImageManager {

    // ─────────────────────────────────────────────────────────────────────────
    //  CHEMINS  (utilise des slashes / pas de \)
    // ─────────────────────────────────────────────────────────────────────────
    public static final String SALLES_DIR =
            "C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/uploads/salles";

    public static final String LOCAUX_DIR =
            "C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/uploads/locaux";

    private static final boolean DEBUG = false;

    // ─────────────────────────────────────────────────────────────────────────
    public static void initializeImageDirectory() {
        try {
            Files.createDirectories(Paths.get(SALLES_DIR));
            Files.createDirectories(Paths.get(LOCAUX_DIR));
            System.out.println("[ImageManager] Dossier salles = " + Paths.get(SALLES_DIR).toAbsolutePath());
            System.out.println("[ImageManager] Dossier locaux = " + Paths.get(LOCAUX_DIR).toAbsolutePath());

            // Affiche les formats lisibles (doit inclure "webp" si la dépendance est OK)
            String[] readers = ImageIO.getReaderFormatNames();
            System.out.println("[ImageManager] Formats ImageIO = " + String.join(", ", readers));
        } catch (IOException e) {
            System.err.println("[ImageManager] Init échouée : " + e.getMessage());
        }
    }

    public static Path getSallesDir() { return Paths.get(SALLES_DIR); }
    public static Path getLocauxDir() { return Paths.get(LOCAUX_DIR); }

    // ─────────────────────────────────────────────────────────────────────────
    //  SÉLECTION
    // ─────────────────────────────────────────────────────────────────────────
    public static File selectImageFile(Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        return fc.showOpenDialog(owner);
    }

    public static boolean isValidImageFile(File f) {
        if (f == null || !f.isFile()) return false;
        String n = f.getName().toLowerCase();
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".gif") || n.endsWith(".webp");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAUVEGARDE → retourne UNIQUEMENT le nom du fichier (comme Symfony)
    // ─────────────────────────────────────────────────────────────────────────
    public static String saveImage(File source) throws IOException {
        return saveImageTo(source, Paths.get(SALLES_DIR));
    }

    public static String saveImageLocal(File source) throws IOException {
        return saveImageTo(source, Paths.get(LOCAUX_DIR));
    }

    private static String saveImageTo(File source, Path targetDir) throws IOException {
        if (source == null || !source.isFile())
            throw new IOException("Fichier source invalide : " + source);

        Files.createDirectories(targetDir);

        String original = source.getName();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > 0) {
            ext = original.substring(dot + 1).toLowerCase();
            original = original.substring(0, dot);
        }
        String slug = slugify(original);
        String uniqid = Long.toHexString(System.currentTimeMillis() / 1000L)
                + Long.toHexString((long) (Math.random() * 0xFFFFFFL));
        String newFilename = slug + "-" + uniqid + (ext.isEmpty() ? "" : "." + ext);

        Files.copy(source.toPath(), targetDir.resolve(newFilename),
                StandardCopyOption.REPLACE_EXISTING);

        if (DEBUG) System.out.println("[ImageManager] Sauvegardé : " + targetDir.resolve(newFilename));
        return newFilename;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRESSION
    // ─────────────────────────────────────────────────────────────────────────
    public static boolean deleteImage(String storedName) {
        if (storedName == null || storedName.isBlank()) return false;
        String safe = Paths.get(storedName).getFileName().toString();
        try {
            if (Files.deleteIfExists(Paths.get(SALLES_DIR).resolve(safe))) return true;
            return Files.deleteIfExists(Paths.get(LOCAUX_DIR).resolve(safe));
        } catch (IOException e) {
            System.err.println("[ImageManager] Suppression échouée : " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT  →  passe par ImageIO pour supporter WEBP
    // ─────────────────────────────────────────────────────────────────────────
    public static Image loadImage(String pathOrName,
                                  double w, double h,
                                  boolean preserveRatio) {
        if (pathOrName == null || pathOrName.isBlank()) return null;

        try {
            // 1) URL distante → JavaFX gère directement
            if (pathOrName.startsWith("http://") || pathOrName.startsWith("https://")) {
                return new Image(pathOrName, w, h, preserveRatio, true, true);
            }

            // 2) Localiser le fichier sur le disque
            File file = resolveFile(pathOrName);
            if (file == null) {
                System.err.println("[ImageManager] INTROUVABLE : '" + pathOrName + "'");
                return null;
            }

            String lower = file.getName().toLowerCase();
            boolean isWebp = lower.endsWith(".webp");

            // 3a) Formats supportés nativement par JavaFX (PNG, JPG, GIF, BMP)
            //     → on laisse JavaFX charger (plus rapide, plus simple).
            if (!isWebp) {
                return new Image(file.toURI().toString(), w, h, preserveRatio, true, true);
            }

            // 3b) WebP → on passe par ImageIO + conversion en JavaFX Image
            BufferedImage bImg = ImageIO.read(file);
            if (bImg == null) {
                System.err.println("[ImageManager] WebP non décodable : " + file);
                System.err.println("    → vérifie la dépendance org.sejda.imageio:webp-imageio dans pom.xml");
                return null;
            }

            // Redimensionner si demandé (préserve le ratio si demandé)
            if (w > 0 && h > 0) {
                bImg = scale(bImg, (int) w, (int) h, preserveRatio);
            }

            return SwingFXUtils.toFXImage(bImg, null);

        } catch (Exception e) {
            System.err.println("[ImageManager] Erreur chargement '" + pathOrName + "' : " + e.getMessage());
            return null;
        }
    }

    /** Localise le fichier en cherchant dans salles puis locaux. */
    private static File resolveFile(String pathOrName) {
        // Chemin absolu existant ?
        File asFile = new File(pathOrName);
        if (asFile.isAbsolute() && asFile.isFile()) return asFile;

        // Extraire juste le nom
        String fileName = pathOrName;
        int idx = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (idx >= 0) fileName = fileName.substring(idx + 1);

        File inSalles = Paths.get(SALLES_DIR).resolve(fileName).toFile();
        if (inSalles.isFile()) return inSalles;
        File inLocaux = Paths.get(LOCAUX_DIR).resolve(fileName).toFile();
        if (inLocaux.isFile()) return inLocaux;
        return null;
    }

    /** Redimensionne un BufferedImage en préservant le ratio si demandé. */
    private static BufferedImage scale(BufferedImage src, int targetW, int targetH, boolean preserveRatio) {
        int outW = targetW, outH = targetH;
        if (preserveRatio) {
            double rx = (double) targetW / src.getWidth();
            double ry = (double) targetH / src.getHeight();
            double r  = Math.min(rx, ry);
            outW = Math.max(1, (int) Math.round(src.getWidth()  * r));
            outH = Math.max(1, (int) Math.round(src.getHeight() * r));
        }
        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        var g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, outW, outH, null);
        g.dispose();
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private static String slugify(String input) {
        if (input == null) return "image";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .toLowerCase();
        return slug.isEmpty() ? "image" : slug;
    }
}
