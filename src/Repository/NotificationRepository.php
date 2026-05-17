<?php
// src/Repository/NotificationRepository.php

namespace App\Repository;

use App\Entity\Notification;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Notification>
 */
class NotificationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Notification::class);
    }

    /**
     * Les N notifications les plus récentes (lues + non lues)
     *
     * @return array<int, Notification>
     */
    public function findRecent(int $limit = 15): array
    {
        return $this->createQueryBuilder('n')
            ->orderBy('n.creeLe', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Nombre de notifications non lues
     */
    public function countUnread(): int
    {
        return (int) $this->createQueryBuilder('n')
            ->select('COUNT(n.id)')
            ->where('n.estLue = false')
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    public function markAllAsRead(): void
    {
        $this->createQueryBuilder('n')
            ->update()
            ->set('n.estLue', 'true')
            ->where('n.estLue = false')
            ->getQuery()
            ->execute();
    }

    /**
     * Notifications liées à une réservation précise
     *
     * @return array<int, Notification>
     */
    public function findByReservation(int $idReservation): array
    {
        return $this->createQueryBuilder('n')
            ->where('n.idReservation = :id')
            ->setParameter('id', $idReservation)
            ->orderBy('n.creeLe', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Réservations dont le rappel n'a pas encore été envoyé (pour la commande Cron)
     * On filtre sur notif_rappel_envoye = 0
     *
     * @return array<int, Notification>
     */
    public function findPendingRappels(): array
    {
        return $this->createQueryBuilder('n')
            ->where('n.type = :type')
            ->andWhere('n.notifRappelEnvoye = 0')
            ->setParameter('type', 'rappel')
            ->getQuery()
            ->getResult();
    }
}