<?php
// src/Repository/FavorisRepository.php

namespace App\Repository;

use App\Entity\Favoris;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Favoris>
 */
class FavorisRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Favoris::class);
    }

    /**
     * @return array<int, Favoris>
     */
    public function findByUser(int $userId): array
    {
        return $this->createQueryBuilder('f')
            ->join('f.ressource', 'r')
            ->addSelect('r')
            ->where('f.user = :user')
            ->setParameter('user', $userId)
            ->orderBy('f.createdAt', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, int>
     */
    public function findIdsByUser(int $userId): array
    {
        $rows = $this->createQueryBuilder('f')
            ->select('IDENTITY(f.ressource) as rid')
            ->where('f.user = :user')
            ->setParameter('user', $userId)
            ->getQuery()
            ->getArrayResult();

        return array_column($rows, 'rid');
    }

    public function findOneByUserAndRessource(int $userId, int $ressourceId): ?Favoris
    {
        return $this->createQueryBuilder('f')
            ->where('f.user = :user')
            ->andWhere('f.ressource = :ressource')
            ->setParameter('user', $userId)
            ->setParameter('ressource', $ressourceId)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function isFavori(int $userId, int $ressourceId): bool
    {
        return (bool) $this->createQueryBuilder('f')
            ->select('COUNT(f.id)')
            ->where('f.user = :user')
            ->andWhere('f.ressource = :ressource')
            ->setParameter('user', $userId)
            ->setParameter('ressource', $ressourceId)
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function countByUser(int $userId): int
    {
        return (int) $this->createQueryBuilder('f')
            ->select('COUNT(f.id)')
            ->where('f.user = :user')
            ->setParameter('user', $userId)
            ->getQuery()
            ->getSingleScalarResult();
    }
}