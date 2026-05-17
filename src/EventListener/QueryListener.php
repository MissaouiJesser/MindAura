<?php

namespace App\EventListener;

use Psr\Log\LoggerInterface;

/**
 * @deprecated Ce listener n'est plus compatible avec Doctrine DBAL 3/4.
 *             À supprimer complètement.
 */
class QueryListener
{
    // La propriété n'est plus utilisée, on l'ignore.
    // @phpstan-ignore property.onlyWritten
    private LoggerInterface $logger;

    public function __construct(LoggerInterface $logger)
    {
        $this->logger = $logger;
    }

    // La méthode postExecute ne peut plus être appelée car l'événement a disparu.
    // On la supprime. Le service peut être supprimé entièrement.
}