<?php

namespace App\Controller;

use App\Entity\Objectif;
use App\Form\ObjectifType;
use App\Repository\ObjectifRepository;
use App\Service\VoiceRssService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
#[Route('/admin/objectifs', name: 'admin_objectif_')]
class ObjectifController extends AbstractController
{
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(Request $request, ObjectifRepository $repo): Response
    {
        $perPage = $request->query->getInt('perPage', 5);

        $result = $repo->searchForAdmin([
            'q'       => $request->query->get('q', ''),
            'type'    => $request->query->get('type', ''),
            'statut'  => $request->query->get('statut', ''),
            'source'  => $request->query->get('source', ''),
            'page'    => $request->query->getInt('page', 1),
            'perPage' => $perPage,
        ]);

        return $this->render('admin/objectifs/index.html.twig', [
            'objectifs' => $result['items'],
            'total'     => $result['total'],
            'page'      => $result['page'],
            'pages'     => $result['pages'],
            'perPage'   => $result['perPage'],
            'q'         => (string) $request->query->get('q', ''),
            'type'      => (string) $request->query->get('type', ''),
            'statut'    => (string) $request->query->get('statut', ''),
            'source'    => (string) $request->query->get('source', ''),
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $objectif = new Objectif();
        $objectif->setSource('admin');

        $form = $this->createForm(ObjectifType::class, $objectif);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // date_creation est initialisée automatiquement via @PrePersist
            $em->persist($objectif);
            $em->flush();

            $this->addFlash('success', 'Objectif créé.');
            return $this->redirectToRoute('admin_objectif_index');
        }

        return $this->render('admin/objectifs/new.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET'])]
    public function show(Objectif $objectif): Response
    {
        return $this->render('admin/objectifs/show.html.twig', [
            'objectif' => $objectif,
        ]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(Objectif $objectif, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(ObjectifType::class, $objectif);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();
            $this->addFlash('success', 'Objectif mis à jour.');
            return $this->redirectToRoute('admin_objectif_index');
        }

        return $this->render('admin/objectifs/edit.html.twig', [
            'objectif' => $objectif,
            'form'     => $form->createView(),
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(Objectif $objectif, Request $request, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete_objectif_' . $objectif->getIdObjectif(), (string) $request->request->get('_token'))) {
            $em->remove($objectif);
            $em->flush();
            $this->addFlash('success', 'Objectif supprimé.');
        } else {
            $this->addFlash('danger', 'Jeton CSRF invalide.');
        }

        return $this->redirectToRoute('admin_objectif_index');
    }

    #[Route('/{id}/tts', name: 'tts', methods: ['GET'])]
    public function tts(Objectif $objectif, VoiceRssService $tts): Response
    {
        // getTitre() and getDescription() are non-nullable strings — no ?? needed
        $titre = $objectif->getTitre();
        $desc  = $objectif->getDescription();
        $text  = trim($titre . '. ' . $desc);

        if ($text === '' || $text === '.') {
            return new Response('Texte vide.', 400, [
                'Content-Type' => 'text/plain',
            ]);
        }

        try {
            $audio = $tts->textToSpeech($text, 'fr-fr', 'mp3');
        } catch (\RuntimeException $e) {
            return new Response('TTS Error: ' . $e->getMessage(), 500, [
                'Content-Type' => 'text/plain',
            ]);
        }

        return new Response($audio, 200, [
            'Content-Type'  => 'audio/mpeg',
            'Cache-Control' => 'private, max-age=3600',
        ]);
    }
}