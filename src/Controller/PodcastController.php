<?php

namespace App\Controller;

use App\Entity\Ressources;
use App\Service\AudioUrlExtractor;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class PodcastController extends AbstractController
{
    #[Route('/podcast/{id}', name: 'podcast_show')]
    public function show(Ressources $ressource): Response
    {
        $embedHtml = $this->getEmbedHtml($ressource->getUrl());

        return $this->render('podcast/index.html.twig', [
            'ressource' => $ressource,
            'embedHtml' => $embedHtml,
        ]);
    }

    #[Route('/podcast/extract-audio', name: 'podcast_extract_audio', methods: ['POST'])]
    public function extractAudio(Request $request, AudioUrlExtractor $extractor): JsonResponse
    {
        $url = $request->request->getString('url');
        if (!$url) {
            return $this->json(['error' => 'URL manquante'], 400);
        }

        $audioUrl = $extractor->extract($url);
        return $this->json(['audioUrl' => $audioUrl]);
    }

    private function getEmbedHtml(?string $url): ?string
    {
        // ... identique à l’original
        if (!$url) return null;
        if (str_contains($url, 'spotify.com')) {
            if (preg_match('/episode\/([a-zA-Z0-9]+)/', $url, $matches)) {
                $id = $matches[1];
                return '<iframe src="https://open.spotify.com/embed/episode/' . $id . '" width="100%" height="152" frameborder="0" allowtransparency="true" allow="encrypted-media"></iframe>';
            }
        }
        if (str_contains($url, 'deezer.com')) {
            if (preg_match('/episode\/(\d+)/', $url, $matches)) {
                $id = $matches[1];
                return '<iframe src="https://widget.deezer.com/widget/auto/episode/' . $id . '" width="100%" height="152" frameborder="0" allowtransparency="true" allow="encrypted-media"></iframe>';
            }
        }
        if (str_contains($url, 'apple.com') && str_contains($url, 'podcast')) {
            return '<a href="' . htmlspecialchars($url) . '" target="_blank" class="apple-link">🎧 Écouter sur Apple Podcasts</a>';
        }
        return null;
    }
}