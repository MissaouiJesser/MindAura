<?php
// src/Controller/NewsTestController.php

namespace App\Controller;

use App\Service\NewsApiService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/test-news', name: 'test_news_')]
class NewsTestController extends AbstractController
{
    public function __construct(
        private NewsApiService $newsApi
    ) {}

    #[Route('', name: 'index')]
    public function index(): JsonResponse
    {
        return $this->json([
            'routes_disponibles' => [
                '/test-news/latest'            => 'Toutes les news',
                '/test-news/psychologie'       => 'Psychologie',
                '/test-news/developpement'     => 'Développement personnel',
                '/test-news/theme/anxiete'     => 'Thème : Anxiété',
                '/test-news/theme/stress'      => 'Thème : Stress',
                '/test-news/theme/mindfulness' => 'Thème : Mindfulness',
                '/test-news/theme/depression'  => 'Thème : Dépression',
                '/test-news/theme/sommeil'     => 'Thème : Sommeil',
                '/test-news/theme/trauma'      => 'Thème : Trauma',
                '/test-news/theme/couple'      => 'Thème : Couple',
                '/test-news/trusted'           => 'Sources spécialisées',
                '/test-news/search/therapie'   => 'Recherche : thérapie',
            ]
        ]);
    }

    #[Route('/latest', name: 'latest')]
    public function latest(): JsonResponse
    {
        $result = $this->newsApi->getLatestNews();
        return $this->jsonSafe($this->formatDebug('Toutes les news', $result));
    }

    #[Route('/psychologie', name: 'psychologie')]
    public function psychologie(): JsonResponse
    {
        $result = $this->newsApi->getPsychologyNews();
        return $this->jsonSafe($this->formatDebug('Psychologie', $result));
    }

    #[Route('/developpement', name: 'developpement')]
    public function developpement(): JsonResponse
    {
        $result = $this->newsApi->getPersonalDevelopmentNews();
        return $this->jsonSafe($this->formatDebug('Développement Personnel', $result));
    }

    #[Route('/theme/{theme}', name: 'theme')]
    public function theme(string $theme): JsonResponse
    {
        $result = $this->newsApi->getNewsByTheme($theme);
        return $this->jsonSafe($this->formatDebug("Thème : {$theme}", $result));
    }

    #[Route('/trusted', name: 'trusted')]
    public function trusted(): JsonResponse
    {
        $result = $this->newsApi->getFromTrustedSources();
        return $this->jsonSafe($this->formatDebug('Sources spécialisées', $result));
    }

    #[Route('/search/{query}', name: 'search')]
    public function search(string $query): JsonResponse
    {
        $result = $this->newsApi->search($query);
        return $this->jsonSafe($this->formatDebug("Recherche : {$query}", $result));
    }

    // -------------------------------------------------------
    // 🛠️ Méthodes privées
    // -------------------------------------------------------

    /**
     * @param array<string, mixed> $result
     * @return array<string, mixed>
     */
    private function formatDebug(string $label, array $result): array
    {
        $articles = $result['articles'] ?? [];

        return [
            'section'      => $label,
            'totalResults' => $result['totalResults'] ?? 0,
            'articleCount' => count($articles),
            'error'        => $result['error'] ?? null,
            'articles'     => array_map(fn ($a) => [
                'title'       => $a['title'] ?? '',
                'source'      => $a['source']['name'] ?? '',
                'publishedAt' => $a['publishedAt'] ?? '',
                'url'         => $a['url'] ?? '',
                'description' => mb_substr($a['description'] ?? '', 0, 100) . '...',
            ], $articles),
        ];
    }

    /**
     * @param array<string, mixed> $data
     */
    private function jsonSafe(array $data): JsonResponse
    {
        return $this->json(
            $data,
            200,
            ['Content-Type' => 'application/json; charset=utf-8'],
            ['json_encode_options' => JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE]
        );
    }
}