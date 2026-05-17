<?php

namespace App\Service;

use App\Entity\Categorie;
use App\Entity\Reclamation;
use App\Entity\Reponse;
use Doctrine\DBAL\Connection;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Insertions via DBAL avec PK explicite (MAX(id)+1).
 *
 * Si la colonne PK n'a pas AUTO_INCREMENT, MySQL/MariaDB met 0 par défaut :
 * première ligne peut passer, la suivante provoque "Duplicate entry '0' for key 'PRIMARY'".
 */
final class IdentitySafeInsertService
{
    public function insertReclamation(EntityManagerInterface $em, Reclamation $reclamation): int
    {
        $meta = $em->getClassMetadata(Reclamation::class);
        $catCol = $meta->getAssociationMapping('categorie')['joinColumns'][0]['name'];

        $data = [
            $catCol => $reclamation->getCategorie()?->getId_categorie(),
            $meta->getColumnName('sujet_reclamation') => $reclamation->getSujet_reclamation(),
            $meta->getColumnName('description_reclamation') => $reclamation->getDescription_reclamation(),
            $meta->getColumnName('dateCreation_reclamation') => $reclamation->getDateCreation_reclamation()->format('Y-m-d H:i:s'),
            $meta->getColumnName('statut_reclamation') => $reclamation->getStatutCode(),
            $meta->getColumnName('id_utilisateur') => $reclamation->getId_utilisateur(),
            $meta->getColumnName('rate_Reclamation') => $reclamation->getRate_Reclamation(),
            $meta->getColumnName('rate_sum') => $reclamation->getRate_sum(),
            $meta->getColumnName('rate_count') => $reclamation->getRate_count(),
        ];

        return $this->insertWithAllocatedPrimaryKey($em, Reclamation::class, $data);
    }

    public function insertReponse(EntityManagerInterface $em, Reponse $reponse): int
    {
        $meta = $em->getClassMetadata(Reponse::class);
        $recCol = $meta->getAssociationMapping('reclamation')['joinColumns'][0]['name'];

        $data = [
            $recCol => $reponse->getReclamation()?->getId_reclamation(),
            $meta->getColumnName('contenu_reponse') => $reponse->getContenu_reponse(),
            $meta->getColumnName('date_reponse') => $reponse->getDate_reponse()->format('Y-m-d H:i:s'),
            $meta->getColumnName('id_utilisateur') => $reponse->getId_utilisateur(),
            $meta->getColumnName('nom_utilisateur') => $reponse->getNom_utilisateur() ?: null,
            $meta->getColumnName('rate_reponse') => $reponse->getRate_reponse(),
            $meta->getColumnName('rate_sum') => $reponse->getRate_sum(),
            $meta->getColumnName('rate_count') => $reponse->getRate_count(),
        ];

        return $this->insertWithAllocatedPrimaryKey($em, Reponse::class, $data);
    }

    public function insertCategorie(EntityManagerInterface $em, Categorie $categorie): int
    {
        $meta = $em->getClassMetadata(Categorie::class);

        $data = [
            $meta->getColumnName('nom_categorie') => $categorie->getNom_categorie(),
            $meta->getColumnName('description') => $categorie->getDescription(),
            $meta->getColumnName('date_creation') => $categorie->getDate_creation()->format('Y-m-d H:i:s'),
        ];

        return $this->insertWithAllocatedPrimaryKey($em, Categorie::class, $data);
    }

    /**
     * @param array<string, mixed> $data Colonnes (hors PK)
     */
    private function insertWithAllocatedPrimaryKey(EntityManagerInterface $em, string $entityClass, array $data): int
    {
        $meta = $em->getClassMetadata($entityClass);
        $conn = $em->getConnection();
        $table = $meta->getTableName();
        $pkField = $meta->getSingleIdentifierFieldName();
        $pkColumn = $meta->getColumnName($pkField);

        $nextId = $this->allocateNextPrimaryKey($conn, $table, $pkColumn);
        $row = [$pkColumn => $nextId] + $data;

        $conn->insert($table, $row);

        return $nextId;
    }

    private function allocateNextPrimaryKey(Connection $conn, string $table, string $pkColumn): int
    {
        $qTable = $conn->quoteIdentifier($table);
        $qCol = $conn->quoteIdentifier($pkColumn);
        $sql = "SELECT COALESCE(MAX({$qCol}), 0) + 1 FROM {$qTable}";

        return (int) $conn->fetchOne($sql);
    }
}
