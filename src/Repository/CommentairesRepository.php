<?php

namespace App\Repository;

use App\Entity\Commentaires;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Commentaires>
 */
class CommentairesRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Commentaires::class);
    }

    /**
     * @return array<int, Commentaires>
     */
    public function findRacinesByRessource(int $ressourceId): array
    {
        return $this->createQueryBuilder('c')
            ->leftJoin('c.reponses', 'r')->addSelect('r')
            ->where('c.ressource = :ressourceId')
            ->andWhere('c.parent IS NULL')
            ->andWhere('c.status != :statusSupprime')
            ->setParameter('ressourceId', $ressourceId)
            ->setParameter('statusSupprime', 'SUPPRIME')
            ->orderBy('c.datePublication', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, Commentaires>
     */
    public function findSignales(): array
    {
        return $this->createQueryBuilder('c')
            ->where('c.status = :status')
            ->setParameter('status', 'SIGNALE')
            ->orderBy('c.datePublication', 'DESC')
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array<int, array{sentiment: string|null, total: int}>
     */
    public function getStatsSentiment(int $ressourceId): array
    {
        return $this->createQueryBuilder('c')
            ->select('c.sentiment, COUNT(c) AS total')
            ->where('c.ressource = :id')
            ->andWhere('c.sentiment IS NOT NULL')
            ->andWhere('c.status = :status')
            ->setParameter('id', $ressourceId)
            ->setParameter('status', 'ACTIF')
            ->groupBy('c.sentiment')
            ->getQuery()
            ->getResult();
    }

    /**
     * Récupère les thèmes les plus fréquents (pour dashboard admin).
     *
     * @return array<int, array<string, mixed>>  // chaque entrée contient 'themes_raw' (string) et 'freq' (int)
     */
    public function getTopThemes(int $limit = 10): array
    {
        $conn = $this->getEntityManager()->getConnection();
        $sql  = '
            SELECT themes_raw, COUNT(*) AS freq
            FROM (
                SELECT JSON_UNQUOTE(JSON_EXTRACT(themes, CONCAT("$[", idx, "]"))) AS themes_raw
                FROM commentaire
                JOIN (
                    SELECT 0 AS idx UNION SELECT 1 UNION SELECT 2
                    UNION SELECT 3 UNION SELECT 4
                ) indices
                WHERE JSON_EXTRACT(themes, CONCAT("$[", idx, "]")) IS NOT NULL
                  AND status = "ACTIF"
            ) t
            GROUP BY themes_raw
            ORDER BY freq DESC
            LIMIT :limit
        ';

        return $conn->executeQuery($sql, ['limit' => $limit], ['limit' => \PDO::PARAM_INT])
                    ->fetchAllAssociative();
    }

    /**
     * Retourne les totaux globaux des sentiments (positif, négatif, neutre).
     *
     * @return array<string, int>  // clés : 'positif', 'negatif', 'neutre'
     */
    public function getSentimentsGlobaux(): array
    {
        $rows = $this->createQueryBuilder('c')
            ->select('c.sentiment, COUNT(c) AS total')
            ->where('c.sentiment IS NOT NULL')
            ->andWhere('c.status = :status')
            ->setParameter('status', 'ACTIF')
            ->groupBy('c.sentiment')
            ->getQuery()
            ->getResult();

        // Initialisation systématique des trois clés
        $result = ['positif' => 0, 'negatif' => 0, 'neutre' => 0];
        foreach ($rows as $row) {
            $result[$row['sentiment']] = (int) $row['total'];
        }
        return $result;
    }
}