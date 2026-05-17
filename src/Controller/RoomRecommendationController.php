<?php
// src/Controller/RoomRecommendationController.php

namespace App\Controller;

use App\Repository\SalleRepository;
use App\Repository\LocalsPsychiatrieRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * ══════════════════════════════════════════════════════════
 *  API — Recommandation IA de salle
 *
 *  GET  /api/recommend-room  → scoring PHP (sans clé)
 *  POST /api/explain-room    → explication OpenRouter (clé backend)
 * ══════════════════════════════════════════════════════════
 */
#[Route('/api', name: 'api_')]
class RoomRecommendationController extends AbstractController
{
    public function __construct(
        private readonly string $openRouterApiKey
    ) {}

    // ── Mapping type de séance → types de salle favoris ─────────────────
    private const SESSION_TYPE_MAP = [
        'individuelle'  => ['Salle de consultation', 'Salle de thérapie'],
        'groupe'        => ['Salle de groupe', 'Salle de relaxation'],
        'relaxation'    => ['Salle de relaxation', "Salle d'attente"],
        'consultation'  => ['Salle de consultation', 'Salle de thérapie'],
        'therapie'      => ['Salle de thérapie', 'Salle de consultation'],
    ];

    private const BRUIT_SCORE   = ['faible' => 25, 'moyen' => 15, 'eleve' => 5];
    private const CONFORT_SCORE = ['eleve' => 20, 'moyen' => 12, 'faible' => 4];

    // Modèles essayés en cascade pour explain-room
    private const EXPLAIN_MODELS = [
        'meta-llama/llama-3.1-8b-instruct:free',
        'meta-llama/llama-3.3-70b-instruct:free',
        'google/gemma-3-27b-it:free',
    ];

    // ══════════════════════════════════════════════════════════════════════
    //  POST /api/explain-room
    //  Body JSON : { "salle": {...}, "criteria": {...} }
    //  Appelle OpenRouter côté serveur → clé jamais exposée au navigateur
    // ══════════════════════════════════════════════════════════════════════
    #[Route('/explain-room', name: 'explain_room', methods: ['POST'])]
    public function explainRoom(
        Request $request,
        HttpClientInterface $httpClient
    ): JsonResponse {
        $body     = json_decode($request->getContent(), true);
        $salle    = $body['salle']    ?? [];
        $criteria = $body['criteria'] ?? [];

        if (empty($salle) || empty($criteria)) {
            return $this->json(['explication' => '', 'debug' => 'Données manquantes'], 400);
        }

        $nomSalle    = $salle['nom']           ?? 'cette salle';
        $typeSalle   = $salle['type']          ?? 'Non précisé';
        $capacite    = $salle['capacite']      ?? '—';
        $etage       = $salle['etage']         ?? 'Non précisé';
        $equipements = $salle['equipements']   ?? 'Non précisés';
        $bruit       = $salle['bruit_label']   ?? '—';
        $confort     = $salle['confort_label'] ?? '—';
        $score       = $salle['score']         ?? '—';
        $localNom    = $salle['local_nom']     ?? '—';

        $typeSeance  = $criteria['type_seance'] ?? 'individuelle';
        $capaciteDem = $criteria['capacite']    ?? 1;
        $bruitDem    = $criteria['bruit']       ?? 'faible';
        $confortDem  = $criteria['confort']     ?? 'eleve';

        $prompt = "Tu es un assistant clinique pour la plateforme MindAura (santé mentale). "
            . "Explique en 2-3 phrases courtes et professionnelles, en français, pourquoi la salle suivante est recommandée pour cette séance. "
            . "Sois précis, bienveillant et mentionne au moins un critère concret.\n\n"
            . "Salle : $nomSalle ($typeSalle) — Local : $localNom\n"
            . "Capacité : $capacite personnes | Étage : $etage | Équipements : $equipements\n"
            . "Niveau sonore simulé : $bruit | Confort simulé : $confort | Score : $score/100\n\n"
            . "Séance demandée : $typeSeance pour $capaciteDem personne(s), bruit souhaité : $bruitDem, confort souhaité : $confortDem.\n\n"
            . "Réponds UNIQUEMENT avec le texte de l'explication, sans titre, sans markdown.";

        $lastError = '';

        foreach (self::EXPLAIN_MODELS as $model) {
            try {
                $response   = $httpClient->request('POST', 'https://openrouter.ai/api/v1/chat/completions', [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->openRouterApiKey,
                        'Content-Type'  => 'application/json',
                        'HTTP-Referer'  => 'https://mindaura.app',
                        'X-Title'       => 'MindAura AI Recommandation',
                    ],
                    'json' => [
                        'model'      => $model,
                        'max_tokens' => 150,
                        'messages'   => [['role' => 'user', 'content' => $prompt]],
                    ],
                    'timeout' => 20,
                ]);

                $statusCode = $response->getStatusCode();
                // getContent(false) ne lance PAS d'exception sur les erreurs HTTP
                $raw  = $response->getContent(false);
                $data = json_decode($raw, true);

                if ($statusCode === 200) {
                    $explication = $data['choices'][0]['message']['content'] ?? '';
                    if (!empty(trim($explication))) {
                        return $this->json(['explication' => trim($explication)]);
                    }
                    $lastError = "Réponse vide du modèle $model";
                    continue;
                }

                // 429 = rate limit, 503 = indisponible → on essaie le suivant
                $lastError = "HTTP $statusCode sur $model : " . ($data['error']['message'] ?? $raw);
                if (in_array($statusCode, [429, 503, 404], true)) {
                    continue;
                }

                // Autre erreur (401, 400…) → inutile de continuer
                break;

            } catch (\Throwable $e) {
                $lastError = $e->getMessage();
                continue;
            }
        }

        // Tous les modèles ont échoué → fallback contextuel (pas de message générique)
        $fallbacks = [
            "La salle $nomSalle, de type $typeSalle, offre une capacité adaptée à votre séance de $typeSeance pour $capaciteDem personne(s). Son niveau sonore $bruit et son confort $confort en font un espace bien adapté à vos besoins.",
            "$nomSalle est une $typeSalle située dans le local $localNom. Avec un score de compatibilité de $score/100, elle répond efficacement à vos critères de séance $typeSeance.",
            "Avec ses équipements ($equipements) et son environnement $bruit, la salle $nomSalle constitue un cadre thérapeutique de qualité pour votre séance de $typeSeance.",
        ];

        // Choisir le fallback selon l'id de salle pour que chaque carte affiche un texte différent
        $idx = abs(crc32($nomSalle . $localNom)) % count($fallbacks);

        return $this->json([
            'explication' => $fallbacks[$idx],
            'debug'       => $lastError,   // visible dans les DevTools du navigateur si besoin
        ]);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GET /api/recommend-room
    // ══════════════════════════════════════════════════════════════════════
    #[Route('/recommend-room', name: 'recommend_room', methods: ['GET'])]
    public function recommend(
        Request $request,
        SalleRepository $salleRepo,
        LocalsPsychiatrieRepository $localRepo
    ): JsonResponse {
        $typeSeance    = strtolower(trim((string) $request->query->get('type',     'individuelle')));
        $capaciteDem   = (int) $request->query->get('capacite', 1);
        $niveauBruit   = strtolower(trim((string) $request->query->get('bruit',   'faible')));
        $niveauConfort = strtolower(trim((string) $request->query->get('confort', 'eleve')));
        $localId       = $request->query->get('local');

        $salles = $localId
            ? $salleRepo->findByLocal((int) $localId)
            : $salleRepo->findDisponibles();

        if (empty($salles)) {
            return $this->json([
                'success'         => false,
                'message'         => 'Aucune salle disponible pour les critères sélectionnés.',
                'recommendations' => [],
            ]);
        }

        $typesFavoris = self::SESSION_TYPE_MAP[$typeSeance]
            ?? ['Salle de consultation', 'Salle de thérapie'];

        $scored = [];
        foreach ($salles as $salle) {
            $score   = 0;
            $details = [];

            // 1) Type séance → /30
            $typeScore = 0;
            if (in_array($salle->getTypeSalle(), $typesFavoris, true)) {
                $typeScore = ($salle->getTypeSalle() === $typesFavoris[0]) ? 30 : 20;
            } else {
                $typeScore = 5;
            }
            $score += $typeScore;
            $details['type_score'] = $typeScore;

            // 2) Capacité → /25
            $cap      = (int) $salle->getCapaciteSalle();
            $capScore = 0;
            if ($cap >= $capaciteDem) {
                $ratio    = $cap / max(1, $capaciteDem);
                $capScore = $ratio <= 1.5 ? 25 : ($ratio <= 3 ? 18 : 10);
            }
            $score += $capScore;
            $details['capacite_score'] = $capScore;

            // 3) Bruit → /25
            $bruitSimule = $this->simulerBruit($salle->getTypeSalle(), $salle->getEquipements());
            $bruitScore  = $this->scorerBruit($niveauBruit, $bruitSimule);
            $score += $bruitScore;
            $details['bruit_score']  = $bruitScore;
            $details['bruit_simule'] = $bruitSimule;

            // 4) Confort → /20
            $confortSimule = $this->simulerConfort($salle->getEtage(), $salle->getEquipements());
            $confortScore  = $this->scorerConfort($niveauConfort, $confortSimule);
            $score += $confortScore;
            $details['confort_score']   = $confortScore;
            $details['confort_simule']  = $confortSimule;

            if ($salle->getDisponibiliteSalle() === 'Disponible')  $score += 5;
            if ($salle->getStatutSalle() === 'En rénovation')      $score -= 15;

            if ($cap >= $capaciteDem) {
                $scored[] = [
                    'salle'   => $salle,
                    'score'   => max(0, min(100, $score)),
                    'details' => $details,
                ];
            }
        }

        if (empty($scored)) {
            return $this->json([
                'success'         => false,
                'message'         => 'Aucune salle ne correspond à la capacité demandée.',
                'recommendations' => [],
            ]);
        }

        usort($scored, fn($a, $b) => $b['score'] <=> $a['score']);

        $top = array_slice($scored, 0, 3);

        $recommendations = array_map(function ($item, $rank) {
            /** @var \App\Entity\Salle $s */
            $s = $item['salle'];
            return [
                'rank'          => $rank + 1,
                'id'            => $s->getIdSalle(),
                'nom'           => $s->getNomSalle(),
                'type'          => $s->getTypeSalle(),
                'capacite'      => $s->getCapaciteSalle(),
                'etage'         => $s->getEtage(),
                'equipements'   => $s->getEquipements(),
                'disponibilite' => $s->getDisponibiliteSalle(),
                'statut'        => $s->getStatutSalle(),
                'image'         => $s->getImageURL(),
                'local_id'      => $s->getLocal()?->getIdLocal(),
                'local_nom'     => $s->getLocal()?->getNomLocal(),
                'local_ville'   => $s->getLocal()?->getVilleLocal(),
                'local_adresse' => $s->getLocal()?->getAdresseLocal(),
                'score'         => $item['score'],
                'score_details' => $item['details'],
                'bruit_label'   => $item['details']['bruit_simule'],
                'confort_label' => $item['details']['confort_simule'],
                'is_best'       => ($rank === 0),
            ];
        }, $top, array_keys($top));

        return $this->json([
            'success'         => true,
            'total_analysed'  => count($scored),
            'criteria'        => [
                'type_seance' => $typeSeance,
                'capacite'    => $capaciteDem,
                'bruit'       => $niveauBruit,
                'confort'     => $niveauConfort,
            ],
            'recommendations' => $recommendations,
        ]);
    }

    // ── Helpers privés ────────────────────────────────────────────────────

    private function simulerBruit(?string $type, ?string $equipements): string
    {
        $typeLower = strtolower((string) $type);
        $equip     = strtolower((string) $equipements);

        if (str_contains($typeLower, 'relaxation') || str_contains($typeLower, 'thérapie')) return 'faible';
        if (str_contains($typeLower, 'groupe') || str_contains($equip, 'sono') || str_contains($equip, 'micro')) return 'eleve';
        return 'moyen';
    }

    private function simulerConfort(?string $etage, ?string $equipements): string
    {
        $equip = strtolower((string) $equipements);
        $pts   = 0;
        foreach (['climatisation', 'chauffage', 'wifi', 'lumière', 'fauteuil', 'canapé', 'tapis'] as $kw) {
            if (str_contains($equip, $kw)) $pts++;
        }
        if ($etage === 'RDC') $pts++;
        return $pts >= 3 ? 'eleve' : ($pts >= 1 ? 'moyen' : 'faible');
    }

    private function scorerBruit(string $demande, string $simule): int
    {
        $map  = ['faible' => 1, 'moyen' => 2, 'eleve' => 3];
        $diff = abs(($map[$demande] ?? 2) - ($map[$simule] ?? 2));
        return self::BRUIT_SCORE[['faible', 'moyen', 'eleve'][$diff]];
    }

    private function scorerConfort(string $demande, string $simule): int
    {
        $map  = ['faible' => 1, 'moyen' => 2, 'eleve' => 3];
        $diff = abs(($map[$demande] ?? 2) - ($map[$simule] ?? 2));
        return self::CONFORT_SCORE[['eleve', 'moyen', 'faible'][$diff]];
    }
}