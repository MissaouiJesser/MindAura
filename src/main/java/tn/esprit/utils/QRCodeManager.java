package tn.esprit.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitaire pour générer des QR codes avec la librairie ZXing.
 *
 * Dépendance Maven à ajouter dans pom.xml :
 * <dependency>
 *     <groupId>com.google.zxing</groupId>
 *     <artifactId>core</artifactId>
 *     <version>3.5.2</version>
 * </dependency>
 * <dependency>
 *     <groupId>com.google.zxing</groupId>
 *     <artifactId>javase</artifactId>
 *     <version>3.5.2</version>
 * </dependency>
 */
public class QRCodeManager {

    private static final int DEFAULT_SIZE = 300;

    /**
     * Génère un QR code JavaFX Image à partir d'un texte/URL.
     *
     * @param content Le contenu à encoder (URL, texte...)
     * @param size    La taille en pixels (largeur = hauteur)
     * @return Image JavaFX du QR code, ou null en cas d'erreur
     */
    public static Image generateQRCodeImage(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            WritableImage writableImage = new WritableImage(size, size);
            PixelWriter pixelWriter = writableImage.getPixelWriter();

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    // Noir pour les modules actifs, blanc sinon
                    pixelWriter.setColor(x, y,
                            bitMatrix.get(x, y)
                                    ? javafx.scene.paint.Color.BLACK
                                    : javafx.scene.paint.Color.WHITE);
                }
            }
            return writableImage;

        } catch (WriterException e) {
            System.err.println("Erreur génération QR code : " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère un QR code avec la taille par défaut (300x300).
     */
    public static Image generateQRCodeImage(String content) {
        return generateQRCodeImage(content, DEFAULT_SIZE);
    }

    /**
     * Sauvegarde un QR code en fichier PNG.
     *
     * @param content  Le contenu à encoder
     * @param filePath Chemin de sortie (ex: "qrcode.png")
     * @param size     Taille en pixels
     * @return true si sauvegarde réussie
     */
    public static boolean saveQRCodeToFile(String content, String filePath, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            File outputFile = new File(filePath);
            ImageIO.write(bufferedImage, "PNG", outputFile);
            System.out.println("QR code sauvegardé : " + outputFile.getAbsolutePath());
            return true;

        } catch (WriterException | IOException e) {
            System.err.println("Erreur sauvegarde QR code : " + e.getMessage());
            return false;
        }
    }
}
