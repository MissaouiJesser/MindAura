<?php

namespace App\Repository;

use App\Entity\Salle;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\Tools\Pagination\Paginator;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Salle>
 */
class SalleRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Salle::class);
    }

    public function save(Salle $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);
        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(Salle $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);
        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * Remplace findAll() avec JOIN sur le local pour éviter le N+1 et unbounded query.
     * Utilise Paginator car fetch-join sur relation ManyToOne (pas de collection).
     *
     * @return array<int, Salle>
     */
    public function findAllWithLocal(int $limit = 50): array
    {
        $qb = $this->createQueryBuilder('s')
            ->leftJoin('s.local', 'l')
            ->addSelect('l')
            ->orderBy('s.nomSalle', 'ASC')
            ->setMaxResults($limit);

        return iterator_to_array(new Paginator($qb, fetchJoinCollection: false));
    }

    /**
     * Salles disponibles uniquement avec LIMIT.
     *
     * @return array<int, Salle>
     */
    public function findDisponibles(int $limit = 50): array
    {
        $qb = $this->createQueryBuilder('s')
            ->leftJoin('s.local', 'l')
            ->addSelect('l')
            ->andWhere('s.disponibiliteSalle = :dispo')
            ->setParameter('dispo', 'Disponible')
            ->orderBy('s.nomSalle', 'ASC')
            ->setMaxResults($limit);

        return iterator_to_array(new Paginator($qb, fetchJoinCollection: false));
    }

    // ── Méthodes statistiques dashboard ─────────────────────────────────────

    /**
     * Compte total des salles (sans charger les entités).
     */
    public function countTotal(): int
    {
        return (int) $this->createQueryBuilder('s')
            ->select('COUNT(s.id)')
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Compte les salles par disponibilité.
     * Retourne ['Disponible' => N, 'En maintenance' => M, ...].
     *
     * @return array<string, int>
     */
    public function countByDisponibilite(): array
    {
        $rows = $this->createQueryBuilder('s')
            ->select('s.disponibiliteSalle AS dispo, COUNT(s.id) AS total')
            ->groupBy('s.disponibiliteSalle')
            ->getQuery()
            ->getResult();

        $result = [];
        foreach ($rows as $row) {
            $result[(string) $row['dispo']] = (int) $row['total'];
        }
        return $result;
    }

    /**
     * Compte les salles par type (pour le graphique dashboard).
     * Retourne ['Salle de réunion' => N, ...] trié par total DESC, limité à $top.
     *
     * @return array<string, int>
     */
    public function countByType(int $top = 5): array
    {
        $rows = $this->createQueryBuilder('s')
            ->select('s.typeSalle AS type, COUNT(s.id) AS total')
            ->groupBy('s.typeSalle')
            ->orderBy('total', 'DESC')
            ->setMaxResults($top)
            ->getQuery()
            ->getResult();

        $result = [];
        foreach ($rows as $row) {
            $result[(string) ($row['type'] ?? 'Autre')] = (int) $row['total'];
        }
        return $result;
    }

    /**
     * Salles d'un local précis avec LIMIT.
     *
     * @return array<int, Salle>
     */
    public function findByLocal(int $idLocal, int $limit = 50): array
    {
        return $this->createQueryBuilder('s')
            ->andWhere('s.local = :id')
            ->setParameter('id', $idLocal)
            ->orderBy('s.nomSalle', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    /**
     * Recherche par nom ou type avec LIMIT.
     *
     * @return array<int, Salle>
     */
    public function findBySearch(string $query, int $limit = 50): array
    {
        return $this->createQueryBuilder('s')
            ->andWhere('s.nomSalle LIKE :q OR s.typeSalle LIKE :q')
            ->setParameter('q', '%' . $query . '%')
            ->orderBy('s.nomSalle', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }
}