<?php

namespace App\Controller;

use App\Service\VoiceRssService;
use App\Entity\Objectif;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

class TtsController extends AbstractController
{
    #[Route('/tts', name: 'tts_form', methods: ['GET'])]
    public function index(): Response
    {
        return $this->render('tts/index.html.twig');
    }

    #[Route('/tts/generate', name: 'tts_generate', methods: ['POST'])]
    public function generate(Request $request, VoiceRssService $tts): Response
    {
        $text  = (string) $request->request->get('text', '');
        $lang  = (string) $request->request->get('lang', 'fr-fr');
        $codec = (string) $request->request->get('codec', 'mp3');

        if (empty(trim($text))) {
            return new Response('Texte vide.', 400);
        }

        try {
            $audio = $tts->textToSpeech($text, $lang, $codec);
        } catch (\RuntimeException $e) {
            return new Response($e->getMessage(), 500);
        }

        $mimeMap = ['mp3' => 'audio/mpeg', 'wav' => 'audio/wav', 'ogg' => 'audio/ogg'];
        $mime = $mimeMap[$codec] ?? 'audio/mpeg';

        return new Response($audio, 200, [
            'Content-Type'        => $mime,
            'Content-Disposition' => 'inline; filename="speech.' . $codec . '"',
        ]);
    }

    #[Route('/tts/objectif/{id}', name: 'tts_objectif', methods: ['GET'])]
    public function readObjectif(
        Objectif $objectif,
        VoiceRssService $tts
    ): Response {
        $text = $objectif->getTitre() . '. ' . $objectif->getDescription();

        try {
            $audio = $tts->textToSpeech($text, 'fr-fr', 'mp3');
        } catch (\RuntimeException $e) {
            return new Response($e->getMessage(), 500);
        }

        return new Response($audio, 200, [
            'Content-Type'  => 'audio/mpeg',
            'Cache-Control' => 'private, max-age=3600',
        ]);
    }
}