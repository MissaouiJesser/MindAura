<?php

namespace App\Service;

use App\Entity\Ressources;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mercure\HubInterface;
use Symfony\Component\Mercure\Update;
use Symfony\Component\Mime\Email;
use Twig\Environment;
use Psr\Log\LoggerInterface;
use App\Entity\Commentaires;

class NotificationServiceRessource
{
    public function __construct(
        private MailerInterface $mailer,
        private HubInterface    $hub,
        private Environment     $twig,
        private LoggerInterface $logger,
        private string $fromEmail = 'minyarguesmi87@gmail.com'
    ) {}

    /**
     * Notifie l’auteur d’une nouvelle ressource (email + Mercure).
     *
     * @return array{
     *     email: bool,
     *     mercure: bool,
     *     errors: array<int, string>
     * }
     */
    public function notifierNouvelleRessource(Ressources $ressource): array
    {
        $results = [
            'email'   => false,
            'mercure' => false,
            'errors'  => [],
        ];

        try {
            $this->envoyerEmail($ressource);
            $results['email'] = true;
            $this->logger->info('[NotificationService] Email envoyé à ' . $ressource->getEmailAuteur());
        } catch (\Exception $e) {
            $results['errors'][] = 'Email : ' . $e->getMessage();
            $this->logger->error('[NotificationService] Échec email : ' . $e->getMessage());
        }

        try {
            $this->publierMercure($ressource);
            $results['mercure'] = true;
            $this->logger->info('[NotificationService] Mercure publié pour ressource #' . $ressource->getId());
        } catch (\Exception $e) {
            $results['errors'][] = 'Mercure : ' . $e->getMessage();
            $this->logger->warning('[NotificationService] Échec Mercure (non bloquant) : ' . $e->getMessage());
        }

        return $results;
    }

    private function envoyerEmail(Ressources $ressource): void
    {
        $destinataire = $ressource->getEmailAuteur();

        if (!$destinataire || !filter_var($destinataire, FILTER_VALIDATE_EMAIL)) {
            $this->logger->warning('[NotificationService] Email auteur invalide ou absent : ' . ($destinataire ?? 'null'));
            return;
        }

        $html = $this->twig->render('emails/nouvelle_ressource.html.twig', [
            'ressource' => $ressource,
        ]);

        $email = (new Email())
            ->from($this->fromEmail)
            ->to($destinataire)
            ->subject('✅ Votre ressource "' . $ressource->getTitre() . '" a été publiée')
            ->html($html);

        $this->mailer->send($email);
    }

    private function publierMercure(Ressources $ressource): void
    {
        $data = json_encode([
            'type'      => 'nouvelle_ressource',
            'id'        => $ressource->getId(),
            'titre'     => $ressource->getTitre(),
            'contenu'   => $ressource->getContenu(),
            'categorie' => $ressource->getCategorie(),
            'niveau'    => $ressource->getNiveau(),
            'image'     => $ressource->getImageUrl(),
            'date'      => $ressource->getDatePublication()->format('d/m/Y'),
        ]);

        // json_encode peut retourner false en cas d'erreur
        if ($data === false) {
            $this->logger->error('[NotificationService] Échec encodage JSON pour Mercure');
            return;
        }

        $update = new Update(
            topics: 'https://votresite.com/ressources/nouvelle',
            data: $data
        );

        $this->hub->publish($update);
    }

    public function notifierNouveauCommentaire(Commentaires $commentaire): void
    {
        $ressource = $commentaire->getRessource();

        if ($ressource === null) {
            return;
        }

        $emailAuteur = $ressource->getEmailAuteur();

        if (!$emailAuteur || !filter_var($emailAuteur, FILTER_VALIDATE_EMAIL)) {
            return;
        }

        try {
            $html = $this->twig->render('emails/nouveau_commentaire.html.twig', [
                'commentaire' => $commentaire,
                'ressource'   => $ressource,
            ]);

            $email = (new Email())
                ->from($this->fromEmail)
                ->to($emailAuteur)
                ->subject('💬 Nouveau commentaire sur "' . $ressource->getTitre() . '"')
                ->html($html);

            $this->mailer->send($email);
            $this->logger->info('[Notif] Email commentaire envoyé à ' . $emailAuteur);
        } catch (\Exception $e) {
            $this->logger->error('[Notif] Échec email commentaire : ' . $e->getMessage());
        }
    }

    public function notifierFavoriAjoute(Ressources $ressource, string $fanName): void
    {
        $emailAuteur = $ressource->getEmailAuteur();

        if (!$emailAuteur || !filter_var($emailAuteur, FILTER_VALIDATE_EMAIL)) {
            return;
        }

        try {
            $html = $this->twig->render('emails/favori_ajoute.html.twig', [
                'ressource' => $ressource,
                'fanName'   => $fanName,
            ]);

            $email = (new Email())
                ->from($this->fromEmail)
                ->to($emailAuteur)
                ->subject('⭐ ' . $fanName . ' a ajouté votre ressource en favori')
                ->html($html);

            $this->mailer->send($email);
            $this->logger->info('[Notif] Email favori envoyé à ' . $emailAuteur);
        } catch (\Exception $e) {
            $this->logger->error('[Notif] Échec email favori : ' . $e->getMessage());
        }
    }
}