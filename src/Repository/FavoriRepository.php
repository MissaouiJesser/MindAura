<?php

namespace App\Repository;

use App\Entity\Evenement;
use App\Entity\Favori;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Favori>
 */
class FavoriRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Favori::class);
    }

    /**
     * @return Favori[]
     */
    public function findByEmail(string $email): array
    {
        return $this->createQueryBuilder('f')
            ->join('f.evenement', 'e')
            ->addSelect('e')
            ->where('f.emailUtilisateur = :email')
            ->setParameter('email', $email)
            ->orderBy('f.dateAjout', 'DESC')
            ->getQuery()
            ->getResult();
    }

    public function isFavori(string $email, Evenement $evenement): bool
    {
        return $this->findOneBy([
            'emailUtilisateur' => $email,
            'evenement'        => $evenement,
        ]) !== null;
    }

    /**
     * @return int[]
     */
    public function getFavoriIds(string $email): array
    {
        $rows = $this->createQueryBuilder('f')
            ->select('IDENTITY(f.evenement) as ev_id')
            ->where('f.emailUtilisateur = :email')
            ->setParameter('email', $email)
            ->getQuery()
            ->getScalarResult();

        return array_column($rows, 'ev_id');
    }
}