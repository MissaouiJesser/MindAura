<?php

namespace App\Repository;

use App\Entity\ActivityLog;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * Repository pour ActivityLog.
 *
 * Méthodes principales :
 *   findRecent(int $limit)         → N derniers logs (dashboard)
 *   findByUser(int $userId, ...)   → logs d'un utilisateur précis
 *   paginateAll(array $params)     → liste paginée pour la page dédiée
 *
 * @extends ServiceEntityRepository<ActivityLog>
 */
class ActivityLogRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, ActivityLog::class);
    }

    // ── N derniers logs (toutes actions confondues) ──────────────────────────

    /**
     * @return ActivityLog[]
     */
    public function findRecent(int $limit = 20): array
    {
        return $this->createQueryBuilder('a')
            ->orderBy('a.createdAt', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    // ── Logs d'un utilisateur précis ─────────────────────────────────────────

    /**
     * @return ActivityLog[]
     */
    public function findByUser(int $userId, int $limit = 50): array
    {
        return $this->createQueryBuilder('a')
            ->where('a.userId = :uid')
            ->setParameter('uid', $userId)
            ->orderBy('a.createdAt', 'DESC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    // ── Liste paginée (page dédiée admin) ────────────────────────────────────

    /**
     * @param array<string, mixed> $params
     * @return array{ items: ActivityLog[], total: int, pages: int, page: int, perPage: int }
     */
    public function paginateAll(array $params = []): array
    {
        $search  = $params['search']  ?? '';
        $action  = $params['action']  ?? '';
        $context = $params['context'] ?? '';
        $page    = max(1, (int)($params['page']    ?? 1));
        $perPage = max(5, (int)($params['perPage'] ?? 25));

        $qb = $this->createQueryBuilder('a');

        if ($search) {
            $qb->andWhere('a.userName LIKE :s OR a.userEmail LIKE :s OR a.label LIKE :s')
               ->setParameter('s', '%' . $search . '%');
        }

        if ($action) {
            $qb->andWhere('a.action = :action')->setParameter('action', $action);
        }

        if ($context) {
            $qb->andWhere('a.context = :ctx')->setParameter('ctx', $context);
        }

        $total = (clone $qb)
            ->select('COUNT(a.id)')
            ->getQuery()
            ->getSingleScalarResult();

        $items = $qb
            ->orderBy('a.createdAt', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        $totalInt = (int) $total;

        return [
            'items'   => $items,
            'total'   => $totalInt,
            'pages'   => (int) ceil($totalInt / $perPage),
            'page'    => $page,
            'perPage' => $perPage,
        ];
    }

    // ── Purge des anciens logs (optionnel, cron) ─────────────────────────────

    public function deleteOlderThan(\DateTimeInterface $threshold): int
    {
        return $this->createQueryBuilder('a')
            ->delete()
            ->where('a.createdAt < :threshold')
            ->setParameter('threshold', $threshold)
            ->getQuery()
            ->execute();
    }
}   