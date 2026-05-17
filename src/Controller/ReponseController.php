<?php

namespace App\Controller;

use App\Entity\Reponse;
use App\Entity\Utilisateurs;
use App\Form\ReponseType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/reponse', name: 'reponse_')]
class ReponseController extends AbstractController
{
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $em): Response
    {
        $reponses = $em->getRepository(Reponse::class)->findAll();

        return $this->render('reponse/index.html.twig', [
            'reponses' => $reponses,
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $reponse = new Reponse();
        // Date set automatically in __construct(), no manual call needed

        $form = $this->createForm(ReponseType::class, $reponse, ['context' => 'admin_new']);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Date set automatically in __construct(), no manual call needed

            $user = $this->getUser();
            if (!$user instanceof Utilisateurs) {
                $this->addFlash('error', 'Vous devez être connecté pour soumettre une réponse.');
                return $this->redirectToRoute('app_login');
            }

            $reponse->setIdUtilisateur($user->getIdUtilisateur());
            $reponse->setNomUtilisateur(trim(
                $user->getPrenomUtilisateur().' '.$user->getNomUtilisateur()
            ));

            try {
                $em->persist($reponse);
                $em->flush();

                $reclamation = $reponse->getReclamation();
                if ($reclamation === null) {
                    return $this->redirectToRoute('reclamation_index');
                }

                return $this->redirectToRoute('reclamation_show', [
                    'id' => $reclamation->getIdReclamation(),
                ]);
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Enregistrement impossible : '.$e->getMessage());
            }
        }

        $reclamationId = $reponse->getReclamation()?->getIdReclamation()
            ?? $request->query->getInt('reclamation_id') ?: null;

        return $this->render('reponse/new.html.twig', [
            'form'           => $form->createView(),
            'reclamation_id' => $reclamationId,
        ]);
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET'])]
    public function show(Reponse $reponse): Response
    {
        return $this->render('reponse/show.html.twig', [
            'reponse' => $reponse,
        ]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Reponse $reponse, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs || $user->getIdUtilisateur() !== $reponse->getId_utilisateur()) {
            throw $this->createAccessDeniedException('Vous ne pouvez modifier que vos propres réponses.');
        }

        $currentUserId   = $reponse->getId_utilisateur();
        $currentUserName = $reponse->getNom_utilisateur();

        // Gestion AJAX directe (depuis reclamation/show.html.twig)
        if ($request->isXmlHttpRequest() && $request->isMethod('POST')) {
            $contenu = trim((string) $request->request->get('contenu_reponse', ''));
            if (strlen($contenu) >= 5) {
                $reponse->setContenu_reponse($contenu);
                $reponse->setId_utilisateur($currentUserId);
                $reponse->setNom_utilisateur($currentUserName);
                try {
                    $em->flush();
                    return $this->json(['success' => true]);
                } catch (\Exception $e) {
                    return $this->json(['success' => false, 'error' => $e->getMessage()], 500);
                }
            }
            return $this->json(['success' => false, 'error' => 'Contenu trop court.'], 400);
        }

        $form = $this->createForm(ReponseType::class, $reponse, ['context' => 'admin_edit']);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $reponse->setId_utilisateur($currentUserId);
            $reponse->setNom_utilisateur($currentUserName);

            try {
                $em->flush();
                return $this->redirectToRoute('reclamation_show', [
                    'id' => $reponse->getReclamation()?->getIdReclamation(),
                ]);
            } catch (\Exception $e) {
                $this->addFlash('error', 'Erreur lors de la modification : '.$e->getMessage());
            }
        }

        return $this->render('reponse/edit.html.twig', [
            'form'    => $form,
            'reponse' => $reponse,
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(Request $request, Reponse $reponse, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs || $user->getIdUtilisateur() !== $reponse->getId_utilisateur()) {
            throw $this->createAccessDeniedException('Vous ne pouvez supprimer que vos propres réponses.');
        }

        $reclamationId = $reponse->getReclamation()?->getIdReclamation();

        if (!$this->isCsrfTokenValid(
            'delete_reponse_'.$reponse->getId_reponse(),
            (string) $request->request->get('_token')
        )) {
            $this->addFlash('error', 'Jeton de sécurité invalide.');
            return $reclamationId
                ? $this->redirectToRoute('reclamation_show', ['id' => $reclamationId])
                : $this->redirectToRoute('reclamation_index');
        }

        $em->remove($reponse);
        $em->flush();
        $this->addFlash('success', 'Réponse supprimée.');

        return $reclamationId
            ? $this->redirectToRoute('reclamation_show', ['id' => $reclamationId])
            : $this->redirectToRoute('reclamation_index');
    }
}