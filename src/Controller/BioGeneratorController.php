<?php

namespace App\Controller;

use App\Service\BioGeneratorService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Endpoints AJAX pour la génération et la sauvegarde de bio IA.
 *
 * Routes :
 *   POST /profil/bio/generate  → génère une bio via OpenAI
 *   POST /profil/bio/save      → sauvegarde la bio générée en base
 *
 * Ces routes sont appelées en XHR depuis site/profil/edit.html.twig.
 * Elles requièrent ROLE_USER (utilisateur connecté).
 */
#[IsGranted('ROLE_USER')]
class BioGeneratorController extends AbstractController
{
    // ──────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Génère une bio IA pour l'utilisateur connecté.
     *
     * Body JSON attendu :
     *   { "style": "professionnel" | "inspirant" | "minimaliste" }
     *
     * Réponse JSON :
     *   { "success": true,  "bio": "…" }
     *   { "success": false, "error": "…" }
     */
    #[Route('/profil/bio/generate', name: 'app_profil_bio_generate', methods: ['POST'])]
    public function generate(
        Request              $request,
        BioGeneratorService  $bioGenerator
    ): JsonResponse {
        // ── Lire et valider le style ───────────────────────────────────
        $data  = json_decode($request->getContent(), true) ?? [];
        $style = trim($data['style'] ?? 'professionnel');

        $validStyles = array_keys(BioGeneratorService::getStyles());
        if (!in_array($style, $validStyles, true)) {
            return $this->json([
                'success' => false,
                'error'   => 'Style invalide. Choisissez : ' . implode(', ', $validStyles),
            ], 400);
        }

        // ── Générer via le service ─────────────────────────────────────
        /** @var \App\Entity\Utilisateurs $currentUser */
        $currentUser = $this->getUser();
        $result = $bioGenerator->generate($currentUser, $style);

        if (isset($result['error'])) {
            return $this->json(['success' => false, 'error' => $result['error']], 500);
        }

        return $this->json(['success' => true, 'bio' => $result['bio']]);
    }

    // ──────────────────────────────────────────────────────────────────────
    //  SAUVEGARDE
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Sauvegarde la bio générée dans le profil de l'utilisateur connecté.
     *
     * Body JSON attendu :
     *   { "bio": "…texte…" }
     *
     * Réponse JSON :
     *   { "success": true }
     *   { "success": false, "error": "…" }
     */
    #[Route('/profil/bio/save', name: 'app_profil_bio_save', methods: ['POST'])]
    public function save(
        Request                $request,
        EntityManagerInterface $em
    ): JsonResponse {
        $data = json_decode($request->getContent(), true) ?? [];
        $bio  = trim($data['bio'] ?? '');

        if (empty($bio)) {
            return $this->json(['success' => false, 'error' => 'La bio ne peut pas être vide.'], 400);
        }

        if (mb_strlen($bio) > 600) {
            return $this->json(['success' => false, 'error' => 'La bio est trop longue (600 caractères max).'], 400);
        }

        /** @var \App\Entity\Utilisateurs $user */
        $user = $this->getUser();
        $user->setBioUtilisateur($bio);
        $em->flush();

        return $this->json(['success' => true]);
    }
}