<?php

// src/Controller/PexelsController.php

namespace App\Controller;

use App\Service\PexelsService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/admin/pexels', name: 'pexels_')]
class PexelsController extends AbstractController
{
    public function __construct(private PexelsService $pexels) {}

    #[Route('/search', name: 'search', methods: ['GET'])]
    public function search(Request $request): JsonResponse
    {
        $query   = trim($request->query->getString('q', ''));
        $perPage = min($request->query->getInt('per_page', 9), 30);
        $color   = $request->query->getString('color', '');

        if (empty($query)) {
            return $this->json(['error' => 'Requête vide'], 400);
        }

        $photos = $this->pexels->search($query, $perPage, 'medium', 'fr-FR', $color);

        return $this->json([
            'photos' => $photos,
            'total'  => count($photos),
            'query'  => $query,
        ]);
    }

    #[Route('/suggest', name: 'suggest', methods: ['GET'])]
    public function suggest(Request $request): JsonResponse
    {
        $categorie = $request->query->getString('categorie', '');
        $contenu   = $request->query->getString('contenu', '');
        $count     = min($request->query->getInt('count', 6), 12);

        if (empty($categorie) && empty($contenu)) {
            return $this->json(['error' => 'Paramètres manquants'], 400);
        }

        if (!empty($categorie) && !empty($contenu)) {
            $photos = $this->pexels->suggestImageForRessource($categorie, $contenu, $count);
        } elseif (!empty($categorie)) {
            $photos = $this->pexels->getImageForCategory($categorie, $count);
        } else {
            $photos = $this->pexels->getImageForContentType($contenu, $count);
        }

        return $this->json([
            'photos'    => $photos,
            'total'     => count($photos),
            'categorie' => $categorie,
            'contenu'   => $contenu,
        ]);
    }

    #[Route('/curated', name: 'curated', methods: ['GET'])]
    public function curated(Request $request): JsonResponse
    {
        $perPage = min($request->query->getInt('per_page', 6), 20);
        $photos  = $this->pexels->getCurated($perPage);

        return $this->json(['photos' => $photos, 'total' => count($photos)]);
    }
}