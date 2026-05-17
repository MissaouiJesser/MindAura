package tn.esprit.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileStorageUtil {

    /**
     * Dossier partagé entre Java et Symfony.
     * DOIT correspondre exactement au dossier public/uploads/evenements de Symfony.
     */
    public static final String SYMFONY_UPLOADS_PATH =
            "C:/Users/Rania/Desktop/1/public/uploads/evenements";

    public static Path getUploadsPath() {
        Path uploadsDir = Paths.get(SYMFONY_UPLOADS_PATH);
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uploadsDir;
    }

    public static String copyImage(File sourceFile) throws IOException {
        Path destDir   = getUploadsPath();
        String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
        Path destFile   = destDir.resolve(fileName);
        Files.copy(sourceFile.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public static void deleteImage(String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) return;

        File file = new File(imageValue);

        if (file.isAbsolute() && file.exists()) {
            file.delete();
            return;
        }

        File inSymfony = Paths.get(SYMFONY_UPLOADS_PATH, imageValue).toFile();
        if (inSymfony.exists()) {
            inSymfony.delete();
            return;
        }

        String fileName = file.getName();
        if (!fileName.isEmpty()) {
            File migrated = Paths.get(SYMFONY_UPLOADS_PATH, fileName).toFile();
            if (migrated.exists()) migrated.delete();
        }
    }

    public static String normalizeImagePath(String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) return null;

        File file = new File(imageValue);
        if (!file.isAbsolute()) {
            return imageValue;
        }

        String fileName = file.getName();
        if (fileName.isEmpty()) return null;

        File inSymfony = Paths.get(SYMFONY_UPLOADS_PATH, fileName).toFile();
        if (inSymfony.exists()) return fileName;

        if (file.exists()) {
            try {
                String newFileName = System.currentTimeMillis() + "_" + fileName;
                Path dest = Paths.get(SYMFONY_UPLOADS_PATH, newFileName);
                Files.createDirectories(dest.getParent());
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                return newFileName;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Résolution robuste d'une image à partir de sa valeur en base.
     * Supporte :
     * - nom de fichier seul
     * - chemin absolu (ancien)
     * - recherche insensible à la casse en dernier recours
     */
    public static File resolveImageFile(String imgValue) {
        if (imgValue == null || imgValue.trim().isEmpty()) return null;

        // 1. Nettoyer la valeur : enlever les chemins éventuels (sans réaffectation)
        String rawClean = imgValue.trim().replace('\\', '/');
        String clean = rawClean.contains("/")
                ? rawClean.substring(rawClean.lastIndexOf('/') + 1)
                : rawClean;

        // 2. Chercher dans le dossier partagé
        File inShared = Paths.get(SYMFONY_UPLOADS_PATH, clean).toFile();
        if (inShared.exists()) return inShared;

        // 3. Essayer en tant que chemin absolu
        File absolute = new File(imgValue);
        if (absolute.exists()) return absolute;

        // 4. Dernier recours : chercher le fichier par nom dans le dossier (insensible à la casse)
        File sharedDir = getUploadsPath().toFile();
        // ✅ clean est maintenant effectively final (plus de réaffectation)
        File[] matches = sharedDir.listFiles((dir, name) -> name.equalsIgnoreCase(clean));
        if (matches != null && matches.length > 0) {
            return matches[0];
        }

        return null;
    }
}