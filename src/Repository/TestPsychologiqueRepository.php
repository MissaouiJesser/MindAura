<?php

namespace App\Repository;

use App\Entity\TestPsychologique;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<TestPsychologique>
 */
class TestPsychologiqueRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, TestPsychologique::class);
    }

    /**
     * Recherche/filter/pagination admin.
     *
     * @param array<string, mixed> $filters
     * @return array{items: TestPsychologique[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchForAdmin(array $filters): array
    {
        $q       = trim((string) ($filters['q']       ?? ''));
        $type    = trim((string) ($filters['type']    ?? ''));
        $statut  = trim((string) ($filters['statut']  ?? ''));
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 5)));

        $qb = $this->createQueryBuilder('t');

        if ($q !== '') {
            $qb->andWhere('LOWER(t.titre_test) LIKE :q OR LOWER(t.description_test) LIKE :q')
               ->setParameter('q', '%' . mb_strtolower($q) . '%');
        }
        if ($type !== '') {
            $qb->andWhere('LOWER(t.type_test) = :type')
               ->setParameter('type', mb_strtolower($type));
        }
        if ($statut === 'actif') {
            $qb->andWhere('t.est_actif = true');
        } elseif ($statut === 'inactif') {
            $qb->andWhere('t.est_actif = false');
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(t.id_test)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('t.date_creation', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    /**
     * Recherche/filter/pagination front.
     *
     * @param array<string, mixed> $filters
     * @return array{items: TestPsychologique[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchForFront(array $filters): array
    {
        $q       = trim((string) ($filters['q']       ?? ''));
        $type    = trim((string) ($filters['type']    ?? ''));
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 6)));

        $qb = $this->createQueryBuilder('t')
            ->andWhere('t.est_actif = true');

        if ($q !== '') {
            $qb->andWhere('LOWER(t.titre_test) LIKE :q OR LOWER(t.description_test) LIKE :q')
               ->setParameter('q', '%' . mb_strtolower($q) . '%');
        }
        if ($type !== '') {
            $qb->andWhere('LOWER(t.type_test) = :type')
               ->setParameter('type', mb_strtolower($type));
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(t.id_test)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('t.date_creation', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    public function countActive(): int
    {
        return (int) $this->createQueryBuilder('t')
            ->select('COUNT(t.id_test)')
            ->andWhere('t.est_actif = true')
            ->getQuery()
            ->getSingleScalarResult();
    }
}