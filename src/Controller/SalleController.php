<?php

namespace App\Controller;

use App\Entity\Salle;
use App\Form\SalleType;
use App\Repository\SalleRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;

#[Route('/salle', name: 'salle_')]
class SalleController extends AbstractController
{
    // ─────────────────────────────────────────────────────────────
    //  LIST
    // ─────────────────────────────────────────────────────────────
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(SalleRepository $repo): Response
    {
        return $this->render('salle/indexSalle.html.twig', [
            'salles' => $repo->findAllWithLocal(),
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
        $salle = new Salle();

        // ✅ CORRECTION : is_new=true → image obligatoire à la création
        $form = $this->createForm(SalleType::class, $salle, [
            'submit_label' => 'Ajouter',
            'is_new'       => true,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // ── Gestion upload image ──────────────────────────────
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename     = $slugger->slug($originalFilename);
                $newFilename      = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();
                $imageFile->move($this->getParameter('images_salles_directory'), $newFilename);
                $salle->setImageURL($newFilename);
            }

            $em->persist($salle);

            // ── Capacité local : +1 à chaque nouvelle salle ───────
            $local = $salle->getLocal();
            if ($local !== null) {
                $local->incrementCapacite();
            }

            $em->flush();

            $this->addFlash('success', 'La salle a été ajoutée avec succès.');
            return $this->redirectToRoute('salle_index');
        }

        return $this->render('salle/newSalle.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  FRONT OFFICE — doit être déclaré AVANT /{id} pour éviter
    //  que Symfony interprète "front" comme un identifiant
    // ─────────────────────────────────────────────────────────────
    #[Route('/front', name: 'front_salles_index', methods: ['GET'])]
    public function frontIndex(SalleRepository $repo): Response
    {
        return $this->render('salle/front_index.html.twig', [
            'salles' => $repo->findAllWithLocal(),
        ]);
    }

    #[Route('/front/{id}', name: 'front_salles_show', methods: ['GET'])]
    public function frontShow(Salle $salle): Response
    {
        return $this->render('salle/front_show.html.twig', [
            'salle' => $salle,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  SHOW
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}', name: 'show', methods: ['GET'])]
    public function show(Salle $salle): Response
    {
        return $this->render('salle/showSalle.html.twig', [
            'salle' => $salle,
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  EDIT
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        Salle $salle,
        EntityManagerInterface $em,
        SluggerInterface $slugger
    ): Response {
        // Mémoriser l'ancien local AVANT que le formulaire ne le remplace
        $ancienLocal = $salle->getLocal();

        // ✅ CORRECTION : is_new=false → image facultative en modification
        $form = $this->createForm(SalleType::class, $salle, [
            'submit_label' => 'Modifier',
            'is_new'       => false,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            // ── Gestion upload image ──────────────────────────────
            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                $ancienneImage = $salle->getImageURL();
                if ($ancienneImage) {
                    $imagesDir    = $this->getParameter('images_salles_directory');
                    assert(is_string($imagesDir));
                    $ancienChemin = $imagesDir . '/' . $ancienneImage;
                    if (file_exists($ancienChemin)) {
                        unlink($ancienChemin);
                    }
                }
                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename     = $slugger->slug($originalFilename);
                $newFilename      = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();
                $imageFile->move($this->getParameter('images_salles_directory'), $newFilename);
                $salle->setImageURL($newFilename);
            }

            // ── Ajustement capacité si le local a changé ──────────
            $nouveauLocal = $salle->getLocal();

            if ($ancienLocal !== $nouveauLocal) {
                if ($ancienLocal !== null) {
                    $ancienLocal->decrementCapacite();
                }
                if ($nouveauLocal !== null) {
                    $nouveauLocal->incrementCapacite();
                }
            }

            $em->flush();

            $this->addFlash('success', 'La salle a été modifiée avec succès.');
            return $this->redirectToRoute('salle_index');
        }

        return $this->render('salle/editSalle.html.twig', [
            'salle' => $salle,
            'form'  => $form->createView(),
        ]);
    }

    // ─────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────
    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(
        Request $request,
        Salle $salle,
        EntityManagerInterface $em
    ): Response {
        if ($this->isCsrfTokenValid('delete' . (string) $salle->getIdSalle(), (string) $request->request->get('_token'))) {

            // ── Capacité local : -1 à chaque suppression ──────────
            $local = $salle->getLocal();
            if ($local !== null) {
                $local->decrementCapacite();
            }

            // ── Supprimer l'image du serveur ──────────────────────
            $image = $salle->getImageURL();
            if ($image) {
                $imagesDir = $this->getParameter('images_salles_directory');
                assert(is_string($imagesDir));
                $chemin = $imagesDir . '/' . $image;
                if (file_exists($chemin)) {
                    unlink($chemin);
                }
            }

            $em->remove($salle);
            $em->flush();

            $this->addFlash('success', 'La salle a été supprimée avec succès.');
        } else {
            $this->addFlash('danger', 'Token CSRF invalide.');
        }

        return $this->redirectToRoute('salle_index');
    }

}