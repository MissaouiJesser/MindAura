<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Contrôleur principal du back-office administrateur.
 *
 * Responsabilité : pages de gestion des ressources métier
 * (locaux, réservations, tests, événements, etc.).
 *
 * ⚠  La gestion CRUD des utilisateurs est déléguée à
 *    App\Controller\UtilisateursController.
 */
#[IsGranted('ROLE_ADMIN')]
class AdminController extends AbstractController
{
    // ── Helper commun ────────────────────────────────────────────────────────

    /**
     * Retourne les données partagées par toutes les vues du back-office admin
     * (ex : informations de l'administrateur connecté).
     *
     * @return array{adminUser: Utilisateurs}
     */
    private function getAdminContext(): array
    {
        /** @var Utilisateurs $user */
        $user = $this->getUser();

        return ['adminUser' => $user];
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SECTIONS DU BACK-OFFICE
    // ════════════════════════════════════════════════════════════════════════

    #[Route('/admin/locaux', name: 'app_admin_locaux')]
    public function locaux(): Response
    {
        return $this->render('admin/locaux.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/reservations', name: 'app_admin_reservations')]
    public function reservations(): Response
    {
        return $this->render('admin/reservations.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/tests-legacy', name: 'app_admin_tests')]
    public function tests(): Response
    {
        return $this->render('admin/tests.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/evenements', name: 'app_admin_evenements')]
    public function evenements(): Response
    {
        return $this->render('admin/evenements.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/participations', name: 'app_admin_participations')]
    public function participations(): Response
    {
        return $this->render('admin/participations.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/ressources-legacy', name: 'app_admin_ressources')]
    public function ressources(): Response
    {
        return $this->render('admin/ressources.html.twig', $this->getAdminContext());
    }

    #[Route('/admin/reclamations', name: 'app_admin_reclamations')]
    public function reclamations(): Response
    {
        return $this->redirectToRoute('reclamation_index');
    }
}