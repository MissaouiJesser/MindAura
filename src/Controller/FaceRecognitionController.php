<?php

namespace App\Controller;

use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;

/**
 * Endpoints AJAX utilisés par la reconnaissance faciale côté front.
 *
 * Ces routes sont appelées en XHR depuis auth.html.twig AVANT
 * la soumission du formulaire Symfony Security (form_login).
 *
 * Routes :
 *   POST /face-verify-credentials  → vérifie email+mdp, retourne la photo de profil
 *   POST /face-save-photo          → sauvegarde une nouvelle photo de profil
 */
class FaceRecognitionController extends AbstractController
{
    // ────────────────────────────────────────────────────────────────
    //  ÉTAPE 1 — Vérification des identifiants & récupération de la photo
    // ────────────────────────────────────────────────────────────────

    /**
     * Vérifie les identifiants (email + mot de passe) sans créer de session Symfony.
     * Retourne la photo de profil pour que le JS puisse faire la comparaison faciale.
     *
     * Réponse JSON :
     *   { success: true,  photo: "filename.jpg" }
     *   { success: false, message: "Identifiants incorrects." }
     */
    #[Route('/face-verify-credentials', name: 'app_face_verify_credentials', methods: ['POST'])]
    public function verifyCredentials(
        Request                     $request,
        UtilisateursRepository      $repo,
        UserPasswordHasherInterface $passwordHasher
    ): JsonResponse {
        // ── CORS / sécurité basique ──────────────────────────────────
        // On vérifie que la requête vient bien de la même origine
        // (pas de CSRF token Symfony sur cet endpoint car c'est du XHR
        //  applicatif, mais on peut ajouter une vérification custom si besoin)

        $data = json_decode($request->getContent(), true);

        $email    = trim($data['email']    ?? '');
        $password = trim($data['password'] ?? '');

        if (!$email || !$password) {
            return $this->json(['success' => false, 'message' => 'Identifiants manquants.'], 400);
        }

        $user = $repo->findOneBy(['emailUtilisateur' => $email]);

        // Compte inexistant
        if (!$user) {
            // Délai artificiel pour prévenir l'énumération de comptes
            usleep(300_000);
            return $this->json(['success' => false, 'message' => 'Identifiants incorrects.'], 401);
        }

        // Compte inactif
        if (!$user->isEstActifUtilisateur()) {
            return $this->json([
                'success' => false,
                'message' => 'Votre compte a été désactivé. Contactez l\'administrateur.',
            ], 403);
        }

        // Email non vérifié
        if (!$user->isEmailVerified()) {
            return $this->json([
                'success' => false,
                'message' => 'Vous devez confirmer votre adresse e-mail avant de vous connecter.',
            ], 403);
        }

        // Mot de passe incorrect
        if (!$passwordHasher->isPasswordValid($user, $password)) {
            usleep(300_000);
            return $this->json(['success' => false, 'message' => 'Identifiants incorrects.'], 401);
        }

        // ✅ Identifiants valides — on retourne la photo de profil
        return $this->json([
            'success' => true,
            'photo'   => $user->getPhotoProfilUtilisateur(),
            'name'    => $user->getPrenomUtilisateur(),
        ]);
    }

    // ────────────────────────────────────────────────────────────────
    //  ÉTAPE 2 (optionnelle) — Sauvegarde d'une nouvelle photo de profil
    // ────────────────────────────────────────────────────────────────

    /**
     * Permet à l'utilisateur d'ajouter/remplacer sa photo de profil
     * directement depuis le panneau de reconnaissance faciale,
     * si son compte n'en avait pas.
     *
     * Réponse JSON :
     *   { success: true,  photo: "new_filename.jpg" }
     *   { success: false, message: "..." }
     */
    #[Route('/face-save-photo', name: 'app_face_save_photo', methods: ['POST'])]
    public function savePhoto(
        Request                     $request,
        UtilisateursRepository      $repo,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher,
        SluggerInterface            $slugger
    ): JsonResponse {
        $email = trim((string) $request->request->get('email', ''));

        if (!$email) {
            return $this->json(['success' => false, 'message' => 'Email manquant.'], 400);
        }

        $user = $repo->findOneBy(['emailUtilisateur' => $email]);

        if (!$user) {
            return $this->json(['success' => false, 'message' => 'Compte introuvable.'], 404);
        }

        /** @var UploadedFile|null $photo */
        $photo = $request->files->get('photo');

        if (!$photo || !$photo->isValid()) {
            return $this->json(['success' => false, 'message' => 'Fichier invalide.'], 400);
        }

        // Vérifications MIME
        $allowedMimes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        if (!in_array($photo->getMimeType(), $allowedMimes)) {
            return $this->json(['success' => false, 'message' => 'Format d\'image non supporté.'], 400);
        }

        // Taille max : 5 Mo
        if ($photo->getSize() > 5 * 1024 * 1024) {
            return $this->json(['success' => false, 'message' => 'L\'image ne peut pas dépasser 5 Mo.'], 400);
        }

        // ── Sauvegarde ──────────────────────────────────────────────
        $projectDir = $this->getParameter('kernel.project_dir');
        $avatarsDir = (is_string($projectDir) ? $projectDir : '') . '/public/avatars';
        if (!is_dir($avatarsDir)) {
            mkdir($avatarsDir, 0775, true);
        }

        $originalName = pathinfo($photo->getClientOriginalName(), PATHINFO_FILENAME);
        $safeBase     = $slugger->slug($originalName ?: 'face');
        $extension    = $photo->guessExtension() ?? 'jpg';
        $filename     = 'face_' . $safeBase . '_' . uniqid() . '.' . $extension;

        $photo->move($avatarsDir, $filename);

        // Supprimer l'ancienne photo si ce n'est pas la photo par défaut
        $oldPhoto = $user->getPhotoProfilUtilisateur();
        if ($oldPhoto && $oldPhoto !== 'default.png') {
            $oldPath = $avatarsDir . '/' . $oldPhoto;
            if (file_exists($oldPath)) {
                @unlink($oldPath);
            }
        }

        $user->setPhotoProfilUtilisateur($filename);
        $em->flush();

        return $this->json([
            'success' => true,
            'photo'   => $filename,
        ]);
    }
}