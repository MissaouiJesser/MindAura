<?php

namespace App\Controller;

use App\Service\TraductionService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

class TraductionController extends AbstractController
{
    public function __construct(
        private readonly TraductionService $translationService
    ) {}

    /**
     * POST /api/translate
     *
     * Body JSON :
     * {
     *   "text":   "Hello world",
     *   "target": "fr",
     *   "source": "en"   // optionnel, défaut "auto"
     * }
     *
     * Réponse :
     * {
     *   "translated": "Bonjour le monde",
     *   "source":     "en",
     *   "target":     "fr",
     *   "original":   "Hello world"
     * }
     */
    #[Route('/api/translate', name: 'api_translate', methods: ['POST'])]
    public function translate(Request $request): JsonResponse
    {
        $body = json_decode($request->getContent(), true);

        // ── Validation ────────────────────────────────────────────────────────
        if (!is_array($body)) {
            return $this->json(['error' => 'Corps JSON invalide.'], 400);
        }

        $text   = trim((string) ($body['text']   ?? ''));
        $target = trim((string) ($body['target'] ?? ''));
        $source = trim((string) ($body['source'] ?? 'auto'));

        if ($text === '') {
            return $this->json(['error' => 'Le champ "text" est requis.'], 422);
        }

        if ($target === '') {
            return $this->json(['error' => 'Le champ "target" est requis (ex: fr, en, ar).'], 422);
        }

        // ── Traduction ────────────────────────────────────────────────────────
        try {
            $result = $this->translationService->translate($text, $target, $source);
        } catch (\RuntimeException $e) {
            return $this->json(['error' => $e->getMessage()], 502);
        }

        return $this->json($result);
    }

    /**
     * GET /api/translate/languages
     * Retourne les langues supportées.
     */
    #[Route('/api/translate/languages', name: 'api_translate_languages', methods: ['GET'])]
    public function languages(): JsonResponse
    {
        return $this->json([
            'languages' => [
                ['code' => 'fr', 'name' => 'Français'],
                ['code' => 'en', 'name' => 'Anglais'],
                ['code' => 'ar', 'name' => 'Arabe'],
                ['code' => 'de', 'name' => 'Allemand'],
                ['code' => 'es', 'name' => 'Espagnol'],
                ['code' => 'it', 'name' => 'Italien'],
                ['code' => 'pt', 'name' => 'Portugais'],
                ['code' => 'nl', 'name' => 'Néerlandais'],
                ['code' => 'ru', 'name' => 'Russe'],
                ['code' => 'zh', 'name' => 'Chinois'],
                ['code' => 'ja', 'name' => 'Japonais'],
                ['code' => 'ko', 'name' => 'Coréen'],
                ['code' => 'tr', 'name' => 'Turc'],
            ],
        ]);
    }
}