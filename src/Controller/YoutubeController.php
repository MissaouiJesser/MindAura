<?php

namespace App\Controller;

use App\Service\YoutubeService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

class YoutubeController extends AbstractController
{
    #[Route('/admin/youtube/search', name: 'youtube_search')]
    public function search(Request $request, YoutubeService $youtube): JsonResponse
    {
        $query = $request->query->getString('q', 'education');
        
        // YoutubeService::suggest() retourne directement un tableau de vidéos formatées
        $videos = $youtube->suggest($query, 10);

        return $this->json(['videos' => $videos]);
    }
}