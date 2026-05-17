<?php
// src/Repository/ReservationLocalRepository.php

namespace App\Repository;

use App\Entity\ReservationLocal;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<ReservationLocal>
 */
class ReservationLocalRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, ReservationLocal::class);
    }

    /**
     * Retourne toutes les réservations triées par date décroissante
     * avec le local chargé en une seule requête (évite le N+1)
     *
     * @return array<int, ReservationLocal>
     */
    public function findAllWithLocal(int $limit = 50): array
    {
        return $this->createQueryBuilder('r')
            ->leftJoin('r.local', 'l')
            ->addSelect('l')
            ->orderBy('r.dateReservation', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Recherche par nom/prénom client ou nom du local
     *
     * @return array<int, ReservationLocal>
     */
    public function search(string $q, int $limit = 50): array
    {
        $q = '%' . strtolower($q) . '%';

        return $this->createQueryBuilder('r')
            ->leftJoin('r.local', 'l')
            ->addSelect('l')
            ->where('LOWER(r.nomCl) LIKE :q OR LOWER(r.prenomCl) LIKE :q OR LOWER(l.nomLocal) LIKE :q')
            ->setParameter('q', $q)
            ->orderBy('r.dateReservation', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Retourne toutes les réservations du patient connecté triées par date décroissante.
     *
     * @return array<int, ReservationLocal>
     */
    public function findByUserWithLocal(int $userId, int $limit = 50): array
    {
        return $this->createQueryBuilder('r')
            ->leftJoin('r.local', 'l')
            ->addSelect('l')
            ->where('r.idUtilisateur = :userId')
            ->setParameter('userId', $userId)
            ->orderBy('r.dateReservation', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Filtre par statut
     *
     * @return array<int, ReservationLocal>
     */
    public function findByStatus(string $status, int $limit = 50): array
    {
        return $this->createQueryBuilder('r')
            ->leftJoin('r.local', 'l')
            ->addSelect('l')
            ->where('r.statusReservation = :status')
            ->setParameter('status', $status)
            ->orderBy('r.dateReservation', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }
}