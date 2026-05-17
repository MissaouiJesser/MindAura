<?php
// src/Service/NewsApiService.php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class NewsApiService
{
    private const PERSONAL_DEVELOPMENT_KEYWORDS = [
        'développement personnel',
        'confiance en soi',
        'motivation',
        'mindfulness',
        'méditation',
        'pleine conscience',
        'résilience',
        'habitudes',
        'productivité',
        'croissance personnelle',
        'intelligence émotionnelle',
        'coaching',
        'PNL',
        'objectifs de vie',
    ];

    private const TRUSTED_DOMAINS = [
        'psychologies.com',
        'cerveau-et-psycho.fr',
        'santepsychique.fr',
        'lepsychiatre.com',
        'passeportsante.net',
        'futura-sciences.com',
    ];

    public function __construct(
        private HttpClientInterface $client,
        private CacheInterface $cache,
        private string $apiKey,
        private string $apiUrl,
    ) {}

    // -------------------------------------------------------
    // 🔥 Toutes les news
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function getLatestNews(int $page = 1, int $pageSize = 12): array
    {
        $cacheKey = "news_latest_{$page}_{$pageSize}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($page, $pageSize) {
            $item->expiresAfter(1800);

            $query = '(psychologie OR "santé mentale" OR "développement personnel" OR mindfulness OR résilience) AND (thérapie OR bien-être OR émotions OR motivation OR coaching)';

            return $this->fetchEverything($query, $page, $pageSize);
        });
    }

    // -------------------------------------------------------
    // 🧠 Psychologie
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function getPsychologyNews(int $page = 1, int $pageSize = 12): array
    {
        $cacheKey = "news_psychology_{$page}_{$pageSize}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($page, $pageSize) {
            $item->expiresAfter(1800);

            $query = 'psychologie OR "santé mentale" OR psychothérapie OR psychanalyse OR anxiété OR dépression OR burnout OR trauma OR "bien-être mental" OR "thérapie"';

            return $this->fetchEverything($query, $page, $pageSize);
        });
    }

    // -------------------------------------------------------
    // 🌱 Développement Personnel
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function getPersonalDevelopmentNews(int $page = 1, int $pageSize = 12): array
    {
        $cacheKey = "news_personal_dev_{$page}_{$pageSize}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($page, $pageSize) {
            $item->expiresAfter(1800);
            $keywords = $this->buildKeywords(self::PERSONAL_DEVELOPMENT_KEYWORDS);
            return $this->fetchEverything($keywords, $page, $pageSize);
        });
    }

    // -------------------------------------------------------
    // 🔍 Recherche libre
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function search(string $userQuery, int $page = 1, int $pageSize = 12): array
    {
        $enrichedQuery = "({$userQuery}) AND (psychologie OR \"santé mentale\" OR \"développement personnel\")";
        $cacheKey = 'news_search_' . md5($userQuery) . "_{$page}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($enrichedQuery, $page, $pageSize) {
            $item->expiresAfter(900);
            return $this->fetchEverything($enrichedQuery, $page, $pageSize);
        });
    }

    // -------------------------------------------------------
    // 🏷️ Par thème
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function getNewsByTheme(string $theme, int $page = 1, int $pageSize = 12): array
    {
        $themes = [
            'anxiete'       => 'anxiété OR "trouble anxieux" OR "attaque de panique"',
            'depression'    => 'dépression OR "trouble dépressif" OR "humeur dépressive"',
            'stress'        => 'stress OR burnout OR "épuisement professionnel"',
            'mindfulness'   => 'mindfulness OR méditation OR "pleine conscience"',
            'relations'     => '"relations humaines" OR communication OR empathie OR "intelligence émotionnelle"',
            'estime-de-soi' => '"estime de soi" OR "confiance en soi" OR "image de soi"',
            'sommeil'       => 'sommeil OR insomnie OR "qualité du sommeil"',
            'trauma'        => 'trauma OR traumatisme OR PTSD OR "stress post-traumatique"',
            'enfant'        => '"psychologie enfant" OR "développement enfant" OR parentalité',
            'couple'        => '"psychologie couple" OR "thérapie de couple" OR "vie amoureuse"',
        ];

        $query    = $themes[$theme] ?? $theme;
        $cacheKey = "news_theme_{$theme}_{$page}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($query, $page, $pageSize) {
            $item->expiresAfter(1800);
            return $this->fetchEverything($query, $page, $pageSize);
        });
    }

    // -------------------------------------------------------
    // 📰 Sources spécialisées
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    public function getFromTrustedSources(int $page = 1, int $pageSize = 12): array
    {
        $cacheKey = "news_trusted_{$page}";

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($page, $pageSize) {
            $item->expiresAfter(3600);

            $response = $this->client->request('GET', $this->apiUrl . '/everything', [
                'query' => [
                    'apiKey'   => $this->apiKey,
                    'domains'  => implode(',', self::TRUSTED_DOMAINS),
                    'language' => 'fr',
                    'sortBy'   => 'publishedAt',
                    'pageSize' => $pageSize,
                    'page'     => $page,
                ],
            ]);

            return $this->formatResponse($response->toArray());
        });
    }

    // -------------------------------------------------------
    // 📋 Thèmes disponibles
    // -------------------------------------------------------

    /**
     * @return array<string, string>
     */
    public function getAvailableThemes(): array
    {
        return [
            'anxiete'       => '😰 Anxiété',
            'depression'    => '😔 Dépression',
            'stress'        => '😤 Stress & Burnout',
            'mindfulness'   => '🧘 Mindfulness',
            'relations'     => '🤝 Relations humaines',
            'estime-de-soi' => '💪 Estime de soi',
            'sommeil'       => '😴 Sommeil',
            'trauma'        => '💔 Trauma',
            'enfant'        => '👶 Psychologie enfant',
            'couple'        => '❤️ Couple',
        ];
    }

    // -------------------------------------------------------
    // 🛠️ Méthodes privées
    // -------------------------------------------------------

    /**
     * @return array<string, mixed>
     */
    private function fetchEverything(string $query, int $page, int $pageSize): array
    {
        try {
            $response = $this->client->request('GET', $this->apiUrl . '/everything', [
                'query' => [
                    'apiKey'   => $this->apiKey,
                    'q'        => $query,
                    'searchIn' => 'title',
                    'language' => 'fr',
                    'sortBy'   => 'publishedAt',
                    'pageSize' => $pageSize,
                    'page'     => $page,
                ],
            ]);

            return $this->formatResponse($response->toArray());

        } catch (\Exception $e) {
            return [
                'articles'     => [],
                'totalResults' => 0,
                'error'        => $e->getMessage(),
            ];
        }
    }

    /**
     * @param array<string, mixed> $data
     * @return array<string, mixed>
     */
    private function formatResponse(array $data): array
    {
        $articles = array_filter(
            $data['articles'] ?? [],
            fn($article) => $article['title'] !== '[Removed]' && !empty($article['description'])
        );

        $cleaned = array_map(fn($article) => $this->cleanArticle($article), $articles);

        return [
            'articles'     => array_values($cleaned),
            'totalResults' => $data['totalResults'] ?? 0,
        ];
    }

    /**
     * @param array<string, mixed> $article
     * @return array<string, mixed>
     */
    private function cleanArticle(array $article): array
    {
        array_walk_recursive($article, function (mixed &$value): void {
            if (is_string($value)) {
                $value = mb_convert_encoding($value, 'UTF-8', 'UTF-8');
                $value = preg_replace('/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/u', '', $value) ?? $value;
            }
        });

        return $article;
    }

    /**
     * @param array<int|string, string> $keywords
     */
    private function buildKeywords(array $keywords): string
    {
        return implode(' OR ', array_map(
            fn($kw) => str_contains($kw, ' ') ? "\"{$kw}\"" : $kw,
            $keywords
        ));
    }
}