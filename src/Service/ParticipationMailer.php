<?php

namespace App\Service;

use App\Entity\Participation;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Twig\Environment;

class ParticipationMailer
{
    public function __construct(
        private MailerInterface $mailer,
        private Environment $twig,
        private string $senderEmail = 'noreply@mindaura.tn',
        private string $senderName  = 'MindAura Events'
    ) {}

    public function sendConfirmation(Participation $participation, string $qrFilePath): void
    {
        $evenement = $participation->getEvenement();
        if ($evenement === null) {
            return;
        }

        $emailParticipant = $participation->getEmail();
        /** @phpstan-ignore identical.alwaysFalse */
        if ($emailParticipant === null) {
            return;
        }

        $html = $this->twig->render('emails/participation_confirmation.html.twig', [
            'participation' => $participation,
        ]);

        $email = (new Email())
            ->from(new Address($this->senderEmail, $this->senderName))
            ->to($emailParticipant)
            ->subject('✅ Confirmation de votre participation – ' . $evenement->getTitreEvenement())
            ->html($html);

        // Attachement du QR code seulement si le fichier existe et est lisible
        if (!empty($qrFilePath) && file_exists($qrFilePath) && is_readable($qrFilePath)) {
            $email->attachFromPath($qrFilePath, 'billet_qr.png', 'image/png');
        }

        $this->mailer->send($email);
    }

    public function sendListeAttenteConfirmation(Participation $participation): void
    {
        $evenement = $participation->getEvenement();
        if ($evenement === null) {
            return;
        }

        $emailParticipant = $participation->getEmail();
        /** @phpstan-ignore identical.alwaysFalse */
        if ($emailParticipant === null) {
            return;
        }

        $html = $this->twig->render('emails/liste_attente_confirmation.html.twig', [
            'participation' => $participation,
        ]);

        $email = (new Email())
            ->from(new Address($this->senderEmail, $this->senderName))
            ->to($emailParticipant)
            ->subject('⏳ Inscription en liste d\'attente – ' . $evenement->getTitreEvenement())
            ->html($html);

        $this->mailer->send($email);
    }

    // Optionnel : méthode pour notifier qu'une place s'est libérée
    public function sendListeAttenteNotification(Participation $participation, ?string $qrFilePath = null): void
    {
        $evenement = $participation->getEvenement();
        if ($evenement === null) {
            return;
        }

        $emailParticipant = $participation->getEmail();
        /** @phpstan-ignore identical.alwaysFalse */
        if ($emailParticipant === null) {
            return;
        }

        $html = $this->twig->render('emails/liste_attente_notification.html.twig', [
            'participation' => $participation,
        ]);

        $email = (new Email())
            ->from(new Address($this->senderEmail, $this->senderName))
            ->to($emailParticipant)
            ->subject('🎉 Une place s\'est libérée ! – ' . $evenement->getTitreEvenement())
            ->html($html);

        if ($qrFilePath && file_exists($qrFilePath)) {
            $email->attachFromPath($qrFilePath, 'billet_qr.png', 'image/png');
        }

        $this->mailer->send($email);
    }
}