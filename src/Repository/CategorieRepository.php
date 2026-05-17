<?php

namespace App\Repository;

use App\Entity\Categorie;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Categorie>
 */
class CategorieRepository extends ServiceEntityRepository
{
    public const PAGE_SIZE = 10;
    public const DEFAULT_SORT = 'date_creation';
    public const DEFAULT_DIRECTION = 'DESC';
    public const SORTABLE_COLUMNS = ['id_categorie', 'nom_categorie', 'description', 'date_creation'];

    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Categorie::class);
    }

    /**
     * Recherche et pagination des catégories avec filtres
     *
     * @param array<string, mixed> $params
     * @return array{items: array<int, Categorie>, total: int, pages: int, page: int, perPage: int}
     */
    public function findFiltered(array $params): array
    {
        $qb = $this->createQueryBuilder('c');

        // Recherche
        if (!empty($params['search'])) {
            $searchTerm = $this->sanitizeSearchTerm($params['search']);
            $qb->andWhere('c.nom_categorie LIKE :search OR c.description LIKE :search')
               ->setParameter('search', '%' . $searchTerm . '%');
        }

        // Tri
        $sort = in_array($params['sort'] ?? self::DEFAULT_SORT, self::SORTABLE_COLUMNS)
            ? $params['sort']
            : self::DEFAULT_SORT;
        $direction = strtoupper($params['direction'] ?? self::DEFAULT_DIRECTION) === 'ASC' ? 'ASC' : 'DESC';
        $qb->orderBy('c.' . $sort, $direction);

        // Pagination
        $page = max(1, (int)($params['page'] ?? 1));
        $perPage = max(1, (int)($params['perPage'] ?? self::PAGE_SIZE));
        $qb->setFirstResult(($page - 1) * $perPage)
           ->setMaxResults($perPage);

        /** @var array<int, Categorie> $items */
        $items = $qb->getQuery()->getResult();

        // Total pour pagination
        $totalQuery = clone $qb;
        $total = (int)$totalQuery->select('COUNT(c.id_categorie)')
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
     * Récupère toutes les catégories filtrées (pour export)
     *
     * @param array<string, mixed> $params
     * @return array<int, Categorie>
     */
    public function findAllFiltered(array $params): array
    {
        $qb = $this->createQueryBuilder('c');

        // Recherche
        if (!empty($params['search'])) {
            $searchTerm = $this->sanitizeSearchTerm($params['search']);
            $qb->andWhere('c.nom_categorie LIKE :search OR c.description LIKE :search')
               ->setParameter('search', '%' . $searchTerm . '%');
        }

        /** @var array<int, Categorie> $result */
        $result = $qb->orderBy('c.date_creation', 'DESC')
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
}