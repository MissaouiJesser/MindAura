<?php

namespace App\Service;

use App\Entity\Evenement;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Twig\Environment;

class EvenementMailer
{
    public function __construct(
        private MailerInterface $mailer,
        private Environment $twig,
        private string $senderEmail = 'noreply@mindaura.tn',
        private string $senderName  = 'MindAura Events'
    ) {}

    public function sendEvenementModifie(Evenement $evenement): void
    {
        foreach ($evenement->getParticipations() as $participation) {
            $emailParticipant = $participation->getEmail();
            /** @phpstan-ignore identical.alwaysFalse */
            if ($emailParticipant === null) {
                continue;
            }

            $html = $this->twig->render('emails/evenement_modifie.html.twig', [
                'participation' => $participation,
                'evenement'     => $evenement,
            ]);

            $email = (new Email())
                ->from(new Address($this->senderEmail, $this->senderName))
                ->to($emailParticipant)
                ->subject('⚠️ Modification de l\'événement – ' . $evenement->getTitreEvenement())
                ->html($html);

            $this->mailer->send($email);
        }
    }

    public function sendEvenementSupprime(Evenement $evenement): void
    {
        foreach ($evenement->getParticipations() as $participation) {
            $emailParticipant = $participation->getEmail();
            /** @phpstan-ignore identical.alwaysFalse */
            /** @phpstan-ignore identical.alwaysFalse */
            if ($emailParticipant === null) {
                continue;
            }

            $html = $this->twig->render('emails/evenement_supprime.html.twig', [
                'participation' => $participation,
                'evenement'     => $evenement,
            ]);

            $email = (new Email())
                ->from(new Address($this->senderEmail, $this->senderName))
                ->to($emailParticipant)
                ->subject('❌ Annulation de l\'événement – ' . $evenement->getTitreEvenement())
                ->html($html);

            $this->mailer->send($email);
        }
    }
}