<?php

namespace App\Repository;

use App\Entity\Utilisateurs;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * Repository dédié à l'entité Utilisateurs.
 *
 * @extends ServiceEntityRepository<Utilisateurs>
 */
class UtilisateursRepository extends ServiceEntityRepository
{
    public const SORTABLE_COLUMNS = [
        'nom'         => 'u.nomUtilisateur',
        'prenom'      => 'u.prenomUtilisateur',
        'email'       => 'u.emailUtilisateur',
        'telephone'   => 'u.telephoneUtilisateur',
        'role'        => 'u.roleUtilisateur',
        'inscription' => 'u.dateInscriptionUtilisateur',
        'naissance'   => 'u.dateNaissanceUtilisateur',
        'statut'      => 'u.estActifUtilisateur',
    ];

    public const DEFAULT_SORT      = 'inscription';
    public const DEFAULT_DIRECTION = 'DESC';
    public const PAGE_SIZE         = 10;

    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Utilisateurs::class);
    }

    // ══════════════════════════════════════════════════════════════════
    //  RECHERCHE / FILTRAGE / TRI / PAGINATION
    // ══════════════════════════════════════════════════════════════════

    /**
     * FIX line 44 : array sans type de valeur → array<string, mixed>
     *
     * @param  array<string, mixed> $params
     * @return array{items: Utilisateurs[], total: int, pages: int, page: int, perPage: int}
     */
    public function findFiltered(array $params = []): array
    {
        $search    = trim((string) ($params['search']    ?? ''));
        $role      = trim((string) ($params['role']      ?? ''));
        $statut    = $params['statut']         ?? '';
        $dateDebut = trim((string) ($params['dateDebut'] ?? ''));
        $dateFin   = trim((string) ($params['dateFin']   ?? ''));
        $sort      = (string) ($params['sort']           ?? self::DEFAULT_SORT);
        $direction = strtoupper((string) ($params['direction'] ?? self::DEFAULT_DIRECTION));
        $page      = max(1, (int) ($params['page']    ?? 1));
        $perPage   = max(1, min(100, (int) ($params['perPage'] ?? self::PAGE_SIZE)));

        $sortCol   = self::SORTABLE_COLUMNS[$sort]      ?? self::SORTABLE_COLUMNS[self::DEFAULT_SORT];
        $direction = in_array($direction, ['ASC', 'DESC']) ? $direction : self::DEFAULT_DIRECTION;

        $qb = $this->createQueryBuilder('u');

        if ('' !== $search) {
            $q = '%' . strtolower($search) . '%';
            $qb->andWhere(
                $qb->expr()->orX(
                    $qb->expr()->like('LOWER(u.prenomUtilisateur)',    ':q'),
                    $qb->expr()->like('LOWER(u.nomUtilisateur)',       ':q'),
                    $qb->expr()->like('LOWER(u.emailUtilisateur)',     ':q'),
                    $qb->expr()->like('LOWER(u.telephoneUtilisateur)', ':q')
                )
            )->setParameter('q', $q);
        }

        if ('' !== $role) {
            $qb->andWhere('u.roleUtilisateur = :role')->setParameter('role', $role);
        }

        if ('' !== $statut) {
            $qb->andWhere('u.estActifUtilisateur = :statut')->setParameter('statut', (bool) $statut);
        }

        if ('' !== $dateDebut) {
            try {
                $qb->andWhere('u.dateInscriptionUtilisateur >= :dateDebut')
                   ->setParameter('dateDebut', new \DateTime($dateDebut));
            } catch (\Exception) {}
        }
        if ('' !== $dateFin) {
            try {
                $qb->andWhere('u.dateInscriptionUtilisateur <= :dateFin')
                   ->setParameter('dateFin', new \DateTime($dateFin));
            } catch (\Exception) {}
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(u.idUtilisateur)')->getQuery()->getSingleScalarResult();

        $qb->orderBy($sortCol, $direction)
           ->setFirstResult(($page - 1) * $perPage)
           ->setMaxResults($perPage);

        $items = $qb->getQuery()->getResult();
        $pages = (int) ceil($total / $perPage);

        return [
            'items'   => $items,
            'total'   => $total,
            'pages'   => max(1, $pages),
            'page'    => $page,
            'perPage' => $perPage,
        ];
    }

    // ══════════════════════════════════════════════════════════════════
    //  STATISTIQUES DASHBOARD
    // ══════════════════════════════════════════════════════════════════

    /**
     * @return array<int, array{day: string, total: int}>
     */
    public function countByDayLastWeek(): array
    {
        $days = [];
        for ($i = 6; $i >= 0; $i--) {
            $d = (new \DateTime())->modify("-{$i} days")->format('Y-m-d');
            $days[$d] = 0;
        }

        $conn = $this->getEntityManager()->getConnection();
        $sql  = "
            SELECT DATE(date_inscription_utilisateur) AS day, COUNT(*) AS total
            FROM utilisateur
            WHERE date_inscription_utilisateur >= :debut
            GROUP BY DATE(date_inscription_utilisateur)
            ORDER BY day ASC
        ";

        $debut  = (new \DateTime('-6 days midnight'))->format('Y-m-d');
        $result = $conn->fetchAllAssociative($sql, ['debut' => $debut]);

        foreach ($result as $row) {
            $days[$row['day']] = (int) $row['total'];
        }

        $out = [];
        foreach ($days as $day => $total) {
            $out[] = ['day' => $day, 'total' => $total];
        }

        return $out;
    }

    // ══════════════════════════════════════════════════════════════════
    //  REQUÊTES MÉTIER
    // ══════════════════════════════════════════════════════════════════

    /** @return Utilisateurs[] */
    public function search(string $query = '', string $role = ''): array
    {
        return $this->findFiltered(['search' => $query, 'role' => $role, 'perPage' => 1000])['items'];
    }

    /** @return Utilisateurs[] */
    public function findByRole(string $role): array
    {
        return $this->applyDefaultOrder(
            $this->createQueryBuilder('u')
                 ->andWhere('u.roleUtilisateur = :role')
                 ->setParameter('role', $role)
        )->getQuery()->getResult();
    }

    /** @return Utilisateurs[] */
    public function findActifs(): array
    {
        return $this->applyDefaultOrder(
            $this->createQueryBuilder('u')
                 ->andWhere('u.estActifUtilisateur = :actif')
                 ->setParameter('actif', true)
        )->getQuery()->getResult();
    }

    /** @return array<int, array{role: string, total: int}> */
    public function countByRole(): array
    {
        return $this->createQueryBuilder('u')
                    ->select('u.roleUtilisateur AS role, COUNT(u.idUtilisateur) AS total')
                    ->groupBy('u.roleUtilisateur')
                    ->orderBy('total', 'DESC')
                    ->getQuery()
                    ->getArrayResult();
    }

    private function applyDefaultOrder(QueryBuilder $qb): QueryBuilder
    {
        return $qb->orderBy('u.dateInscriptionUtilisateur', 'DESC');
    }
}