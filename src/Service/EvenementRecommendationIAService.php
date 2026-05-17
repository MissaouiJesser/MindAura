<?php

namespace App\Service;

use App\Entity\Evenement;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class EvenementRecommendationIAService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $groqApiKey,
    ) {}

    /**
     * @return array{tenue: array{titre: string, style: string, pieces: list<array{item: string, emoji: string, conseil: string}>}, produits: array{titre: string, items: list<array{item: string, emoji: string, pourquoi: string}>}, conseils: list<string>, ambiance: string}
     */
    public function genererRecommandation(Evenement $evenement): array
    {
        if (empty($this->groqApiKey)) {
            throw new \RuntimeException('Clé API Groq manquante. Configurez GROQ_API_KEY.');
        }

        $titre = $evenement->getTitreEvenement();
        $description = $evenement->getDescriptionEvenement() ?? '';
        $lieu = $evenement->getLieuEvenement();
        $date = $evenement->getDatedebutEvenemnt()?->format('d/m/Y') ?? 'date à préciser';
        $type = $evenement->getTypeEvenement()?->getLibelle() ?? 'événement';

        $prompt = <<<PROMPT
Tu es un expert en style et organisation d'événements. Pour l'événement suivant :

Titre : {$titre}
Type : {$type}
Lieu : {$lieu}
Date : {$date}
Description : {$description}

Génère une recommandation personnalisée pour le participant sous forme d'objet JSON strict (sans texte avant ou après) avec cette structure exacte :

{
  "tenue": {
    "titre": "Tenue recommandée",
    "style": "Description du style vestimentaire adapté (max 40 mots)",
    "pieces": [
      {"item": "Nom du vêtement/accessoire", "emoji": "👕", "conseil": "conseil (max 20 mots)"}
    ]
  },
  "produits": {
    "titre": "Produits essentiels",
    "items": [
      {"item": "Nom du produit", "emoji": "📦", "pourquoi": "Pourquoi l'apporter (max 20 mots)"}
    ]
  },
  "conseils": ["conseil pratique 1", "conseil 2"],
  "ambiance": "phrase motivante (max 25 mots)"
}

Règles : 3-4 pièces de tenue, 2-3 produits, 2-4 conseils.
PROMPT;

        try {
            $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->groqApiKey,
                    'Content-Type'  => 'application/json',
                ],
                'json' => [
                    'model'       => 'llama-3.3-70b-versatile',
                    'temperature' => 0.8,
                    'max_tokens'  => 800,
                    'messages'    => [['role' => 'user', 'content' => $prompt]],
                ],
            ]);

            if ($response->getStatusCode() !== 200) {
                throw new \RuntimeException('HTTP ' . $response->getStatusCode() . ' : ' . $response->getContent(false));
            }

            $data = $response->toArray();
            $content = $data['choices'][0]['message']['content'] ?? null;
            if (!is_string($content)) {
                throw new \RuntimeException('Réponse Groq vide ou invalide');
            }

            $clean = preg_replace('/^```json\s*/i', '', $content) ?? '';
$clean = preg_replace('/\s*```$/', '', $clean) ?? '';
$clean = trim($clean);

            $result = json_decode($clean, true);
            if (!is_array($result)) {
                throw new \RuntimeException('JSON de recommandation invalide');
            }

            return [
                'tenue'    => $result['tenue'] ?? ['titre' => 'Tenue', 'style' => '', 'pieces' => []],
                'produits' => $result['produits'] ?? ['titre' => 'Produits', 'items' => []],
                'conseils' => $result['conseils'] ?? [],
                'ambiance' => $result['ambiance'] ?? 'Passez un excellent moment !',
            ];

        } catch (\Throwable $e) {
            throw new \RuntimeException('Erreur API Groq : ' . $e->getMessage());
        }
    }
}