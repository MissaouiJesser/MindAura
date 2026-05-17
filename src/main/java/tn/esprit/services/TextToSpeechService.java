package tn.esprit.services;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.security.cert.X509Certificate;
import javazoom.jl.decoder.*;
import javazoom.jl.player.*;

public class TextToSpeechService {

    private static final String API_KEY = "be8dc243ced0484c95997a5a2e071202";
    private static volatile boolean estEnLecture = false; // ← flag global
    private static Thread playThread;

    private static HttpClient createUnsafeClient() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());
        return HttpClient.newBuilder().sslContext(sslContext).build();
    }

    public static void parler(String texte) {
        // ✅ Si déjà en lecture → on arrête et on ne relance pas
        if (estEnLecture) {
            System.out.println("⏹ Déjà en lecture, ignoré.");
            return;
        }

        estEnLecture = true;

        playThread = new Thread(() -> {
            try {
                String textEncode = java.net.URLEncoder.encode(texte, "UTF-8");
                String url = "https://api.voicerss.org/" +
                        "?key=" + API_KEY +
                        "&hl=fr-fr" +
                        "&src=" + textEncode +
                        "&c=MP3" +
                        "&f=44khz_16bit_stereo";

                System.out.println("📤 TTS request...");

                HttpClient client = createUnsafeClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url)).GET().build();

                HttpResponse<byte[]> response =
                        client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                byte[] mp3Data = response.body();

                String preview = new String(mp3Data, 0, Math.min(50, mp3Data.length));
                if (preview.startsWith("ERROR")) {
                    System.out.println("❌ VoiceRSS: " + preview);
                    estEnLecture = false;
                    return;
                }

                System.out.println("✅ Lecture démarrée...");

                // ✅ Lecture frame par frame avec vérification du flag
                InputStream is = new ByteArrayInputStream(mp3Data);
                Bitstream bitstream = new Bitstream(is);
                Decoder decoder = new Decoder();
                AudioDevice audioDevice = FactoryRegistry.systemRegistry().createAudioDevice();
                audioDevice.open(decoder);

                Header frame;
                while (estEnLecture && (frame = bitstream.readFrame()) != null) {
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frame, bitstream);
                    audioDevice.write(output.getBuffer(), 0, output.getBufferLength());
                    bitstream.closeFrame();
                }

                audioDevice.flush();
                audioDevice.close();
                bitstream.close();

                System.out.println("✅ Lecture terminée.");

            } catch (Exception e) {
                System.out.println("⏹ Lecture interrompue: " + e.getMessage());
            } finally {
                estEnLecture = false;
            }
        });

        playThread.setDaemon(true);
        playThread.start();
    }

    public static void arreter() {
        System.out.println("⏹ Arrêt demandé...");
        estEnLecture = false; // ← le thread s'arrête à la prochaine frame
        if (playThread != null) {
            playThread.interrupt();
            playThread = null;
        }
    }

    public static boolean isEnLecture() {
        return estEnLecture;
    }
}