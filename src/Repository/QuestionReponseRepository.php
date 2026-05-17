<?php

namespace App\Repository;

use App\Entity\QuestionReponse;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<QuestionReponse>
 */
class QuestionReponseRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, QuestionReponse::class);
    }

    /**
     * Recherche / filtres / pagination (liste globale admin).
     *
     * @param array<string, mixed> $filters
     * @return array{items: QuestionReponse[], total: int, page: int, perPage: int, pages: int}
     */
    public function searchForAdmin(array $filters): array
    {
        $q       = trim((string) ($filters['q']       ?? ''));
        $testId  = (int) ($filters['testId']  ?? 0);
        $type    = trim((string) ($filters['type']    ?? ''));
        $oblig   = trim((string) ($filters['oblig']   ?? ''));
        $page    = max(1, (int) ($filters['page']    ?? 1));
        $perPage = max(1, min(100, (int) ($filters['perPage'] ?? 5)));

        $qb = $this->createQueryBuilder('qr')
            ->leftJoin('qr.testPsychologique', 't')
            ->addSelect('t');

        if ($q !== '') {
            $qb->andWhere('LOWER(qr.texte_question) LIKE :q OR LOWER(COALESCE(t.titre_test, \'\')) LIKE :q')
               ->setParameter('q', '%' . mb_strtolower($q) . '%');
        }
        if ($testId > 0) {
            $qb->andWhere('t.id_test = :testId')->setParameter('testId', $testId);
        }
        if ($type !== '') {
            $qb->andWhere('qr.type_question = :typ')->setParameter('typ', $type);
        }
        if ($oblig === 'oui') {
            $qb->andWhere('qr.est_obligatoire = true');
        } elseif ($oblig === 'non') {
            $qb->andWhere('qr.est_obligatoire = false OR qr.est_obligatoire IS NULL');
        }

        $countQb = clone $qb;
        $total   = (int) $countQb->select('COUNT(qr.id_question_reponse)')->getQuery()->getSingleScalarResult();
        $pages   = max(1, (int) ceil($total / $perPage));
        $page    = min($page, $pages);

        $items = $qb
            ->orderBy('t.titre_test', 'ASC')
            ->addOrderBy('qr.ordre_question', 'ASC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return compact('items', 'total', 'page', 'perPage', 'pages');
    }
}