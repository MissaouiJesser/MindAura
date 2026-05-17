<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\DomCrawler\Crawler;

class AudioUrlExtractor
{
    public function __construct(private HttpClientInterface $httpClient) {}

    public function extract(string $pageUrl): ?string
    {
        if (preg_match('/\.(mp3|m4a|wav|ogg|aac|flac)(\?.*)?$/i', $pageUrl)) {
            return $pageUrl;
        }

        try {
            $response = $this->httpClient->request('GET', $pageUrl, [
                'headers' => [
                    'User-Agent'      => 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
                        . 'AppleWebKit/537.36 (KHTML, like Gecko) '
                        . 'Chrome/120.0.0.0 Safari/537.36',
                    'Accept-Language' => 'fr-FR,fr;q=0.9,en;q=0.8',
                ],
                'timeout' => 15,
            ]);

            $html    = $response->getContent();
            $crawler = new Crawler($html);

            // 1. Balise <audio src="...">
            $audio = $crawler->filter('audio[src]')->first();
            if ($audio->count()) {
                return $this->absoluteUrl((string) $audio->attr('src'), $pageUrl);
            }

            // 2. Balise <source> dans <audio>
            $source = $crawler->filter('audio source[src]')->first();
            if ($source->count()) {
                return $this->absoluteUrl((string) $source->attr('src'), $pageUrl);
            }

            // 3. Liens directs audio
            $link = $crawler->filter(
                'a[href$=".mp3"], a[href$=".m4a"], a[href$=".wav"], '
                . 'a[href$=".ogg"], a[href$=".aac"], a[href$=".flac"]'
            )->first();
            if ($link->count()) {
                return $this->absoluteUrl((string) $link->attr('href'), $pageUrl);
            }

            // 4. Patterns JSON/JS dans le HTML
            $ext = 'mp3|m4a|wav|ogg|aac|flac';

            $patterns = [
                '/"audioUrl"\s*:\s*"([^"]+\.(?:' . $ext . ')[^"]*)"/i',
                '/"audio_url"\s*:\s*"([^"]+\.(?:' . $ext . ')[^"]*)"/i',
                '/"enclosure_url"\s*:\s*"([^"]+\.(?:' . $ext . ')[^"]*)"/i',
                '/mp3\s*:\s*"([^"]+\.mp3[^"]*)"/i',
                '/"(?:src|url)"\s*:\s*"([^"]+\.(?:' . $ext . ')[^"]*)"/i',
                '/https?:\/\/[^\s"<>]+\.(?:' . $ext . ')(?:\?[^\s"<>]*)?/i',
            ];

            foreach ($patterns as $pattern) {
                if (preg_match($pattern, $html, $matches)) {
                    $candidate = $matches[1] ?? $matches[0];
                    if (filter_var($candidate, FILTER_VALIDATE_URL)) {
                        return $candidate;
                    }
                }
            }

            // 5. Iframes players connus
            $iframeSrc = null;
            $crawler->filter('iframe[src]')->each(function (Crawler $node) use (&$iframeSrc) {
                $src = $node->attr('src') ?? '';
                $knownPlayers = [
                    'art19.com', 'buzzsprout.com', 'podbean.com',
                    'simplecast.com', 'transistor.fm', 'megaphone.fm',
                ];
                foreach ($knownPlayers as $player) {
                    if (str_contains($src, $player)) {
                        $iframeSrc = $src;
                    }
                }
            });

            if ($iframeSrc) {
                return null;
            }

            return null;

        } catch (\Exception $e) {
            return null;
        }
    }

    private function absoluteUrl(string $url, string $baseUrl): string
    {
        if (str_starts_with($url, 'http')) {
            return $url;
        }
        $parts = parse_url($baseUrl);
        if ($parts === false || !isset($parts['scheme'], $parts['host'])) {
            return $url;
        }
        if (str_starts_with($url, '/')) {
            return $parts['scheme'] . '://' . $parts['host'] . $url;
        }
        return rtrim($baseUrl, '/') . '/' . ltrim($url, '/');
    }
}