<?php

namespace App\Controller;

use App\Entity\Evenement;
use App\Repository\EvenementRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/evenements')]
class ChateventController extends AbstractController
{
    #[Route('/{id}/recommendation', name: 'event_recommendation', methods: ['GET', 'POST'])]
    public function recommendation(int $id, EvenementRepository $evenementRepo): JsonResponse
    {
        $evenement = $evenementRepo->find($id);
        if (!$evenement) {
            return $this->json(['error' => 'Événement introuvable'], 404);
        }

        $groqKey = $_ENV['GROQ_API_KEY'] ?? getenv('GROQ_API_KEY');
        if (!is_string($groqKey) || $groqKey === '') {
            return $this->json(['error' => 'Clé API Groq manquante ou invalide'], 500);
        }

        try {
            $recommendation = $this->callGroqForRecommendation($evenement, $groqKey);
            return $this->json(['recommendation' => $recommendation]);
        } catch (\Throwable $e) {
            return $this->json(['error' => $e->getMessage()], 500);
        }
    }

    /**
     * @param Evenement $evenement
     * @param string $apiKey
     * @return array{tenue: array{titre: string, style: string, pieces: list<array{item: string, emoji: string, conseil: string}>}, produits: array{titre: string, items: list<array{item: string, emoji: string, pourquoi: string}>}, conseils: list<string>, ambiance: string}
     */
    private function callGroqForRecommendation(Evenement $evenement, string $apiKey): array
    {
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

        $postFields = json_encode([
            'model'       => 'llama-3.3-70b-versatile',
            'temperature' => 0.8,
            'max_tokens'  => 800,
            'messages'    => [['role' => 'user', 'content' => $prompt]],
        ]);

        if ($postFields === false) {
            throw new \RuntimeException('Erreur d\'encodage JSON du prompt');
        }

        $ch = curl_init('https://api.groq.com/openai/v1/chat/completions');
        if ($ch === false) {
            throw new \RuntimeException('Impossible d\'initialiser cURL');
        }

        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST           => true,
            CURLOPT_TIMEOUT        => 30,
            CURLOPT_HTTPHEADER     => [
                'Content-Type: application/json',
                'Authorization: Bearer ' . $apiKey,
            ],
            CURLOPT_POSTFIELDS     => $postFields,
            CURLOPT_SSL_VERIFYPEER => false,
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($error !== '') {
            throw new \RuntimeException('cURL error: ' . $error);
        }
        if (!is_string($response)) {
            throw new \RuntimeException('Réponse cURL invalide');
        }
        if ($httpCode !== 200) {
            throw new \RuntimeException("HTTP $httpCode: " . substr($response, 0, 300));
        }

        $data = json_decode($response, true);
        if (!is_array($data)) {
            throw new \RuntimeException('Réponse JSON invalide');
        }

        $content = $data['choices'][0]['message']['content'] ?? null;
        if (!is_string($content)) {
            throw new \RuntimeException('Réponse Groq vide ou mal formée');
        }

        $clean = preg_replace('/^```json\s*/i', '', $content) ?? '';
$clean = preg_replace('/\s*```$/', '', $clean) ?? '';
$clean = trim($clean);

        $result = json_decode($clean, true);
        if (!is_array($result)) {
            throw new \RuntimeException('JSON de recommandation invalide : ' . substr($clean, 0, 200));
        }

        // Structure par défaut
        return [
            'tenue'    => $result['tenue'] ?? ['titre' => 'Tenue', 'style' => '', 'pieces' => []],
            'produits' => $result['produits'] ?? ['titre' => 'Produits', 'items' => []],
            'conseils' => $result['conseils'] ?? [],
            'ambiance' => $result['ambiance'] ?? 'Passez un excellent moment !',
        ];
    }
}