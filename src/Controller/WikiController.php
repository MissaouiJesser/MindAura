<?php
// src/Controller/WikiController.php

namespace App\Controller;

use App\Service\WikipediaService;
use App\Service\OpenLibraryService;
use App\Repository\RessourcesRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/wiki', name: 'wiki_')]
class WikiController extends AbstractController
{
    public function __construct(
        private WikipediaService   $wikipedia,
        private OpenLibraryService $openLibrary
    ) {}

    #[Route('/summary', name: 'summary', methods: ['GET'])]
    public function summary(Request $request): JsonResponse
    {
        $title = trim($request->query->getString('title', ''));
        $lang  = $request->query->getString('lang', 'fr');

        if (empty($title)) {
            return $this->json(['error' => 'Paramètre title manquant'], 400);
        }

        $data = $this->wikipedia->getSummary($title, $lang);

        if (!$data) {
            return $this->json(['error' => 'Article introuvable'], 404);
        }

        return $this->json($data);
    }

    #[Route('/search', name: 'search', methods: ['GET'])]
    public function search(Request $request): JsonResponse
    {
        $query = trim($request->query->getString('q', ''));
        $lang  = $request->query->getString('lang', 'fr');
        $limit = min($request->query->getInt('limit', 5), 10);

        if (empty($query)) {
            return $this->json(['error' => 'Paramètre q manquant'], 400);
        }

        $results = $this->wikipedia->search($query, $lang, $limit);

        return $this->json(['results' => $results, 'query' => $query]);
    }

    #[Route('/category', name: 'category', methods: ['GET'])]
    public function byCategory(Request $request): JsonResponse
    {
        $categorie = $request->query->getString('cat', '');
        $lang      = $request->query->getString('lang', 'fr');

        $articles = $this->wikipedia->getForCategory($categorie, $lang);

        return $this->json(['articles' => $articles]);
    }

    #[Route('/ressource/{id}', name: 'ressource', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function forRessource(
        int                  $id,
        Request              $request,
        RessourcesRepository $repo
    ): JsonResponse {
        $ressource = $repo->find($id);
        if (!$ressource) {
            return $this->json(['error' => 'Ressource introuvable'], 404);
        }

        $lang      = $request->query->getString('lang', 'fr');
        $titre     = $ressource->getTitre();
        $categorie = $ressource->getCategorie();

        $wikiArticle = $this->wikipedia->getForRessource($titre, $categorie, $lang);
        $books       = $this->openLibrary->getForRessource($titre, $categorie, 3);

        return $this->json([
            'wikipedia' => $wikiArticle,
            'livres'    => $books,
            'ressource' => [
                'id'        => $id,
                'titre'     => $titre,
                'categorie' => $categorie,
            ],
        ]);
    }

    #[Route('/books', name: 'books', methods: ['GET'])]
    public function books(Request $request): JsonResponse
    {
        $query = trim($request->query->getString('q', ''));
        $lang  = $request->query->getString('lang', 'fre');
        $limit = min($request->query->getInt('limit', 6), 12);

        if (empty($query)) {
            return $this->json(['error' => 'Paramètre q manquant'], 400);
        }

        $books = $this->openLibrary->search($query, $lang ?: null, $limit);

        return $this->json(['livres' => $books, 'total' => count($books)]);
    }

    #[Route('/books/category', name: 'books_category', methods: ['GET'])]
    public function booksByCategory(Request $request): JsonResponse
    {
        $categorie = $request->query->getString('cat', '');
        $limit     = min($request->query->getInt('limit', 4), 8);

        $books = $this->openLibrary->getForCategory($categorie, $limit);

        return $this->json(['livres' => $books, 'total' => count($books)]);
    }
}