<?php

namespace App\Controller;

use App\Entity\Produit;
use App\Entity\CommandeItem;
use App\Form\ProduitType;
use App\Repository\ProduitRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use App\Service\DescriptionIaService;
use Symfony\Component\Routing\Annotation\Route;
use App\Service\StockAlerteService;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\Form\FormInterface;

#[Route('/admin/produit')]
class ProduitAdminController extends AbstractController
{
    // ─── LISTE ───────────────────────────────────────────────────
    #[Route('/', name: 'admin_produit_index', methods: ['GET'])]
    public function index(ProduitRepository $repo): Response
    {
        return $this->render('admin/produit/index.html.twig', [
            'produits' => $repo->findAll(),
        ]);
    }

    // ─── CRÉER ───────────────────────────────────────────────────
    #[Route('/new', name: 'admin_produit_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response {
        $produit = new Produit();
        $form    = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->handleImageUpload($form, $produit, $slugger);
            $em->persist($produit);
            $em->flush();
            $this->addFlash('success', 'Produit créé avec succès.');
            return $this->redirectToRoute('admin_produit_index');
        }

        return $this->render('admin/produit/form.html.twig', [
            'produit' => $produit,
            'form'    => $form->createView(),
            'titre'   => 'Nouveau produit',
        ]);
    }

    // ─── MODIFIER ────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'admin_produit_edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        Produit $produit,
        EntityManagerInterface $em,
        SluggerInterface $slugger,
        StockAlerteService $stockAlerte
    ): Response {
        $form = $this->createForm(ProduitType::class, $produit);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->handleImageUpload($form, $produit, $slugger);
            $em->flush();

            // ✅ Notifier les clients en attente si le produit est de nouveau disponible
            $stockAlerte->notifierSiDisponible($produit);

            $this->addFlash('success', 'Produit mis à jour.');
            return $this->redirectToRoute('admin_produit_index');
        }

        return $this->render('admin/produit/form.html.twig', [
            'produit' => $produit,
            'form'    => $form->createView(),
            'titre'   => 'Modifier : ' . $produit->getNom(),
        ]);
    }

    // ─── GÉNÉRER DESCRIPTION IA ──────────────────────────────────
    #[Route('/ia/description', name: 'admin_produit_ia_description', methods: ['POST'])]
    public function genererDescriptionIa(
        Request $request,
        DescriptionIaService $iaService
    ): JsonResponse {
        $data = json_decode($request->getContent(), true);
        $nom  = trim($data['nom'] ?? '');

        if (empty($nom)) {
            return $this->json(['error' => 'Le nom du produit est requis.'], 400);
        }

        $prix        = !empty($data['prix']) ? $data['prix'] : null;
        $description = $iaService->genererDescription($nom, $prix);

        return $this->json(['description' => $description]);
    }

    // ─── SUPPRIMER ───────────────────────────────────────────────
    #[Route('/{id}/delete', name: 'admin_produit_delete', methods: ['POST'])]
    public function delete(
        Request $request,
        Produit $produit,
        EntityManagerInterface $em
    ): Response {
        $token = $request->request->get('_token');
        if (is_string($token) && $this->isCsrfTokenValid('delete' . $produit->getId(), $token)) {
            // Supprimer d'abord les CommandeItem liés pour respecter la contrainte FK
            $commandeItems = $em->getRepository(CommandeItem::class)->findBy(['produit' => $produit]);
            foreach ($commandeItems as $item) {
                $em->remove($item);
            }
            $em->flush();

            $em->remove($produit);
            $em->flush();
            $this->addFlash('success', 'Produit supprimé.');
        } else {
            $this->addFlash('danger', 'Token CSRF invalide.');
        }
        return $this->redirectToRoute('admin_produit_index');
    }

    // ─── HELPERS ─────────────────────────────────────────────────
    /**
     * Gère l'upload de l'image du produit.
     */
    private function handleImageUpload(FormInterface $form, Produit $produit, SluggerInterface $slugger): void
    {
        $imageFile = $form->get('imageFile')->getData();
        if (!$imageFile) {
            return;
        }

        $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
        $safeFilename     = $slugger->slug($originalFilename);
        $newFilename      = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

        try {
            $imageFile->move(
                $this->getParameter('produits_images_directory'),
                $newFilename
            );
            $produit->setImage($newFilename);
        } catch (FileException $e) {
            // Log l'erreur si besoin
        }
    }
}