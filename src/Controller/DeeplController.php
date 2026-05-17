<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class DeeplController extends AbstractController
{
    #[Route('/api/translate', name: 'api_translate', methods: ['POST'])]
    public function translate(Request $request, HttpClientInterface $client): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $text = $data['text'] ?? '';
        $targetLang = $data['target_lang'] ?? $data['target'] ?? 'EN';
        $sourceLang = $data['source_lang'] ?? $data['source'] ?? null;

        if (empty($text)) {
            return $this->json(['error' => 'Texte vide'], 400);
        }

        $apiKey = $_ENV['DEEPL_API_KEY_RECLAMATION'];
        $isFree = str_ends_with($apiKey, ':fx'); // Les clés Free finissent par :fx
        $apiUrl = $isFree
            ? 'https://api-free.deepl.com/v2/translate'
            : 'https://api.deepl.com/v2/translate';

        try {
            $jsonPayload = [
                'text'        => [$text],
                'target_lang' => strtoupper($targetLang),
            ];

            if ($sourceLang) {
                $jsonPayload['source_lang'] = strtoupper($sourceLang);
            }

            $response = $client->request('POST', $apiUrl, [
                'headers' => [
                    'Authorization' => 'DeepL-Auth-Key ' . $apiKey,
                    'Content-Type'  => 'application/json',
                ],
                'json' => $jsonPayload,
            ]);

            if ($response->getStatusCode() !== 200) {
                return $this->json(['error' => 'Erreur DeepL: HTTP ' . $response->getStatusCode()], 500);
            }

            $result = $response->toArray();

            if (!isset($result['translations'][0]['text'])) {
                return $this->json(['error' => 'Réponse DeepL invalide'], 500);
            }

            return $this->json([
                'translated' => $result['translations'][0]['text'],
                'detected_lang' => $result['translations'][0]['detected_source_language'] ?? null,
            ]);

        } catch (\Exception $e) {
            return $this->json(['error' => 'Erreur de traduction: ' . $e->getMessage()], 500);
        }
    }
}