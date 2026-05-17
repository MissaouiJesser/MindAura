<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;

/**
 * Service de traduction gratuit via MyMemory API.
 */
class TraductionService
{
    private const API_URL = 'https://api.mymemory.translated.net/get';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $email = ''
    ) {}

    /**
     * @return array{translated: string, source: string, target: string, original: string}
     */
    public function translate(string $text, string $target, string $source = 'fr'): array
    {
        if (trim($text) === '') {
            return ['translated' => '', 'source' => $source, 'target' => $target, 'original' => $text];
        }

        if ($source === 'auto') {
            $source = 'fr';
        }

        $langPair = $source . '|' . $target;

        $query = ['q' => $text, 'langpair' => $langPair];
        if ($this->email !== '') {
            $query['de'] = $this->email;
        }

        try {
            $response = $this->httpClient->request('GET', self::API_URL, [
                'query'   => $query,
                'timeout' => 10,
                'headers' => [
                    'User-Agent' => 'Mozilla/5.0 (compatible; MindAura/1.0)',
                ],
            ]);

            $statusCode = $response->getStatusCode();
            if ($statusCode !== 200) {
                throw new \RuntimeException('MyMemory HTTP ' . $statusCode);
            }

            $data           = $response->toArray();
            $responseStatus = $data['responseStatus'] ?? 0;

            if ((int)$responseStatus === 403 || (int)$responseStatus === 429) {
                throw new \RuntimeException('Quota MyMemory dépassé.');
            }

            if ((string)$responseStatus !== '200') {
                throw new \RuntimeException('MyMemory erreur ' . $responseStatus . ': ' . ($data['responseDetails'] ?? 'inconnue'));
            }

            $translated = $data['responseData']['translatedText'] ?? null;

            if ($translated === null || $translated === '') {
                throw new \RuntimeException('Traduction vide retournée par MyMemory');
            }

            return [
                'translated' => $translated,
                'source'     => $source,
                'target'     => $target,
                'original'   => $text,
            ];

        } catch (TransportExceptionInterface $e) {
            throw new \RuntimeException('Impossible de joindre MyMemory : ' . $e->getMessage(), 0, $e);
        }
    }
}