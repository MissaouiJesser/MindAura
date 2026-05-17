<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * Service qui appelle l'API Groq (LLaMA 3) pour générer
 * automatiquement une description marketing d'un produit.
 */
class DescriptionIaService
{
    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly string $groqApiKey,
    ) {}

    /**
     * Génère une description produit en français via Groq (LLaMA 3.3 70B).
     *
     * @param string      $nom   Nom du produit
     * @param string|null $prix  Prix du produit (optionnel)
     * @return string            Description générée (ou message d'erreur)
     */
    public function genererDescription(string $nom, ?string $prix = null): string
    {
        $prixInfo = $prix ? " vendu à {$prix} USD" : '';

        $prompt = <<<PROMPT
Tu es un expert en copywriting e-commerce. Génère une description produit professionnelle, 
attrayante et convaincante en français pour le produit suivant : "{$nom}"{$prixInfo}.

La description doit :
- Expliquer clairement ce qu'est ce produit et à quoi il sert
- Mettre en avant ses bénéfices et avantages pour l'acheteur
- Être engageante et donner envie d'acheter
- Faire entre 80 et 150 mots
- Ne pas inclure de titre, juste le texte de description
- Ne pas utiliser de listes à puces, seulement des paragraphes fluides

Réponds UNIQUEMENT avec la description, sans introduction ni explication.
PROMPT;

        try {
            $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
                'headers' => [
                    'Authorization' => 'Bearer ' . $this->groqApiKey,
                    'Content-Type'  => 'application/json',
                ],
                'json' => [
                    'model'       => 'llama-3.3-70b-versatile',
                    'messages'    => [
                        ['role' => 'user', 'content' => $prompt],
                    ],
                    'temperature' => 0.7,
                    'max_tokens'  => 300,
                ],
                'timeout' => 15,
            ]);

            $data = $response->toArray();
            return trim($data['choices'][0]['message']['content'] ?? 'Erreur : réponse vide.');
        } catch (\Throwable $e) {
            return 'Erreur lors de la génération : ' . $e->getMessage();
        }
    }
}
