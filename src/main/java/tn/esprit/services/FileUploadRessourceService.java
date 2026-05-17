package tn.esprit.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class FileUploadRessourceService {

    private static final String BASE_URL         = "http://localhost:8000";
    private static final String UPLOAD_RESOURCE  = BASE_URL + "/admin/ressources/api/upload/resource";
    private static final String UPLOAD_IMAGE     = BASE_URL + "/admin/ressources/api/upload/image";

    public String uploadRessource(File file) throws Exception {
        return uploadFile(file, UPLOAD_RESOURCE);
    }

    public String uploadImage(File file) throws Exception {
        return uploadFile(file, UPLOAD_IMAGE);
    }

    private String uploadFile(File file, String endpointUrl) throws Exception {
        String boundary = "----JavaBoundary" + System.currentTimeMillis();

        HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);

        try (OutputStream os = conn.getOutputStream()) {
            String partHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + file.getName() + "\"\r\n"
                    + "Content-Type: " + detectMimeType(file) + "\r\n"
                    + "\r\n";
            os.write(partHeader.getBytes("UTF-8"));
            Files.copy(file.toPath(), os);
            String partFooter = "\r\n--" + boundary + "--\r\n";
            os.write(partFooter.getBytes("UTF-8"));
            os.flush();
        }

        int httpCode = conn.getResponseCode();
        InputStream is = (httpCode >= 200 && httpCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (httpCode < 200 || httpCode >= 300) {
            throw new RuntimeException("Upload échoué [HTTP " + httpCode + "] : " + sb);
        }

        String json = sb.toString();
        System.out.println("📥 Réponse Symfony brute : " + json);

        if (json.contains("\"error\"")) {
            throw new RuntimeException("Erreur Symfony : " + json);
        }

        // ✅ CORRIGÉ : extraire l'URL puis supprimer les backslashes échappés (\/ → /)
        String url = json.replaceAll(".*\"url\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        if (url.equals(json)) {
            throw new RuntimeException("Réponse inattendue de Symfony : " + json);
        }

        // ✅ Supprimer les backslashes échappés que PHP/Symfony peut ajouter dans le JSON
        url = url.replace("\\/", "/");

        System.out.println("✅ Upload OK → " + url);
        return url;
    }

    private String detectMimeType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf"))  return "application/pdf";
        if (name.endsWith(".mp4"))  return "video/mp4";
        if (name.endsWith(".avi"))  return "video/x-msvideo";
        if (name.endsWith(".mkv"))  return "video/x-matroska";
        if (name.endsWith(".mov"))  return "video/quicktime";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg"))  return "image/jpeg";
        if (name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif"))  return "image/gif";
        try {
            String probed = Files.probeContentType(file.toPath());
            return probed != null ? probed : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}