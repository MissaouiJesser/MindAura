<?php

namespace App\Service;

use LucianoTonet\GroqPHP\Groq;
use LucianoTonet\GroqPHP\GroqException;
use Psr\Log\LoggerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class GroqService
{
    private Groq $groq;
    private string $model;
    /** @var list<string> */
    private array $fallbackModels;
    private LoggerInterface $logger;
    private CacheInterface $cache;

    // TTL du cache pour une analyse (1 heure)
    private const CACHE_TTL = 3600;

    public function __construct(
        string $groqApiKey,
        string $groqModel,
        string $fallbackModelsCsv,
        LoggerInterface $logger,
        CacheInterface $cache
    ) {
        $this->groq           = new Groq($groqApiKey);
        $this->model          = $groqModel;
        $this->fallbackModels = $this->parseFallbackModels($fallbackModelsCsv);
        $this->logger         = $logger;
        $this->cache          = $cache;
    }

    public function analyzeTestResult(
        string $testTitle,
        string $testType,
        int $score,
        int $answeredQuestions,
        int $totalQuestions
    ): string {
        // Cache : même test + même score = même réponse
        $cacheKey = 'groq_' . md5($testTitle . $testType . $score . $totalQuestions);

        return $this->cache->get($cacheKey, function (ItemInterface $item) use (
            $testTitle, $testType, $score, $answeredQuestions, $totalQuestions
        ): string {
            $item->expiresAfter(self::CACHE_TTL);

            $prompt = $this->buildPrompt($testTitle, $testType, $score, $answeredQuestions, $totalQuestions);
            $models = $this->orderedModelsToTry();

            foreach ($models as $index => $model) {
                if ($index > 0) {
                    $this->logger->info('Groq essai modèle de secours', ['model' => $model]);
                }
                $text = $this->generateWithModel($model, $prompt);
                if ($text !== null) {
                    return $text;
                }
            }

            // Ne pas mettre en cache l'échec — on réessaie la prochaine fois
            $item->expiresAfter(0);
            return 'Analyse temporairement indisponible.';
        });
    }

    private function generateWithModel(string $model, string $prompt): ?string
    {
        try {
            $response = $this->groq->chat()->completions()->create([
                'model'    => $model,
                'messages' => [
                    ['role' => 'user', 'content' => $prompt],
                ],
            ]);

            // Narrow the type: Stream objects don't have 'choices'
            if (!is_array($response)) {
                $this->logger->warning('Groq réponse inattendue (stream)', ['model' => $model]);
                return null;
            }

            $content = $response['choices'][0]['message']['content'] ?? null;

            if (!is_string($content) || trim($content) === '') {
                $this->logger->warning('Groq réponse vide', ['model' => $model]);
                return null;
            }

            return trim($content);

        } catch (GroqException $e) {
            $this->logger->error('Groq erreur', [
                'model'   => $model,
                'message' => $e->getMessage(),
                'code'    => $e->getCode(),
            ]);
            return null;
        }
    }

    private function buildPrompt(
        string $testTitle,
        string $testType,
        int $score,
        int $answeredQuestions,
        int $totalQuestions
    ): string {
        $percentage = $totalQuestions > 0
            ? round(($score / $totalQuestions) * 100)
            : 0;

        return sprintf(
            "Tu es un expert en évaluation pédagogique. Analyse le résultat suivant de manière bienveillante et constructive.\n\n" .
            "Test : %s\n" .
            "Type : %s\n" .
            "Score : %d / %d (%.0f%%)\n" .
            "Questions répondues : %d / %d\n\n" .
            "Fournis une analyse courte (3-4 phrases) qui :\n" .
            "1. Évalue le niveau de performance\n" .
            "2. Identifie les points forts\n" .
            "3. Propose une piste d'amélioration concrète\n\n" .
            "Réponds directement sans introduction ni titre.",
            $testTitle,
            $testType,
            $score,
            $totalQuestions,
            $percentage,
            $answeredQuestions,
            $totalQuestions
        );
    }

    /**
     * @return list<string>
     */
    private function orderedModelsToTry(): array
    {
        return array_values(array_unique(
            array_merge([$this->model], $this->fallbackModels)
        ));
    }

    /**
     * @return list<string>
     */
    private function parseFallbackModels(string $csv): array
    {
        if (trim($csv) === '') {
            return [];
        }

        /** @var list<string> */
        return array_values(array_filter(
            array_map('trim', explode(',', $csv)),
            static fn (string $m): bool => $m !== ''
        ));
    }
}