<?php
// src/Service/ReclamationPriorityService.php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class ReclamationPriorityService
{
    private const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
    private const MODEL = 'llama-3.3-70b-versatile';

    public function __construct(
        private HttpClientInterface $httpClient,
        private CacheInterface $cache,
        private string $groqApiKey
    ) {}

    public function classifierPriorite(int $id, string $texte): string
    {
        if (trim($texte) === '') {
            return 'INCONNUE';
        }

        if (empty($this->groqApiKey) || !str_starts_with($this->groqApiKey, 'gsk_')) {
            return 'INCONNUE';
        }

        return $this->cache->get('priorite_rec_' . $id, function (ItemInterface $item) use ($texte) {
            try {
                $response = $this->httpClient->request('POST', self::GROQ_API_URL, [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $this->groqApiKey,
                        'Content-Type' => 'application/json',
                    ],
                    'json' => [
                        'model' => self::MODEL,
                        'temperature' => 0,
                        'max_tokens' => 8,
                        'messages' => [
                            [
                                'role' => 'system',
                                'content' => 'Tu es un classificateur de priorite. Reponds uniquement avec un mot parmi: HAUTE, MOYENNE, BASSE.',
                            ],
                            [
                                'role' => 'user',
                                'content' => "Classifie cette reclamation selon son urgence.\nTexte: {$texte}\nReponse attendue: HAUTE ou MOYENNE ou BASSE.",
                            ],
                        ],
                    ],
                    'timeout' => 12,
                ]);

                $data = $response->toArray(false);
                $raw = strtoupper(trim((string) ($data['choices'][0]['message']['content'] ?? '')));
                $clean = preg_replace('/[^A-Z]/', '', $raw) ?? '';

                $priority = match (true) {
                    str_contains($clean, 'HAUTE') => 'HAUTE',
                    str_contains($clean, 'MOYENNE') => 'MOYENNE',
                    str_contains($clean, 'BASSE') => 'BASSE',
                    default => 'INCONNUE',
                };

                if ($priority === 'INCONNUE') {
                    $item->expiresAfter(120);
                    return 'INCONNUE';
                }

                $item->expiresAfter(3600);
                return $priority;
            } catch (\Throwable $e) {
                $item->expiresAfter(120);
                return 'INCONNUE';
            }
        });
    }
}