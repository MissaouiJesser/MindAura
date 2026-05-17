<?php
namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class YoutubeService
{
    public function __construct(
        private HttpClientInterface $http,
        private string $youtubeApiKey = ''   // propriété existante
    ) {}

    /**
     * @return array<int, array<string, mixed>>
     */
    public function suggest(string $query, int $limit = 6): array
    {
        // ✅ Utiliser la bonne propriété
        if (empty($this->youtubeApiKey)) {
            return [];
        }

        $response = $this->http->request('GET', 'https://www.googleapis.com/youtube/v3/search', [
            'query' => [
                'part'       => 'snippet',
                'q'          => $query,
                'maxResults' => $limit,
                'type'       => 'video',
                'key'        => $this->youtubeApiKey,   // ← correction ici
            ],
        ]);

        $data = $response->toArray(false);

        if (!isset($data['items']) || !is_array($data['items'])) {
            return [];
        }

        $results = [];
        foreach ($data['items'] as $item) {
            if (!is_array($item)) continue;

            $snippet   = is_array($item['snippet'] ?? null) ? $item['snippet'] : [];
            $idBlock   = is_array($item['id'] ?? null) ? $item['id'] : [];
            $videoId   = is_string($idBlock['videoId'] ?? null) ? $idBlock['videoId'] : '';
            $thumbs    = is_array($snippet['thumbnails'] ?? null) ? $snippet['thumbnails'] : [];
            $medium    = is_array($thumbs['medium'] ?? null) ? $thumbs['medium'] : [];

            $results[] = [
                'id'          => $videoId,
                'titre'       => is_string($snippet['title'] ?? null) ? $snippet['title'] : '',
                'description' => is_string($snippet['description'] ?? null) ? $snippet['description'] : '',
                'thumbnail'   => is_string($medium['url'] ?? null) ? $medium['url'] : '',
                'chaine'      => is_string($snippet['channelTitle'] ?? null) ? $snippet['channelTitle'] : '',
                'date'        => is_string($snippet['publishedAt'] ?? null) ? $snippet['publishedAt'] : '',
                'url'         => 'https://www.youtube.com/watch?v=' . $videoId,
                'embed'       => 'https://www.youtube.com/embed/' . $videoId,
            ];
        }

        return $results;
    }
}