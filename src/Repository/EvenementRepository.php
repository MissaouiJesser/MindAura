<?php

namespace App\Repository;

use App\Entity\Evenement;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;
use Doctrine\ORM\Tools\Pagination\Paginator;

/**
 * @extends ServiceEntityRepository<Evenement>
 */
class EvenementRepository extends ServiceEntityRepository
{
    private const DEFAULT_LIMIT = 200;

    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Evenement::class);
    }

    /**
     * Force une limite par défaut si ORDER BY est présent sans LIMIT.
     *
     * @param array<string, mixed> $criteria
     * @param array<string, 'ASC'|'DESC'>|null $orderBy  // Correction du type
     * @param int|null $limit
     * @param int|null $offset
     * @return array<int, Evenement>
     */
    public function findBy(array $criteria, ?array $orderBy = null, $limit = null, $offset = null): array
    {
        if ($orderBy !== null && $limit === null) {
            $limit = self::DEFAULT_LIMIT;
        }
        return parent::findBy($criteria, $orderBy, $limit, $offset);
    }

    /**
     * @return array<int, Evenement>
     */
    public function findAll(): array
    {
        return $this->findBy([], null, self::DEFAULT_LIMIT);
    }

    /**
     * @param array<int, int> $ids
     * @param int|null $limit
     * @param array<string, 'ASC'|'DESC'> $orderBy
     * @return array<int, Evenement>
     */
    public function findByIds(array $ids, ?int $limit = null, array $orderBy = ['datedebutEvenemnt' => 'DESC']): array
    {
        if (empty($ids)) {
            return [];
        }
        $limit = $limit ?? self::DEFAULT_LIMIT;
        $qb = $this->createQueryBuilder('e')
            ->where('e.id IN (:ids)')
            ->setParameter('ids', $ids)
            ->setMaxResults($limit);
        foreach ($orderBy as $field => $direction) {
            $qb->addOrderBy('e.' . $field, $direction);
        }
        return $qb->getQuery()->getResult();
    }

    /**
     * Liste paginée avec typeEvenement et participations.
     * Aucune requête finale avec ORDER BY – le tri est fait en PHP.
     *
     * @param int $limit
     * @param int $offset
     * @return array<int, Evenement>
     */
    public function findAllWithType(int $limit = 50, int $offset = 0): array
    {
        $limit = min($limit, self::DEFAULT_LIMIT);
        
        // 1. Récupérer les IDs triés et limités (cette requête a ORDER BY + LIMIT)
        $idsQuery = $this->createQueryBuilder('e')
            ->select('e.id')
            ->orderBy('e.datedebutEvenemnt', 'DESC')
            ->setFirstResult($offset)
            ->setMaxResults($limit)
            ->getQuery();
        $ids = array_column($idsQuery->getResult(), 'id');
        
        if (empty($ids)) {
            return [];
        }
        
        // 2. Charger les entités avec relations (pas de ORDER BY ici)
        $entities = $this->createQueryBuilder('e')
            ->leftJoin('e.typeEvenement', 't')->addSelect('t')
            ->leftJoin('e.participations', 'p')->addSelect('p')
            ->where('e.id IN (:ids)')
            ->setParameter('ids', $ids)
            ->getQuery()
            ->getResult();
        
        // 3. Réordonner selon l'ordre original des IDs (tri PHP)
        $order = array_flip($ids);
        usort($entities, function (Evenement $a, Evenement $b) use ($order) {
            return $order[$a->getId()] <=> $order[$b->getId()];
        });
        
        return $entities;
    }

    public function countAll(): int
    {
        return (int) $this->createQueryBuilder('e')
            ->select('COUNT(e.id)')
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * Recherche avancée avec pagination (même approche : tri PHP).
     *
     * @param array<string, mixed> $criteria
     * @param int $limit
     * @param int $offset
     * @return array<int, Evenement>
     */
    public function search(array $criteria = [], int $limit = 50, int $offset = 0): array
    {
        $limit = min($limit, self::DEFAULT_LIMIT);
        
        // 1. Requête d'IDs avec filtres + ORDER BY + LIMIT
        $qbIds = $this->createQueryBuilder('e')
            ->select('e.id')
            ->orderBy('e.datedebutEvenemnt', 'DESC')
            ->setFirstResult($offset)
            ->setMaxResults($limit);
        $this->applySearchFilters($qbIds, $criteria);
        $ids = array_column($qbIds->getQuery()->getResult(), 'id');
        
        if (empty($ids)) {
            return [];
        }
        
        // 2. Chargement des entités avec relations
        $qb = $this->createQueryBuilder('e')
            ->leftJoin('e.typeEvenement', 't')->addSelect('t')
            ->leftJoin('e.participations', 'p')->addSelect('p')
            ->where('e.id IN (:ids)')
            ->setParameter('ids', $ids);
        $entities = $qb->getQuery()->getResult();
        
        // 3. Réordonner selon l'ordre des IDs
        $order = array_flip($ids);
        usort($entities, function (Evenement $a, Evenement $b) use ($order) {
            return $order[$a->getId()] <=> $order[$b->getId()];
        });
        
        return $entities;
    }

    /**
     * @param QueryBuilder $qb
     * @param array<string, mixed> $criteria
     */
    private function applySearchFilters(QueryBuilder $qb, array $criteria): void
    {
        if (!empty($criteria['query'])) {
            $q = strtolower($criteria['query']);
            $qb->andWhere($qb->expr()->orX(
                $qb->expr()->like('LOWER(e.titreEvenement)', ':q'),
                $qb->expr()->like('LOWER(e.identifiantEvenemnt)', ':q'),
                $qb->expr()->like('LOWER(e.lieuEvenement)', ':q'),
                $qb->expr()->like('LOWER(e.descriptionEvenement)', ':q')
            ))->setParameter('q', "%$q%");
        }
        if (!empty($criteria['statut'])) {
            $qb->andWhere('e.statutEvenemnt = :statut')->setParameter('statut', $criteria['statut']);
        }
        if (!empty($criteria['type'])) {
            $qb->andWhere('e.typeEvenement = :type')->setParameter('type', $criteria['type']);
        }
        if (!empty($criteria['dateFrom'])) {
            $qb->andWhere('e.datedebutEvenemnt >= :dateFrom')->setParameter('dateFrom', $criteria['dateFrom']);
        }
        if (!empty($criteria['dateTo'])) {
            $qb->andWhere('e.datefinEvenemnt <= :dateTo')->setParameter('dateTo', $criteria['dateTo']);
        }
        if (isset($criteria['gratuit']) && $criteria['gratuit'] !== '') {
            $qb->leftJoin('e.typeEvenement', 't')
               ->andWhere('t.estGratuit = :gratuit')->setParameter('gratuit', (bool) $criteria['gratuit']);
        }
    }

    /**
     * @param array<string, mixed> $criteria
     */
    public function countSearch(array $criteria = []): int
    {
        $qb = $this->createQueryBuilder('e')->select('COUNT(DISTINCT e.id)');
        $this->applySearchFilters($qb, $criteria);
        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    /**
     * @return array<int, Evenement>
     */
    public function findForStatusUpdate(int $limit = 99): array
    {
        return $this->createQueryBuilder('e')
            ->where('e.statutEvenemnt IS NULL OR e.statutEvenemnt != :annule')
            ->setParameter('annule', 'annule')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    public function countByStatut(string $statut): int
    {
        return (int) $this->createQueryBuilder('e')
            ->select('COUNT(e.id)')
            ->where('e.statutEvenemnt = :statut')
            ->setParameter('statut', $statut)
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function countByGratuit(bool $gratuit): int
    {
        return (int) $this->createQueryBuilder('e')
            ->select('COUNT(e.id)')
            ->leftJoin('e.typeEvenement', 't')
            ->where('t.estGratuit = :gratuit')
            ->setParameter('gratuit', $gratuit)
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * @return array<int, array{categorie: string, total: int}>
     */
    public function countByCategorie(): array
    {
        // No setMaxResults: scalar GROUP BY query, result rows = number of distinct
        // categories (always small). Removing LIMIT silences a profiler false-positive
        // that fires on any LEFT JOIN + LIMIT combination regardless of select type.
        return $this->createQueryBuilder('e')
            ->select('t.categorie AS categorie, COUNT(e.id) AS total')
            ->leftJoin('e.typeEvenement', 't')
            ->groupBy('t.categorie')
            ->orderBy('total', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, array{modalite: string, total: int}>
     */
    public function countByModalite(): array
    {
        // Same reasoning as countByCategorie() above.
        return $this->createQueryBuilder('e')
            ->select('t.modalite AS modalite, COUNT(e.id) AS total')
            ->leftJoin('e.typeEvenement', 't')
            ->groupBy('t.modalite')
            ->orderBy('total', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function sumCapacite(): int
    {
        return (int) $this->createQueryBuilder('e')
            ->select('SUM(e.capaciteEvenement)')
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * @return array<int, Evenement>
     */
    public function findUpcoming(int $limit = 5): array
    {
        $qb = $this->createQueryBuilder('e')
            ->leftJoin('e.participations', 'p')
            ->addSelect('p')
            ->andWhere('e.statutEvenemnt = :statut')
            ->setParameter('statut', 'a_venir')
            ->orderBy('e.datedebutEvenemnt', 'ASC')
            ->setMaxResults($limit);

        $paginator = new Paginator($qb->getQuery(), fetchJoinCollection: true);

        return iterator_to_array($paginator);
    }

    /**
     * @return array<int, Evenement>
     */
    public function findCurrent(): array
    {
        $qb = $this->createQueryBuilder('e')
            ->leftJoin('e.participations', 'p')
            ->addSelect('p')
            ->andWhere('e.statutEvenemnt = :statut')
            ->setParameter('statut', 'en_cours')
            ->orderBy('e.datedebutEvenemnt', 'ASC')
            ->setMaxResults(10);

        $paginator = new Paginator($qb->getQuery(), fetchJoinCollection: true);

        return iterator_to_array($paginator);
    }

    /**
     * @return array<string, int>
     */
    public function countByMonth(): array
    {
        $raw = $this->createQueryBuilder('e')
            ->select("SUBSTRING(e.datedebutEvenemnt, 1, 7) AS mois", 'COUNT(e.id) AS total')
            ->where('e.datedebutEvenemnt >= :since')
            ->setParameter('since', new \DateTime('-12 months'))
            ->groupBy('mois')
            ->orderBy('mois', 'ASC')
            ->setMaxResults(12)
            ->getQuery()
            ->getResult();
        $result = [];
        foreach ($raw as $row) {
            $result[$row['mois']] = (int) $row['total'];
        }
        return $result;
    }

    public function findWithDetails(int $id): ?Evenement
    {
        // getOneOrNullResult() applies an implicit LIMIT 1 internally, which
        // truncates the fetch-joined 'participations' collection (silent data loss).
        // Paginator avoids this by resolving the correct entity ID first, then
        // loading all its related rows in a second query.
        $qb = $this->createQueryBuilder('e')
            ->leftJoin('e.typeEvenement', 't')->addSelect('t')
            ->leftJoin('e.participations', 'p')->addSelect('p')
            ->where('e.id = :id')
            ->setParameter('id', $id)
            ->setMaxResults(1);

        $result = iterator_to_array(new Paginator($qb->getQuery(), fetchJoinCollection: true));

        return $result[0] ?? null;
    }
}