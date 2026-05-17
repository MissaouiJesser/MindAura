<?php

namespace App\Controller;

use App\Service\EmotionAnalysisService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Endpoint AJAX pour l'analyse d'émotions faciales via IA.
 *
 * Route :
 *   POST /emotion/analyze  → reçoit une image base64, retourne les émotions détectées
 *
 * Appelé depuis la section « Analyse des sentiments » de home.html.twig.
 * Requiert ROLE_USER (utilisateur connecté).
 */
#[IsGranted('ROLE_USER')]
class EmotionController extends AbstractController
{
    #[Route('/emotion/analyze', name: 'app_emotion_analyze', methods: ['POST'])]
    public function analyze(
        Request                $request,
        EmotionAnalysisService $emotionService
    ): JsonResponse {
        // ── Lire le body JSON ─────────────────────────────────────────
        $data = json_decode($request->getContent(), true);

        if (!is_array($data) || empty($data['image'])) {
            return $this->json([
                'success' => false,
                'error'   => 'Image manquante dans la requête.',
            ], 400);
        }

        $image = trim((string) $data['image']);

        if (!str_starts_with($image, 'data:image/') && !preg_match('/^[A-Za-z0-9+\/=]+$/', $image)) {
            return $this->json([
                'success' => false,
                'error'   => 'Format d\'image invalide (attendu : base64 ou data URI).',
            ], 400);
        }

        // ── Déléguer au service ──────────────────────────────────────
        $result = $emotionService->analyze($image);

        if (!$result['success']) {
            // 502 Bad Gateway : erreur côté API externe
            return $this->json($result, 502);
        }

        return $this->json($result);
    }
}
