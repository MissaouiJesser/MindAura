<?php

namespace App\Repository;

use App\Entity\LocalsPsychiatrie;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\Tools\Pagination\Paginator;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<LocalsPsychiatrie>
 */
class LocalsPsychiatrieRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, LocalsPsychiatrie::class);
    }

    /**
     * Remplace findAll() avec JOIN sur les salles pour éviter le N+1.
     *
     * @return array<int, LocalsPsychiatrie>
     */
    public function findAllWithSalles(int $limit = 50): array
    {
        // Étape 1 : récupérer les IDs des locaux avec LIMIT (pas de JOIN → pas de lignes dupliquées)
        $ids = $this->createQueryBuilder('l')
            ->select('l.id')
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getSingleColumnResult();

        if (empty($ids)) {
            return [];
        }

        // Étape 2 : charger les locaux + salles uniquement pour ces IDs
        // Paginator est obligatoire ici : setMaxResults sur un fetch-join OneToMany
        // applique le LIMIT sur les lignes SQL (pas les entités) → collections partiellement hydratées.
        // Paginator exécute 2 requêtes pour corriger ce comportement.
        $qb = $this->createQueryBuilder('l')
            ->leftJoin('l.salles', 's')
            ->addSelect('s')
            ->andWhere('l.id IN (:ids)')
            ->setParameter('ids', $ids)
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults(count($ids));

        return iterator_to_array(new Paginator($qb, fetchJoinCollection: true));
    }

    public function save(LocalsPsychiatrie $entity, bool $flush = false): void
    {
        $this->getEntityManager()->persist($entity);
        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    public function remove(LocalsPsychiatrie $entity, bool $flush = false): void
    {
        $this->getEntityManager()->remove($entity);
        if ($flush) {
            $this->getEntityManager()->flush();
        }
    }

    /**
     * Recherche des locaux par ville ou nom avec JOIN pour éviter le N+1.
     *
     * @return array<int, LocalsPsychiatrie>
     */
    public function findBySearch(string $query, int $limit = 50): array
    {
        $ids = $this->createQueryBuilder('l')
            ->select('l.id')
            ->andWhere('l.nomLocal LIKE :query OR l.villeLocal LIKE :query')
            ->setParameter('query', '%' . $query . '%')
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getSingleColumnResult();

        if (empty($ids)) {
            return [];
        }

        $qb = $this->createQueryBuilder('l')
            ->leftJoin('l.salles', 's')
            ->addSelect('s')
            ->andWhere('l.id IN (:ids)')
            ->setParameter('ids', $ids)
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults(count($ids));

        return iterator_to_array(new Paginator($qb, fetchJoinCollection: true));
    }

    /**
     * Locaux disponibles avec JOIN pour éviter le N+1.
     *
     * @return array<int, LocalsPsychiatrie>
     */
    public function findDisponibles(int $limit = 50): array
    {
        $ids = $this->createQueryBuilder('l')
            ->select('l.id')
            ->andWhere('l.disponibiliteLocal = :dispo')
            ->setParameter('dispo', 'Disponible')
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getSingleColumnResult();

        if (empty($ids)) {
            return [];
        }

        $qb = $this->createQueryBuilder('l')
            ->leftJoin('l.salles', 's')
            ->addSelect('s')
            ->andWhere('l.id IN (:ids)')
            ->setParameter('ids', $ids)
            ->orderBy('l.nomLocal', 'ASC')
            ->setMaxResults(count($ids));

        return iterator_to_array(new Paginator($qb, fetchJoinCollection: true));
    }
}
