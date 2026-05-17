<?php

namespace App\Controller;

use App\Entity\Commentaires;
use App\Entity\Ressources;
use App\Entity\Utilisateurs;
use App\Repository\CommentairesRepository;
use App\Repository\RessourcesRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\HttpFoundation\JsonResponse;
use App\Service\AudioUrlExtractor;
use App\Service\ArticleExtractor;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use App\Service\YoutubeService;
use App\Service\CommentAnalysisService;
use App\Service\TranslationService;
use App\Service\NotificationServiceRessource;

use Dompdf\Dompdf;
use Dompdf\Options;

#[Route('/ressources', name: 'ressources_')]
class RessourcesController extends AbstractController
{
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(Request $request, RessourcesRepository $repo): Response
    {
        $q         = $request->query->getString('q') ?: null;
        $type      = $request->query->getString('type') ?: null;
        $niveau    = $request->query->getString('niveau') ?: null;
        $categorie = $request->query->getString('categorie') ?: null;
        $sort      = $request->query->getString('sort') ?: 'date_desc';
        $page      = max(1, $request->query->getInt('page', 1));
        $limit     = 4;

        $total      = $repo->countSearch($q, $type, $niveau, $categorie);
        $pages      = max(1, (int) ceil($total / $limit));
        $ressources = $repo->search($q, $type, $niveau, $categorie, $sort, $page, $limit);

        foreach ($ressources as $r) {
            if ($r->getImageUrl() && str_starts_with($r->getImageUrl(), '/uploads')) {
                $r->setImageUrl($this->encodeFilePath($r->getImageUrl()));
            }
            if ($r->getUrl() && str_starts_with($r->getUrl(), '/uploads')) {
                $r->setUrl($this->encodeFilePath($r->getUrl()));
            }
        }

        return $this->render('ressources/indexR.html.twig', [
            'ressources'       => $ressources,
            'contenuChoices'   => Ressources::getContenuChoices(),
            'niveauChoices'    => Ressources::getNiveauChoices(),
            'categorieChoices' => Ressources::getCategorieChoices(),
            'currentSort'      => $sort,
            'currentQ'         => $q,
            'currentType'      => $type,
            'currentNiveau'    => $niveau,
            'currentCategorie' => $categorie,
            'pagination'       => [
                'current' => $page,
                'pages'   => $pages,
                'total'   => $total,
            ],
        ]);
    }

  #[Route('/recommended', name: 'recommended', methods: ['GET'])]
public function recommended(Request $request, RessourcesRepository $repo): JsonResponse
{
    $categorie = $request->query->getString('categorie') ?: null;
    $excludeId = $request->query->getInt('exclude') ?: null;
    $limit     = min($request->query->getInt('limit', 3), 20);

    $ressources = $repo->findRecommended($categorie, $excludeId, $limit);
    return $this->json($ressources);
}

    #[Route('/extract-audio', name: 'extract_audio', methods: ['POST'])]
    public function extractAudio(Request $request, AudioUrlExtractor $extractor): JsonResponse
    {
        $url = $request->request->getString('url');
        if (!$url) {
            return $this->json(['error' => 'URL manquante'], 400);
        }
        $audioUrl = $extractor->extract($url);
        return $this->json(['audioUrl' => $audioUrl]);
    }

    #[Route('/extract-article', name: 'extract_article', methods: ['POST'])]
    public function extractArticle(Request $request, ArticleExtractor $extractor): JsonResponse
    {
        $url = $request->request->getString('url');
        if (!$url || !filter_var($url, FILTER_VALIDATE_URL)) {
            return $this->json(['success' => false, 'error' => 'URL invalide'], 400);
        }
        $data = $extractor->extract($url);
        return $this->json($data);
    }

    #[Route('/{id}/export-pdf', name: 'export_pdf', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function exportPdf(int $id, RessourcesRepository $repo, ArticleExtractor $extractor): Response
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        $data    = $extractor->extract((string) $ressource->getUrl());
        $titre   = $data['title']   ?? $ressource->getTitre();
        $contenu = $data['content'] ?? '';
        $contenu = strip_tags($contenu, '<p><h2><h3><li><ul><ol><strong><em><br>');

        $html = $this->renderView('ressources/pdf_export.html.twig', [
            'ressource'  => $ressource,
            'titre'      => $titre,
            'contenu'    => $contenu,
            'exportedAt' => new \DateTime(),
        ]);

        $options = new Options();
        $options->set('defaultFont', 'DejaVu Sans');
        $options->set('isHtml5ParserEnabled', true);

        $projectDir = $this->getParameter('kernel.project_dir');
        if (!is_string($projectDir)) {
            throw new \RuntimeException('Le paramètre kernel.project_dir doit être une chaîne.');
        }
        $options->set('chroot', $projectDir . '/public');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return new Response($dompdf->output(), 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="article-' . $id . '.pdf"',
        ]);
    }

    #[Route('/{id}/export-pdf-traduit', name: 'export_pdf_traduit', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function exportPdfTraduit(int $id, RessourcesRepository $repo, Request $request): Response
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        $data    = json_decode($request->getContent(), true);
        $titre   = strip_tags($data['titre']  ?? $ressource->getTitre());
        $contenu = $data['contenu'] ?? '';
        $contenu = strip_tags($contenu, '<p><h2><h3><li><ul><ol><strong><em><br>');

        $html = $this->renderView('ressources/pdf_export.html.twig', [
            'ressource'  => $ressource,
            'titre'      => $titre,
            'contenu'    => $contenu,
            'exportedAt' => new \DateTime(),
        ]);

        $options = new Options();
        $options->set('defaultFont', 'DejaVu Sans');
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', true);

        $projectDir = $this->getParameter('kernel.project_dir');
        if (!is_string($projectDir)) {
            throw new \RuntimeException('Le paramètre kernel.project_dir doit être une chaîne.');
        }
        $options->set('chroot', $projectDir . '/public');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $filename = 'article-' . $id . '-' . date('Ymd') . '.pdf';

        return new Response($dompdf->output(), 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => 'attachment; filename="' . $filename . '"',
        ]);
    }

    #[Route('/{id}', name: 'show', methods: ['GET', 'POST'], requirements: ['id' => '\d+'])]
    public function show(
        int $id,
        Request $request,
        RessourcesRepository $repo,
        CommentairesRepository $comRepo,
        EntityManagerInterface $em,
        ValidatorInterface $validator,
        YoutubeService $youtube,
        CommentAnalysisService $analysisService,
        NotificationServiceRessource $notificationService
    ): Response {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException('Ressource introuvable.');
        }

        $ressource->incrementVues();
        $em->flush();

        if ($ressource->getImageUrl() && str_starts_with($ressource->getImageUrl(), '/uploads')) {
            $ressource->setImageUrl($this->encodeFilePath($ressource->getImageUrl()));
        }
        if ($ressource->getUrl() && str_starts_with($ressource->getUrl(), '/uploads')) {
            $ressource->setUrl($this->encodeFilePath($ressource->getUrl()));
        }

        $suggestions = [];
        if ($ressource->getContenu() === 'Video') {
            try {
                $query       = $ressource->getTitre() . ' ' . $ressource->getCategorie();
                $suggestions = $youtube->suggest($query, 6);
            } catch (\Exception) {
                $suggestions = [];
            }
        }

        $recommandations = $repo->findRecommendedEntities(
            $ressource->getCategorie(),
            $ressource->getId(),
            3
        );

        if ($request->isMethod('POST')) {
            $user = $this->getUser();
            if (!$user) {
                $this->addFlash('error', 'Vous devez être connecté pour commenter.');
                return $this->redirectToRoute('ressources_show', ['id' => $id]);
            }

            /** @var Utilisateurs $user */
            $token = $request->request->getString('_token');
            if (!$this->isCsrfTokenValid('comment_' . $id, $token)) {
                $this->addFlash('error', 'Token de sécurité invalide.');
                return $this->redirectToRoute('ressources_show', ['id' => $id]);
            }

            $userId = $user->getIdUtilisateur();
            if ($userId === null) {
                $this->addFlash('error', 'Identifiant utilisateur invalide.');
                return $this->redirectToRoute('ressources_show', ['id' => $id]);
            }

            $commentaire = new Commentaires();
            $commentaire
                ->setRessource($ressource)
                ->setUserName($user->getUserIdentifier())
                ->setContenu(trim($request->request->getString('contenu')))
                ->setIdUser($userId)   // ← int garanti
                ->setDatePublication(new \DateTime());

            $parentId = $request->request->getInt('parent_id');
            if ($parentId) {
                /** @var Commentaires|null $parent */
                $parent = $comRepo->find($parentId);
                if ($parent && $parent->getRessource() && $parent->getRessource()->getId() === $id) {
                    $commentaire->setParent($parent);
                }
            }

            $errors = $validator->validate($commentaire);
            if (count($errors) > 0) {
                foreach ($errors as $error) {
                    $this->addFlash('error', $error->getMessage());
                }
                return $this->redirectToRoute('ressources_show', ['id' => $id, '_fragment' => 'commentaires']);
            }

            $em->persist($commentaire);
            $em->flush();

            $result = $analysisService->analyze($commentaire->getContenu());
           $commentaire->setSentiment($result['sentiment']);
$commentaire->setThemes($result['themes']);
            $commentaire->setAiSummary($result['summary'] ?? null);
            $em->flush();

            try {
                $notificationService->notifierNouveauCommentaire($commentaire);
            } catch (\Exception) {
            }

            $this->addFlash('success', 'Commentaire ajouté.');
            return $this->redirectToRoute('ressources_show', ['id' => $id, '_fragment' => 'commentaires']);
        }

        return $this->render('ressources/showR.html.twig', [
            'ressource'       => $ressource,
            'commentaires'    => $comRepo->findRacinesByRessource($id),
            'suggestions'     => $suggestions,
            'recommandations' => $recommandations,
        ]);
    }

    #[Route('/{id}/translate', name: 'translate', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function translate(int $id, Request $request, RessourcesRepository $repo, TranslationService $translator): JsonResponse
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            return $this->json(['error' => 'Non trouvé'], 404);
        }

        $lang = $request->request->getString('lang') ?: 'FR';

        try {
            $translated = $translator->translateRessource([
                'titre'       => $ressource->getTitre(),
                'description' => $ressource->getResume(),
            ], $lang);
            return $this->json($translated);
        } catch (\Exception $e) {
            return $this->json(['error' => $e->getMessage()], 500);
        }
    }

    #[Route('/{id}/like', name: 'like', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function like(int $id, RessourcesRepository $repo, EntityManagerInterface $em): Response
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            return $this->json(['error' => 'Non trouvé'], 404);
        }

        $ressource->incrementLikes();
        $em->flush();

        return $this->json(['likes' => $ressource->getLikes()]);
    }

    private function encodeFilePath(string $path): string
    {
        if (str_starts_with($path, 'http://') || str_starts_with($path, 'https://')) {
            return $path;
        }
        $parts   = explode('/', $path);
        $encoded = array_map(fn($part) => rawurlencode($part), $parts);
        return implode('/', $encoded);
    }
}