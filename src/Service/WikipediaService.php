<?php
namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class WikipediaService
{
    // Mots-clés Wikipedia par catégorie MindAura
    private const CATEGORY_KEYWORDS = [
        'gestion_stress'            => 'Gestion du stress',
        'confiance_en_soi'          => 'Confiance en soi',
        'motivation'                => 'Motivation',
        'communication'             => 'Communication interpersonnelle',
        'bien_etre'                 => 'Bien-être',
        'protectivite'              => 'Protection psychologique',
        'intelligence_emotionnelle' => 'Intelligence émotionnelle',
    ];

    public function __construct(private HttpClientInterface $http) {}

    // ──────────────────────────────────────────────────────────
    // Résumé d'un article Wikipedia par titre exact
    // ──────────────────────────────────────────────────────────

    /**
     * FIX PHPStan: retour array<string, mixed>|null
     *
     * @return array<string, mixed>|null
     */
    public function getSummary(string $title, string $lang = 'fr'): ?array
    {
        $url = sprintf(
            'https://%s.wikipedia.org/api/rest_v1/page/summary/%s',
            $lang,
            rawurlencode(str_replace(' ', '_', $title))
        );

        try {
            $response = $this->http->request('GET', $url);

            if ($response->getStatusCode() !== 200) {
                return null;
            }

            $data = $response->toArray(false);

            return [
                'titre'       => $data['title']       ?? $title,
                'resume'      => $data['extract']      ?? '',
                'url'         => $data['content_urls']['desktop']['page'] ?? '',
                'image'       => $data['thumbnail']['source'] ?? null,
                'description' => $data['description'] ?? null,
            ];
        } catch (\Exception) {
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────
    // Recherche d'articles par mot-clé
    // ──────────────────────────────────────────────────────────

    /**
     * FIX PHPStan: retour array<int, array<string, mixed>>
     *
     * @return array<int, array<string, mixed>>
     */
    public function search(string $query, string $lang = 'fr', int $limit = 5): array
    {
        $url = sprintf('https://%s.wikipedia.org/w/api.php', $lang);

        try {
            $response = $this->http->request('GET', $url, [
                'query' => [
                    'action'      => 'query',
                    'list'        => 'search',
                    'srsearch'    => $query,
                    'srlimit'     => $limit,
                    'format'      => 'json',
                    'utf8'        => 1,
                ],
            ]);

            $data  = $response->toArray(false);
            $items = $data['query']['search'] ?? [];

            if (!is_array($items)) return [];

            $results = [];
            foreach ($items as $item) {
                if (!is_array($item)) continue;
                $results[] = [
                    'titre'   => is_string($item['title'] ?? null) ? $item['title'] : '',
                    'resume'  => strip_tags(is_string($item['snippet'] ?? null) ? $item['snippet'] : ''),
                    'url'     => sprintf('https://%s.wikipedia.org/wiki/%s', $lang, rawurlencode(is_string($item['title'] ?? null) ? str_replace(' ', '_', $item['title']) : '')),
                    'pageId'  => $item['pageid'] ?? null,
                ];
            }

            return $results;
        } catch (\Exception) {
            return [];
        }
    }

    // ──────────────────────────────────────────────────────────
    // Articles liés à une catégorie MindAura
    // ──────────────────────────────────────────────────────────

    /**
     * FIX PHPStan: retour array<int, array<string, mixed>>
     *
     * @return array<int, array<string, mixed>>
     */
    public function getForCategory(string $categorie, string $lang = 'fr'): array
    {
        $keyword = self::CATEGORY_KEYWORDS[$categorie] ?? $categorie;

        if (empty($keyword)) return [];

        return $this->search($keyword, $lang, 5);
    }

    // ──────────────────────────────────────────────────────────
    // Article Wikipedia le plus pertinent pour une ressource
    // ──────────────────────────────────────────────────────────

    /**
     * FIX PHPStan: retour array<string, mixed>|null
     *
     * @return array<string, mixed>|null
     */
    public function getForRessource(string $titre, ?string $categorie, string $lang = 'fr'): ?array
    {
        // 1. Essai titre exact
        $summary = $this->getSummary($titre, $lang);
        if ($summary) return $summary;

        // 2. Repli sur recherche par mot-clé catégorie (titre + catégorie)
        $keyword = $categorie ? (self::CATEGORY_KEYWORDS[$categorie] ?? $categorie) : '';
        
        $searchTerm = trim($titre . ' ' . $keyword);
        $results = $this->search($searchTerm, $lang, 1);

        if (!empty($results)) {
            $top = $results[0];
            $summary = $this->getSummary(is_string($top['titre']) ? $top['titre'] : '', $lang);
            if ($summary) return $summary;
        }

        // 3. Repli juste sur la catégorie si pas de résultats avec le titre
        if (!empty($keyword)) {
            $results = $this->search($keyword, $lang, 1);
            if (!empty($results)) {
                $top = $results[0];
                return $this->getSummary(is_string($top['titre']) ? $top['titre'] : '', $lang);
            }
        }

        // 4. Repli uniquement sur le titre si tout échoue
        $titleOnly = $this->search($titre, $lang, 1);
        if (!empty($titleOnly)) {
            $top = $titleOnly[0];
            return $this->getSummary(is_string($top['titre']) ? $top['titre'] : '', $lang);
        }

        return null;
    }
}