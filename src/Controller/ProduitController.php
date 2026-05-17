<?php

namespace App\Controller;

use App\Entity\Produit;
use App\Repository\ProduitRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/produits')]
class ProduitController extends AbstractController
{
    /**
     * Affiche la liste des produits actifs (boutique utilisateur).
     */
    #[Route('/', name: 'produit_liste', methods: ['GET'])]
    public function liste(ProduitRepository $repo): Response
    {
        return $this->render('admin/produit/liste.html.twig', [
            'produits' => $repo->findActifs(),
        ]);
    }

    /**
     * Enregistre l'adresse email d'un client qui souhaite être alerté
     * par email dès que le produit est de nouveau en stock.
     *
     * Appelé en AJAX (fetch POST) depuis liste.html.twig.
     * Retourne du JSON : { success: true, message: "..." }
     *                 ou { success: false, error: "..." }
     */
    #[Route('/{id}/alerte-email', name: 'produit_alerte_email', methods: ['POST'])]
    public function alerteEmail(
        Produit $produit,
        Request $request,
        EntityManagerInterface $em
    ): JsonResponse {
        // Produit déjà disponible → pas besoin d'alerte
        if ($produit->isDisponible()) {
            return $this->json([
                'success' => false,
                'error'   => 'Ce produit est déjà disponible.',
            ], 400);
        }

        // Lecture du corps JSON envoyé par le frontend
        $data  = json_decode($request->getContent(), true);
        $email = trim($data['email'] ?? '');

        if (empty($email)) {
            return $this->json([
                'success' => false,
                'error'   => 'Adresse email requise.',
            ], 400);
        }

        // Validation de l'adresse email
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return $this->json([
                'success' => false,
                'error'   => 'Adresse email invalide. Exemple : votre@email.com',
            ], 400);
        }

        // Vérifier si l'email est déjà enregistré pour ce produit
        if (in_array($email, $produit->getAlertesEmail(), true)) {
            return $this->json([
                'success' => true,
                'message' => 'Vous êtes déjà inscrit aux alertes pour "' . $produit->getNom() . '".',
            ]);
        }

        // Ajout de l'email à la liste d'alertes du produit
        $produit->addAlerteEmail($email);
        $em->flush();

        return $this->json([
            'success' => true,
            'message' => 'Vous serez alerté par email dès que "' . $produit->getNom() . '" sera de nouveau disponible.',
        ]);
    }
}
