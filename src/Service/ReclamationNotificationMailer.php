<?php

namespace App\Service;

use App\Entity\Reclamation;
use App\Entity\Utilisateurs;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;

class ReclamationNotificationMailer
{
    public function __construct(
        private readonly MailerInterface $mailer,
        private readonly string $fromAddress,
        private readonly string $fromName,
        private readonly string $adminNotificationTo,
        private readonly string $traiteeExpediteurAddress,
        private readonly string $traiteeExpediteurName,
    ) {}

    public function notifyAdminNewReclamation(Reclamation $reclamation, ?Utilisateurs $submitter = null): void
    {
        $email = (new TemplatedEmail())
            ->from(new Address($this->fromAddress, $this->fromName))
            ->to($this->adminNotificationTo)
            ->subject('Nouvelle réclamation #' . $reclamation->getId_reclamation())
            ->htmlTemplate('emails/reclamation_created.html.twig')
            ->context([
                'reclamation' => $reclamation,
                'submitter' => $submitter,
            ]);

        $this->mailer->send($email);
    }

    public function notifyUserReclamationTreated(Reclamation $reclamation, Utilisateurs $submitter): void
    {
        $to = (string) ($submitter->getEmailUtilisateur() ?? '');
        if (trim($to) === '') {
            return;
        }

        $email = (new TemplatedEmail())
            ->from(new Address($this->traiteeExpediteurAddress, $this->traiteeExpediteurName))
            ->to($to)
            ->subject('Votre réclamation #' . $reclamation->getId_reclamation() . ' a été traitée')
            ->htmlTemplate('emails/reclamation_traitee.html.twig')
            ->context([
                'reclamation' => $reclamation,
                'submitter' => $submitter,
            ]);

        $this->mailer->send($email);
    }
}