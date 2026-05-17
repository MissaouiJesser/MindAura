<?php

namespace App\Controller;

use App\Entity\LocalsPsychiatrie;
use App\Form\LocalsPsychiatrieType;
use App\Repository\LocalsPsychiatrieRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;

#[Route('/locaux-psychiatrie', name: 'locaux_psychiatrie_')]
class LocalsPsychiatrieController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  LIST
    // ─────────────────────────────────────────────────────────────
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(LocalsPsychiatrieRepository $repo): Response
    {
        return $this->render('locaux_psychiatrie/index.html.twig', [
            'locaux' => $repo->findAllWithSalles(),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────────────────────
    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response {
        $local = new LocalsPsychiatrie();

        // ✅ CORRECTION : is_new=true → image obligatoire à la création
        $form = $this->createForm(LocalsPsychiatrieType::class, $local, [
            'submit_label' => 'Ajouter',
            'is_new'       => true,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // ── Gestion upload image (mapped=false → getData() depuis le form) ──
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename     = $slugger->slug($originalFilename);
                $newFilename      = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

                $imageFile->move(
                    $this->getParameter('images_locaux_directory'),
                    $newFilename
                );
                $local->setImageURL($newFilename);
            }

            $em->persist($local);
            $em->flush();

            $this->addFlash('success', 'Le local a été ajouté avec succès.');
            return $this->redirectToRoute('locaux_psychiatrie_index');
        }

        return $this->render('locaux_psychiatrie/new.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  FRONT OFFICE — doit être déclaré AVANT /{id} pour éviter
    //  que Symfony interprète "front" comme un identifiant
    // ─────────────────────────────────────────────────────────────
    #[Route('/front', name: 'front_locaux_index', methods: ['GET'])]
    public function frontIndex(LocalsPsychiatrieRepository $repo): Response
    {
        return $this->render('locaux_psychiatrie/front_index.html.twig', [
            'locaux' => $repo->findAllWithSalles(),
        ]);
    }

    #[Route('/front/{id}', name: 'front_locaux_show', methods: ['GET'])]
    public function frontShow(LocalsPsychiatrie $local): Response
    {
        return $this->render('locaux_psychiatrie/front_show.html.twig', [
            'local' => $local,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  SHOW
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}', name: 'show', methods: ['GET'])]
    public function show(LocalsPsychiatrie $local): Response
    {
        return $this->render('locaux_psychiatrie/show.html.twig', [
            'local' => $local,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        LocalsPsychiatrie $local,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response {
        // ✅ CORRECTION : is_new=false → image facultative en modification
        $form = $this->createForm(LocalsPsychiatrieType::class, $local, [
            'submit_label' => 'Modifier',
            'is_new'       => false,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // ── Gestion upload image (mapped=false → getData() depuis le form) ──
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                // Supprimer l'ancienne image si elle existe
                $ancienneImage = $local->getImageURL();
                if ($ancienneImage) {
                    $imagesDir    = $this->getParameter('images_locaux_directory');
                    assert(is_string($imagesDir));
                    $ancienChemin = $imagesDir . '/' . $ancienneImage;
                    if (file_exists($ancienChemin)) {
                        unlink($ancienChemin);
                    }
                }

                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename     = $slugger->slug($originalFilename);
                $newFilename      = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

                $imageFile->move(
                    $this->getParameter('images_locaux_directory'),
                    $newFilename
                );
                $local->setImageURL($newFilename);
            }

            // ── Propagation : local Indisponible → toutes ses salles aussi ──
            $local->propagateDisponibilite();

            $em->flush();

            $this->addFlash('success', 'Le local a été modifié avec succès.');
            return $this->redirectToRoute('locaux_psychiatrie_index');
        }

        return $this->render('locaux_psychiatrie/edit.html.twig', [
            'local' => $local,
            'form'  => $form->createView(),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(
        Request $request,
        LocalsPsychiatrie $local,
        EntityManagerInterface $em
    ): Response {
        if ($this->isCsrfTokenValid('delete' . (string) $local->getIdLocal(), (string) $request->request->get('_token'))) {
            // Supprimer l'image du serveur
            $image = $local->getImageURL();
            if ($image) {
                $imagesDir = $this->getParameter('images_locaux_directory');
                assert(is_string($imagesDir));
                $chemin = $imagesDir . '/' . $image;
                if (file_exists($chemin)) {
                    unlink($chemin);
                }
            }
            $em->remove($local);
            $em->flush();
            $this->addFlash('success', 'Le local a été supprimé avec succès.');
        } else {
            $this->addFlash('danger', 'Token CSRF invalide.');
        }

        return $this->redirectToRoute('locaux_psychiatrie_index');
    }

}