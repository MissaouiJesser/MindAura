<?php

namespace App\Twig;

use Twig\Extension\AbstractExtension;
use Twig\TwigFunction;

class PodcastExtension extends AbstractExtension
{
    public function getFunctions(): array
    {
        return [
            new TwigFunction('get_podcast_embed', [$this, 'getEmbedHtml']),
        ];
    }

    public function getEmbedHtml(?string $url): ?string
    {
        if (!$url) return null;
        if (!filter_var($url, FILTER_VALIDATE_URL)) return null;

        if (str_contains($url, 'spotify.com')) {

            // ── Episode ──
            // https://open.spotify.com/episode/1vqO9k8TiI9QnOPq0...
            if (preg_match('#/episode/([a-zA-Z0-9]+)#', $url, $m)) {
                return '<iframe style="border-radius:16px"
                    src="https://open.spotify.com/embed/episode/' . $m[1] . '?utm_source=generator"
                    width="100%"
                    height="152"
                    frameborder="0"
                    allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                    loading="lazy">
                </iframe>';
            }

            // ── Show (série entière) ──
            // https://open.spotify.com/show/5a0zPZlpuMg2oTRxqdZy...
            if (preg_match('#/show/([a-zA-Z0-9]+)#', $url, $m)) {
                return '<iframe style="border-radius:16px"
                    src="https://open.spotify.com/embed/show/' . $m[1] . '?utm_source=generator"
                    width="100%"
                    height="352"
                    frameborder="0"
                    allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
                    loading="lazy">
                </iframe>';
            }

            // Spotify reconnu mais format inconnu
            return '<a href="' . htmlspecialchars($url) . '"
                target="_blank" rel="noopener noreferrer"
                style="display:inline-flex;align-items:center;gap:10px;padding:14px 24px;
                       background:#1DB954;color:#fff;border-radius:40px;
                       text-decoration:none;font-size:14px;font-weight:700;">
                🎧 Écouter sur Spotify
            </a>';
        }

        // ── Deezer ──
        if (str_contains($url, 'deezer.com')) {
            if (preg_match('#/episode/(\d+)#', $url, $m)) {
                return '<iframe
                    src="https://widget.deezer.com/widget/auto/episode/' . $m[1] . '"
                    width="100%" height="152" frameborder="0"
                    allowtransparency="true" allow="encrypted-media">
                </iframe>';
            }
        }

        // ── SoundCloud ──
        if (str_contains($url, 'soundcloud.com')) {
            return '<iframe width="100%" height="166" scrolling="no" frameborder="no"
                allow="autoplay"
                src="https://w.soundcloud.com/player/?url=' . urlencode($url)
                . '&color=%23534AB7&auto_play=false&show_user=true">
            </iframe>';
        }

        // ── YouTube ──
        if (str_contains($url, 'youtube.com') || str_contains($url, 'youtu.be')) {
            $videoId = null;
            if (preg_match('#[?&]v=([a-zA-Z0-9_-]{11})#', $url, $m)) {
                $videoId = $m[1];
            } elseif (preg_match('#youtu\.be/([a-zA-Z0-9_-]{11})#', $url, $m)) {
                $videoId = $m[1];
            }
            if ($videoId) {
                return '<iframe width="100%" height="315"
                    src="https://www.youtube.com/embed/' . $videoId . '"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media;
                           gyroscope; picture-in-picture"
                    allowfullscreen>
                </iframe>';
            }
        }

        // ── Fichier audio direct ──
        if (preg_match('#\.(mp3|m4a|wav|ogg|aac|flac)(\?.*)?$#i', $url)) {
            return '<audio controls style="width:100%;border-radius:16px;">'
                . '<source src="' . htmlspecialchars($url) . '">'
                . '</audio>';
        }

        return null;
    }
}