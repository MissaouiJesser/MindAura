<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\Validator\Validator\ValidatorInterface;

/**
 * Gestion du profil personnel de l'utilisateur connecté (front-office).
 *
 * Routes :
 *   GET  /profil         → voir son profil
 *   GET  /profil/edit    → formulaire de modification
 *   POST /profil/edit    → traitement des modifications
 */
#[IsGranted('ROLE_USER')]
class ProfilController extends AbstractController
{
    // ── Helper : contexte commun ─────────────────────────────────────────

    /**
     * @return array<string, mixed>
     */
    private function getProfilContext(): array
    {
        /** @var Utilisateurs $user */
        $user = $this->getUser();

        $roleMap = [
            'ROLE_ADMIN'       => ['label' => 'Administrateur', 'emoji' => '🛡️',  'slug' => 'admin'],
            'ROLE_PSYCHOLOGUE' => ['label' => 'Psychologue',    'emoji' => '🧠',  'slug' => 'psychologue'],
            'ROLE_COACH'       => ['label' => 'Coach',          'emoji' => '🎯',  'slug' => 'coach'],
            'ROLE_PATIENT'     => ['label' => 'Patient',        'emoji' => '💚',  'slug' => 'patient'],
            'ROLE_USER'        => ['label' => 'Utilisateur',    'emoji' => '👤',  'slug' => 'user'],
        ];

        $role     = $user->getRoleUtilisateur() ?? 'ROLE_USER';
        $roleInfo = $roleMap[$role] ?? $roleMap['ROLE_USER'];

        return [
            'user'       => $user,
            'roleLabel'  => $roleInfo['label'],
            'roleEmoji'  => $roleInfo['emoji'],
            'roleSlug'   => $roleInfo['slug'],
            'roleColor'  => 'green',
        ];
    }

    // ══════════════════════════════════════════════════════════════════
    //  VOIR LE PROFIL
    // ══════════════════════════════════════════════════════════════════

    #[Route('/profil', name: 'app_profil', methods: ['GET'])]
    public function show(): Response
    {
        return $this->render('site/profil/show.html.twig', $this->getProfilContext());
    }

    // ══════════════════════════════════════════════════════════════════
    //  MODIFIER LE PROFIL
    // ══════════════════════════════════════════════════════════════════

    #[Route('/profil/edit', name: 'app_profil_edit', methods: ['GET', 'POST'])]
    public function edit(
        Request                     $request,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher,
        SluggerInterface            $slugger,
        ValidatorInterface          $validator
    ): Response {
        /** @var Utilisateurs $user */
        $user        = $this->getUser();
        $fieldErrors = [];

        if ($request->isMethod('POST')) {

            $prenom    = trim((string) $request->request->get('prenom_utilisateur', ''));
            $nom       = trim((string) $request->request->get('nom_utilisateur', ''));
            $telephone = trim((string) $request->request->get('telephone_utilisateur', ''));
            $bio       = trim((string) $request->request->get('bio_utilisateur', ''));
            $dateNaiss = (string) $request->request->get('date_naissance_utilisateur', '');
            $mdp       = (string) $request->request->get('mdp_utilisateur', '');
            $mdpConfirm= (string) $request->request->get('mdp_confirm', '');

            // Mise à jour des champs textuels
            $user->setPrenomUtilisateur($prenom);
            $user->setNomUtilisateur($nom);
            $user->setTelephoneUtilisateur($telephone);
            $user->setBioUtilisateur($bio);

            if (!empty($dateNaiss)) {
                try {
                    $user->setDateNaissanceUtilisateur(new \DateTime($dateNaiss));
                } catch (\Exception) {
                    $fieldErrors['date_naissance_utilisateur'] = 'La date de naissance est invalide.';
                }
            }

            // Mot de passe (optionnel)
            if (!empty($mdp)) {
                $user->setPlainPassword($mdp);
            }

            // Validation
            $violations = $validator->validate($user);
            foreach ($violations as $violation) {
                $prop = $violation->getPropertyPath();
                $fieldName = match($prop) {
                    'prenomUtilisateur'        => 'prenom_utilisateur',
                    'nomUtilisateur'           => 'nom_utilisateur',
                    'telephoneUtilisateur'     => 'telephone_utilisateur',
                    'dateNaissanceUtilisateur' => 'date_naissance_utilisateur',
                    'plainPassword'            => 'mdp_utilisateur',
                    default                    => $prop,
                };
                if (!isset($fieldErrors[$fieldName])) {
                    $fieldErrors[$fieldName] = $violation->getMessage();
                }
            }

            if (!empty($mdp) && empty($fieldErrors['mdp_utilisateur']) && $mdp !== $mdpConfirm) {
                $fieldErrors['mdp_confirm'] = 'Les mots de passe ne correspondent pas.';
            }

            if (empty($fieldErrors)) {
                // Photo de profil
                /** @var UploadedFile|null $uploadedFile */
                $uploadedFile = $request->files->get('photo_profil');
                if ($uploadedFile && $uploadedFile->isValid()) {
                    $projectDir = $this->getParameter('kernel.project_dir');
                    $avatarsDir = (is_string($projectDir) ? $projectDir : '') . '/public/avatars';
                    if (!is_dir($avatarsDir)) {
                        mkdir($avatarsDir, 0775, true);
                    }
                    $base      = $slugger->slug(pathinfo($uploadedFile->getClientOriginalName(), PATHINFO_FILENAME));
                    $ext       = $uploadedFile->guessExtension() ?? 'jpg';
                    $filename  = $base . '_' . uniqid() . '.' . $ext;
                    $uploadedFile->move($avatarsDir, $filename);
                    $user->setPhotoProfilUtilisateur($filename);
                }

                // Hachage mot de passe si fourni
                if (!empty($mdp)) {
                    $user->setMdpUtilisateur($passwordHasher->hashPassword($user, $mdp));
                    $user->eraseCredentials();
                }

                $em->flush();
                $this->addFlash('success', 'Votre profil a été mis à jour avec succès ! ✨');
                return $this->redirectToRoute('app_profil');
            }
        }

        return $this->render('site/profil/edit.html.twig', array_merge(
            $this->getProfilContext(),
            ['fieldErrors' => $fieldErrors]
        ));
    }
}