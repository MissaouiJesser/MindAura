<?php

namespace App\Service;

class BadWordsFilter
{
    /**
     * @var array<int, string>
     */
    private array $badWords = [
        'violence',
        'connard', 'connasse', 'salope', 'pute', 'putain', 'merde',
        'enculé', 'enculer', 'bâtard', 'batard', 'idiot', 'idiote',
        'imbécile', 'imbecile', 'crétin', 'cretin', 'abruti', 'débile',
        'debile', 'con', 'conne', 'nique', 'niquer', 'fdp', 'pd',
        'trouduc', 'ta gueule', 'tagueule', 'cul', 'bordel',
        'fuck', 'fucking', 'shit', 'bitch', 'asshole', 'bastard',
        'cunt', 'dick', 'pussy', 'motherfucker', 'bullshit', 'damn',
        'whore', 'slut', 'retard', 'faggot',
        'kalb', 'zebi', 'wled', 'hmaq',
    ];

    public function containsBadWords(string $text): bool
    {
        $normalized = $this->normalize($text);

        foreach ($this->badWords as $word) {
            $pattern = '/(?<![a-z0-9])' . preg_quote($this->normalize($word), '/') . '(?![a-z0-9])/u';
            if (preg_match($pattern, $normalized)) {
                return true;
            }
        }

        return false;
    }

    public function clean(string $text): string
    {
        $result = $text;

        foreach ($this->badWords as $word) {
            $replaced = preg_replace_callback(
                '/' . preg_quote($word, '/') . '/ui',
                fn($m) => str_repeat('*', mb_strlen($m[0])),
                $result
            );
            if (is_string($replaced)) {
                $result = $replaced;
            }
        }

        return $result;
    }

    private function normalize(string $text): string
    {
        $lowercase = mb_strtolower($text, 'UTF-8');

        $withoutAccents = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $lowercase);
        if ($withoutAccents === false) {
            $withoutAccents = $lowercase;
        }

        $cleaned = preg_replace('/[^a-z0-9\s]/u', '', $withoutAccents);
        // ✅ Correction : preg_replace peut retourner null, mais jamais false
        if ($cleaned === null) {
            $cleaned = $withoutAccents;
        }

        return trim($cleaned);
    }
}