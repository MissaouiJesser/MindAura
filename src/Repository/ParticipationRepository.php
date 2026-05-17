<?php
namespace App\Repository;

use App\Entity\Participation;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\Persistence\ManagerRegistry;

/**
 * @extends ServiceEntityRepository<Participation>
 */
class ParticipationRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Participation::class);
    }

    /**
     * Recherche paginée avec filtres et eager loading de l'événement associé.
     *
     * @param array{query?: string, evenement?: int, statut?: string} $criteria
     * @param int $limit  Nombre max de résultats (défaut 50)
     * @param int $offset Décalage pour la pagination
     * @return list<Participation>
     */
    public function search(array $criteria = [], int $limit = 50, int $offset = 0): array
    {
        $qb = $this->createQueryBuilder('p')
            ->leftJoin('p.evenement', 'e')
            ->addSelect('e')
            ->leftJoin('e.typeEvenement', 't')
            ->addSelect('t')
            ->orderBy('p.dateInscription', 'DESC')
            ->setMaxResults($limit)
            ->setFirstResult($offset);

        if (!empty($criteria['query'])) {
            // ✅ email est un Embedded : p.email.value au lieu de p.email
            $qb->andWhere('p.nom LIKE :q OR p.prenom LIKE :q OR p.email.value LIKE :q')
               ->setParameter('q', '%' . $criteria['query'] . '%');
        }

        if (!empty($criteria['evenement'])) {
            $qb->andWhere('e.id = :eid')
               ->setParameter('eid', $criteria['evenement']);
        }

        if (!empty($criteria['statut'])) {
            $qb->andWhere('p.statut = :statut')
               ->setParameter('statut', $criteria['statut']);
        }

        return $qb->getQuery()->getResult();
    }

    /**
     * Compte le nombre de résultats pour la pagination de search().
     *
     * @param array{query?: string, evenement?: int, statut?: string} $criteria
     */
    public function countSearch(array $criteria = []): int
    {
        $qb = $this->createQueryBuilder('p')
            ->select('COUNT(p.id)')
            ->leftJoin('p.evenement', 'e');

        if (!empty($criteria['query'])) {
            // ✅ email est un Embedded : p.email.value au lieu de p.email
            $qb->andWhere('p.nom LIKE :q OR p.prenom LIKE :q OR p.email.value LIKE :q')
               ->setParameter('q', '%' . $criteria['query'] . '%');
        }

        if (!empty($criteria['evenement'])) {
            $qb->andWhere('e.id = :eid')
               ->setParameter('eid', $criteria['evenement']);
        }

        if (!empty($criteria['statut'])) {
            $qb->andWhere('p.statut = :statut')
               ->setParameter('statut', $criteria['statut']);
        }

        return (int) $qb->getQuery()->getSingleScalarResult();
    }

    public function countByEvenement(int $evenementId): int
    {
        return (int) $this->createQueryBuilder('p')
            ->select('COUNT(p.id)')
            ->where('p.evenement = :eid AND p.statut = :s')
            ->setParameter('eid', $evenementId)
            ->setParameter('s', 'confirme')
            ->getQuery()
            ->getSingleScalarResult();
    }

    public function findFirstEnAttente(int $evenementId): ?Participation
    {
        return $this->createQueryBuilder('p')
            ->where('p.evenement = :eid AND p.statut = :s')
            ->setParameter('eid', $evenementId)
            ->setParameter('s', 'en_attente')
            ->orderBy('p.positionAttente', 'ASC')
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();
    }

    public function reorderListeAttente(int $evenementId, EntityManagerInterface $em): void
    {
        $attente = $this->createQueryBuilder('p')
            ->where('p.evenement = :eid AND p.statut = :s')
            ->setParameter('eid', $evenementId)
            ->setParameter('s', 'en_attente')
            ->orderBy('p.positionAttente', 'ASC')
            ->getQuery()
            ->getResult();

        $pos = 1;
        foreach ($attente as $p) {
            $p->setPositionAttente($pos++);
        }
        $em->flush();
    }

    public function isAlreadyRegistered(int $evenementId, string $email): bool
    {
        $count = (int) $this->createQueryBuilder('p')
            ->select('COUNT(p.id)')
            // ✅ Correction : p.email.value car Email est un Embeddable
            ->where('p.evenement = :eid AND p.email.value = :email AND p.statut != :cancelled')
            ->setParameter('eid', $evenementId)
            ->setParameter('email', $email)
            ->setParameter('cancelled', 'annule')
            ->getQuery()
            ->getSingleScalarResult();

        return $count > 0;
    }

    /**
     * @return array{total: int, confirme: int, annule: int, en_attente: int}
     */
    public function getStats(): array
    {
        $rows = $this->createQueryBuilder('p')
            ->select('p.statut, COUNT(p.id) as nb')
            ->groupBy('p.statut')
            ->getQuery()
            ->getResult();

        $stats = ['total' => 0, 'confirme' => 0, 'annule' => 0, 'en_attente' => 0];
        foreach ($rows as $row) {
            $statut = $row['statut'];
            if (isset($stats[$statut])) {
                $stats[$statut] = (int) $row['nb'];
            }
            $stats['total'] += (int) $row['nb'];
        }
        return $stats;
    }
}