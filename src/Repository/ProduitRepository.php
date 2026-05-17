<?php
namespace App\Repository;

use App\Entity\Produit;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Produit>
 */
class ProduitRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Produit::class);
    }

    /**
     * @param int $limit
     * @return Produit[]
     */
    public function findActifs(int $limit = 50): array
    {
        return $this->createQueryBuilder('p')
            ->andWhere('p.estActif = true')
            ->orderBy('p.nom', 'ASC')
            ->setMaxResults($limit)
            ->getQuery()
            ->getResult();
    }

    public function getPrixMoyen(): float
    {
        $result = $this->createQueryBuilder('p')
            ->select('AVG(p.prix)')
            ->andWhere('p.estActif = true')
            ->getQuery()
            ->getSingleScalarResult();
        return round((float) $result, 2);
    }

    public function getValeurTotaleStock(): float
    {
        $result = $this->createQueryBuilder('p')
            ->select('SUM(p.prix * p.stock)')
            ->andWhere('p.stock IS NOT NULL')
            ->andWhere('p.estActif = true')
            ->getQuery()
            ->getSingleScalarResult();
        return round((float) $result, 2);
    }

    public function countRuptureStock(): int
    {
        return (int) $this->createQueryBuilder('p')
            ->select('COUNT(p.id)')
            ->andWhere('p.stock = 0')
            ->andWhere('p.estActif = true')
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function countNumeriques(): int
    {
        return (int) $this->createQueryBuilder('p')
            ->select('COUNT(p.id)')
            ->andWhere('p.stock IS NULL')
            ->andWhere('p.estActif = true')
            ->getQuery()
            ->getSingleScalarResult();
    }

    /**
     * @return Produit[]
     */
    public function findTop5PlusChers(): array
    {
        return $this->createQueryBuilder('p')
            ->andWhere('p.estActif = true')
            ->orderBy('p.prix', 'DESC')
            ->setMaxResults(5)
            ->getQuery()
            ->getResult();
    }

    /**
     * @return array{actif: int, inactif: int}
     */
    public function countByStatut(): array
    {
        $rows = $this->createQueryBuilder('p')
            ->select('p.estActif as statut, COUNT(p.id) as total')
            ->groupBy('p.estActif')
            ->getQuery()
            ->getResult();

        $result = ['actif' => 0, 'inactif' => 0];
        foreach ($rows as $row) {
            $key = $row['statut'] ? 'actif' : 'inactif';
            $result[$key] = (int) $row['total'];
        }
        return $result;
    }

    /**
     * @return array<string, int>
     */
    public function countByMonth(): array
    {
        $conn  = $this->getEntityManager()->getConnection();
        $debut = (new \DateTime('-12 months'))->format('Y-m-d');

        $sql = "SELECT DATE_FORMAT(date_creation, '%Y-%m') AS mois, COUNT(id) AS total
                FROM produit
                WHERE date_creation >= :debut
                GROUP BY mois
                ORDER BY mois ASC";

        $rows = $conn->fetchAllAssociative($sql, ['debut' => $debut]);

        $result = [];
        foreach ($rows as $row) {
            $result[$row['mois']] = (int) $row['total'];
        }
        return $result;
    }
}