<?php
// src/Service/NotificationService.php

namespace App\Service;

use App\Entity\Notification;
use App\Entity\ReservationLocal;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Service centralisé pour créer des notifications de réservation.
 *
 * Usage dans le contrôleur :
 *   $this->notificationService->notifierCreation($reservation);
 *   $this->notificationService->notifierModification($reservation);
 *   $this->notificationService->notifierSuppression($reservation);
 */
class NotificationService
{
    public function __construct(private readonly EntityManagerInterface $em) {}

    // ── Helpers privés ────────────────────────────────────────────────────

    /**
     * Construit et persiste une notification.
     */
    private function creer(
        string $type,
        string $message,
        ?int   $idReservation = null,
        ?string $lien = null
    ): Notification {
        $notif = new Notification();
        $notif->setType($type);
        $notif->setMessage($message);
        $notif->setIdReservation($idReservation);
        $notif->setLien($lien);

        $this->em->persist($notif);
        $this->em->flush();

        return $notif;
    }

    /**
     * Résumé court d'une réservation pour le message.
     */
    private function resume(ReservationLocal $r): string
    {
        $client = trim($r->getPrenomCl() . ' ' . $r->getNomCl()) ?: 'Client inconnu';
        $date   = $r->getDateReservation()?->format('d/m/Y') ?? '—';
        $local  = $r->getLocal()?->getNomLocal() ?? '—';
        return sprintf('%s — %s (%s)', $client, $date, $local);
    }

    // ── API publique ──────────────────────────────────────────────────────

    /**
     * Notification lors de la CRÉATION d'une réservation.
     */
    public function notifierCreation(ReservationLocal $r, string $lien = ''): Notification
    {
        $message = sprintf(
            'Nouvelle réservation créée pour %s',
            $this->resume($r)
        );
        return $this->creer(Notification::TYPE_CREATION, $message, $r->getIdReservation(), $lien);
    }

    /**
     * Notification lors de la MODIFICATION d'une réservation.
     */
    public function notifierModification(ReservationLocal $r, string $lien = ''): Notification
    {
        $message = sprintf(
            'Réservation #%d modifiée — %s',
            $r->getIdReservation(),
            $this->resume($r)
        );
        return $this->creer(Notification::TYPE_MODIFICATION, $message, $r->getIdReservation(), $lien);
    }

    /**
     * Notification lors de la SUPPRESSION d'une réservation.
     * On passe les infos en paramètre car l'entité sera supprimée.
     */
    public function notifierSuppression(
        int    $idReservation,
        string $nomClient,
        string $dateReservation
    ): Notification {
        $message = sprintf(
            'Réservation #%d supprimée — %s (prévue le %s)',
            $idReservation,
            $nomClient,
            $dateReservation
        );
        return $this->creer(Notification::TYPE_SUPPRESSION, $message, null);
    }

    /**
     * Notification de RAPPEL (J-1 avant la réservation).
     * notif_rappel_envoye est mis à 1 pour éviter les doublons.
     */
    public function notifierRappel(ReservationLocal $r, string $lien = ''): Notification
    {
        $message = sprintf(
            'Rappel : réservation demain pour %s',
            $this->resume($r)
        );

        $notif = new Notification();
        $notif->setType(Notification::TYPE_RAPPEL);
        $notif->setMessage($message);
        $notif->setIdReservation($r->getIdReservation());
        $notif->setLien($lien);
        $notif->setNotifRappelEnvoye(1); // marquer comme "envoyé"

        $this->em->persist($notif);
        $this->em->flush();

        return $notif;
    }
}