<?php

namespace App\Controller;

use App\Entity\Commande;
use App\Entity\CommandeItem;
use App\Repository\ProduitRepository;
use App\Service\PaiementMailer;
use Doctrine\ORM\EntityManagerInterface;
use Stripe\Checkout\Session as StripeSession;
use Stripe\Stripe;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

#[Route('/paiement')]
class PaiementController extends AbstractController
{
    public function __construct(
        private string $stripeSecretKey,
        private ProduitRepository $produitRepository,
        private EntityManagerInterface $em,
        private PaiementMailer $paiementMailer
    ) {
        Stripe::setApiKey($this->stripeSecretKey);
    }

    private function decrementerStock(Commande $commande): void
    {
        foreach ($commande->getItems() as $item) {
            $produit = $item->getProduit();
            if (!$produit || $produit->getStock() === null) {
                continue;
            }
            $ancienStock  = $produit->getStock();
            $nouveauStock = max(0, $ancienStock - $item->getQuantite());
            $produit->setStock($nouveauStock);
        }
        $this->em->flush();
    }

    /**
     * Récupère l'email de l'utilisateur connecté.
     * Retourne null si non connecté.
     */
    private function getUserEmail(): ?string
    {
        $user = $this->getUser();
        if (!$user) {
            return null;
        }
        return method_exists($user, 'getEmailUtilisateur')
            ? $user->getEmailUtilisateur()
            : $user->getUserIdentifier();
    }

    // ──────────────────────────────────────────────────────────────
    // ROUTE : alerte stock (inchangée)
    // ──────────────────────────────────────────────────────────────
    #[Route('/alerte-stock/{id}', name: 'paiement_alerte_stock', methods: ['POST'])]
    public function alerteStock(int $id, Request $request): JsonResponse
    {
        $produit = $this->produitRepository->find($id);
        if (!$produit) {
            return $this->json(['success' => false, 'error' => 'Produit introuvable.'], 404);
        }
        if ($produit->isDisponible()) {
            return $this->json(['success' => false, 'error' => 'Ce produit est déjà disponible.'], 400);
        }
        $data = json_decode($request->getContent(), true);
        $tel = trim((string) ($data['tel'] ?? ''));
        if ($tel === '') {
            return $this->json(['success' => false, 'error' => 'Numéro de téléphone requis.'], 400);
        }
        $cleanTel = (string) preg_replace('/[\s\-\(\)]/', '', $tel);
        if (!preg_match('/^\+?[0-9]{8,15}$/', $cleanTel)) {
            return $this->json(['success' => false, 'error' => 'Numéro de téléphone invalide.'], 400);
        }
        return $this->json([
            'success' => true,
            'message' => 'Vous serez alerté par SMS dès que "' . $produit->getNom() . '" sera de nouveau disponible.',
        ]);
    }

    // ──────────────────────────────────────────────────────────────
    // ROUTE : paiement à la livraison
    // Vérifie que l'utilisateur est connecté et utilise son email
    // ──────────────────────────────────────────────────────────────
    #[Route('/livraison', name: 'paiement_livraison', methods: ['POST'])]
    public function livraison(Request $request): JsonResponse
    {
        // ── Vérification connexion ──────────────────────────────
        $userEmail = $this->getUserEmail();
        if (!$userEmail) {
            return $this->json([
                'success'      => false,
                'error'        => 'Vous devez être connecté(e) pour passer une commande.',
                'redirect'     => '/login',          // le JS peut rediriger
                'require_login' => true,
            ], 401);
        }

        $data   = json_decode($request->getContent(), true);
        $panier = $data['panier'] ?? [];
        if (empty($panier)) {
            return $this->json(['success' => false, 'error' => 'Panier vide.'], 400);
        }

        $commande = new Commande();
        // ── Utiliser l'email du compte connecté ────────────────
        $commande->setUserEmail($userEmail);

        foreach ($panier as $panierItem) {
            $produit = $this->produitRepository->find($panierItem['produit_id']);
            if (!$produit || !$produit->isDisponible()) {
                continue;
            }
            $quantite = max(1, (int) ($panierItem['quantite'] ?? 1));
            $stock    = $produit->getStock();
            if ($stock !== null && $stock < $quantite) {
                return $this->json([
                    'success' => false,
                    'error'   => sprintf(
                        'Stock insuffisant pour "%s" : %d disponible(s), vous en demandez %d.',
                        $produit->getNom(),
                        $stock,
                        $quantite
                    ),
                    'stock_insuffisant' => [
                        'produit_id' => $produit->getId(),
                        'stock'      => $stock,
                    ],
                ], 409);
            }
            $item = new CommandeItem();
            $item->setProduit($produit)
                 ->setQuantite($quantite)
                 ->setPrixUnitaire((string) $produit->getPrix());
            $commande->addItem($item);
        }

        if ($commande->getItems()->isEmpty()) {
            return $this->json(['success' => false, 'error' => 'Aucun produit disponible dans le panier.'], 400);
        }

        $commande->recalculerTotal();
        $commande->setStatutPaiement('livraison');
        $this->em->persist($commande);
        $this->em->flush();
        $this->decrementerStock($commande);

        return $this->json(['success' => true, 'commande_id' => $commande->getId()]);
    }

    // ──────────────────────────────────────────────────────────────
    // ROUTE : checkout Stripe
    // Vérifie que l'utilisateur est connecté et utilise son email
    // ──────────────────────────────────────────────────────────────
    #[Route('/checkout', name: 'paiement_checkout', methods: ['POST'])]
    public function checkout(Request $request): JsonResponse
    {
        // ── Vérification connexion ──────────────────────────────
        $userEmail = $this->getUserEmail();
        if (!$userEmail) {
            return $this->json([
                'error'         => 'Vous devez être connecté(e) pour passer une commande.',
                'redirect'      => '/login',
                'require_login' => true,
            ], 401);
        }

        $data   = json_decode($request->getContent(), true);
        $panier = $data['panier'] ?? [];
        if (empty($panier)) {
            return $this->json(['error' => 'Panier vide.'], 400);
        }

        $commande = new Commande();
        // ── Utiliser l'email du compte connecté ────────────────
        $commande->setUserEmail($userEmail);

        $lineItems = [];
        foreach ($panier as $panierItem) {
            $produit = $this->produitRepository->find($panierItem['produit_id']);
            if (!$produit || !$produit->isDisponible()) {
                continue;
            }
            $quantite = max(1, (int) ($panierItem['quantite'] ?? 1));
            $stock    = $produit->getStock();
            if ($stock !== null && $stock < $quantite) {
                return $this->json([
                    'error' => sprintf(
                        'Stock insuffisant pour "%s" : %d disponible(s), vous en demandez %d.',
                        $produit->getNom(),
                        $stock,
                        $quantite
                    ),
                    'stock_insuffisant' => [
                        'produit_id' => $produit->getId(),
                        'stock'      => $stock,
                    ],
                ], 409);
            }
            $item = new CommandeItem();
            $item->setProduit($produit)
                 ->setQuantite($quantite)
                 ->setPrixUnitaire((string) $produit->getPrix());
            $commande->addItem($item);

            $prix        = (float) $produit->getPrix();
            $lineItems[] = [
                'price_data' => [
                    'currency'     => 'usd',
                    'unit_amount'  => (int) round($prix * 100),
                    'product_data' => ['name' => $produit->getNom()],
                ],
                'quantity' => $quantite,
            ];
        }

        if ($commande->getItems()->isEmpty()) {
            return $this->json(['error' => 'Aucun produit disponible dans le panier.'], 400);
        }

        $commande->recalculerTotal();
        $this->em->persist($commande);
        $this->em->flush();

        try {
            $stripeSession = StripeSession::create([
                'payment_method_types' => ['card'],
                'line_items'           => $lineItems,
                'mode'                 => 'payment',
                'customer_email'       => $userEmail,   // pré-rempli dans Stripe
                'success_url'          => $this->generateUrl(
                    'paiement_success',
                    ['id' => $commande->getId()],
                    UrlGeneratorInterface::ABSOLUTE_URL
                ) . '?session_id={CHECKOUT_SESSION_ID}',
                'cancel_url'           => $this->generateUrl(
                    'produit_liste', [],
                    UrlGeneratorInterface::ABSOLUTE_URL
                ),
                'metadata'             => ['commande_id' => (string) $commande->getId()],
            ]);
        } catch (\Stripe\Exception\AuthenticationException $e) {
            return $this->json(['error' => 'Clé Stripe invalide. Vérifiez STRIPE_SECRET_KEY dans .env'], 500);
        } catch (\Exception $e) {
            return $this->json(['error' => 'Erreur Stripe : ' . $e->getMessage()], 500);
        }

        $commande->setStripeSession($stripeSession->id);
        $this->em->flush();

        return $this->json(['checkout_url' => $stripeSession->url]);
    }

    // ──────────────────────────────────────────────────────────────
    // ROUTE : succès paiement Stripe (inchangée)
    // ──────────────────────────────────────────────────────────────
    #[Route('/success/{id}', name: 'paiement_success', methods: ['GET'])]
    public function success(Commande $commande, Request $request): Response
    {
        $sessionId = $request->query->get('session_id');
        if (is_string($sessionId) && $sessionId !== '' && $commande->getStatutPaiement() === Commande::STATUT_EN_ATTENTE) {
            try {
                $session = StripeSession::retrieve($sessionId);
                if ($session->payment_status === 'paid') {
                    $commande->setStatutPaiement(Commande::STATUT_PAYE);
                    $this->em->flush();
                    $this->decrementerStock($commande);
                    $this->paiementMailer->sendConfirmationCommande($commande);
                }
            } catch (\Exception $e) {
                // log silencieux
            }
        }
        return $this->render('paiement/success.html.twig', [
            'commande' => $commande,
        ]);
    }
}