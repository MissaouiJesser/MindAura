<?php

namespace App\Repository;

use App\Entity\Objectif;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Objectif>
 */
class ObjectifRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Objectif::class);
    }

    /** Filtre liste (valeur select) → libellé catégorie en base */
    private function resolveCategorieFromTypeFilter(string $type): string
    {
        return match ($type) {
            'developpement' => 'développement personnel',
            'performance'   => 'carrière',
            'bien-etre'     => 'santé mentale',
            'relations'     => 'éducation',
            'autre'         => 'autre',
            default         => $type,
        };
    }

    /**
     * @param array<string, mixed> $filters
     * @return array{items: Objectif[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchForAdmin(array $filters): array
    {
        $q       = trim((string) ($filters['q']       ?? ''));
        $type    = trim((string) ($filters['type']    ?? ''));
        $statut  = trim((string) ($filters['statut']  ?? ''));
        $source  = trim((string) ($filters['source']  ?? ''));
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 5)));

        $qb = $this->createQueryBuilder('o');

        if ($q !== '') {
            $qb->andWhere('LOWER(o.titre) LIKE :q OR LOWER(o.description) LIKE :q OR LOWER(o.categorie) LIKE :q')
               ->setParameter('q', '%' . mb_strtolower($q) . '%');
        }
        if ($type !== '') {
            $categorie = $this->resolveCategorieFromTypeFilter($type);
            $qb->andWhere('LOWER(o.categorie) = :catfilt')
               ->setParameter('catfilt', mb_strtolower($categorie));
        }
        if ($statut !== '') {
            $qb->andWhere('LOWER(o.statut) = :statut')
               ->setParameter('statut', mb_strtolower($statut));
        }
        if ($source !== '') {
            $qb->andWhere('LOWER(o.source) = :source')
               ->setParameter('source', mb_strtolower($source));
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(o.id_objectif)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('o.date_creation', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    /**
     * @param array<string, mixed> $filters
     * @return array{items: Objectif[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchAdoptedByUser(int $userId, array $filters): array
    {
        $q       = trim((string) ($filters['q']       ?? ''));
        $type    = trim((string) ($filters['type']    ?? ''));
        $statut  = trim((string) ($filters['statut']  ?? ''));
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 8)));

        $qb = $this->createQueryBuilder('o')
            ->andWhere('o.source = :source')
            ->andWhere('o.id_utilisateur = :uid')
            ->setParameter('source', 'patient')
            ->setParameter('uid', $userId);

        if ($q !== '') {
            $qb->andWhere('LOWER(o.titre) LIKE :q OR LOWER(o.description) LIKE :q OR LOWER(o.categorie) LIKE :q')
               ->setParameter('q', '%' . mb_strtolower($q) . '%');
        }
        if ($type !== '') {
            $knownCats = ['developpement', 'performance', 'bien-etre', 'relations', 'autre'];
            if (\in_array($type, $knownCats, true)) {
                $categorie = $this->resolveCategorieFromTypeFilter($type);
                $qb->andWhere('LOWER(o.categorie) = :catfilt2')
                   ->setParameter('catfilt2', mb_strtolower($categorie));
            } else {
                $qb->andWhere('LOWER(o.type_objectif) LIKE :typfree OR LOWER(o.categorie) LIKE :typfree')
                   ->setParameter('typfree', '%' . mb_strtolower($type) . '%');
            }
        }
        if ($statut !== '') {
            $qb->andWhere('LOWER(o.statut) = :statut')
               ->setParameter('statut', mb_strtolower($statut));
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(o.id_objectif)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('o.date_creation', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    /**
     * @return Objectif[]
     */
    public function findSuggestedForScore(string $testType, int $score): array
    {
        return $this->createQueryBuilder('o')
            ->andWhere('o.source = :source')
            ->andWhere('LOWER(o.type_test) = :typeTest')
            ->andWhere('o.score_min <= :score')
            ->andWhere('o.score_max >= :score')
            ->setParameter('source', 'admin')
            ->setParameter('typeTest', mb_strtolower($testType))
            ->setParameter('score', $score)
            ->orderBy('o.date_creation', 'DESC')
            ->setMaxResults(50)
            ->getQuery()
            ->getResult();
    }

    public function countAdminCatalog(): int
    {
        return (int) $this->createQueryBuilder('o')
            ->select('COUNT(o.id_objectif)')
            ->andWhere('o.source = :source')
            ->setParameter('source', 'admin')
            ->getQuery()
            ->getSingleScalarResult();
    }
}