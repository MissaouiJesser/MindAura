<?php

namespace App\Controller;

use App\Service\ArticleExtractor;
use App\Service\NotificationServiceRessource;
use App\Entity\Ressources;
use App\Repository\CommentairesRepository;
use App\Repository\RessourcesRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\String\Slugger\SluggerInterface;
use App\Service\AudioUrlExtractor;
use League\Flysystem\FilesystemOperator;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/admin/ressources', name: 'admin_ressources_')]
class AdminRessourcesController extends AbstractController
{
    public function __construct(
        private NotificationServiceRessource $notificationService,
        private FilesystemOperator $defaultStorage
    ) {}

    // ─────────────────────────────────────────────────────
    // INDEX
    // ─────────────────────────────────────────────────────
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(Request $request, RessourcesRepository $repo): Response
    {
        // FIX lignes 44/46 : getString() garantit string|null propre pour PHPStan
        $q         = $request->query->getString('q') ?: null;
        $type      = $request->query->getString('type') ?: null;
        $niveau    = $request->query->getString('niveau') ?: null;
        $categorie = $request->query->getString('categorie') ?: null;
        $sort      = $request->query->getString('sort') ?: 'date_desc';
        $page      = max(1, $request->query->getInt('page', 1));
        $limit     = 9;

        $total      = $repo->countSearch($q, $type, $niveau, $categorie);
        $nbPages    = max(1, (int) ceil($total / $limit));
        $ressources = $repo->search($q, $type, $niveau, $categorie, $sort, $page, $limit);

        $params = [
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
                'page'    => $page,
                'nbPages' => $nbPages,
                'total'   => $total,
            ],
        ];

        return $this->render('admin/ressources/indexR.html.twig', $params);
    }

    // ─────────────────────────────────────────────────────
    // EXTRACT AUDIO
    // ─────────────────────────────────────────────────────
    #[Route('/extract-audio', name: 'extract_audio', methods: ['POST'])]
    public function extractAudio(Request $request, AudioUrlExtractor $extractor): JsonResponse
    {
        // FIX ligne 77 : getString() → string garanti, plus de bool|float|int|...
        $url = $request->request->getString('url');
        if (!$url) {
            return $this->json(['error' => 'URL manquante'], 400);
        }
        $audioUrl = $extractor->extract($url);
        return $this->json(['audioUrl' => $audioUrl]);
    }

    // ─────────────────────────────────────────────────────
    // EXTRACT ARTICLE
    // ─────────────────────────────────────────────────────
    #[Route('/extract-article', name: 'extract_article', methods: ['POST'])]
    public function extractArticle(Request $request, ArticleExtractor $extractor): JsonResponse
    {
        // FIX ligne 93 : getString() → string garanti
        $url = $request->request->getString('url');

        if (!$url || !filter_var($url, FILTER_VALIDATE_URL)) {
            return $this->json(['success' => false, 'error' => 'URL invalide'], 400);
        }

        $data = $extractor->extract($url);
        return $this->json($data);
    }

    // ─────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────
    #[Route('/dashboard', name: 'dashboard', methods: ['GET'])]
    public function dashboard(RessourcesRepository $repo, CommentairesRepository $comRepo): Response
    {
        return $this->render('admin/ressources/dashboardR.html.twig', [
            'byType'      => $repo->countByType(),
            'byCategorie' => $repo->countByCategorie(),
            'byNiveau'    => $repo->countByNiveau(),
            'topVues'     => $repo->findTopVues(5),
            'topLikes'    => $repo->findTopLikes(5),
            'signales'    => $comRepo->findSignales(),
            'total'       => $repo->count([]),
            'topThemes'   => $comRepo->getTopThemes(10),
            'sentiments'  => $comRepo->getSentimentsGlobaux(),
        ]);
    }

    // ─────────────────────────────────────────────────────
    // FORMULAIRE CRÉATION (GET)
    // ─────────────────────────────────────────────────────
    #[Route('/new', name: 'new', methods: ['GET'])]
    public function new(): Response
    {
        return $this->render('admin/ressources/newR.html.twig', [
            'contenuChoices'   => Ressources::getContenuChoices(),
            'niveauChoices'    => Ressources::getNiveauChoices(),
            'categorieChoices' => Ressources::getCategorieChoices(),
        ]);
    }

    // ─────────────────────────────────────────────────────
    // CRÉATION (POST)
    // ─────────────────────────────────────────────────────
    #[Route('/new', name: 'create', methods: ['POST'])]
    public function create(
        Request $request,
        EntityManagerInterface $em,
        SluggerInterface $slugger,
        ValidatorInterface $validator
    ): Response {
        $ressource = new Ressources();
        $this->hydrateRessource($ressource, $request);

        // FIX ligne 149 : getString() évite bool|float|int|...
        $type = $request->request->getString('contenu');
        if (in_array($type, ['Video', 'PDF'])) {
            $uploadedFile = $request->files->get('resource_file');
            if ($uploadedFile instanceof UploadedFile) {
                $ressource->setUrl($this->handleFileUploadFlysystem($uploadedFile, $slugger));
            }
        } elseif (in_array($type, ['Article', 'Podcast'])) {
            $ressource->setUrl($request->request->getString('url') ?: null);
        }

        // FIX ligne 157 : getString() évite float|int|...
        $imageFile = $request->files->get('image_file');
        if ($imageFile instanceof UploadedFile) {
            $ressource->setImageUrl($this->handleFileUploadFlysystem($imageFile, $slugger, 'images'));
        } else {
            $pexelsUrl = $request->request->getString('pexels_image_url');
            if ($pexelsUrl !== '') {
                $ressource->setImageUrl($pexelsUrl);
            }
        }

        $errors = $validator->validate($ressource);
        if (count($errors) > 0) {
            $errorMessages = [];
            foreach ($errors as $error) {
                $errorMessages[] = $error->getMessage();
            }
            return $this->render('admin/ressources/newR.html.twig', [
                'contenuChoices'   => Ressources::getContenuChoices(),
                'niveauChoices'    => Ressources::getNiveauChoices(),
                'categorieChoices' => Ressources::getCategorieChoices(),
                'errors'           => $errorMessages,
                'old'              => $request->request->all(),
            ]);
        }

        $ressource->setNbrVues(0)->setLikes(0);
        $em->persist($ressource);
        $em->flush();

        try {
            $results = $this->notificationService->notifierNouvelleRessource($ressource);
            if ($results['email']) {
                $this->addFlash('success', 'Ressource créée ✅ — Email envoyé à ' . $ressource->getEmailAuteur());
            } else {
                $this->addFlash('warning', 'Ressource créée ✅ — Email non envoyé : ' . implode(', ', $results['errors']));
            }
        } catch (\Exception $e) {
            $this->addFlash('warning', 'Ressource créée mais erreur notification : ' . $e->getMessage());
        }

        return $this->redirectToRoute('admin_ressources_index');
    }

    // ─────────────────────────────────────────────────────
    // TEST EMAIL
    // ─────────────────────────────────────────────────────
    #[Route('/test-email', name: 'test_email', methods: ['GET'])]
    public function testEmail(): Response
    {
        try {
            $email = (new \Symfony\Component\Mime\Email())
                ->from('minyarguesmi87@gmail.com')
                ->to('minyarguesmi87@gmail.com')
                ->subject('Test email MindAura')
                ->html('<p>✅ Email de test fonctionnel</p>');

            $this->container->get('mailer.mailer')->send($email);

            return new Response('✅ Email envoyé avec succès');
        } catch (\Exception $e) {
            return new Response('❌ Erreur : ' . $e->getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // TEST FLYSYSTEM
    // ─────────────────────────────────────────────────────
    #[Route('/test-flysystem', name: 'test_flysystem', methods: ['GET'])]
    public function testFlysystem(): Response
    {
        $results = [];

        try {
            $this->defaultStorage->write('test/hello.txt', 'MindAura Flysystem fonctionne ! ' . date('Y-m-d H:i:s'));
            $results[] = '✅ Écriture : OK';
        } catch (\Exception $e) {
            $results[] = '❌ Écriture : ' . $e->getMessage();
        }

        try {
            $content   = $this->defaultStorage->read('test/hello.txt');
            $results[] = '✅ Lecture : OK → "' . $content . '"';
        } catch (\Exception $e) {
            $results[] = '❌ Lecture : ' . $e->getMessage();
        }

        try {
            $exists    = $this->defaultStorage->fileExists('test/hello.txt');
            $results[] = '✅ Existence : ' . ($exists ? 'fichier trouvé' : 'fichier introuvable');
        } catch (\Exception $e) {
            $results[] = '❌ Existence : ' . $e->getMessage();
        }

        try {
            $files = $this->defaultStorage->listContents('test', false);
            $count = 0;
            foreach ($files as $file) {
                $count++;
            }
            $results[] = '✅ Listing : ' . $count . ' fichier(s) dans /test/';
        } catch (\Exception $e) {
            $results[] = '❌ Listing : ' . $e->getMessage();
        }

        try {
            $this->defaultStorage->copy('test/hello.txt', 'test/hello-copy.txt');
            $results[] = '✅ Copie : OK';
        } catch (\Exception $e) {
            $results[] = '❌ Copie : ' . $e->getMessage();
        }

        try {
            $this->defaultStorage->delete('test/hello-copy.txt');
            $results[] = '✅ Suppression : OK';
        } catch (\Exception $e) {
            $results[] = '❌ Suppression : ' . $e->getMessage();
        }

        try {
            // FIX lignes 271/282 : getParameter() peut retourner UnitEnum — on assert is_string
            $projectDirRaw = $this->getParameter('kernel.project_dir');
            if (!is_string($projectDirRaw)) {
                throw new \LogicException('kernel.project_dir doit être une chaîne.');
            }
            $imageContent = file_get_contents($projectDirRaw . '/public/images/logo.png');
            if ($imageContent !== false) {
                $this->defaultStorage->write('images/logo-test.png', $imageContent);
                $results[] = '✅ Upload image : OK';
            }
        } catch (\Exception $e) {
            $results[] = '❌ Upload image : ' . $e->getMessage();
        }

        // FIX ligne 282 : is_string() assertion au lieu de (string) cast
        $projectDirParam = $this->getParameter('kernel.project_dir');
        if (!is_string($projectDirParam)) {
            throw new \LogicException('kernel.project_dir doit être une chaîne.');
        }
        $projectDir = $projectDirParam;

        $html = '<html><head><meta charset="UTF-8">
        <style>
            body { font-family: sans-serif; padding: 30px; background: #f8f7f4; }
            h2   { color: #534AB7; }
            li   { padding: 8px 12px; margin: 6px 0; border-radius: 8px;
                   background: white; border-left: 4px solid #ccc; font-size: 14px; }
            .path { font-size: 12px; color: #888; margin-top: 8px; display: block; }
        </style></head><body>
        <h2>🧪 Test Flysystem — MindAura</h2>
        <ul>';

        foreach ($results as $r) {
            $html .= '<li>' . htmlspecialchars($r) . '</li>';
        }

        $html .= '</ul>';
        $html .= '<span class="path">📁 Dossier de test : '
               . htmlspecialchars($projectDir)
               . '/public/uploads/flysystem-test/</span>';
        $html .= '</body></html>';

        return new Response($html);
    }
// ─────────────────────────────────────────────────────
// UPLOAD DEPUIS JAVA (API)
// ─────────────────────────────────────────────────────
#[Route('/api/upload/resource', name: 'api_upload_resource', methods: ['POST'])]
public function apiUploadResource(
    Request $request,
    SluggerInterface $slugger
): JsonResponse {
    /** @var UploadedFile|null $file */
    $file = $request->files->get('file');
    
    if (!$file instanceof UploadedFile) {
        return $this->json(['error' => 'Aucun fichier reçu'], 400);
    }

    try {
        $url = $this->handleFileUploadFlysystem($file, $slugger, 'resources');
        return $this->json(['url' => $url]);
    } catch (\Exception $e) {
        return $this->json(['error' => $e->getMessage()], 500);
    }
}

#[Route('/api/upload/image', name: 'api_upload_image', methods: ['POST'])]
public function apiUploadImage(
    Request $request,
    SluggerInterface $slugger
): JsonResponse {
    /** @var UploadedFile|null $file */
    $file = $request->files->get('file');
    
    if (!$file instanceof UploadedFile) {
        return $this->json(['error' => 'Aucun fichier reçu'], 400);
    }

    try {
        $url = $this->handleFileUploadFlysystem($file, $slugger, 'images');
        return $this->json(['url' => $url]);
    } catch (\Exception $e) {
        return $this->json(['error' => $e->getMessage()], 500);
    }
}
    // ─────────────────────────────────────────────────────
    // FORMULAIRE ÉDITION (GET)
    // ─────────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'edit', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function edit(int $id, RessourcesRepository $repo): Response
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        return $this->render('admin/ressources/editR.html.twig', [
            'ressource'        => $ressource,
            'contenuChoices'   => Ressources::getContenuChoices(),
            'niveauChoices'    => Ressources::getNiveauChoices(),
            'categorieChoices' => Ressources::getCategorieChoices(),
        ]);
    }

    // ─────────────────────────────────────────────────────
    // DÉTAIL (GET)
    // ─────────────────────────────────────────────────────
    #[Route('/{id}', name: 'show', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function show(int $id, RessourcesRepository $repo, CommentairesRepository $comRepo): Response
    {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        return $this->render('admin/ressources/showR.html.twig', [
            'ressource'    => $ressource,
            'commentaires' => $ressource->getCommentaires(),
        ]);
    }

    // ─────────────────────────────────────────────────────
    // MISE À JOUR (POST)
    // ─────────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'update', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function update(
        int $id,
        Request $request,
        RessourcesRepository $repo,
        EntityManagerInterface $em,
        SluggerInterface $slugger,
        ValidatorInterface $validator
    ): Response {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        $this->hydrateRessource($ressource, $request);

        // FIX ligne 350 : getString() pour $type et $url
        $type         = $request->request->getString('contenu');
        $uploadedFile = $request->files->get('resource_file');
        if ($uploadedFile instanceof UploadedFile && in_array($type, ['Video', 'PDF'])) {
            $ressource->setUrl($this->handleFileUploadFlysystem($uploadedFile, $slugger));
        } elseif (in_array($type, ['Article', 'Podcast'])) {
            $url = $request->request->getString('url');
            if ($url !== '') {
                $ressource->setUrl($url);
            }
        }

        // FIX ligne 358 : getString() pour pexels_image_url
        $imageFile = $request->files->get('image_file');
        if ($imageFile instanceof UploadedFile) {
            $ressource->setImageUrl($this->handleFileUploadFlysystem($imageFile, $slugger, 'images'));
        } else {
            $pexelsUrl = $request->request->getString('pexels_image_url');
            if ($pexelsUrl !== '') {
                $ressource->setImageUrl($pexelsUrl);
            }
        }

        $errors = $validator->validate($ressource);
        if (count($errors) > 0) {
            foreach ($errors as $error) {
                $this->addFlash('error', $error->getMessage());
            }
            return $this->redirectToRoute('admin_ressources_edit', ['id' => $id]);
        }

        $em->flush();
        $this->addFlash('success', 'Ressource modifiée avec succès.');
        return $this->redirectToRoute('admin_ressources_index');
    }

    // ─────────────────────────────────────────────────────
    // SUPPRESSION (POST)
    // ─────────────────────────────────────────────────────
    #[Route('/{id}/delete', name: 'delete', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function delete(
        int $id,
        Request $request,
        RessourcesRepository $repo,
        EntityManagerInterface $em
    ): Response {
        $ressource = $repo->find($id);
        if (!$ressource) {
            throw $this->createNotFoundException();
        }

        // FIX ligne 387 : getString() → string garanti pour isCsrfTokenValid()
        $token = $request->request->getString('_token');
        if ($this->isCsrfTokenValid('delete' . $id, $token)) {
            $em->remove($ressource);
            $em->flush();
            $this->addFlash('success', 'Ressource supprimée avec succès.');
        }

        return $this->redirectToRoute('admin_ressources_index');
    }

    // ─────────────────────────────────────────────────────
    // MÉTHODES PRIVÉES
    // ─────────────────────────────────────────────────────

    /**
     * FIX lignes 402–414 : getString() / getInt() garantissent les types attendus par PHPStan.
     * trim() n'accepte que string → getString() obligatoire.
     * setResume/setContenu/… n'acceptent que string → getString() obligatoire.
     * new \DateTime() n'accepte que string → getString() obligatoire.
     */
    private function hydrateRessource(Ressources $ressource, Request $request): void
    {
        $ressource
            ->setTitre(trim($request->request->getString('titre')))
            ->setResume($request->request->getString('resume'))
            ->setContenu($request->request->getString('contenu'))
            ->setCategorie($request->request->getString('categorie'))
            ->setNiveau($request->request->getString('niveau'))
            ->setTags($request->request->getString('tags'))
            ->setEmailAuteur($request->request->getString('email_auteur') ?: null)
            ->setDureeLecture(max(0, $request->request->getInt('duree_lecture')));

        // FIX ligne 414 : getString() garantit string pour new \DateTime()
        $dateStr = $request->request->getString('date_publication');
        if ($dateStr !== '') {
            try {
                $ressource->setDatePublication(new \DateTime($dateStr));
            } catch (\Exception) {}
        }
    }

    /**
     * FIX ligne 430 : vérification de fopen() avant fclose() pour éviter resource|false.
     */
    private function handleFileUploadFlysystem(
        UploadedFile $file,
        SluggerInterface $slugger,
        string $subdir = 'resources'
    ): string {
        $safeFilename = $slugger->slug(pathinfo($file->getClientOriginalName(), PATHINFO_FILENAME));
        $newFilename  = $safeFilename . '-' . uniqid() . '.' . $file->guessExtension();
        $path         = $subdir . '/' . $newFilename;

        // FIX ligne 430 : fopen peut retourner false → on vérifie avant fclose
        $stream = fopen($file->getPathname(), 'r');
        if ($stream === false) {
            throw new \RuntimeException('Impossible d\'ouvrir le fichier uploadé : ' . $file->getPathname());
        }

        $this->defaultStorage->writeStream($path, $stream);
        fclose($stream);

        return '/uploads/flysystem-test/' . $path;
    }
}