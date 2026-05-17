<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class PexelsService
{
    private const ENDPOINT_SEARCH  = 'https://api.pexels.com/v1/search';
    private const ENDPOINT_CURATED = 'https://api.pexels.com/v1/curated';

    private const CATEGORY_KEYWORDS = [
        'gestion_stress'           => 'meditation calm mindfulness',
        'confiance_en_soi'         => 'confidence success motivation person',
        'motivation'               => 'motivation goal achievement success',
        'communication'            => 'communication people talking meeting',
        'bien_etre'                => 'wellness nature peaceful yoga',
        'protectivite'             => 'focus productivity work desk',
        'intelligence_emotionnelle'=> 'empathy emotions psychology mind',
    ];

    private const CONTENT_KEYWORDS = [
        'Article' => 'reading book knowledge learning',
        'Video'   => 'video screen digital technology',
        'Podcast' => 'podcast microphone audio headphones',
        'PDF'     => 'document paper notes study',
    ];

    public function __construct(
        private HttpClientInterface $httpClient,
        private CacheInterface $cache,
        private string $apiKey
    ) {}

    /**
     * Recherche des photos par mot-clé.
     *
     * @param string $query   Mots-clés de recherche
     * @param int    $perPage Nombre de résultats (max 80)
     * @param string $size    Taille désirée : small | medium | large
     * @param string $locale   Code de langue (fr-FR, en-US…)
     * @param string $color    Filtrer par couleur
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    public function search(
        string $query,
        int    $perPage   = 6,
        string $size      = 'medium',
        string $locale    = 'fr-FR',
        string $color     = ''
    ): array {
        if (empty(trim($query))) {
            return [];
        }

        $cacheKey = 'pexels_' . md5($query . $perPage . $size . $locale . $color);

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($query, $perPage, $size, $locale, $color) {
            $item->expiresAfter(3600);

            $params = [
                'query'    => $query,
                'per_page' => min($perPage, 80),
                'locale'   => $locale,
            ];
            if (!empty($color)) {
                $params['color'] = $color;
            }

            try {
                $response = $this->httpClient->request('GET', self::ENDPOINT_SEARCH, [
                    'headers' => ['Authorization' => $this->apiKey],
                    'query'   => $params,
                    'timeout' => 8,
                ]);
                $data = $response->toArray();
                return $this->formatPhotos($data['photos'] ?? [], $size);
            } catch (\Exception) {
                return [];
            }
        });
    }

    /**
     * Photos pour une catégorie MindAura.
     *
     * @param string $categorie Code de la catégorie
     * @param int    $count     Nombre de photos
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    public function getImageForCategory(string $categorie, int $count = 3): array
    {
        $keyword = self::CATEGORY_KEYWORDS[$categorie] ?? 'personal development psychology';
        return $this->search($keyword, $count, 'medium');
    }

    /**
     * Photos pour un type de contenu.
     *
     * @param string $contenu 'Article', 'Video', 'Podcast', 'PDF'
     * @param int    $count   Nombre de photos
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    public function getImageForContentType(string $contenu, int $count = 3): array
    {
        $keyword = self::CONTENT_KEYWORDS[$contenu] ?? 'education learning';
        return $this->search($keyword, $count, 'medium');
    }

    /**
     * Suggestion d'images combinant catégorie et type de contenu.
     *
     * @param string $categorie Code de catégorie
     * @param string $contenu   Type de contenu
     * @param int    $count     Nombre de photos
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    public function suggestImageForRessource(
        string $categorie,
        string $contenu,
        int    $count = 6
    ): array {
        $catKeyword  = self::CATEGORY_KEYWORDS[$categorie]  ?? 'wellness';
        $typeKeyword = self::CONTENT_KEYWORDS[$contenu]     ?? 'learning';

        $results = $this->search("$catKeyword $typeKeyword", $count, 'medium');
        if (empty($results)) {
            $results = $this->search($catKeyword, $count, 'medium');
        }
        return $results;
    }

    /**
     * Photos curées (sélection éditoriale Pexels).
     *
     * @param int $perPage Nombre de photos (max 80)
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    public function getCurated(int $perPage = 6): array
    {
        $cacheKey = 'pexels_curated_' . $perPage;

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($perPage) {
            $item->expiresAfter(7200);
            try {
                $response = $this->httpClient->request('GET', self::ENDPOINT_CURATED, [
                    'headers' => ['Authorization' => $this->apiKey],
                    'query'   => ['per_page' => $perPage],
                    'timeout' => 8,
                ]);
                $data = $response->toArray();
                return $this->formatPhotos($data['photos'] ?? [], 'medium');
            } catch (\Exception) {
                return [];
            }
        });
    }

    /**
     * Récupère une photo par son ID.
     *
     * @param int $photoId ID Pexels
     *
     * @return null|array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }
     */
    public function getPhoto(int $photoId): ?array
    {
        $cacheKey = 'pexels_photo_' . $photoId;

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($photoId) {
            $item->expiresAfter(86400);
            try {
                $response = $this->httpClient->request('GET', "https://api.pexels.com/v1/photos/$photoId", [
                    'headers' => ['Authorization' => $this->apiKey],
                    'timeout' => 8,
                ]);
                $photo = $response->toArray();
                $formatted = $this->formatPhotos([$photo], 'medium');
                return $formatted[0] ?? null;
            } catch (\Exception) {
                return null;
            }
        });
    }

    /**
     * Formate un tableau de photos Pexels.
     *
     * @param array<int, array<string, mixed>> $photos Données brutes de photos
     * @param string                           $size   Taille désirée
     *
     * @return array<int, array{
     *     id: int,
     *     url: string,
     *     url_original: string|null,
     *     url_large: string|null,
     *     url_medium: string|null,
     *     url_small: string|null,
     *     url_tiny: string|null,
     *     photographer: string,
     *     photographer_url: string|null,
     *     alt: string,
     *     width: int,
     *     height: int,
     *     avg_color: string,
     *     credit: string,
     *     pexels_url: string|null
     * }>
     */
    private function formatPhotos(array $photos, string $size = 'medium'): array
    {
        return array_map(function (array $photo) use ($size) {
            return [
                'id'           => $photo['id'],
                'url'          => $this->getPhotoUrl($photo, $size),
                'url_original' => $photo['src']['original'] ?? null,
                'url_large'    => $photo['src']['large']    ?? null,
                'url_medium'   => $photo['src']['medium']   ?? null,
                'url_small'    => $photo['src']['small']    ?? null,
                'url_tiny'     => $photo['src']['tiny']     ?? null,
                'photographer' => $photo['photographer']    ?? 'Unknown',
                'photographer_url' => $photo['photographer_url'] ?? null,
                'alt'          => $photo['alt']             ?? '',
                'width'        => $photo['width']           ?? 0,
                'height'       => $photo['height']          ?? 0,
                'avg_color'    => $photo['avg_color']       ?? '#CCCCCC',
                'credit'       => 'Photo by ' . ($photo['photographer'] ?? 'Pexels') . ' on Pexels',
                'pexels_url'   => $photo['url'] ?? null,
            ];
        }, $photos);
    }

    /**
     * Extrait l'URL d'une photo selon la taille demandée.
     *
     * @param array<string, mixed> $photo Données de la photo
     * @param string               $size  Taille ('small', 'medium', 'large', 'original')
     *
     * @return string
     */
    private function getPhotoUrl(array $photo, string $size): string
    {
        return match($size) {
            'small'    => $photo['src']['small']    ?? $photo['src']['medium']   ?? '',
            'large'    => $photo['src']['large']    ?? $photo['src']['medium']   ?? '',
            'original' => $photo['src']['original'] ?? $photo['src']['large']    ?? '',
            default    => $photo['src']['medium']   ?? '',
        };
    }
}