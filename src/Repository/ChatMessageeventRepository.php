<?php

namespace App\Repository;

use App\Entity\ChatMessageevent;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<ChatMessageevent>
 */
class ChatMessageeventRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, ChatMessageevent::class);
    }

    /**
     * Récupère les N derniers messages d'un événement (ordre chronologique).
     *
     * @return list<ChatMessageevent>
     */
    public function findByEvenement(int $evenementId, int $limit = 100): array
    {
        return $this->createQueryBuilder('m')
            ->where('m.evenement = :id')
            ->setParameter('id', $evenementId)
            ->orderBy('m.sentAt', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Récupère uniquement les messages après un timestamp (pour le polling AJAX).
     *
     * @return list<ChatMessageevent>
     */
    public function findAfter(int $evenementId, \DateTimeInterface $after): array
    {
        return $this->createQueryBuilder('m')
            ->where('m.evenement = :id')
            ->andWhere('m.sentAt > :after')
            ->setParameter('id', $evenementId)
            ->setParameter('after', $after)
            ->orderBy('m.sentAt', 'ASC')
            ->getQuery()
            ->getResult();
    }
}