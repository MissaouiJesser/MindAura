<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class OpenLibraryService
{
    private const SEARCH_URL  = 'https://openlibrary.org/search.json';
    private const COVER_URL   = 'https://covers.openlibrary.org/b/id/%s-M.jpg';
    private const BOOK_URL    = 'https://openlibrary.org%s';

    private const CATEGORY_QUERIES = [
        'gestion_stress'            => 'gestion stress anxiete psychologie',
        'confiance_en_soi'          => 'confiance soi estime personnelle',
        'motivation'                => 'motivation psychologie positive reussite',
        'communication'             => 'communication interpersonnelle assertivite',
        'bien_etre'                 => 'bien-etre bonheur pleine conscience mindfulness',
        'protectivite'              => 'productivite gestion temps efficacite',
        'intelligence_emotionnelle' => 'intelligence emotionnelle empathie emotions',
    ];

    public function __construct(
        private HttpClientInterface $httpClient,
        private CacheInterface      $cache
    ) {}

    /**
     * Recherche des livres par mot-clé.
     *
     * @param string      $query   Texte de recherche
     * @param string|null $lang    Code langue ('fre', 'eng' ou null)
     * @param int         $limit   Nombre max de résultats (1‑20)
     * @param string|null $subject Filtre par sujet (ex: 'psychology')
     *
     * @return array<int, array{
     *     key: string,
     *     titre: string,
     *     auteurs: array<int, string>,
     *     auteur: string,
     *     annee: int|null,
     *     pages: int|null,
     *     couverture: string|null,
     *     url: string|null,
     *     note: float|null,
     *     langues: array<int, string>,
     *     sujets: array<int, string>
     * }>
     */
    public function search(
        string  $query,
        ?string $lang  = 'fre',
        int     $limit = 6,
        ?string $subject = null
    ): array {
        $cacheKey = 'openlibrary_' . md5($query . $lang . $limit . $subject);

        try {
            return $this->cache->get($cacheKey, function (ItemInterface $item) use ($query, $lang, $limit, $subject) {
                $item->expiresAfter(7200);

                $params = [
                    'q'       => $query,
                    'limit'   => min($limit, 20),
                    'fields'  => 'key,title,author_name,cover_i,first_publish_year,number_of_pages_median,language,subject,ratings_average',
                ];

                if ($lang) {
                    $params['language'] = $lang;
                }
                if ($subject) {
                    $params['subject'] = $subject;
                }

                $response = $this->httpClient->request('GET', self::SEARCH_URL, [
                    'query'   => $params,
                    'headers' => [
                        'User-Agent' => 'MindAura/1.0 (https://mindaura.example.com)',
                        'Accept'     => 'application/json',
                    ],
                    'timeout' => 10,
                ]);

                if ($response->getStatusCode() !== 200) {
                    return [];
                }

                $data  = $response->toArray();
                $books = $data['docs'] ?? [];

                $formatted = array_filter(array_map([$this, 'formatBook'], $books));
                return array_values($formatted);
            });
        } catch (\Exception) {
            return [];
        }
    }

    /**
     * Livres recommandés par catégorie MindAura.
     *
     * @param string $categorie Code de la catégorie (ex: 'gestion_stress')
     * @param int    $limit     Nombre de livres à retourner
     *
     * @return array<int, array{
     *     key: string,
     *     titre: string,
     *     auteurs: array<int, string>,
     *     auteur: string,
     *     annee: int|null,
     *     pages: int|null,
     *     couverture: string|null,
     *     url: string|null,
     *     note: float|null,
     *     langues: array<int, string>,
     *     sujets: array<int, string>
     * }>
     */
    public function getForCategory(string $categorie, int $limit = 4): array
    {
        $query = self::CATEGORY_QUERIES[$categorie] ?? 'développement personnel psychologie';

        $books = $this->search($query, 'fre', $limit);

        if (count($books) < 2) {
            $enQuery = $this->translateQueryToEn($categorie);
            $enBooks = $this->search($enQuery, 'eng', $limit);
            $books   = array_merge($books, $enBooks);
        }

        usort($books, fn($a, $b) => ($b['note'] ?? 0) <=> ($a['note'] ?? 0));
        return array_slice($books, 0, $limit);
    }

    /**
     * Livres recommandés pour une ressource (titre + catégorie).
     *
     * @param string $titre     Titre de la ressource
     * @param string $categorie Catégorie MindAura
     * @param int    $limit     Nombre de livres à retourner
     *
     * @return array<int, array{
     *     key: string,
     *     titre: string,
     *     auteurs: array<int, string>,
     *     auteur: string,
     *     annee: int|null,
     *     pages: int|null,
     *     couverture: string|null,
     *     url: string|null,
     *     note: float|null,
     *     langues: array<int, string>,
     *     sujets: array<int, string>
     * }>
     */
    public function getForRessource(string $titre, string $categorie, int $limit = 3): array
    {
        $byTitle    = $this->search($titre, null, $limit);
        $byCategory = $this->getForCategory($categorie, $limit);

        $seen  = [];
        $books = [];

        foreach (array_merge($byTitle, $byCategory) as $book) {
            if (!isset($seen[$book['key']])) {
                $seen[$book['key']] = true;
                $books[] = $book;
            }
            if (count($books) >= $limit) {
                break;
            }
        }

        return $books;
    }

    /**
     * Formate un document Open Library en tableau uniforme.
     *
     * @param array<string, mixed> $doc Document brut de l'API Open Library
     *
     * @return null|array{
     *     key: string,
     *     titre: string,
     *     auteurs: array<int, string>,
     *     auteur: string,
     *     annee: int|null,
     *     pages: int|null,
     *     couverture: string|null,
     *     url: string|null,
     *     note: float|null,
     *     langues: array<int, string>,
     *     sujets: array<int, string>
     * }
     */
    private function formatBook(array $doc): ?array
    {
        $titre = $doc['title'] ?? null;
        if (empty($titre)) {
            return null;
        }

        $coverId = $doc['cover_i'] ?? null;
        $key     = $doc['key']    ?? null;

        return [
            'key'          => $key,
            'titre'        => $titre,
            'auteurs'      => $doc['author_name'] ?? [],
            'auteur'       => isset($doc['author_name'][0]) ? $doc['author_name'][0] : 'Auteur inconnu',
            'annee'        => $doc['first_publish_year'] ?? null,
            'pages'        => $doc['number_of_pages_median'] ?? null,
            'couverture'   => $coverId ? sprintf(self::COVER_URL, $coverId) : null,
            'url'          => $key ? sprintf(self::BOOK_URL, $key) : null,
            'note'         => isset($doc['ratings_average']) ? round((float)$doc['ratings_average'], 1) : null,
            'langues'      => $doc['language'] ?? [],
            'sujets'       => array_slice($doc['subject'] ?? [], 0, 5),
        ];
    }

    private function translateQueryToEn(string $categorie): string
    {
        $map = [
            'gestion_stress'            => 'stress management anxiety psychology',
            'confiance_en_soi'          => 'self confidence self esteem',
            'motivation'                => 'motivation psychology positive thinking',
            'communication'             => 'interpersonal communication assertiveness',
            'bien_etre'                 => 'wellbeing happiness mindfulness',
            'protectivite'              => 'productivity time management',
            'intelligence_emotionnelle' => 'emotional intelligence empathy',
        ];
        return $map[$categorie] ?? 'personal development psychology';
    }
}