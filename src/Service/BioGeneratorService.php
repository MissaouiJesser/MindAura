<?php

namespace App\Service;

use App\Entity\Utilisateurs;
use Psr\Log\LoggerInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * Génère des biographies IA via OpenRouter (gratuit).
 * Utilise openrouter/free : router automatique qui choisit un modèle gratuit disponible.
 */
class BioGeneratorService
{
    private const BASE_URL     = 'https://openrouter.ai/api/v1/chat/completions';
    private const MODEL        = 'openrouter/auto'; // sélectionne automatiquement un modèle gratuit
    private const TIMEOUT_SECS = 25;

    private const STYLE_PROMPTS = [
        'professionnel' => 'Tu es un rédacteur professionnel spécialisé en personal branding. Génère une biographie professionnelle concise (2 phrases maximum, 40 mots max). Ton : formel, précis, orienté résultats. Aucun emoji. Réponds UNIQUEMENT avec le texte de la bio, sans guillemets ni explication.',
        'inspirant'     => 'Tu es un copywriter créatif. Génère une biographie inspirante (2 phrases max, 40 mots max). Ton : chaleureux et motivant. Un seul emoji maximum. Réponds UNIQUEMENT avec le texte de la bio, sans guillemets ni explication.',
        'minimaliste'   => 'Tu es un expert en communication minimaliste. Génère une bio très courte (25 mots max). Ton : épuré, impactant. Aucun emoji. Réponds UNIQUEMENT avec le texte de la bio, sans guillemets ni explication.',
    ];

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly LoggerInterface     $logger,
        private readonly string              $openrouterApiKey,
    ) {}

    /** @return array<string, mixed> */
    public function generate(Utilisateurs $user, string $style): array
    {
        $style      = array_key_exists($style, self::STYLE_PROMPTS) ? $style : 'professionnel';
        $systemText = self::STYLE_PROMPTS[$style];
        $userPrompt = $this->buildUserPrompt($user);

        try {
            $response = $this->httpClient->request('POST', self::BASE_URL, [
                'timeout' => self::TIMEOUT_SECS,
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->openrouterApiKey,
                    'Content-Type'  => 'application/json',
                    'HTTP-Referer'  => 'https://mindaura.app',
                    'X-Title'       => 'MindAura',
                ],
                'json' => [
                    'model'    => self::MODEL,
                    'messages' => [
                        ['role' => 'system', 'content' => $systemText],
                        ['role' => 'user',   'content' => $userPrompt],
                    ],
                    'max_tokens'  => 200,
                    'temperature' => 0.8,
                ],
            ]);

            $statusCode = $response->getStatusCode();
            $rawBody    = $response->getContent(false);

            $this->logger->debug('BioGenerator response', [
                'status' => $statusCode,
                'body'   => $rawBody,
            ]);

            if ($statusCode !== 200) {
                $decoded = json_decode($rawBody, true);
                $apiMsg  = $decoded['error']['message'] ?? $rawBody;

                $this->logger->error('BioGenerator API error', [
                    'status'  => $statusCode,
                    'message' => $apiMsg,
                ]);

                return match ($statusCode) {
                    401     => ['error' => 'Clé API invalide. Vérifiez OPENROUTER_API_KEY dans .env.'],
                    429     => ['error' => 'Limite atteinte (50 req/jour gratuit). Réessayez demain.'],
                    default => ['error' => 'Erreur API (' . $statusCode . ') : ' . $apiMsg],
                };
            }

            $data = json_decode($rawBody, true);
            $bio  = trim($data['choices'][0]['message']['content'] ?? '');

            if (!$bio) {
                return ['error' => "L'IA n'a pas généré de réponse."];
            }

            $this->logger->info('BioGenerator success', [
                'user'  => $user->getIdUtilisateur(),
                'style' => $style,
                'model' => $data['model'] ?? 'unknown',
            ]);

            return ['bio' => trim($bio, '"\'')];

        } catch (\Throwable $e) {
            $this->logger->error('BioGenerator exception', ['error' => $e->getMessage()]);
            return ['error' => 'Erreur réseau : ' . $e->getMessage()];
        }
    }

    private function buildUserPrompt(Utilisateurs $user): string
    {
        $roleMap = [
            'ROLE_PATIENT'     => 'Patient',
            'ROLE_PSYCHOLOGUE' => 'Psychologue',
            'ROLE_COACH'       => 'Coach',
            'ROLE_ADMIN'       => 'Administrateur',
            'ROLE_USER'        => 'Utilisateur',
        ];

        $role = $roleMap[$user->getRoleUtilisateur() ?? 'ROLE_USER'] ?? 'Utilisateur';

        return implode("\n", [
            'Prénom : '    . $user->getPrenomUtilisateur(),
            'Nom : '       . $user->getNomUtilisateur(),
            'Rôle : '      . $role,
            'Plateforme : MindAura (bien-être et santé mentale)',
            'Génère une bio adaptée à ce profil.',
        ]);
    }

    /** @return array<string, string> */
    public static function getStyles(): array
    {
        return [
            'professionnel' => 'Professionnel',
            'inspirant'     => 'Inspirant',
            'minimaliste'   => 'Minimaliste',
        ];
    }
}