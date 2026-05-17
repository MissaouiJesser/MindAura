<?php
// src/Controller/ReservationLocalController.php

namespace App\Controller;

use App\Service\EmailReservationService;
use App\Entity\ReservationLocal;
use App\Form\ReservationLocalType;
use App\Repository\LocalsPsychiatrieRepository;
use App\Repository\NotificationRepository;
use App\Repository\ReservationLocalRepository;
use App\Repository\SalleRepository;
use App\Service\NotificationService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

#[Route('/reservations', name: 'reservation_local_')]
class ReservationLocalController extends AbstractController
{
    // ── INDEX ──────────────────────────────────────────────────────────────
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(ReservationLocalRepository $repo): Response
    {
        return $this->render('reservation/index_reservation_local.html.twig', [
            'reservations' => $repo->findAllWithLocal(),
        ]);
    }

    // ── NEW (back-office) ──────────────────────────────────────────────────
    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        NotificationService $notifService
    ): Response {
        $reservation = new ReservationLocal();

        /** @var \App\Entity\Utilisateurs $user */
        $user = $this->getUser();
        $reservation->setIdUtilisateur($user->getIdUtilisateur());

        $form = $this->createForm(ReservationLocalType::class, $reservation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($reservation);
            $em->flush();

            $lien = $this->generateUrl(
                'reservation_local_show',
                ['id' => $reservation->getIdReservation()],
                UrlGeneratorInterface::ABSOLUTE_PATH
            );

            $notifService->notifierCreation($reservation, $lien);

            $this->addFlash('success', 'Réservation créée avec succès.');
            return $this->redirectToRoute('reservation_local_index');
        }

        return $this->render('reservation/new_reservation_local.html.twig', [
            'form' => $form,
        ]);
    }

    // ── FRONT NEW ──────────────────────────────────────────────────────────
    #[Route('/front/new', name: 'front_new', methods: ['GET', 'POST'])]
    public function frontNew(
        Request $request,
        EntityManagerInterface $em,
        LocalsPsychiatrieRepository $localRepo,
        SalleRepository $salleRepo,
        NotificationService $notifService,
        EmailReservationService $emailService
    ): Response {
        $reservation = new ReservationLocal();
        $user = $this->getUser();

        if ($user) {
            /** @var \App\Entity\Utilisateurs $user */
            $reservation->setIdUtilisateur($user->getIdUtilisateur());
            $reservation->setNomCl($user->getNomUtilisateur());
            $reservation->setPrenomCl($user->getPrenomUtilisateur());
        }

        $localId = $request->query->get('local');
        $salleId = $request->query->get('salle');

        if ($localId) {
            $local = $localRepo->find($localId);
            if ($local) $reservation->setLocal($local);
        }

        if ($salleId) {
            $salle = $salleRepo->find($salleId);
            if ($salle) $reservation->setSalle($salle);
        }

        $form = $this->createForm(ReservationLocalType::class, $reservation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($reservation);
            $em->flush();

            $lien = $this->generateUrl(
                'reservation_local_show',
                ['id' => $reservation->getIdReservation()],
                UrlGeneratorInterface::ABSOLUTE_PATH
            );

            // ── 📧 Email de confirmation ──
            /** @var \App\Entity\Utilisateurs|null $user */
            $emailUser = $user?->getEmailUtilisateur();
            if ($emailUser) {
                try {
                    $emailService->envoyerConfirmation($reservation, $emailUser);
                    $this->addFlash('success', 'Réservation créée avec succès. Un email de confirmation vous a été envoyé à ' . $emailUser . '.');
                } catch (\Throwable $e) {
                    $this->addFlash('success', 'Réservation créée avec succès.');
                    $this->addFlash('warning', 'L\'email de confirmation n\'a pas pu être envoyé. Veuillez vérifier votre adresse email.');
                }
            } else {
                $this->addFlash('success', 'Réservation créée avec succès.');
                $this->addFlash('warning', 'Aucun email de confirmation envoyé : adresse email introuvable sur votre compte.');
            }

            $notifService->notifierCreation($reservation, $lien);

            return $this->redirectToRoute('reservation_local_front_index');
        }

        return $this->render('reservation/front_new_reservation_local.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    // ── FRONT INDEX ────────────────────────────────────────────────────────
    #[Route('/front', name: 'front_index', methods: ['GET'])]
    public function frontIndex(ReservationLocalRepository $repo): Response
    {
        /** @var \App\Entity\Utilisateurs|null $user */
        $user   = $this->getUser();
        $userId = $user?->getIdUtilisateur() ?? 0;

        return $this->render('reservation/front_index_reservation_local.html.twig', [
            'reservations' => $repo->findByUserWithLocal($userId),
        ]);
    }

    // ── FRONT CALENDRIER ───────────────────────────────────────────────────
    #[Route('/front/calendrier', name: 'front_calendrier', methods: ['GET'])]
    public function frontCalendrier(ReservationLocalRepository $repo): Response
    {
        /** @var \App\Entity\Utilisateurs|null $user */
        $user   = $this->getUser();
        $userId = $user?->getIdUtilisateur() ?? 0;

        return $this->render('reservation/front_calendrier_reservation.html.twig', [
            'reservations' => $repo->findByUserWithLocal($userId),
        ]);
    }

    // ── FRONT EDIT ────────────────────────────────────────────────────────
    #[Route('/front/{id}/edit', name: 'front_edit', methods: ['GET', 'POST'])]
    public function frontEdit(
        Request $request,
        ReservationLocal $reservation,
        EntityManagerInterface $em,
        NotificationService $notifService
    ): Response {
        $form = $this->createForm(ReservationLocalType::class, $reservation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();

            $lien = $this->generateUrl(
                'reservation_local_show',
                ['id' => $reservation->getIdReservation()],
                UrlGeneratorInterface::ABSOLUTE_PATH
            );
            $notifService->notifierModification($reservation, $lien);

            $this->addFlash('success', 'Réservation modifiée avec succès.');
            return $this->redirectToRoute('reservation_local_front_index');
        }

        return $this->render('reservation/front_edit_reservation_local.html.twig', [
            'reservation' => $reservation,
            'form'        => $form->createView(),
        ]);
    }

        // ── SHOW ───────────────────────────────────────────────────────────────
    #[Route('/{id}', name: 'show', methods: ['GET'])]
    public function show(ReservationLocal $reservation): Response
    {
        return $this->render('reservation/show_reservation_local.html.twig', [
            'reservation' => $reservation,
        ]);
    }

    // ── EDIT ───────────────────────────────────────────────────────────────
    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(
        Request $request,
        ReservationLocal $reservation,
        EntityManagerInterface $em,
        NotificationService $notifService
    ): Response {
        $form = $this->createForm(ReservationLocalType::class, $reservation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();

            $lien = $this->generateUrl(
                'reservation_local_show',
                ['id' => $reservation->getIdReservation()],
                UrlGeneratorInterface::ABSOLUTE_PATH
            );
            $notifService->notifierModification($reservation, $lien);

            $this->addFlash('success', 'Réservation modifiée avec succès.');
            return $this->redirectToRoute('reservation_local_show', ['id' => $reservation->getIdReservation()]);
        }

        return $this->render('reservation/edit_reservation_local.html.twig', [
            'reservation' => $reservation,
            'form'        => $form,
        ]);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────
    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(
        Request $request,
        ReservationLocal $reservation,
        EntityManagerInterface $em,
        NotificationService $notifService,
        EmailReservationService $emailService
    ): Response {
        if ($this->isCsrfTokenValid(
            'delete' . $reservation->getIdReservation(),
            $request->getPayload()->getString('_token')
        )) {
            $idReservation   = $reservation->getIdReservation() ?? 0;
            $nomClient       = trim($reservation->getPrenomCl() . ' ' . $reservation->getNomCl());
            $dateReservation = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';

            /** @var \App\Entity\Utilisateurs|null $user */
            $user = $this->getUser();
            $emailUser = $user?->getEmailUtilisateur();

            if ($emailUser) {
                try {
                    $emailService->envoyerAnnulation($reservation, $emailUser);
                } catch (\Throwable $e) {
                    $this->addFlash('warning', 'L\'email d\'annulation n\'a pas pu être envoyé.');
                }
            }

            $em->remove($reservation);
            $em->flush();

            $notifService->notifierSuppression($idReservation, $nomClient, $dateReservation);

            $this->addFlash('success', 'Réservation supprimée. Un email d\'annulation a été envoyé au patient.');
        }

        return $this->redirectToRoute('reservation_local_index');
    }

    // ── API : liste des notifications ──────────────────────────────────────
    #[Route('/notifications/list', name: 'notifications_list', methods: ['GET'])]
    public function notificationsList(NotificationRepository $repo): JsonResponse
    {
        $notifications = $repo->findRecent(15);
        $unread        = $repo->countUnread();

        $data = array_map(function ($n) {
            return [
                'id'      => $n->getId(),
                'type'    => $n->getType(),
                'label'   => $n->getTypeLabel(),
                'message' => $n->getMessage(),
                'lien'    => $n->getLien(),
                'estLue'  => $n->isEstLue(),
                'temps'   => $n->getTempsRelatif(),
                'couleur' => $n->getTypeColor(),
                'icone'   => $n->getTypeIcon(),
            ];
        }, $notifications);

        return $this->json(['notifications' => $data, 'unread' => $unread]);
    }

    // ── API : marquer toutes les notifications comme lues ──────────────────
    #[Route('/notifications/mark-read', name: 'notifications_mark_read', methods: ['POST'])]
    public function markAllRead(NotificationRepository $repo): JsonResponse
    {
        $repo->markAllAsRead();
        return $this->json(['success' => true]);
    }

    // ── PAGE IA RECOMMANDATION ─────────────────────────────────────────────
    #[Route('/front/ai-recommend', name: 'ai_recommend', methods: ['GET'])]
    public function aiRecommend(
        LocalsPsychiatrieRepository $localRepo
    ): Response {
        return $this->render('reservation/front_ai_recommendation.html.twig', [
            'locaux' => $localRepo->findDisponibles(),
        ]);
    }
}