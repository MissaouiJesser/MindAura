<?php

namespace App\Repository;

use App\Entity\Message;
use App\Entity\Utilisateurs;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\Persistence\ManagerRegistry;

/**
 * Repository pour les messages de chat.
 *
 * @extends ServiceEntityRepository<Message>
 */
class MessageRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Message::class);
    }

    /**
     * Retourne tous les messages d'une conversation entre deux utilisateurs,
     * triés par date croissante.
     *
     * @return Message[]
     */
    public function findConversation(Utilisateurs $userA, Utilisateurs $userB): array
    {
        return $this->createQueryBuilder('m')
            ->where(
                '(m.sender = :a AND m.receiver = :b) OR (m.sender = :b AND m.receiver = :a)'
            )
            ->setParameter('a', $userA)
            ->setParameter('b', $userB)
            ->orderBy('m.createdAt', 'ASC')
            ->getQuery()
            ->getResult();
    }

    /**
     * Retourne la liste des utilisateurs avec qui $user a échangé des messages,
     * avec le dernier message et le nombre de messages non lus.
     *
     * @return array<int, array{user: Utilisateurs, lastMessage: Message, unread: int}>
     */
    public function findConversationList(Utilisateurs $user): array
    {
        // Récupérer tous les messages impliquant cet utilisateur
        $messages = $this->createQueryBuilder('m')
            ->where('m.sender = :u OR m.receiver = :u')
            ->setParameter('u', $user)
            ->orderBy('m.createdAt', 'DESC')
            ->getQuery()
            ->getResult();

        // Grouper par interlocuteur (garder uniquement le dernier message par conversation)
        $conversations = [];
        foreach ($messages as $msg) {
            /** @var Message $msg */
            $other = $msg->getSender()->getIdUtilisateur() === $user->getIdUtilisateur()
                ? $msg->getReceiver()
                : $msg->getSender();

            $otherId = $other->getIdUtilisateur();

            if (!isset($conversations[$otherId])) {
                $conversations[$otherId] = [
                    'user'        => $other,
                    'lastMessage' => $msg,
                    'unread'      => 0,
                ];
            }

            // Compter les non-lus reçus par $user
            if (
                $msg->getReceiver()->getIdUtilisateur() === $user->getIdUtilisateur()
                && !$msg->isRead()
            ) {
                $conversations[$otherId]['unread']++;
            }
        }

        return array_values($conversations);
    }

    /**
     * Marque tous les messages envoyés par $sender à $receiver comme lus.
     */
    public function markAsRead(Utilisateurs $sender, Utilisateurs $receiver): void
    {
        $this->createQueryBuilder('m')
            ->update()
            ->set('m.isRead', ':true')
            ->where('m.sender = :sender AND m.receiver = :receiver AND m.isRead = :false')
            ->setParameter('true',     true)
            ->setParameter('false',    false)
            ->setParameter('sender',   $sender)
            ->setParameter('receiver', $receiver)
            ->getQuery()
            ->execute();
    }

    /**
     * Nombre total de messages non lus pour un utilisateur.
     */
    public function countUnread(Utilisateurs $user): int
    {
        return (int) $this->createQueryBuilder('m')
            ->select('COUNT(m.id)')
            ->where('m.receiver = :u AND m.isRead = :false')
            ->setParameter('u',     $user)
            ->setParameter('false', false)
            ->getQuery()
            ->getSingleScalarResult();
    }
}
