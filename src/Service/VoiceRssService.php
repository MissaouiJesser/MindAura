<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class VoiceRssService
{
    private const API_URL = 'https://api.voicerss.org/';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $apiKey,
        private readonly string $defaultLang = 'fr-fr',
        private readonly string $defaultFormat = '16khz_16bit_stereo',
    ) {}

    /**
     * Retourne le contenu audio binaire (MP3)
     *
     * @throws \RuntimeException si l'API retourne une erreur
     */
    public function textToSpeech(
        string $text,
        string $lang = null,
        string $codec = 'mp3',
        int $rate = 0,
    ): string {
        $response = $this->httpClient->request('GET', self::API_URL, [
            'verify_peer' => false,   // ← ajouter
             'verify_host' => false, 
            'query' => [
                'key'  => $this->apiKey,
                'hl'   => $lang ?? $this->defaultLang,
                'src'  => $text,
                'c'    => strtoupper($codec),
                'f'    => $this->defaultFormat,
                'r'    => $rate,
                
            ],
        ]);

        $content = $response->getContent();

        // VoiceRSS retourne du texte brut en cas d'erreur
        if (str_starts_with($content, 'ERROR:')) {
            throw new \RuntimeException('VoiceRSS API error: ' . $content);
        }

        return $content;
    }

    /**
     * Retourne l'audio encodé en Base64 (utile pour l'affichage HTML inline)
     */
    public function textToSpeechBase64(string $text, string $lang = null): string
    {
        $audio = $this->textToSpeech($text, $lang);
        return 'data:audio/mp3;base64,' . base64_encode($audio);
    }
}