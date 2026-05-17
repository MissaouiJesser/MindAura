<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\BinaryFileResponse;
use Symfony\Component\HttpFoundation\ResponseHeaderBag;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

class FileController extends AbstractController
{
    public function __construct(
        // Inject kernel.project_dir directly — PHPStan sees string, not mixed
        #[Autowire('%kernel.project_dir%')]
        private readonly string $projectDir,
    ) {}

    /**
     * Sert les fichiers uploadés depuis public/uploads/
     * Route : /uploads/{path} — capte tous les sous-chemins
     */
    #[Route('/uploads/{path}', name: 'serve_upload', requirements: ['path' => '.+'])]
    public function serveFile(string $path): Response
    {
        // ✅ Sécurité : empêcher la traversée de répertoires (path traversal)
        if (str_contains($path, '..') || str_contains($path, "\0")) {
            throw $this->createNotFoundException('Chemin invalide.');
        }

        $filePath = $this->projectDir . '/public/uploads/' . $path;

        // ✅ Vérifier que le fichier existe
        if (!file_exists($filePath) || !is_file($filePath)) {
            throw $this->createNotFoundException(
                sprintf('Fichier introuvable : uploads/%s', $path)
            );
        }

        $response = new BinaryFileResponse($filePath);

        // ✅ Affichage inline pour PDF/images, téléchargement pour le reste
        $extension = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
        $inlineTypes = ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'mp4', 'mp3'];

        if (in_array($extension, $inlineTypes)) {
            $response->setContentDisposition(ResponseHeaderBag::DISPOSITION_INLINE);
        } else {
            $response->setContentDisposition(
                ResponseHeaderBag::DISPOSITION_ATTACHMENT,
                basename($filePath)
            );
        }

        // ✅ Cache 1 heure pour les fichiers statiques
        $response->setMaxAge(3600);
        $response->setPublic();

        return $response;
    }
}