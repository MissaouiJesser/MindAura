<?php
namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class DeepLTranslationService
{
    private string $apiKey;
    private string $apiUrl;

    public function __construct(
        private HttpClientInterface $client,
        string $deepLApiKey,
        bool $isFreeKey = true
    ) {
        $this->apiKey = $deepLApiKey;
        // URL différente selon Free ou Pro
        $this->apiUrl = $isFreeKey
            ? 'https://api-free.deepl.com/v2/translate'
            : 'https://api.deepl.com/v2/translate';
    }

    public function translate(string $text, string $targetLang, string $sourceLang = null): string
    {
        $params = [
            'text'        => [$text],
            'target_lang' => strtoupper($targetLang),
        ];

        if ($sourceLang) {
            $params['source_lang'] = strtoupper($sourceLang);
        }

        $response = $this->client->request('POST', $this->apiUrl, [
            'headers' => [
                'Authorization' => 'DeepL-Auth-Key ' . $this->apiKey,
                'Content-Type'  => 'application/json',
            ],
            'json' => $params,
        ]);

        $data = $response->toArray();

        return $data['translations'][0]['text'] ?? '';
    }
}