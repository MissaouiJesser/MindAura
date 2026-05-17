<?php

namespace App\Service;

use Psr\Log\LoggerInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  SyncNotifierService — Envoie les notifications de Symfony vers JavaFX
 * ══════════════════════════════════════════════════════════════════════════
 *
 *  Usage dans un contrôleur Symfony après un persist/flush ou un remove :
 *
 *      public function __construct(
 *          private readonly SyncNotifierService $sync,
 *      ) {}
 *
 *      public function nouvelUtilisateur(...) {
 *          // ... persist + flush
 *          $this->sync->notifyJava('user', 'create', $user->getId());
 *          return ...;
 *      }
 *
 *  Le service est résilient : si l'app Java n'est pas lancée, l'appel HTTP
 *  échoue silencieusement (timeout court). On ne casse JAMAIS la requête
 *  web pour un problème de sync.
 * ══════════════════════════════════════════════════════════════════════════
 */
final class SyncNotifierService
{
    // URL du serveur HTTP embarqué dans l'app JavaFX (SyncListener)
    private const JAVA_URL = 'http://localhost:8101/api/sync/notify';

    // ⚠ Doit être STRICTEMENT identique à SHARED_SECRET côté Java
    private const SHARED_SECRET = 'MindAura-Sync-Secret-2026';

    public function __construct(
        private readonly HttpClientInterface $httpClient,
        private readonly LoggerInterface $logger,
    ) {}

    /**
     * Notifie l'app JavaFX qu'une entité a changé.
     *
     * @param string $entity  Nom de l'entité (ex: "user", "reclamation")
     * @param string $action  Action : "create", "update", "delete"
     * @param mixed  $id      Identifiant de l'entité affectée
     * @param string $message Message libre (optionnel)
     *
     * @return bool true si Java a répondu 200, false sinon
     */
    public function notifyJava(string $entity, string $action, $id, string $message = ''): bool
    {
        $payload = [
            'entity'  => $entity,
            'action'  => $action,
            'id'      => (string)$id,
            'message' => $message,
            'source'  => 'symfony',
        ];

        try {
            $response = $this->httpClient->request('POST', self::JAVA_URL, [
                'headers' => [
                    'Content-Type'   => 'application/json',
                    'X-Sync-Secret'  => self::SHARED_SECRET,
                ],
                'json'    => $payload,
                'timeout' => 2,        // 2s max pour ne pas ralentir la page web
            ]);

            $status = $response->getStatusCode();
            if ($status === 200) {
                $this->logger->info('[SyncNotifier] ✅ Notif envoyée à Java', $payload);
                return true;
            }

            $this->logger->warning('[SyncNotifier] ⚠ Java a répondu ' . $status, $payload);
            return false;

        } catch (\Throwable $e) {
            // App Java probablement éteinte — on ne plante pas la requête web
            $this->logger->info('[SyncNotifier] Java injoignable (probablement éteint) : '
                . $e->getMessage());
            return false;
        }
    }
}
