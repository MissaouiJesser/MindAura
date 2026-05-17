<?php

namespace App\Repository;

use App\Entity\ReponseClient;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\QueryBuilder;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<ReponseClient>
 */
class ReponseClientRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, ReponseClient::class);
    }

    /**
     * @param array<string, mixed> $filters
     */
    private function createAdminListQueryBuilder(array $filters): QueryBuilder
    {
        $q      = trim((string) ($filters['q']      ?? ''));
        $testId = (int) ($filters['testId'] ?? 0);

        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.testPsychologique', 't')
            ->leftJoin('r.questionReponse', 'qst')
            ->addSelect('t', 'qst');

        if ($testId > 0) {
            $qb->andWhere('t.id_test = :testId')->setParameter('testId', $testId);
        }
        if ($q !== '') {
            $orX = $qb->expr()->orX(
                'LOWER(t.titre_test) LIKE :q',
                'LOWER(COALESCE(qst.texte_question, \'\')) LIKE :q',
                'LOWER(COALESCE(r.option_choisie, \'\')) LIKE :q',
                'LOWER(COALESCE(r.reponse_texte_libre, \'\')) LIKE :q'
            );
            $qb->setParameter('q', '%' . mb_strtolower($q) . '%');
            if (ctype_digit($q)) {
                $orX->add('r.id_utilisateur = :uidExact');
                $qb->setParameter('uidExact', (int) $q);
            }
            $qb->andWhere($orX);
        }

        return $qb;
    }

    /**
     * Liste admin : toutes les réponses utilisateurs (filtres + pagination).
     *
     * @param array<string, mixed> $filters
     * @return array{items: ReponseClient[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchForAdmin(array $filters): array
    {
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 5)));

        $qb      = $this->createAdminListQueryBuilder($filters);
        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(r.id_reponse_client)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('r.date_reponse', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    /**
     * Export PDF / Excel : mêmes filtres que la liste, sans pagination.
     * Limité à 10 000 lignes pour éviter les problèmes mémoire.
     *
     * @param array<string, mixed> $filters
     * @return ReponseClient[]
     */
    public function findAllForAdminExport(array $filters): array
    {
        return $this->createAdminListQueryBuilder($filters)
            ->orderBy('r.date_reponse', 'DESC')
            ->setMaxResults(10000)
            ->getQuery()
            ->getResult();
    }

    /**
     * @param array<string, mixed> $filters
     */
    private function createUserListQueryBuilder(int $userId, array $filters): QueryBuilder
    {
        $q      = trim((string) ($filters['q']      ?? ''));
        $testId = (int) ($filters['testId'] ?? 0);

        $qb = $this->createQueryBuilder('r')
            ->leftJoin('r.testPsychologique', 't')
            ->leftJoin('r.questionReponse', 'qst')
            ->addSelect('t', 'qst')
            ->andWhere('r.id_utilisateur = :uid')
            ->setParameter('uid', $userId);

        if ($testId > 0) {
            $qb->andWhere('t.id_test = :testId')->setParameter('testId', $testId);
        }
        if ($q !== '') {
            $qb->andWhere(
                'LOWER(t.titre_test) LIKE :uq OR LOWER(COALESCE(qst.texte_question, \'\')) LIKE :uq OR '
                . 'LOWER(COALESCE(r.option_choisie, \'\')) LIKE :uq OR LOWER(COALESCE(r.reponse_texte_libre, \'\')) LIKE :uq'
            )->setParameter('uq', '%' . mb_strtolower($q) . '%');
        }

        return $qb;
    }

    /**
     * @param array<string, mixed> $filters
     * @return array{items: ReponseClient[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchUserResponses(int $userId, array $filters): array
    {
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 10)));

        $qb      = $this->createUserListQueryBuilder($userId, $filters);
        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(r.id_reponse_client)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('r.date_reponse', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }

    /**
     * Export PDF / Excel — réponses du seul utilisateur, mêmes filtres que la liste.
     * Limité à 10 000 lignes pour éviter les problèmes mémoire.
     *
     * @param array<string, mixed> $filters
     * @return ReponseClient[]
     */
    public function findAllForUserExport(int $userId, array $filters): array
    {
        return $this->createUserListQueryBuilder($userId, $filters)
            ->orderBy('r.date_reponse', 'DESC')
            ->setMaxResults(10000)
            ->getQuery()
            ->getResult();
    }

    public function countAllResponses(): int
    {
        return (int) $this->createQueryBuilder('r')
            ->select('COUNT(r.id_reponse_client)')
            ->getQuery()
            ->getSingleScalarResult();
    }
}