<?php
// src/Service/CommentAnalysisService.php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class CommentAnalysisService
{
    public function __construct(
        private HttpClientInterface $httpClient,
        private string $apiKey,
        private string $projectDir = ''
    ) {}

    /**
     * Analyse un commentaire et retourne le sentiment, les thèmes et un résumé.
     *
     * @return array{
     *     sentiment: 'positif'|'negatif'|'neutre',
     *     themes: array<int, string>,
     *     summary: string|null
     * }
     */
    public function analyze(string $contenu): array
    {
        $prompt = <<<PROMPT
Tu es un assistant pour une application de développement personnel et psychologie (MindAura).

Analyse ce commentaire utilisateur et retourne UNIQUEMENT un JSON valide, sans texte avant ou après :
{
  "sentiment": "positif" ou "negatif" ou "neutre",
  "themes": ["theme1", "theme2"],
  "summary": "Résumé en une phrase"
}

Commentaire : "{$contenu}"
PROMPT;

        try {
            $response = $this->httpClient->request('POST', 'https://api.anthropic.com/v1/messages', [
                'headers' => [
                    'x-api-key'         => $this->apiKey,
                    'anthropic-version' => '2023-06-01',
                    'content-type'      => 'application/json',
                ],
                'json' => [
                    'model'      => 'claude-sonnet-4-5',
                    'max_tokens' => 500,
                    'messages'   => [
                        ['role' => 'user', 'content' => $prompt]
                    ],
                ],
            ]);

            $data   = $response->toArray();
            $text   = $data['content'][0]['text'];
            $result = json_decode($text, true);

            return $result ?? ['sentiment' => 'neutre', 'themes' => [], 'summary' => null];

        } catch (\Exception $e) {
            $logDir  = $this->projectDir ? $this->projectDir . '/var/log' : sys_get_temp_dir();
            $logFile = $logDir . '/mindaura_ai_error.log';
            @file_put_contents($logFile, date('Y-m-d H:i:s') . ' — ' . $e->getMessage() . "\n", FILE_APPEND);

            return ['sentiment' => 'neutre', 'themes' => [], 'summary' => null];
        }
    }
}