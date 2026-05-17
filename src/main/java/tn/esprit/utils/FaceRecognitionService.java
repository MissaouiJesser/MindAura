package tn.esprit.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de reconnaissance faciale locale via DeepFace (Python).
 *
 * PRE-REQUIS (une seule fois) :
 *   pip install deepface tf-keras opencv-python
 *
 * face_verify.py doit etre a la racine du projet (meme niveau que pom.xml).
 *
 * CORRECTIONS APPORTÉES :
 *  1. Détection automatique de Python (python3 / python / chemin absolu Windows)
 *  2. Suppression du timeout interne — le caller (FaceAuthController) gère le timeout
 *  3. Meilleure extraction du JSON depuis stdout (TF affiche des logs avant)
 *  4. Messages d'erreur clairs et sans accents pour Windows
 */
public class FaceRecognitionService {

    public static final double SIMILARITY_THRESHOLD = 60.0;

    private static final String SCRIPT_PATH = "face_verify.py";
    private static final String TEMP_DIR    = System.getProperty("java.io.tmpdir");

    /**
     * Liste des chemins Python à essayer, dans l'ordre de préférence.
     * Ajoutez votre chemin Python Windows si nécessaire.
     */
    private static final String[] PYTHON_CANDIDATES = {
            // Windows — chemin absolu (adaptez si nécessaire)
            "C:\\Users\\LOQ\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
            "C:\\Python311\\python.exe",
            "C:\\Python310\\python.exe",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
            // Linux / macOS
            "/usr/bin/python3",
            "/usr/local/bin/python3",
            "/opt/homebrew/bin/python3",
            // PATH (fonctionne si Python est dans le PATH)
            "python3",
            "python"
    };

    /** Cache du chemin Python trouvé pour éviter de re-détecter à chaque appel */
    private static volatile String cachedPythonPath = null;

    // ─── Méthode principale ───────────────────────────────────────────────────

    public static FaceCompareResult compareFaces(String profilePhotoPath, byte[] capturedBytes) {

        // 1. Résoudre le chemin absolu de la photo de profil
        Path profileAbs = Paths.get("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/" + profilePhotoPath).toAbsolutePath();
        if (!profileAbs.toFile().exists()) {
            // Essayer aussi sans le préfixe src/main/resources
            Path alt = Paths.get(profilePhotoPath).toAbsolutePath();
            if (alt.toFile().exists()) {
                profileAbs = alt;
            } else {
                return FaceCompareResult.error(
                        "Photo de profil introuvable :\n" + profileAbs
                );
            }
        }

        // 2. Sauvegarder la capture webcam dans un fichier temporaire
        File captureFile = null;
        try {
            captureFile = File.createTempFile("mindaura_capture_", ".jpg", new File(TEMP_DIR));
            captureFile.deleteOnExit();
            Files.write(captureFile.toPath(), capturedBytes);
        } catch (IOException e) {
            return FaceCompareResult.error("Erreur sauvegarde capture : " + e.getMessage());
        }

        // 3. Détecter Python
        String pythonPath = resolvePythonPath();
        if (pythonPath == null) {
            return FaceCompareResult.error(
                    "Python introuvable.\n" +
                            "Verifiez que Python 3.x est installe et accessible.\n" +
                            "Ou ajoutez son chemin dans FaceRecognitionService.PYTHON_CANDIDATES"
            );
        }

        // 4. Lancer le script Python
        try {
            List<String> command = new ArrayList<>();
            command.add(pythonPath);
            command.add("-X"); command.add("utf8");  // Force UTF-8 sur Windows
            command.add(SCRIPT_PATH);
            command.add(profileAbs.toString());
            command.add(captureFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Lire stdout et stderr en parallèle pour éviter les blocages
            String[] stdout = {""};
            String[] stderr = {""};
            Thread outThread = new Thread(() -> {
                try { stdout[0] = readStream(process.getInputStream()); }
                catch (IOException ignored) {}
            });
            Thread errThread = new Thread(() -> {
                try { stderr[0] = readStream(process.getErrorStream()); }
                catch (IOException ignored) {}
            });
            outThread.start();
            errThread.start();

            process.waitFor();
            outThread.join();
            errThread.join();

            // Chercher la ligne JSON dans stdout
            String jsonLine = extractJsonLine(stdout[0]);

            if (jsonLine == null || jsonLine.isBlank()) {
                return FaceCompareResult.error(buildErrorMessage(stderr[0]));
            }

            return parseJsonResult(jsonLine.trim());

        } catch (IOException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Cannot run") || msg.contains("No such file")) {
                return FaceCompareResult.error(
                        "Impossible de lancer Python : " + pythonPath + "\n" +
                                "Verifiez le chemin dans FaceRecognitionService."
                );
            }
            return FaceCompareResult.error("Erreur execution Python : " + msg);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FaceCompareResult.error("Verification interrompue.");

        } finally {
            if (captureFile != null) captureFile.delete();
        }
    }

    // ─── Détection automatique de Python ─────────────────────────────────────

    /**
     * Teste les chemins Python candidats et retourne le premier qui fonctionne.
     * Résultat mis en cache pour les appels suivants.
     */
    private static String resolvePythonPath() {
        if (cachedPythonPath != null) return cachedPythonPath;

        for (String candidate : PYTHON_CANDIDATES) {
            if (testPython(candidate)) {
                cachedPythonPath = candidate;
                System.out.println("[FaceRecognition] Python trouve : " + candidate);
                return candidate;
            }
        }
        return null;
    }

    private static boolean testPython(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(path, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = readStream(p.getInputStream());
            int code = p.waitFor();
            return code == 0 && (out.contains("Python 3") || out.contains("Python 2"));
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Extraction JSON depuis stdout ────────────────────────────────────────

    private static String extractJsonLine(String stdout) {
        if (stdout == null || stdout.isBlank()) return null;
        for (String line : stdout.split("\n")) {
            String t = line.trim();
            if (t.startsWith("{") && t.contains("\"success\"")) return t;
        }
        return null;
    }

    // ─── Message d'erreur depuis stderr ──────────────────────────────────────

    private static String buildErrorMessage(String stderr) {
        if (stderr == null || stderr.isBlank()) {
            return "Script Python sans reponse. Verifiez face_verify.py et l'installation DeepFace.";
        }
        String s = stderr.toLowerCase();
        if (s.contains("no module named 'deepface'") || s.contains("modulenotfounderror")) {
            return "DeepFace non installe.\nExec dans terminal :\npip install deepface tf-keras opencv-python";
        }
        if (s.contains("no module named 'cv2'")) {
            return "OpenCV non installe.\nExec : pip install opencv-python";
        }
        if (s.contains("no module named 'tensorflow'") || s.contains("tf-keras")) {
            return "TensorFlow non installe.\nExec : pip install tf-keras";
        }
        if (s.contains("no module named")) {
            return "Dependance Python manquante.\nExec : pip install deepface tf-keras opencv-python";
        }
        // Dernière ligne non vide de stderr
        return stderr.lines()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .reduce((a, b) -> b)
                .map(FaceRecognitionService::removeAccents)
                .orElse("Erreur Python inconnue. Consultez la console.");
    }

    // ─── Parsing JSON minimal ─────────────────────────────────────────────────

    private static FaceCompareResult parseJsonResult(String json) {
        try {
            boolean success    = extractBool(json,   "success");
            double  distance   = extractDouble(json, "distance");
            double  similarity = extractDouble(json, "similarity");
            String  message    = removeAccents(extractString(json, "message"));

            if (success) return FaceCompareResult.success(similarity, distance, message);
            else         return FaceCompareResult.failure(similarity, distance, message);
        } catch (Exception e) {
            return FaceCompareResult.error("Reponse Python invalide : " + json);
        }
    }

    private static boolean extractBool(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return false;
        String rest = json.substring(idx + key.length() + 2).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        return rest.startsWith("true");
    }

    private static double extractDouble(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0.0;
        String rest = json.substring(idx + key.length() + 2).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        StringBuilder sb = new StringBuilder();
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
        }
        return sb.length() > 0 ? Double.parseDouble(sb.toString()) : 0.0;
    }

    private static String extractString(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return "";
        String rest = json.substring(idx + key.length() + 2).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        if (!rest.startsWith("\"")) return "";
        rest = rest.substring(1);
        int end = rest.indexOf("\"");
        return end >= 0 ? rest.substring(0, end) : rest;
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }

    /** Supprime les accents pour éviter les problèmes d'encodage sur Windows */
    private static String removeAccents(String s) {
        if (s == null) return "";
        return s.replace('é','e').replace('è','e').replace('ê','e').replace('ë','e')
                .replace('à','a').replace('â','a').replace('ä','a')
                .replace('î','i').replace('ï','i')
                .replace('ô','o').replace('ö','o')
                .replace('ù','u').replace('û','u').replace('ü','u')
                .replace('ç','c')
                .replace('É','E').replace('È','E').replace('Ê','E')
                .replace('À','A').replace('Â','A')
                .replace('Î','I').replace('Ô','O')
                .replace('Ù','U').replace('Û','U').replace('Ç','C');
    }

    // ─── Modèle de résultat ───────────────────────────────────────────────────

    public static class FaceCompareResult {
        public enum Status { SUCCESS, FAILURE, ERROR }

        public final Status status;
        public final double similarity;
        public final double distance;
        public final String message;

        private FaceCompareResult(Status s, double sim, double dist, String msg) {
            this.status = s; this.similarity = sim; this.distance = dist; this.message = msg;
        }

        public boolean isSuccess() { return status == Status.SUCCESS; }

        static FaceCompareResult success(double sim, double dist, String msg) {
            return new FaceCompareResult(Status.SUCCESS, sim, dist, msg);
        }
        static FaceCompareResult failure(double sim, double dist, String msg) {
            return new FaceCompareResult(Status.FAILURE, sim, dist, msg);
        }
        static FaceCompareResult error(String msg) {
            return new FaceCompareResult(Status.ERROR, 0, -1, msg);
        }

        @Override
        public String toString() {
            return "FaceCompareResult{status=" + status
                    + ", similarity=" + similarity
                    + ", distance=" + distance
                    + ", message='" + message + "'}";
        }
    }
}