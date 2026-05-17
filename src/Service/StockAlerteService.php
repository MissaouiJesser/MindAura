<?php

namespace App\Service;

use App\Entity\Produit;
use Doctrine\ORM\EntityManagerInterface;
use Psr\Log\LoggerInterface;


class StockAlerteService
{
    public function __construct(
        private PaiementMailer         $paiementMailer,
        private EntityManagerInterface $em,
        private LoggerInterface        $logger
    ) {}

    
    public function notifierSiDisponible(Produit $produit): void
    {
        
        if (!$produit->isDisponible()) {
            $this->logger->info(
                '[StockAlerte] Produit "' . $produit->getNom() . '" toujours en rupture, pas d\'alerte envoyée.'
            );
            return;
        }

        $emails = $produit->getAlertesEmail();

        if (empty($emails)) {
            $this->logger->info(
                '[StockAlerte] Produit "' . $produit->getNom() . '" disponible mais aucun email en attente.'
            );
            return;
        }

        $this->logger->info(
            '[StockAlerte] Envoi alerte email pour "' . $produit->getNom() . '" à '
            . count($emails) . ' adresse(s) : ' . implode(', ', $emails)
        );

        foreach ($emails as $email) {
            try {
                $this->paiementMailer->sendAlerteDisponibilite($produit, $email);
                $this->logger->info('[StockAlerte] ✅ Alerte email envoyée à ' . $email);
            } catch (\Throwable $e) {
                $this->logger->error(
                    '[StockAlerte] ❌ Échec alerte vers ' . $email . ' : ' . $e->getMessage()
                );
                
            }
        }

        
        $produit->clearAlertesEmail();
        $this->em->flush();

        $this->logger->info(
            '[StockAlerte] Liste alertesEmail vidée pour "' . $produit->getNom() . '".'
        );
    }
}
