<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;

class QuotableService
{
    private const BASE_URL = 'https://zenquotes.io/api/random';

    private const FALLBACKS = [
        'personnalité' => [
            ['content' => 'Connais-toi toi-même et tu connaîtras l\'univers et les dieux.', 'author' => 'Socrate'],
            ['content' => 'Soyez vous-même, tous les autres sont déjà pris.', 'author' => 'Oscar Wilde'],
            ['content' => 'Ce que nous pensons, nous le devenons.', 'author' => 'Bouddha'],
        ],
        'stress' => [
            ['content' => 'Respirez. Lâchez prise. Et rappelez-vous que ce seul moment est le seul que vous ayez vraiment.', 'author' => 'Oprah Winfrey'],
            ['content' => 'Le calme est une superforce.', 'author' => 'Ralph Waldo Emerson'],
            ['content' => 'Dans la tempête, soyez votre propre ancre.', 'author' => 'Proverbe'],
        ],
        'logique' => [
            ['content' => 'La logique vous mènera d\'un point A à un point B. L\'imagination vous mènera partout.', 'author' => 'Albert Einstein'],
            ['content' => 'La connaissance s\'acquiert par l\'expérience, tout le reste n\'est que de l\'information.', 'author' => 'Albert Einstein'],
            ['content' => 'Penser est le travail le plus difficile qui soit, c\'est pourquoi si peu de gens s\'y livrent.', 'author' => 'Henry Ford'],
        ],
        'mémoire' => [
            ['content' => 'L\'intelligence, c\'est la capacité de s\'adapter au changement.', 'author' => 'Stephen Hawking'],
            ['content' => 'L\'éducation est l\'arme la plus puissante pour changer le monde.', 'author' => 'Nelson Mandela'],
            ['content' => 'Apprendre sans réfléchir est vain. Réfléchir sans apprendre est dangereux.', 'author' => 'Confucius'],
        ],
        'default' => [
            ['content' => 'Le seul moyen de faire du bon travail est d\'aimer ce que vous faites.', 'author' => 'Steve Jobs'],
            ['content' => 'La vie, c\'est ce qui se passe quand vous êtes occupé à faire d\'autres projets.', 'author' => 'John Lennon'],
            ['content' => 'Croyez en vous-même et tout devient possible.', 'author' => 'Anonyme'],
        ],
    ];

    public function __construct(private readonly HttpClientInterface $httpClient) {}

    /**
     * @return array{content: string, author: string, tags: string[]}
     */
    public function getRandomQuote(string $testType = ''): array
    {
        // On tente ZenQuotes, mais on retourne toujours quelque chose
        try {
            $response = $this->httpClient->request('GET', self::BASE_URL, [
                'timeout' => 3,
                'headers' => [
                    'Accept'     => 'application/json',
                    'User-Agent' => 'Mozilla/5.0 (compatible; MindAura/1.0)',
                ],
            ]);

            if ($response->getStatusCode() === 200) {
                $data = $response->toArray();
                if (!empty($data[0]['q']) && !empty($data[0]['a'])) {
                    return [
                        'content' => $data[0]['q'],
                        'author'  => $data[0]['a'],
                        'tags'    => [$this->resolveTag($testType)],
                    ];
                }
            }
        } catch (\Throwable) {
            // Silencieux — on tombe sur les fallbacks
        }

        return $this->getFallback($testType);
    }

    /**
     * @return array{content: string, author: string, tags: string[]}
     */
    private function getFallback(string $testType = ''): array
    {
        $key      = strtolower($testType);
        $pool     = self::FALLBACKS[$key] ?? self::FALLBACKS['default'];
        $picked   = $pool[array_rand($pool)];

        return [
            'content' => $picked['content'],
            'author'  => $picked['author'],
            'tags'    => [$this->resolveTag($testType)],
        ];
    }

    private function resolveTag(string $testType): string
    {
        return match (strtolower($testType)) {
            'personnalité' => 'Personnalité',
            'stress'       => 'Bien-être',
            'logique'      => 'Sagesse',
            'mémoire'      => 'Intelligence',
            default        => 'Inspiration',
        };
    }
}