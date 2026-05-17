<?php

namespace App\Controller;

use App\Entity\Message;
use App\Entity\Utilisateurs;
use App\Repository\MessageRepository;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/chat')]
#[IsGranted('ROLE_USER')]
class ChatController extends AbstractController
{
    public function __construct(private string $chatUploadsDir) {}
    /** GET /chat — page Twig principale */
    #[Route('', name: 'app_chat', methods: ['GET'])]
    public function page(): Response
    {
        return $this->render('site/chat.html.twig');
    }

    /** GET /chat/conversations */
    #[Route('/conversations', name: 'app_chat_conversations', methods: ['GET'])]
    public function conversations(MessageRepository $repo): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me   = $this->getUser();
        $list = $repo->findConversationList($me);
        $data = [];
        foreach ($list as $conv) {
            $other = $conv['user'];
            $last  = $conv['lastMessage'];

            // Construire le snippet du dernier message sans tronquer les JSON (fichiers/vocaux)
            $rawContent = $last->getContent();
            if (str_starts_with(trim($rawContent), '{')) {
                // Message fichier/image/vocal : passer le JSON brut (le JS côté client le formate)
                $snippet = $rawContent;
            } else {
                $snippet = mb_strimwidth($rawContent, 0, 65, '…');
            }

            $data[] = [
                'userId'      => $other->getIdUtilisateur(),
                'name'        => $other->getPrenomUtilisateur() . ' ' . $other->getNomUtilisateur(),
                'avatar'      => $other->getPhotoProfilUtilisateur(),
                'role'        => $other->getRoleUtilisateur(),
                'lastMessage' => $snippet,
                'lastTime'    => $last->getCreatedAt()->format('H:i'),
                'lastDate'    => $last->getCreatedAt()->format('d/m/Y'),
                'unread'      => $conv['unread'],
                'isMine'      => $last->getSender()->getIdUtilisateur() === $me->getIdUtilisateur(),
            ];
        }
        return $this->json($data);
    }

    /** GET /chat/messages/{userId} */
    #[Route('/messages/{userId}', name: 'app_chat_messages', methods: ['GET'], requirements: ['userId' => '\d+'])]
    public function messages(int $userId, MessageRepository $msgRepo, UtilisateursRepository $userRepo): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me    = $this->getUser();
        $other = $userRepo->find($userId);
        if (!$other) {
            return $this->json(['error' => 'Utilisateur introuvable.'], 404);
        }
        $msgRepo->markAsRead($other, $me);
        $messages = $msgRepo->findConversation($me, $other);
        $data = [];
        foreach ($messages as $msg) {
            $sender = $msg->getSender();
            $isMine = $sender->getIdUtilisateur() === $me->getIdUtilisateur();
            $raw    = $msg->getContent();

            // Détecter si le contenu est un message fichier (JSON)
            $decoded = null;
            if (str_starts_with(trim($raw), '{')) {
                $decoded = json_decode($raw, true);
            }

            $entry = [
                'id'         => $msg->getId(),
                'isMine'     => $isMine,
                'senderName' => $sender->getPrenomUtilisateur() . ' ' . $sender->getNomUtilisateur(),
                'avatar'     => $sender->getPhotoProfilUtilisateur(),
                'time'       => $msg->getCreatedAt()->format('H:i'),
                'date'       => $msg->getCreatedAt()->format('d/m/Y'),
                'isRead'     => $msg->isRead(),
                'type'       => 'text',
                'content'    => $raw,
            ];

            if ($decoded && isset($decoded['type'])) {
                $entry['type']          = $decoded['type'];
                $entry['fileUrl']       = $decoded['fileUrl']       ?? '';
                $entry['fileName']      = $decoded['fileName']      ?? '';
                $entry['fileSize']      = $decoded['fileSize']      ?? '';
                $entry['audioDuration'] = $decoded['audioDuration'] ?? '';
                $entry['content']       = $decoded['caption']       ?? '';
            }

            // Ajouter les réactions
            $rawReactions = $msg->getReactions() ?? [];
            $entry['reactions'] = array_values($rawReactions);

            $data[] = $entry;
        }
        return $this->json([
            'messages' => $data,
            'other'    => [
                'userId' => $other->getIdUtilisateur(),
                'name'   => $other->getPrenomUtilisateur() . ' ' . $other->getNomUtilisateur(),
                'avatar' => $other->getPhotoProfilUtilisateur(),
                'role'   => $other->getRoleUtilisateur(),
            ],
        ]);
    }

    /** POST /chat/send */
    #[Route('/send', name: 'app_chat_send', methods: ['POST'])]
    public function send(Request $request, UtilisateursRepository $userRepo, EntityManagerInterface $em): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me         = $this->getUser();
        $payload    = json_decode($request->getContent(), true) ?? [];
        $receiverId = (int) ($payload['receiverId'] ?? 0);
        $content    = trim((string) ($payload['content'] ?? ''));
        if (!$receiverId || $content === '') {
            return $this->json(['error' => 'Données invalides.'], 400);
        }
        if (mb_strlen($content) > 1000) {
            return $this->json(['error' => 'Message trop long.'], 400);
        }
        if ($receiverId === $me->getIdUtilisateur()) {
            return $this->json(['error' => 'Vous ne pouvez pas vous écrire.'], 400);
        }
        $receiver = $userRepo->find($receiverId);
        if (!$receiver) {
            return $this->json(['error' => 'Destinataire introuvable.'], 404);
        }
        $msg = new Message($me, $receiver, $content);
        $em->persist($msg);
        $em->flush();
        return $this->json([
            'id'      => $msg->getId(),
            'content' => $msg->getContent(),
            'time'    => $msg->getCreatedAt()->format('H:i'),
            'date'    => $msg->getCreatedAt()->format('d/m/Y'),
            'isMine'  => true,
        ], 201);
    }

    /** POST /chat/send-file — images, fichiers, messages vocaux */
    #[Route('/send-file', name: 'app_chat_send_file', methods: ['POST'])]
    public function sendFile(
        Request                $request,
        UtilisateursRepository $userRepo,
        EntityManagerInterface $em
    ): JsonResponse {
        $chatUploadsDir = $this->chatUploadsDir;
        /** @var Utilisateurs $me */
        $me         = $this->getUser();
        $receiverId = (int) $request->request->get('receiverId', 0);
        $isAudio    = (bool) $request->request->get('isAudio', false);
        $caption    = trim((string) $request->request->get('caption', ''));

        if (!$receiverId) {
            return $this->json(['error' => 'Destinataire manquant.'], 400);
        }
        if ($receiverId === $me->getIdUtilisateur()) {
            return $this->json(['error' => 'Vous ne pouvez pas vous envoyer un fichier.'], 400);
        }
        $receiver = $userRepo->find($receiverId);
        if (!$receiver) {
            return $this->json(['error' => 'Destinataire introuvable.'], 404);
        }

        /** @var \Symfony\Component\HttpFoundation\File\UploadedFile|null $uploaded */
        $uploaded = $request->files->get('file');
        if (!$uploaded) {
            return $this->json(['error' => 'Aucun fichier reçu.'], 400);
        }

        // Lire la taille AVANT move() (après move getSize() retourne 0)
        $originalSize = $uploaded->getSize();

        // Validation taille (20 Mo max)
        if ($originalSize > 20 * 1024 * 1024) {
            return $this->json(['error' => 'Fichier trop volumineux (max 20 Mo).'], 400);
        }

        // Déterminer le type MIME réel
        $mimeReal = $uploaded->getMimeType();
        $mime     = ($mimeReal !== null && $mimeReal !== '') ? $mimeReal : $uploaded->getClientMimeType();
        $mimeBase = explode(';', $mime)[0]; // strip "audio/webm;codecs=opus" → "audio/webm"
        $isImage  = str_starts_with($mimeBase, 'image/');
        $isAudioM = $isAudio || str_starts_with($mimeBase, 'audio/');
        $type     = $isImage ? 'image' : ($isAudioM ? 'audio' : 'file');

        // Whitelist MIME (base uniquement, sans les paramètres codecs)
        $allowed = [
            'image/jpeg','image/png','image/gif','image/webp','image/svg+xml',
            'audio/webm','audio/ogg','audio/mpeg','audio/wav','audio/aac','audio/mp4',
            'video/webm', // certains navigateurs encodent l'audio en video/webm
            'application/pdf',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
            'application/vnd.ms-powerpoint',
            'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            'text/plain',
            'application/zip',
            'application/x-rar-compressed','application/x-zip-compressed',
            'application/octet-stream', // fallback générique pour les blobs audio
        ];
        // Pour les fichiers audio forcés (isAudio=1), on accepte sans restriction MIME
        if (!$isAudio && !in_array($mimeBase, $allowed, true)) {
            return $this->json(['error' => 'Type de fichier non autorisé ('.$mimeBase.').'], 400);
        }

        // Sauvegarde
        if (!is_dir($chatUploadsDir)) {
            mkdir($chatUploadsDir, 0775, true);
        }
        // Déterminer l'extension depuis le MIME pour les vocaux (plus fiable que le nom client)
        $mimeToExt = [
            'audio/webm' => 'webm', 'audio/ogg'  => 'ogg',
            'audio/mpeg' => 'mp3',  'audio/mp3'  => 'mp3',
            'audio/wav'  => 'wav',  'audio/aac'  => 'aac',
            'audio/mp4'  => 'mp4',  'video/webm' => 'webm',
        ];
        if ($isAudioM && isset($mimeToExt[$mimeBase])) {
            // Toujours déduire depuis le MIME pour les fichiers audio (fiable cross-browser)
            $ext = $mimeToExt[$mimeBase];
        } elseif ($isAudioM) {
            // MIME audio inconnu → fallback webm
            $ext = 'webm';
        } else {
            // Image ou fichier générique : utiliser l'extension du nom client
            $ext = $uploaded->getClientOriginalExtension();
        }
        $safeName = uniqid('chat_', true) . '.' . $ext;
        $uploaded->move($chatUploadsDir, $safeName);

        $fileUrl  = '/uploads/chat/' . $safeName;
        $origName = $uploaded->getClientOriginalName() ?: $safeName;
        $fileSize = $this->fmtSize($originalSize); // utiliser la taille lue avant move()

        // Construire le contenu JSON du message
        $msgContent = json_encode([
            'type'     => $type,
            'fileUrl'  => $fileUrl,
            'fileName' => $origName,
            'fileSize' => $fileSize,
            'caption'  => $caption,
        ]);
        if ($msgContent === false) {
            return $this->json(['error' => 'Erreur d\'encodage du message.'], 500);
        }

        $msg = new Message($me, $receiver, $msgContent);
        $em->persist($msg);
        $em->flush();

        return $this->json([
            'id'           => $msg->getId(),
            'type'         => $type,
            'fileUrl'      => $fileUrl,
            'fileName'     => $origName,
            'fileSize'     => $fileSize,
            'audioDuration'=> '',
            'time'         => $msg->getCreatedAt()->format('H:i'),
            'date'         => $msg->getCreatedAt()->format('d/m/Y'),
            'isMine'       => true,
        ], 201);
    }

    private function fmtSize(int $bytes): string
    {
        if ($bytes < 1024)       return $bytes . ' o';
        if ($bytes < 1048576)    return round($bytes / 1024, 1) . ' Ko';
        return round($bytes / 1048576, 1) . ' Mo';
    }

    /** POST /chat/react */
    #[Route('/react', name: 'app_chat_react', methods: ['POST'])]
    public function react(
        Request                $request,
        MessageRepository      $msgRepo,
        EntityManagerInterface $em
    ): JsonResponse {
        /** @var Utilisateurs $me */
        $me      = $this->getUser();
        $payload = json_decode($request->getContent(), true) ?? [];
        $msgId   = (int)($payload['messageId'] ?? 0);
        $emoji   = trim((string)($payload['emoji'] ?? ''));

        if (!$msgId || !$emoji) {
            return $this->json(['error' => 'Données invalides.'], 400);
        }

        $msg = $msgRepo->find($msgId);
        if (!$msg) {
            return $this->json(['error' => 'Message introuvable.'], 404);
        }

        // Vérifier que l'utilisateur fait partie de la conversation
        $meId = $me->getIdUtilisateur();
        if ($msg->getSender()->getIdUtilisateur() !== $meId && $msg->getReceiver()->getIdUtilisateur() !== $meId) {
            return $this->json(['error' => 'Accès refusé.'], 403);
        }

        // Les réactions sont stockées dans un champ JSON séparé
        // On utilise le champ `extra` ou on ajoute une table dédiée
        // Solution simple : stocker dans une colonne JSON du message
        $extra = $msg->getReactions() ?? [];
        $key   = $me->getIdUtilisateur() . '_' . $emoji;

        // Toggle : si déjà réagi avec ce même emoji, on retire
        if (isset($extra[$key])) {
            unset($extra[$key]);
        } else {
            $extra[$key] = ['userId' => $meId, 'emoji' => $emoji, 'name' => $me->getPrenomUtilisateur()];
        }

        $msg->setReactions($extra);
        $em->flush();

        return $this->json(['ok' => true, 'reactions' => array_values($extra)]);
    }

    /** POST /chat/delete-msg */
    #[Route('/delete-msg', name: 'app_chat_delete_msg', methods: ['POST'])]
    public function deleteMsg(
        Request                $request,
        MessageRepository      $msgRepo,
        EntityManagerInterface $em
    ): JsonResponse {
        /** @var Utilisateurs $me */
        $me      = $this->getUser();
        $payload = json_decode($request->getContent(), true) ?? [];
        $msgId   = (int)($payload['messageId'] ?? 0);

        if (!$msgId) {
            return $this->json(['error' => 'ID manquant.'], 400);
        }

        $msg = $msgRepo->find($msgId);
        if (!$msg) {
            return $this->json(['error' => 'Message introuvable.'], 404);
        }

        if ($msg->getSender()->getIdUtilisateur() !== $me->getIdUtilisateur()) {
            return $this->json(['error' => 'Vous ne pouvez supprimer que vos propres messages.'], 403);
        }

        // Supprimer le fichier physique si c'est une image ou un vocal
        $raw = $msg->getContent();
        if (str_starts_with(trim($raw), '{')) {
            $decoded = json_decode($raw, true);
            if ($decoded && isset($decoded['fileUrl'])) {
                $projectDir = $this->getParameter('kernel.project_dir');
                assert(is_string($projectDir));
                $filePath = $projectDir . '/public' . $decoded['fileUrl'];
                if (file_exists($filePath)) {
                    @unlink($filePath);
                }
            }
        }

        $em->remove($msg);
        $em->flush();

        return $this->json(['ok' => true]);
    }

    /** POST /chat/edit-msg */
    #[Route('/edit-msg', name: 'app_chat_edit_msg', methods: ['POST'])]
    public function editMsg(
        Request                $request,
        MessageRepository      $msgRepo,
        EntityManagerInterface $em
    ): JsonResponse {
        /** @var Utilisateurs $me */
        $me      = $this->getUser();
        $payload = json_decode($request->getContent(), true) ?? [];
        $msgId   = (int)($payload['messageId'] ?? 0);
        $content = trim((string)($payload['content'] ?? ''));

        if (!$msgId || $content === '') {
            return $this->json(['error' => 'Données invalides.'], 400);
        }
        if (mb_strlen($content) > 1000) {
            return $this->json(['error' => 'Message trop long.'], 400);
        }

        $msg = $msgRepo->find($msgId);
        if (!$msg) {
            return $this->json(['error' => 'Message introuvable.'], 404);
        }
        if ($msg->getSender()->getIdUtilisateur() !== $me->getIdUtilisateur()) {
            return $this->json(['error' => 'Vous ne pouvez modifier que vos propres messages.'], 403);
        }
        if ($msg->getContent() !== $content) {
            $msg->setContent($content);
            $em->flush();
        }

        return $this->json(['ok' => true, 'content' => $content]);
    }
    #[Route('/read/{userId}', name: 'app_chat_mark_read', methods: ['POST'], requirements: ['userId' => '\d+'])]
    public function markRead(int $userId, UtilisateursRepository $userRepo, MessageRepository $msgRepo): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me    = $this->getUser();
        $other = $userRepo->find($userId);
        if (!$other) return $this->json(['error' => 'Utilisateur introuvable.'], 404);
        $msgRepo->markAsRead($other, $me);
        return $this->json(['ok' => true]);
    }

    /** GET /chat/users/search?q= */
    #[Route('/users/search', name: 'app_chat_users_search', methods: ['GET'])]
    public function searchUsers(Request $request, UtilisateursRepository $userRepo): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me    = $this->getUser();
        $query = trim((string) $request->query->get('q', ''));
        if (mb_strlen($query) < 2) return $this->json([]);
        $results = $userRepo->findFiltered(['search' => $query, 'perPage' => 10])['items'];
        $data = [];
        foreach ($results as $user) {
            if ($user->getIdUtilisateur() === $me->getIdUtilisateur()) continue;
            $data[] = [
                'userId' => $user->getIdUtilisateur(),
                'name'   => $user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur(),
                'avatar' => $user->getPhotoProfilUtilisateur(),
                'role'   => $user->getRoleUtilisateur(),
            ];
        }
        return $this->json($data);
    }

    /** GET /chat/unread-count */
    #[Route('/unread-count', name: 'app_chat_unread_count', methods: ['GET'])]
    public function unreadCount(MessageRepository $repo): JsonResponse
    {
        /** @var Utilisateurs $me */
        $me = $this->getUser();
        return $this->json(['count' => $repo->countUnread($me)]);
    }
}