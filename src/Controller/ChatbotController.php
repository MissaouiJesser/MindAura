<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

class ChatbotController extends AbstractController
{
    // openrouter/free = sélectionne automatiquement un modèle gratuit disponible
    // Si indisponible, on essaie les fallbacks dans l'ordre
    private const MODEL           = 'openrouter/free';
    private const MODEL_FALLBACK1 = 'meta-llama/llama-3.3-70b-instruct:free';
    private const MODEL_FALLBACK2 = 'google/gemma-3-27b-it:free';
    private const MODEL_FALLBACK3 = 'mistralai/mistral-7b-instruct:free';

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $openRouterApiKey,
        private LoggerInterface $logger
    ) {}

    #[Route('/api/chatbot/message', name: 'api_chatbot_message', methods: ['POST'])]
    public function message(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);

        if (!isset($data['messages']) || !is_array($data['messages'])) {
            return $this->json(['error' => 'Messages manquants.'], 400);
        }

        // Validation basique des messages
        foreach ($data['messages'] as $msg) {
            if (!isset($msg['role'], $msg['content'])) {
                return $this->json(['error' => 'Format de message invalide.'], 400);
            }
            if (!in_array($msg['role'], ['user', 'assistant'])) {
                return $this->json(['error' => 'Rôle invalide : ' . $msg['role']], 400);
            }
        }

        // ── Vérification clé API ──────────────────────────────────────────────
        if (empty($this->openRouterApiKey)) {
            $this->logger->error('[Chatbot] OPENROUTER_API_KEY est vide ! Vérifiez services.yaml et .env.local');
            return $this->json(['error' => 'Clé API manquante côté serveur.'], 500);
        }

        // ── Ajout du system prompt ────────────────────────────────────────────
        $messages = array_merge(
            [[
                'role'    => 'system',
                'content' => 'Tu es MindAura AI, assistant bienveillant spécialisé en santé mentale. Réponds toujours en français. Sois empathique, doux et professionnel.',
            ]],
            $data['messages']
        );

        // ── Essai avec le modèle principal puis les fallbacks ─────────────────
        $modelsToTry = [
            self::MODEL,
            self::MODEL_FALLBACK1,
            self::MODEL_FALLBACK2,
            self::MODEL_FALLBACK3,
        ];

        foreach ($modelsToTry as $model) {
            $this->logger->info('[Chatbot] Tentative avec le modèle', [
                'model'          => $model,
                'messages_count' => count($messages),
            ]);

            $result = $this->callOpenRouter($messages, $model);

            if ($result['success']) {
                return $this->json(['response' => $result['text']]);
            }

            // Si le modèle n'est pas trouvé (404) ou indispo, on essaie le suivant
            if (in_array($result['statusCode'], [404, 503])) {
                $this->logger->warning('[Chatbot] Modèle indisponible, essai du suivant', [
                    'model'   => $model,
                    'message' => $result['error'],
                ]);
                continue;
            }

            // Autre erreur (401, 429, 500...) → on arrête et on renvoie l'erreur
            $this->logger->error('[Chatbot] Erreur OpenRouter', [
                'model'      => $model,
                'statusCode' => $result['statusCode'],
                'message'    => $result['error'],
            ]);

            return $this->json([
                'error'      => 'Erreur : ' . $result['error'],
                'statusCode' => $result['statusCode'],
            ], 502);
        }

        // Tous les modèles ont échoué
        return $this->json([
            'error' => 'Aucun modèle gratuit disponible pour le moment. Réessayez dans quelques instants.',
        ], 503);
    }

    // ── Méthode interne : appel à OpenRouter ──────────────────────────────────
    /**
     * @param array<int, array{role: string, content: string}> $messages
     * @return array{success: true, text: string}|array{success: false, statusCode: int, error: string}
     */
    private function callOpenRouter(array $messages, string $model): array
    {
        try {
            $response = $this->httpClient->request('POST', 'https://openrouter.ai/api/v1/chat/completions', [
                'headers' => [
                    'Content-Type'  => 'application/json',
                    'Authorization' => 'Bearer ' . $this->openRouterApiKey,
                    'HTTP-Referer'  => 'https://mindaura.local',
                    'X-Title'       => 'MindAura AI',
                ],
                'json' => [
                    'model'       => $model,
                    'messages'    => $messages,
                    'max_tokens'  => 1024,
                    'temperature' => 0.7,
                ],
                'timeout' => 30,
            ]);

            $statusCode = $response->getStatusCode();
            $rawBody    = $response->getContent(false);

            $this->logger->info('[Chatbot] Réponse OpenRouter', [
                'model'      => $model,
                'statusCode' => $statusCode,
            ]);

            if ($statusCode !== 200) {
                $errorBody = json_decode($rawBody, true);
                $errorMsg  = is_array($errorBody) && isset($errorBody['error']['message']) && is_string($errorBody['error']['message'])
                    ? $errorBody['error']['message']
                    : $rawBody;
                return ['success' => false, 'statusCode' => $statusCode, 'error' => $errorMsg];
            }

            $body = json_decode($rawBody, true);
            $text = is_array($body) && isset($body['choices'][0]['message']['content']) && is_string($body['choices'][0]['message']['content'])
                ? $body['choices'][0]['message']['content']
                : null;

            if (empty($text)) {
                return ['success' => false, 'statusCode' => 500, 'error' => 'Réponse vide du modèle.'];
            }

            return ['success' => true, 'text' => $text];

        } catch (\Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface $e) {
            $this->logger->error('[Chatbot] Erreur réseau / timeout', ['message' => $e->getMessage()]);
            return ['success' => false, 'statusCode' => 503, 'error' => $e->getMessage()];

        } catch (\Exception $e) {
            $this->logger->error('[Chatbot] Exception inattendue', [
                'class'   => get_class($e),
                'message' => $e->getMessage(),
            ]);
            return ['success' => false, 'statusCode' => 500, 'error' => $e->getMessage()];
        }
    }
}