<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * Analyse les émotions faciales à partir d'une image via l'API Face++ (Megvii).
 *
 * API utilisée : Face++ Detect API
 *   → Endpoint : https://api-us.faceplusplus.com/facepp/v3/detect
 *   → Attribut : return_attributes=emotion
 *   → Labels   : anger, disgust, fear, happiness, neutral, sadness, surprise
 *   → Gratuit  : compte Face++ avec API Key + API Secret
 *
 * Obtenir des clés gratuites :
 *   1. Créer un compte sur https://www.faceplusplus.com (gratuit)
 *   2. Aller dans Dashboard → Apps → Create App
 *   3. Copier API Key et API Secret
 *   4. Ajouter dans .env :
 *        FACEPP_API_KEY=votre_api_key
 *        FACEPP_API_SECRET=votre_api_secret
 */
class EmotionAnalysisService
{
    private const API_URL = 'https://api-us.faceplusplus.com/facepp/v3/detect';
    private const TIMEOUT = 30;

    // ── Mapping des labels Face++ → labels internes ───────────────────────
    // Face++ retourne : anger, disgust, fear, happiness, neutral, sadness, surprise
    // On normalise vers : angry, disgust, fear, happy, neutral, sad, surprise
    private const LABEL_MAP = [
        'anger'    => 'angry',
        'disgust'  => 'disgust',
        'fear'     => 'fear',
        'happiness'=> 'happy',
        'neutral'  => 'neutral',
        'sadness'  => 'sad',
        'surprise' => 'surprise',
    ];

    private const EMOJI_MAP = [
        'happy'    => '😊',
        'sad'      => '😢',
        'angry'    => '😠',
        'neutral'  => '😐',
        'surprise' => '😲',
        'fear'     => '😨',
        'disgust'  => '🤢',
    ];

    private const LABEL_FR = [
        'happy'    => 'Heureux',
        'sad'      => 'Triste',
        'angry'    => 'En colère',
        'neutral'  => 'Neutre',
        'surprise' => 'Surpris',
        'fear'     => 'Apeuré',
        'disgust'  => 'Dégoûté',
    ];

    private const COLOR_MAP = [
        'happy'    => ['bg' => '#D8F3DC', 'fg' => '#1B4332', 'accent' => '#52B788'],
        'sad'      => ['bg' => '#DBE9F4', 'fg' => '#1E3A5F', 'accent' => '#5B9BD5'],
        'angry'    => ['bg' => '#FDE2E2', 'fg' => '#8B1A1A', 'accent' => '#E74C3C'],
        'neutral'  => ['bg' => '#F0F4F8', 'fg' => '#5A6475', 'accent' => '#8896A6'],
        'surprise' => ['bg' => '#FFF3E0', 'fg' => '#8B4A00', 'accent' => '#F39C12'],
        'fear'     => ['bg' => '#EDE9F6', 'fg' => '#4A3D6E', 'accent' => '#7B5EA7'],
        'disgust'  => ['bg' => '#E8F0D9', 'fg' => '#4A5D1F', 'accent' => '#7D9A3B'],
    ];

    private const ADVICE = [
        'happy'    => 'Votre joie est une ressource précieuse. Notez ce qui vous rend heureux aujourd\'hui — ces moments sont la meilleure protection contre le stress futur.',
        'sad'      => 'La tristesse est une émotion légitime. Nos psychologues sont là pour vous accompagner — prendre rendez-vous peut être un premier pas apaisant.',
        'angry'    => 'La colère indique souvent un besoin non respecté. Essayez la respiration profonde (4 s inspire, 7 s retiens, 8 s expire) avant d\'agir.',
        'neutral'  => 'Un état stable est une excellente base pour la pleine conscience. Profitez-en pour explorer nos exercices de développement personnel.',
        'surprise' => 'Cette énergie nouvelle est précieuse. Canalisez-la vers un projet qui vous tient à cœur — la surprise est le début de la créativité.',
        'fear'     => 'La peur signale un besoin de sécurité. Parler à un coach ou un thérapeute peut transformer cette émotion en clarté et en action.',
        'disgust'  => 'Le rejet intérieur est un message important. Identifier ce que vous voulez écarter de votre vie est un acte puissant de soin de soi.',
    ];

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly LoggerInterface     $logger,
        private readonly string              $faceppApiKey,
        private readonly string              $faceppApiSecret,
    ) {}

    // ══════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Analyse une image encodée en base64 (data URI ou base64 brut).
     *
     * @return array{
     *   success: bool,
     *   dominant?: array{label:string, french:string, emoji:string, score:float, color:array<string,string>, advice:string},
     *   all?: array<int, array{label:string, french:string, emoji:string, score:float, color:array<string,string>}>,
     *   error?: string
     * }
     */
    public function analyze(string $base64Image): array
    {
        if (empty($this->faceppApiKey) || empty($this->faceppApiSecret)) {
            return ['success' => false, 'error' => 'Clés API Face++ manquantes. Configurez FACEPP_API_KEY et FACEPP_API_SECRET dans .env.'];
        }

        // ── Extraire les données binaires ────────────────────────────────
        $imageData = $this->decodeBase64($base64Image);
        if ($imageData === false) {
            return ['success' => false, 'error' => 'Image invalide ou corrompue.'];
        }

        if (strlen($imageData) > 2 * 1024 * 1024) {
            return ['success' => false, 'error' => 'L\'image est trop lourde (2 Mo maximum pour Face++).'];
        }

        // ── Appel Face++ via multipart/form-data ─────────────────────────
        try {
            // Symfony HttpClient ne gère pas nativement le multipart avec fichier binaire,
            // on utilise donc une requête cURL directe via stream_context / file_get_contents
            /** @var array{success: bool, dominant?: array{label:string, french:string, emoji:string, score:float, color:array<string,string>, advice:string}, all?: array<int, array{label:string, french:string, emoji:string, score:float, color:array<string,string>}>, error?: string} $result */
            $result = $this->callFaceppApi($imageData);
            return $result;

        } catch (\Throwable $e) {
            $this->logger->error('Face++ API exception', ['error' => $e->getMessage()]);
            return ['success' => false, 'error' => 'Erreur réseau : ' . $e->getMessage()];
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  APPEL API FACE++ (multipart/form-data avec image binaire)
    // ══════════════════════════════════════════════════════════════════════

    /** @return array<string, mixed> */
    private function callFaceppApi(string $imageData): array
    {
        $boundary = '----MindAuraBoundary' . bin2hex(random_bytes(8));

        $body = '';
        // api_key
        $body .= "--{$boundary}\r\n";
        $body .= "Content-Disposition: form-data; name=\"api_key\"\r\n\r\n";
        $body .= $this->faceppApiKey . "\r\n";
        // api_secret
        $body .= "--{$boundary}\r\n";
        $body .= "Content-Disposition: form-data; name=\"api_secret\"\r\n\r\n";
        $body .= $this->faceppApiSecret . "\r\n";
        // return_attributes
        $body .= "--{$boundary}\r\n";
        $body .= "Content-Disposition: form-data; name=\"return_attributes\"\r\n\r\n";
        $body .= "emotion\r\n";
        // image_base64  ← on envoie le base64 directement (plus simple que multipart file)
        $body .= "--{$boundary}\r\n";
        $body .= "Content-Disposition: form-data; name=\"image_base64\"\r\n\r\n";
        $body .= base64_encode($imageData) . "\r\n";
        $body .= "--{$boundary}--\r\n";

        $response = $this->httpClient->request('POST', self::API_URL, [
            'timeout' => self::TIMEOUT,
            'headers' => [
                'Content-Type' => 'multipart/form-data; boundary=' . $boundary,
            ],
            'body' => $body,
        ]);

        $status = $response->getStatusCode();
        $raw    = json_decode($response->getContent(false), true);

        if ($status !== 200) {
            $apiMsg = $raw['error_message'] ?? 'Erreur inconnue';
            $this->logger->error('Face++ API error', ['status' => $status, 'message' => $apiMsg]);
            return ['success' => false, 'error' => 'Erreur API Face++ (' . $status . ') : ' . $apiMsg];
        }

        if (empty($raw['faces'])) {
            return ['success' => false, 'error' => 'Aucun visage détecté. Assurez-vous que votre visage est bien visible et bien éclairé.'];
        }

        // On prend le premier visage détecté (le plus grand)
        $emotionRaw = $raw['faces'][0]['attributes']['emotion'] ?? null;
        if (!$emotionRaw) {
            return ['success' => false, 'error' => 'Impossible de lire les émotions du visage détecté.'];
        }

        $this->logger->info('Face++ emotion analysis success', [
            'face_count' => count($raw['faces']),
        ]);

        return ['success' => true] + $this->formatResults($emotionRaw);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════════════

    private function decodeBase64(string $input): string|false
    {
        if (preg_match('#^data:image/(\w+);base64,(.+)$#', $input, $matches)) {
            $input = $matches[2];
        }
        $decoded = base64_decode($input, true);
        if ($decoded === false || strlen($decoded) < 100) {
            return false;
        }
        return $decoded;
    }

    /**
     * Face++ retourne un objet plat : { "anger": 12.3, "happiness": 78.1, ... }
     * On le transforme en tableau trié par score.
     *
     * @param array<string, float|int|string> $emotionRaw
     * @return array<string, mixed>
     */
    private function formatResults(array $emotionRaw): array
    {
        // Normaliser les labels et convertir en tableau [{label, score}]
        $emotions = [];
        foreach ($emotionRaw as $faceppLabel => $score) {
            $label = self::LABEL_MAP[strtolower($faceppLabel)] ?? strtolower($faceppLabel);
            $emotions[] = ['label' => $label, 'score' => (float) $score];
        }

        // Trier par score décroissant
        usort($emotions, fn($a, $b) => $b['score'] <=> $a['score']);

        $top = $emotions[0];

        return [
            'dominant' => $this->formatEmotion($top['label'], $top['score'] / 100, withAdvice: true),
            'all'      => array_map(
                fn(array $e) => $this->formatEmotion($e['label'], $e['score'] / 100, withAdvice: false),
                array_slice($emotions, 0, 7)
            ),
        ];
    }

    /**
     * @return array<string, mixed>
     */
    private function formatEmotion(string $label, float $score, bool $withAdvice): array
    {
        $out = [
            'label'  => $label,
            'french' => self::LABEL_FR[$label]  ?? ucfirst($label),
            'emoji'  => self::EMOJI_MAP[$label] ?? '🤔',
            'score'  => round($score * 100, 1),
            'color'  => self::COLOR_MAP[$label] ?? self::COLOR_MAP['neutral'],
        ];

        if ($withAdvice) {
            $out['advice'] = self::ADVICE[$label] ?? self::ADVICE['neutral'];
        }

        return $out;
    }
}