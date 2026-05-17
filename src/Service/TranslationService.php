<?php
namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class TranslationService
{
    // Correspondance langues DeepL → MyMemory
    private const LANG_MAP = [
        'FR'    => 'fr',
        'EN-US' => 'en',
        'AR'    => 'ar',
    ];

    public function __construct(
        private HttpClientInterface $http,
        private string $email = ''
    ) {}

    public function translate(string $text, string $targetLang, string $sourceLang = 'fr'): string
    {
        if (empty(trim($text))) return $text;

        $target = self::LANG_MAP[$targetLang] ?? strtolower($targetLang);
        $source = self::LANG_MAP[$sourceLang] ?? strtolower($sourceLang);

        if (empty($source) || $source === 'auto') {
            $source = 'fr';
        }

        $params = [
            'q'        => $text,
            'langpair' => $source . '|' . $target,
        ];

        if ($this->email) {
            $params['de'] = $this->email;
        }

        $response = $this->http->request('GET',
            'https://api.mymemory.translated.net/get',
            ['query' => $params]
        );

        $data = $response->toArray();

        if ($data['responseStatus'] !== 200) {
            throw new \RuntimeException('MyMemory: ' . ($data['responseDetails'] ?? 'Erreur inconnue'));
        }

        return $data['responseData']['translatedText'] ?? $text;
    }

    /**
     * FIX PHPStan: types de valeurs explicites dans les signatures array.
     *
     * @param array<string, string> $fields   Ex: ['titre' => '...', 'description' => '...']
     * @return array<string, string>
     */
    public function translateRessource(array $fields, string $targetLang): array
    {
        return [
            'titre'       => $this->translate($fields['titre'] ?? '', $targetLang),
            'description' => $this->translate($fields['description'] ?? '', $targetLang),
        ];
    }
}