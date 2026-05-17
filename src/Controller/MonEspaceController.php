<?php

namespace App\Controller;

use App\Repository\CommandeRepository;
use App\Repository\ParticipationRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Espace personnel de l'utilisateur connecté :
 *   GET /mon-espace/participations  → liste des participations du compte connecté
 *   GET /mon-espace/commandes       → liste des commandes/produits achetés du compte connecté
 */
#[IsGranted('ROLE_USER')]
class MonEspaceController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  PARTICIPATIONS AUX ÉVÉNEMENTS
    // ─────────────────────────────────────────────────────────────

    /**
     * Affiche toutes les participations enregistrées avec l'email
     * de l'utilisateur actuellement connecté.
     */
    #[Route('/mon-espace/participations', name: 'mon_espace_participations', methods: ['GET'])]
    public function mesParticipations(ParticipationRepository $participationRepo): Response
    {
        /** @var \App\Entity\Utilisateurs $user */
        $user = $this->getUser();

        // FIX : on s'assure que l'email n'est pas null avant de l'utiliser
        $emailUtilisateur = $user->getEmailUtilisateur() ?? '';

        // On filtre par l'email de l'utilisateur connecté
        $participations = $participationRepo->search([
            'query' => $emailUtilisateur, // FIX : plus de string|null, garanti string
        ]);

        // Filtrage strict sur l'email (search() fait un LIKE, on re-filtre)
        // FIX : strtolower() reçoit désormais des string garanties
        $participations = array_filter(
            $participations,
            fn($p) => strtolower((string) $p->getEmail()) === strtolower($emailUtilisateur)
        );

        return $this->render('mon_espace/participations.html.twig', [
            'participations' => array_values($participations),
            'user'           => $user,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  COMMANDES / PRODUITS ACHETÉS
    // ─────────────────────────────────────────────────────────────

    /**
     * Affiche toutes les commandes passées avec l'email
     * de l'utilisateur actuellement connecté.
     */
    #[Route('/mon-espace/commandes', name: 'mon_espace_commandes', methods: ['GET'])]
    public function mesCommandes(CommandeRepository $commandeRepo): Response
    {
        /** @var \App\Entity\Utilisateurs $user */
        $user = $this->getUser();

        // FIX : on garantit un string non-null pour findByEmail()
        $emailUtilisateur = $user->getEmailUtilisateur() ?? '';

        $commandes = $commandeRepo->findByEmail($emailUtilisateur);

        return $this->render('mon_espace/commandes.html.twig', [
            'commandes' => $commandes,
            'user'      => $user,
        ]);
    }
  #[Route('/mon-espace/profil', name: 'app_profile', methods: ['GET'])]
public function monProfil(): Response
{
    $user = $this->getUser();
    
    // The IsGranted attribute guarantees a user, but static analysis needs a hint
    if (!$user instanceof \App\Entity\Utilisateurs) {
        throw $this->createAccessDeniedException('Vous devez être connecté pour accéder à cette page.');
    }

    $roles = $user->getRoles();

    // Role mapping
    $roleLabel = 'Utilisateur';
    $roleEmoji = '👤';
    if (in_array('ROLE_ADMIN', $roles)) {
        $roleLabel = 'Administrateur';
        $roleEmoji = '🛡️';
    } elseif (in_array('ROLE_THERAPEUTE', $roles)) {
        $roleLabel = 'Thérapeute';
        $roleEmoji = '🧘';
    }

    return $this->render('site/profil/show.html.twig', [
        'user' => $user,
        'roleEmoji' => $roleEmoji,
        'roleLabel' => $roleLabel,
    ]);
}
}