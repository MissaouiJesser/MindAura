<?php

namespace App\Repository;

use App\Entity\Reclamation;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Reclamation>
 */
class ReclamationRepository extends ServiceEntityRepository
{
    public const PAGE_SIZE = 10;
    public const DEFAULT_SORT = 'dateCreation_reclamation';
    public const DEFAULT_DIRECTION = 'DESC';
    public const SORTABLE_COLUMNS = ['id_reclamation', 'sujet_reclamation', 'statut_reclamation', 'dateCreation_reclamation', 'rate_Reclamation'];

    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Reclamation::class);
    }

    /**
     * Recherche et pagination des réclamations avec filtres
     *
     * @param array<string, mixed> $params
     * @return array{items: array<int, Reclamation>, total: int, pages: int, page: int, perPage: int}
     */
    public function findFiltered(array $params): array
    {
        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.categorie', 'c')
            ->addSelect('c');

        // Recherche
        if (!empty($params['search'])) {
            $searchTerm = $this->sanitizeSearchTerm($params['search']);
            $qb->andWhere('r.sujet_reclamation LIKE :search OR r.description_reclamation LIKE :search')
               ->setParameter('search', '%' . $searchTerm . '%');
        }

        // Filtres
        if (!empty($params['statut'])) {
            $statusList = $this->expandStatusAliases((array) $params['statut']);
            if ($statusList !== []) {
                $qb->andWhere('r.statut_reclamation IN (:statuts)')
                   ->setParameter('statuts', $statusList);
            }
        }

        if (!empty($params['categorie'])) {
            $qb->andWhere('c.id_categorie = :categorie')
               ->setParameter('categorie', $params['categorie']);
        }

        if (!empty($params['dateDebut'])) {
            $qb->andWhere('r.dateCreation_reclamation >= :dateDebut')
               ->setParameter('dateDebut', new \DateTime($params['dateDebut']));
        }

        if (!empty($params['dateFin'])) {
            $qb->andWhere('r.dateCreation_reclamation <= :dateFin')
               ->setParameter('dateFin', new \DateTime($params['dateFin'] . ' 23:59:59'));
        }

        if (!empty($params['userId'])) {
            $qb->andWhere('r.id_utilisateur = :filterUserId')
               ->setParameter('filterUserId', (int) $params['userId']);
        }

        // Tri
        $sort = in_array($params['sort'] ?? self::DEFAULT_SORT, self::SORTABLE_COLUMNS)
            ? $params['sort']
            : self::DEFAULT_SORT;
        $direction = strtoupper($params['direction'] ?? self::DEFAULT_DIRECTION) === 'ASC' ? 'ASC' : 'DESC';
        $qb->orderBy('r.' . $sort, $direction);

        // Pagination
        $page = max(1, (int)($params['page'] ?? 1));
        $perPage = max(1, (int)($params['perPage'] ?? self::PAGE_SIZE));
        $qb->setFirstResult(($page - 1) * $perPage)
           ->setMaxResults($perPage);

        /** @var array<int, Reclamation> $items */
        $items = $qb->getQuery()->getResult();

        // Total pour pagination
        $totalQuery = clone $qb;
        $total = (int)$totalQuery->select('COUNT(r.id_reclamation)')
                                ->setFirstResult(0)
                                ->setMaxResults(null)
                                ->getQuery()
                                ->getSingleScalarResult();

        $pages = (int) ceil($total / $perPage);

        return [
            'items' => $items,
            'total' => $total,
            'pages' => $pages,
            'page' => $page,
            'perPage' => $perPage,
        ];
    }

    /**
     * Récupère toutes les réclamations filtrées (pour export)
     *
     * @param array<string, mixed> $params
     * @return array<int, Reclamation>
     */
    public function findAllFiltered(array $params): array
    {
        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.categorie', 'c')
            ->addSelect('c');

        // Recherche
        if (!empty($params['search'])) {
            $searchTerm = $this->sanitizeSearchTerm($params['search']);
            $qb->andWhere('r.sujet_reclamation LIKE :search OR r.description_reclamation LIKE :search')
               ->setParameter('search', '%' . $searchTerm . '%');
        }

        // Filtres
        if (!empty($params['statut'])) {
            $statusList = $this->expandStatusAliases((array) $params['statut']);
            if ($statusList !== []) {
                $qb->andWhere('r.statut_reclamation IN (:statuts)')
                   ->setParameter('statuts', $statusList);
            }
        }

        if (!empty($params['categorie'])) {
            $qb->andWhere('c.id_categorie = :categorie')
               ->setParameter('categorie', $params['categorie']);
        }

        if (!empty($params['dateDebut'])) {
            $qb->andWhere('r.dateCreation_reclamation >= :dateDebut')
               ->setParameter('dateDebut', new \DateTime($params['dateDebut']));
        }

        if (!empty($params['dateFin'])) {
            $qb->andWhere('r.dateCreation_reclamation <= :dateFin')
               ->setParameter('dateFin', new \DateTime($params['dateFin'] . ' 23:59:59'));
        }

        if (!empty($params['userId'])) {
            $qb->andWhere('r.id_utilisateur = :filterUserId')
               ->setParameter('filterUserId', (int) $params['userId']);
        }

        /** @var array<int, Reclamation> $result */
        $result = $qb->orderBy('r.dateCreation_reclamation', 'DESC')
                     ->getQuery()
                     ->getResult();

        return $result;
    }

    /**
     * Nettoie le terme de recherche pour éviter les erreurs UTF-8
     */
    private function sanitizeSearchTerm(string $term): string
    {
        if (!mb_check_encoding($term, 'UTF-8')) {
            $term = (string) mb_convert_encoding($term, 'UTF-8', 'auto');
        }

        return addslashes($term);
    }

    /**
     * @param array<int,string> $inputStatuses
     * @return array<int,string>
     */
    private function expandStatusAliases(array $inputStatuses): array
    {
        $aliases = [];
        foreach ($inputStatuses as $raw) {
            $status = trim((string) $raw);
            if ($status === '') {
                continue;
            }

            match ($status) {
                'EN_ATTENTE', 'En attente' => $aliases = [...$aliases, 'EN_ATTENTE', 'En attente'],
                'EN_COURS', 'En cours', 'En cours de traitement' => $aliases = [...$aliases, 'EN_COURS', 'En cours', 'En cours de traitement'],
                'TRAITEE', 'Traitée', 'Résolu' => $aliases = [...$aliases, 'TRAITEE', 'Traitée', 'Résolu'],
                'REJETEE', 'Rejetée', 'Rejeté' => $aliases = [...$aliases, 'REJETEE', 'Rejetée', 'Rejeté'],
                default => $aliases[] = $status,
            };
        }

        return array_values(array_unique($aliases));
    }
}