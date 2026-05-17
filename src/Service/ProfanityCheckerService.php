<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ProfanityCheckerService
{
    private const API_URL     = 'https://vector.profanity.dev';
    private const API_TIMEOUT = 3.0;

    private const FR_WORDS = [
        'bordel','chier','merdique','caca','chiotte','chiottes','cul','foutre',
        'con','fesse','fesses','débile','débiles','idiot','idiote','idiots',
        'imbécile','crétin','crétins','abruti','abrutis','taré','tarée',
        'merde','putain','bâtard','batard','bite','couille','couilles',
        'couillon','pénis','penis','vagin','vajin','anus','branler',
        'branleur','dégueulasse','degueulasse','porc','cochon',
        'connard','connards','connasse','connasses','salope','salopes',
        'pute','putes','enculé','enculer','fils de pute','fdp','niquer',
        'nique','ordure','ordures','salopard','salopards','fumier','fumiers',
        'raclure','raclures','baiser','baise','sucer','suce',
        'viol','violer','sodomiser','sodomie',
        'nègre','negre','pédé','pede','tapette','bougnoule','youpin',
    ];

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly LoggerInterface $logger,
    ) {}

    /**
     * @return array{
     *   isProfanity: bool,
     *   score: float,
     *   source: 'api'|'local',
     *   flaggedFor?: string|null
     * }
     */
    public function check(string $text): array
    {
        if (trim($text) === '') {
            return ['isProfanity' => false, 'score' => 0.0, 'source' => 'local', 'flaggedFor' => null];
        }

        $apiResult = $this->callExternalApi($text);
        if ($apiResult !== null) {
            $localHit = $this->localFrCheck($text);
            if ($localHit !== null) {
                return ['isProfanity' => true, 'score' => 1.0, 'source' => 'local', 'flaggedFor' => $localHit];
            }
            return ['isProfanity' => (bool) $apiResult['isProfanity'], 'score' => (float) $apiResult['score'], 'source' => 'api', 'flaggedFor' => null];
        }

        $localHit = $this->localFrCheck($text);
        return [
            'isProfanity' => $localHit !== null,
            'score' => $localHit !== null ? 1.0 : 0.0,
            'source' => 'local',
            'flaggedFor' => $localHit,
        ];
    }

    public function isProfane(string $text): bool
    {
        return $this->check($text)['isProfanity'];
    }

    /**
     * @return array{isProfanity: bool, score: float}|null
     */
    private function callExternalApi(string $text): ?array
    {
        try {
            $response = $this->httpClient->request('POST', self::API_URL, [
                'headers' => ['Content-Type' => 'application/json'],
                'json' => ['message' => $text],
                'timeout' => self::API_TIMEOUT,
            ]);

            $data = $response->toArray(false);
            if (!isset($data['isProfanity'])) {
                return null;
            }

            return [
                'isProfanity' => (bool) $data['isProfanity'],
                'score' => (float) ($data['score'] ?? 0.0),
            ];
        } catch (TransportExceptionInterface $e) {
            $this->logger->warning('[ProfanityChecker] API externe indisponible, fallback local.', ['error' => $e->getMessage()]);
            return null;
        } catch (\Throwable $e) {
            $this->logger->error('[ProfanityChecker] Erreur inattendue API externe.', ['error' => $e->getMessage()]);
            return null;
        }
    }

    private function localFrCheck(string $text): ?string
    {
        $normalized = $this->normalize($text);
        foreach (self::FR_WORDS as $word) {
            $pattern = '/\b' . preg_quote($this->normalize($word), '/') . '\b/iu';
            if (preg_match($pattern, $normalized)) {
                return $word;
            }
        }
        return null;
    }

    private function normalize(string $text): string
    {
        $text = mb_strtolower($text, 'UTF-8');
        $text = strtr($text, [
            '@' => 'a', '4' => 'a', '3' => 'e', '1' => 'i',
            '0' => 'o', '5' => 's', '!' => 'i', '$' => 's', '€' => 'e',
        ]);
        $text = \Normalizer::normalize($text, \Normalizer::FORM_D);
        return preg_replace('/\p{Mn}/u', '', $text) ?? $text;
    }
}