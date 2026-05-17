<?php

namespace App\Repository;

use App\Entity\Ressources;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\ORM\Tools\Pagination\Paginator;

/**
 * @extends ServiceEntityRepository<Ressources>
 */
class RessourcesRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Ressources::class);
    }

    /**
     * FIX PHPStan : surcharge find() pour retourner ?Ressources au lieu de object|null.
     *
     * {@inheritdoc}
     */
    public function find(mixed $id, mixed $lockMode = null, mixed $lockVersion = null): ?Ressources
    {
        /** @var ?Ressources */
        return parent::find($id, $lockMode, $lockVersion);
    }

    // ──────────────────────────────────────────────────────────────────
    // Comptage pour la pagination
    // ──────────────────────────────────────────────────────────────────

    public function countSearch(
        ?string $q,
        ?string $type,
        ?string $niveau,
        ?string $categorie
    ): int {
        $qb = $this->createQueryBuilder('r')
            ->select('COUNT(r.id)');

        $this->applyFilters($qb, $q, $type, $niveau, $categorie);

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    // ──────────────────────────────────────────────────────────────────
    // Recherche paginée
    // ──────────────────────────────────────────────────────────────────

    /**
     * @return Ressources[]
     */
    public function search(
        ?string $q,
        ?string $type,
        ?string $niveau,
        ?string $categorie,
        ?string $sort = 'date_desc',
        int $page = 1,
        int $limit = 8
    ): array {
        $qb = $this->createQueryBuilder('r');

        $this->applyFilters($qb, $q, $type, $niveau, $categorie);

        match ($sort) {
            'vues_desc'  => $qb->orderBy('r.nbr_vues', 'DESC'),
            'likes_desc' => $qb->orderBy('r.likes', 'DESC'),
            'date_asc'   => $qb->orderBy('r.date_publication', 'ASC'),
            default      => $qb->orderBy('r.date_publication', 'DESC'),
        };

        $qb->setFirstResult(($page - 1) * $limit)
           ->setMaxResults($limit);

        /** @var Ressources[] */
        return $qb->getQuery()->getResult();
    }

    // ──────────────────────────────────────────────────────────────────
    // Dashboard — méthodes statistiques
    // ──────────────────────────────────────────────────────────────────

    /**
     * @return array<int, array{contenu: string, total: int}>
     */
    public function countByType(): array
    {
        /** @var array<int, array{contenu: string, total: int}> */
        return $this->createQueryBuilder('r')
            ->select('r.contenu, COUNT(r) AS total')
            ->groupBy('r.contenu')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, array{categorie: string, total: int}>
     */
    public function countByCategorie(): array
    {
        /** @var array<int, array{categorie: string, total: int}> */
        return $this->createQueryBuilder('r')
            ->select('r.categorie, COUNT(r) AS total')
            ->groupBy('r.categorie')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, array{niveau: string, total: int}>
     */
    public function countByNiveau(): array
    {
        /** @var array<int, array{niveau: string, total: int}> */
        return $this->createQueryBuilder('r')
            ->select('r.niveau, COUNT(r) AS total')
            ->groupBy('r.niveau')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return Ressources[]
     */
    public function findTopVues(int $limit = 5): array
    {
        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.commentaires', 'c')
            ->addSelect('c')
            ->orderBy('r.nbr_vues', 'DESC')
            ->setMaxResults($limit);

        $paginator = new Paginator($qb->getQuery(), fetchJoinCollection: true);

        return iterator_to_array($paginator);
    }

    /**
     * @return Ressources[]
     */
    public function findTopLikes(int $limit = 5): array
    {
        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.commentaires', 'c')
            ->addSelect('c')
            ->orderBy('r.likes', 'DESC')
            ->setMaxResults($limit);

        $paginator = new Paginator($qb->getQuery(), fetchJoinCollection: true);

        return iterator_to_array($paginator);
    }

    // ──────────────────────────────────────────────────────────────────
    // Recommandations JSON (endpoint /recommended)
    // ──────────────────────────────────────────────────────────────────

    /**
     * @return Ressources[]
     */
    public function findRecommended(?string $categorie, ?int $excludeId, int $limit = 6): array
    {
        $qb = $this->createQueryBuilder('r')
            ->orderBy('r.likes', 'DESC')
            ->addOrderBy('r.nbr_vues', 'DESC')
            ->setMaxResults($limit);

        if ($categorie) {
            $qb->andWhere('r.categorie = :cat')->setParameter('cat', $categorie);
        }
        if ($excludeId) {
            $qb->andWhere('r.id != :exc')->setParameter('exc', $excludeId);
        }

        /** @var Ressources[] */
        return $qb->getQuery()->getResult();
    }

    // ──────────────────────────────────────────────────────────────────
    // Recommandations entités (page show — même catégorie)
    // ──────────────────────────────────────────────────────────────────

    /**
     * @return Ressources[]
     */
    public function findRecommendedEntities(?string $categorie, ?int $excludeId, int $limit = 6): array
    {
        $results = [];

        // 1. Try to find resources in the same category
        if ($categorie) {
            $qb = $this->createQueryBuilder('r')
                ->where('r.categorie = :cat')
                ->setParameter('cat', $categorie)
                ->orderBy('r.likes', 'DESC')
                ->addOrderBy('r.nbr_vues', 'DESC')
                ->setMaxResults($limit);

            if ($excludeId !== null) {
                $qb->andWhere('r.id != :exc')->setParameter('exc', $excludeId);
            }

            $results = $qb->getQuery()->getResult();
        }

        // 2. If we don't have enough results, complement with popular ones from ANY category
        if (count($results) < $limit) {
            $qbGlobal = $this->createQueryBuilder('r')
                ->orderBy('r.likes', 'DESC')
                ->addOrderBy('r.nbr_vues', 'DESC')
                ->setMaxResults($limit - count($results));

            if ($excludeId !== null) {
                $qbGlobal->andWhere('r.id != :exc')->setParameter('exc', $excludeId);
            }

            if (!empty($results)) {
                $ids = array_map(fn($res) => $res->getId(), $results);
                $qbGlobal->andWhere('r.id NOT IN (:ids)')->setParameter('ids', $ids);
            }

            $globalResults = $qbGlobal->getQuery()->getResult();
            $results = array_merge($results, $globalResults);
        }

        return $results;
    }

    // ──────────────────────────────────────────────────────────────────
    // Helper privé — filtres communs
    // ──────────────────────────────────────────────────────────────────

    private function applyFilters(
        \Doctrine\ORM\QueryBuilder $qb,
        ?string $q,
        ?string $type,
        ?string $niveau,
        ?string $categorie
    ): void {
        if ($q) {
            $qb->andWhere('r.titre LIKE :q OR r.resume LIKE :q OR r.tags LIKE :q')
               ->setParameter('q', '%' . $q . '%');
        }
        if ($type) {
            $qb->andWhere('r.contenu = :type')->setParameter('type', $type);
        }
        if ($niveau) {
            $qb->andWhere('r.niveau = :niveau')->setParameter('niveau', $niveau);
        }
        if ($categorie) {
            $qb->andWhere('r.categorie = :categorie')->setParameter('categorie', $categorie);
        }
    }
    
}