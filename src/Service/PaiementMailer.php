<?php

namespace App\Service;

use App\Entity\Commande;
use App\Entity\Produit;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Twig\Environment;

class PaiementMailer
{
    public function __construct(
        private MailerInterface $mailer,
        private Environment     $twig,
        private string $senderEmail = 'noreply@mindaura.tn',
        private string $senderName  = 'MindAura Events'
    ) {}

    public function sendConfirmationCommande(Commande $commande): void
    {
        $clientEmail = $commande->getUserEmail();
        /** @phpstan-ignore identical.alwaysFalse */
        if ($clientEmail === null) {
            return;
        }

        $html = $this->twig->render('emails/commande_confirmation.html.twig', [
            'commande' => $commande,
        ]);

        $email = (new Email())
            ->from(new Address($this->senderEmail, $this->senderName))
            ->to($clientEmail)
            ->subject('✅ Confirmation de votre commande #' . $commande->getId() . ' — MindAura')
            ->html($html);

        $this->mailer->send($email);
    }

    public function sendAlerteDisponibilite(Produit $produit, string $clientEmail): void
    {
        $siteUrl = $_ENV['SITE_URL'] ?? 'http://localhost:8000';

        $html = $this->twig->render('emails/alerte_disponibilite.html.twig', [
            'produit'  => $produit,
            'siteUrl'  => $siteUrl,
            'email'    => $clientEmail,
        ]);

        $email = (new Email())
            ->from(new Address($this->senderEmail, $this->senderName))
            ->to($clientEmail)
            ->subject('📦 ' . $produit->getNom() . ' est de nouveau disponible ! — MindAura')
            ->html($html);

        $this->mailer->send($email);
    }
}