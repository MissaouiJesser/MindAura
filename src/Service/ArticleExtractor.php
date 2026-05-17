<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\DomCrawler\Crawler;

class ArticleExtractor
{
    /** @var array<int, string> */
    private array $userAgents = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0',
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    ];

    public function __construct(
        private HttpClientInterface $httpClient
    ) {}

    /** @return array<string, mixed> */
    public function extract(string $url): array
    {
        $userAgent = $this->userAgents[array_rand($this->userAgents)];

        try {
            $response = $this->httpClient->request('GET', $url, [
                'headers' => [
                    'User-Agent'                => $userAgent,
                    'Accept'                    => 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
                    'Accept-Language'           => 'fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7',
                    'Connection'                => 'keep-alive',
                    'Upgrade-Insecure-Requests' => '1',
                    'Sec-Fetch-Dest'            => 'document',
                    'Sec-Fetch-Mode'            => 'navigate',
                    'Sec-Fetch-Site'            => 'none',
                    'Sec-Fetch-User'            => '?1',
                    'Cache-Control'             => 'max-age=0',
                ],
                'timeout'       => 20,
                'max_redirects' => 10,
                'verify_peer'   => false,
                'verify_host'   => false,
            ]);

            $statusCode = $response->getStatusCode();

            if ($statusCode === 404) {
                return ['success' => false, 'error' => 'Page introuvable (404).', 'url' => $url];
            }
            if ($statusCode === 403) {
                return ['success' => false, 'error' => 'Accès refusé par le site (403).', 'url' => $url];
            }
            if ($statusCode >= 400) {
                return ['success' => false, 'error' => "Erreur HTTP $statusCode.", 'url' => $url];
            }

            $html = $response->getContent();
            $html = $this->fixEncoding($html, $response);

            $crawler = new Crawler($html, $url);

            // ✅ Extraire UNIQUEMENT le contenu textuel (paragraphes + titres)
            $content = $this->extractContentOnly($crawler);

            if (strlen(strip_tags($content)) < 200) {
                return [
                    'success' => false,
                    'error'   => 'Contenu insuffisant. Ce site protège peut-être son contenu.',
                    'url'     => $url,
                ];
            }

            return [
                'success'     => true,
                'title'       => $this->extractTitle($crawler),
                'content'     => $content,       // ✅ Contenu textuel pur uniquement
                'image'       => $this->extractImage($crawler, $url),
                'author'      => $this->extractAuthor($crawler),
                'publishedAt' => $this->extractDate($crawler),
                'siteName'    => $this->extractSiteName($crawler),
                'url'         => $url,
            ];

        } catch (\Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface $e) {
            return ['success' => false, 'error' => 'Impossible de contacter le site : ' . $e->getMessage(), 'url' => $url];
        } catch (\Exception $e) {
            return ['success' => false, 'error' => 'Erreur : ' . $e->getMessage(), 'url' => $url];
        }
    }

    // ════════════════════════════════════════════════════════
    // ✅ NOUVELLE MÉTHODE — Extrait UNIQUEMENT le contenu texte
    // Retourne seulement <p>, <h2>, <h3>, <ul>/<ol>/<li>
    // Sans images, liens, boutons, publicités, etc.
    // ════════════════════════════════════════════════════════
    private function extractContentOnly(Crawler $crawler): string
    {
        $selectors = [
            'article',
            '[itemprop="articleBody"]',
            '.article-body',
            '.article-content',
            '.article__body',
            '.article__content',
            '.post-content',
            '.post-body',
            '.entry-content',
            '.entry-body',
            '.content-body',
            '.content-article',
            '.single-content',
            '.page-content',
            'main article',
            'main .content',
            '#article-body',
            '#article-content',
            '#content article',
            '.story-body',
            '.story-content',
        ];

        $rawHtml = '';

        foreach ($selectors as $selector) {
            try {
                $node = $crawler->filter($selector);
                if ($node->count() && strlen(strip_tags($node->first()->html())) > 200) {
                    $rawHtml = $node->first()->html();
                    break;
                }
            } catch (\Exception) {}
        }

        // Fallback : paragraphes du <main>
        if (!$rawHtml) {
            try {
                $main = $crawler->filter('main');
                if ($main->count()) {
                    $rawHtml = $main->first()->html();
                }
            } catch (\Exception) {}
        }

        // Dernier recours : body
        if (!$rawHtml) {
            try {
                $body = $crawler->filter('body');
                if ($body->count()) {
                    $rawHtml = $body->first()->html();
                }
            } catch (\Exception) {}
        }

        if (!$rawHtml) return '';

        return $this->extractTextNodes($rawHtml);
    }

    // ════════════════════════════════════════════════════════
    // ✅ Extrait uniquement p, h2, h3, li depuis le HTML brut
    // Supprime tout le reste (images, nav, aside, scripts, etc.)
    // ════════════════════════════════════════════════════════
    private function extractTextNodes(string $html): string
    {
        // Éléments à supprimer complètement
        $removeSelectors = [
            'script', 'style', 'nav', 'aside', 'footer', 'header',
            'form', 'iframe', 'noscript', 'img', 'figure', 'picture',
            'video', 'audio', 'svg', 'canvas', 'button', 'input',
            '.ad', '.ads', '.advertisement', '.social-share', '.share-buttons',
            '.newsletter', '.related-posts', '.related-articles',
            '.sidebar', '.widget', '.comments', '.cookie-banner',
            '.popup', '.modal', '[class*="banner"]', '[class*="promo"]',
            '[class*="pub"]', '[class*="sponsor"]', '[class*="widget"]',
            'a[href*="abonnement"]', 'a[href*="subscribe"]',
        ];

        try {
            $crawler = new Crawler('<div id="__wrap__">' . $html . '</div>');

            // Supprimer les éléments indésirables
            foreach ($removeSelectors as $selector) {
                try {
                    $crawler->filter($selector)->each(function (Crawler $node) {
                        foreach ($node as $domNode) {
                            $domNode->parentNode?->removeChild($domNode);
                        }
                    });
                } catch (\Exception) {}
            }

            $output = '';

            // ✅ Extraire uniquement les balises textuelles utiles
            $crawler->filter('#__wrap__ p, #__wrap__ h2, #__wrap__ h3, #__wrap__ li')->each(
                function (Crawler $node) use (&$output) {
                    $tag  = $node->nodeName();
                    $text = trim($node->text());

                    // Ignorer les textes trop courts (boutons, légendes, etc.)
                    if (strlen($text) < 30) return;

                    // Ignorer les textes qui ressemblent à des liens de navigation
                    if (preg_match('/^(voir aussi|lire aussi|en savoir plus|suivez|abonnez|partager|tweeter|découvrez)/i', $text)) return;

                    $text = htmlspecialchars($text, ENT_QUOTES, 'UTF-8');

                    $output .= match ($tag) {
                        'h2'    => "<h2>{$text}</h2>\n",
                        'h3'    => "<h3>{$text}</h3>\n",
                        'li'    => "<li>{$text}</li>\n",
                        default => "<p>{$text}</p>\n",
                    };
                }
            );

            return trim($output);

        } catch (\Exception) {
            // Fallback basique si le crawler échoue
            return $this->collectParagraphsFallback($html);
        }
    }

    // ════════════════════════════════════════════════════════
    // Fallback : extraction basique par regex si DomCrawler échoue
    // ════════════════════════════════════════════════════════
    private function collectParagraphsFallback(string $html): string
    {
        // Supprimer scripts et styles
        $html = (string) preg_replace('/<script[^>]*>.*?<\/script>/si', '', $html);
        $html = (string) preg_replace('/<style[^>]*>.*?<\/style>/si', '', $html);

        $output = '';

        // Extraire les <p>
        preg_match_all('/<p[^>]*>(.*?)<\/p>/si', $html, $matches);
        foreach ($matches[1] as $p) {
            $text = trim(strip_tags((string) $p));
            if (strlen($text) > 30) {
                $output .= '<p>' . htmlspecialchars($text, ENT_QUOTES, 'UTF-8') . '</p>' . "\n";
            }
        }

        // Extraire les <h2> et <h3>
        preg_match_all('/<(h[23])[^>]*>(.*?)<\/\1>/si', $html, $hMatches, PREG_SET_ORDER);
        foreach ($hMatches as $h) {
            $text = trim(strip_tags((string) $h[2]));
            if (strlen($text) > 5) {
                $tag     = htmlspecialchars((string) $h[1]);
                $output .= "<{$tag}>" . htmlspecialchars($text, ENT_QUOTES, 'UTF-8') . "</{$tag}>\n";
            }
        }

        return trim($output);
    }

    private function fixEncoding(string $html, \Symfony\Contracts\HttpClient\ResponseInterface $response): string
    {
        $contentType = '';
        try {
            $headers     = $response->getHeaders();
            $contentType = $headers['content-type'][0] ?? '';
        } catch (\Exception) {}

        $charset = null;

        if (preg_match('/charset=([^\s;]+)/i', $contentType, $m)) {
            $charset = strtoupper(trim($m[1]));
        }
        if (!$charset && preg_match('/<meta[^>]+charset=["\']?([^"\'\s;>]+)/i', $html, $m)) {
            $charset = strtoupper(trim($m[1]));
        }
        if (!$charset && preg_match('/content-type[^>]+charset=([^\s"\'>;]+)/i', $html, $m)) {
            $charset = strtoupper(trim($m[1]));
        }

        if (!$charset || $charset === 'UTF-8' || $charset === 'UTF8') {
            if (!mb_check_encoding($html, 'UTF-8')) {
                $converted = mb_convert_encoding($html, 'UTF-8', 'ISO-8859-1');
                return $converted ?: $html;
            }
            return $html;
        }

        try {
            $converted = mb_convert_encoding($html, 'UTF-8', $charset);
            return $converted ?: $html;
        } catch (\Exception) {
            return $html;
        }
    }

    private function extractTitle(Crawler $crawler): string
    {
        $og = $crawler->filter('meta[property="og:title"]');
        if ($og->count()) return trim((string) $og->attr('content'));

        $tw = $crawler->filter('meta[name="twitter:title"]');
        if ($tw->count()) return trim((string) $tw->attr('content'));

        $ld = $this->extractJsonLd($crawler);
        if (!empty($ld['headline'])) return trim((string) $ld['headline']);

        $title = $crawler->filter('title');
        if ($title->count()) return trim($title->text());

        $h1 = $crawler->filter('h1');
        if ($h1->count()) return trim($h1->first()->text());

        return 'Article sans titre';
    }

    private function extractImage(Crawler $crawler, string $baseUrl): ?string
    {
        $og = $crawler->filter('meta[property="og:image"]');
        if ($og->count()) return $this->absoluteUrl((string) $og->attr('content'), $baseUrl);

        $tw = $crawler->filter('meta[name="twitter:image"]');
        if ($tw->count()) return $this->absoluteUrl((string) $tw->attr('content'), $baseUrl);

        $ld = $this->extractJsonLd($crawler);
        if (!empty($ld['image'])) {
            $img = is_array($ld['image']) ? ($ld['image']['url'] ?? $ld['image'][0]) : $ld['image'];
            return $this->absoluteUrl((string) $img, $baseUrl);
        }

        return null;
    }

    private function extractAuthor(Crawler $crawler): ?string
    {
        $ld = $this->extractJsonLd($crawler);
        if (!empty($ld['author'])) {
            $author = $ld['author'];
            if (is_array($author)) return $author['name'] ?? ($author[0]['name'] ?? null);
            return $author;
        }

        $meta = $crawler->filter('meta[name="author"]');
        if ($meta->count()) return trim((string) $meta->attr('content'));

        $itemprop = $crawler->filter('[itemprop="author"]');
        if ($itemprop->count()) return trim($itemprop->first()->text());

        return null;
    }

    private function extractDate(Crawler $crawler): ?string
    {
        $ld = $this->extractJsonLd($crawler);
        if (!empty($ld['datePublished'])) return $ld['datePublished'];

        foreach ([
            'meta[property="article:published_time"]',
            'meta[name="publication_date"]',
            'meta[name="date"]',
        ] as $selector) {
            $node = $crawler->filter($selector);
            if ($node->count()) return $node->attr('content');
        }

        $time = $crawler->filter('time[datetime]');
        if ($time->count()) return $time->first()->attr('datetime');

        return null;
    }

    private function extractSiteName(Crawler $crawler): ?string
    {
        $og = $crawler->filter('meta[property="og:site_name"]');
        if ($og->count()) return trim((string) $og->attr('content'));
        return null;
    }

    /** @return array<string, mixed> */
    private function extractJsonLd(Crawler $crawler): array
    {
        $scripts = $crawler->filter('script[type="application/ld+json"]');
        foreach ($scripts as $script) {
            try {
                $data = json_decode($script->textContent, true);
                if (!is_array($data)) continue;
                if (isset($data['@type']) && in_array($data['@type'], ['Article', 'NewsArticle', 'BlogPosting'])) {
                    return $data;
                }
                if (isset($data[0])) {
                    foreach ($data as $item) {
                        if (isset($item['@type']) && in_array($item['@type'], ['Article', 'NewsArticle', 'BlogPosting'])) {
                            return $item;
                        }
                    }
                }
            } catch (\Exception) {}
        }
        return [];
    }

    private function absoluteUrl(string $url, string $baseUrl): string
    {
        if (str_starts_with($url, 'http')) return $url;
        $parts = parse_url($baseUrl);
        if ($parts === false || !isset($parts['scheme'], $parts['host'])) {
            return $url;
        }
        $base = $parts['scheme'] . '://' . $parts['host'];
        return str_starts_with($url, '/') ? $base . $url : $base . '/' . ltrim($url, '/');
    }
}