<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class GroqServiceR
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $groqApiKey,
    ) {}

    public function suggestReponse(string $sujet, string $description): string
    {
        $prompt = sprintf(
            "Tu es un assistant de support client. Voici une réclamation :\n\nSujet : %s\nDescription : %s\n\nPropose une réponse professionnelle et courtoise en français en 2-3 phrases.",
            $sujet,
            $description
        );

        try {
            $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->groqApiKey,
                    'Content-Type'  => 'application/json',
                ],
                'json' => [
'model' => 'llama-3.3-70b-versatile',                    'messages'   => [
                        ['role' => 'user', 'content' => $prompt],
                    ],
                    'max_tokens' => 300,
                ],
                'timeout' => 15,
            ]);

            $data = $response->toArray();
            return trim($data['choices'][0]['message']['content'] ?? 'Impossible de générer une suggestion.');

        } catch (\Throwable $e) {
            return 'Erreur : ' . $e->getMessage();
        }
    }
}