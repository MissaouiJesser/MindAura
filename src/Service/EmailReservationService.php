<?php
// src/Service/EmailReservationService.php

namespace App\Service;

use App\Entity\ReservationLocal;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;

class EmailReservationService
{
    public function __construct(
        private MailerInterface $mailer,
        private string $fromEmail,
        private string $fromName,
    ) {}

    /**
     * Envoie un email de confirmation de réservation au patient.
     */
    public function envoyerConfirmation(ReservationLocal $reservation, string $destinataireEmail): void
    {
        $local  = $reservation->getLocal();
        $salle  = $reservation->getSalle();
        $nomLieu = $local?->getNomLocal() ?? ($salle?->getNomSalle() ?? '—');
        $typeLieu = $local ? 'Local' : 'Salle';

        $date   = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';
        $debut  = $reservation->getHeureDebutReservation()?->format('H:i') ?? '—';
        $fin    = $reservation->getHeureFinReservation()?->format('H:i') ?? '—';
        $prix   = $reservation->getPrixReservation() ? number_format((float)$reservation->getPrixReservation(), 2, ',', ' ') . ' DT' : '—';
        $motif  = $reservation->getMotifReservation() ?? '—';
        $status = $reservation->getStatusReservation() ?? 'En attente';
        $nom    = $reservation->getNomCl() ?? '';
        $prenom = $reservation->getPrenomCl() ?? '';
        $id     = $reservation->getIdReservation();

        $html = $this->buildHtml(
            nom: $nom, prenom: $prenom,
            nomLieu: $nomLieu, typeLieu: $typeLieu,
            date: $date, debut: $debut, fin: $fin,
            prix: $prix, motif: $motif, status: $status, id: $id
        );

        $email = (new Email())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($destinataireEmail)
            ->subject("✅ Confirmation de réservation #$id — MindAura")
            ->html($html);

        $this->mailer->send($email);
    }

    /**
     * Envoie un email d'annulation au patient lorsque sa réservation est supprimée.
     */
    public function envoyerAnnulation(ReservationLocal $reservation, string $destinataireEmail): void
    {
        $local   = $reservation->getLocal();
        $salle   = $reservation->getSalle();
        $nomLieu = $local?->getNomLocal() ?? ($salle?->getNomSalle() ?? '—');
        $typeLieu = $local ? 'Local' : 'Salle';

        $date   = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';
        $debut  = $reservation->getHeureDebutReservation()?->format('H:i') ?? '—';
        $fin    = $reservation->getHeureFinReservation()?->format('H:i') ?? '—';
        $prix   = $reservation->getPrixReservation() ? number_format((float)$reservation->getPrixReservation(), 2, ',', ' ') . ' DT' : '—';
        $motif  = $reservation->getMotifReservation() ?? '—';
        $nom    = $reservation->getNomCl() ?? '';
        $prenom = $reservation->getPrenomCl() ?? '';
        $id     = $reservation->getIdReservation();

        $html = $this->buildAnnulationHtml(
            nom: $nom, prenom: $prenom,
            nomLieu: $nomLieu, typeLieu: $typeLieu,
            date: $date, debut: $debut, fin: $fin,
            prix: $prix, motif: $motif, id: $id
        );

        $email = (new Email())
            ->from(new Address($this->fromEmail, $this->fromName))
            ->to($destinataireEmail)
            ->subject("❌ Annulation de réservation #$id — MindAura")
            ->html($html);

        $this->mailer->send($email);
    }

    // ── HTML confirmation ────────────────────────────────────────────────────

    private function buildHtml(
        string $nom, string $prenom,
        string $nomLieu, string $typeLieu,
        string $date, string $debut, string $fin,
        string $prix, string $motif, string $status, ?int $id
    ): string {
        $statusColor = match($status) {
            'Confirmée'  => '#16A34A',
            'Refusée'    => '#DC2626',
            default      => '#D97706',
        };

        return <<<HTML
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Confirmation de réservation — MindAura</title>
</head>
<body style="margin:0;padding:0;background:#F0F4F8;font-family:'Segoe UI',Arial,sans-serif;">

<table width="100%" cellpadding="0" cellspacing="0" style="background:#F0F4F8;padding:40px 0;">
<tr><td align="center">

  <table width="620" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 20px 60px rgba(27,67,50,.12);">

    <tr>
      <td style="background:linear-gradient(135deg,#1B4332 0%,#2D6A4F 60%,#52B788 100%);padding:44px 48px 36px;">
        <table width="100%" cellpadding="0" cellspacing="0">
          <tr>
            <td>
              <div style="font-family:Georgia,serif;font-size:28px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;line-height:1.1;">
                🌿 MindAura
              </div>
              <div style="font-size:11px;color:rgba(255,255,255,.65);letter-spacing:2px;text-transform:uppercase;margin-top:4px;">
                Bien-être &amp; Sérénité
              </div>
            </td>
            <td align="right">
              <div style="background:rgba(255,255,255,.15);border:1px solid rgba(255,255,255,.2);border-radius:99px;padding:6px 16px;display:inline-block;">
                <span style="font-size:12px;color:rgba(255,255,255,.9);font-weight:600;">Réservation #$id</span>
              </div>
            </td>
          </tr>
        </table>

        <div style="margin-top:32px;">
          <div style="font-size:13px;color:rgba(255,255,255,.7);margin-bottom:8px;text-transform:uppercase;letter-spacing:1px;">Confirmation envoyée à</div>
          <div style="font-family:Georgia,serif;font-size:34px;font-weight:700;color:#ffffff;line-height:1.15;">
            $prenom $nom
          </div>
        </div>

        <div style="margin-top:24px;display:inline-block;background:rgba(255,255,255,.12);border:1.5px solid rgba(255,255,255,.25);border-radius:12px;padding:10px 20px;">
          <span style="font-size:13px;font-weight:700;color:#ffffff;">Statut :</span>
          <span style="font-size:13px;font-weight:700;color:#ffffff;background:{$statusColor};border-radius:99px;padding:3px 14px;margin-left:8px;">
            $status
          </span>
        </div>
      </td>
    </tr>

    <tr>
      <td style="padding:36px 48px 0;">
        <p style="font-size:15.5px;color:#374151;line-height:1.8;margin:0;">
          Bonjour <strong>$prenom $nom</strong>,<br/>
          Votre réservation a bien été enregistrée sur la plateforme <strong>MindAura</strong>.
          Vous trouverez ci-dessous le récapitulatif complet de votre demande.
        </p>
      </td>
    </tr>

    <tr>
      <td style="padding:28px 48px 0;">
        <div style="background:#F8FBF9;border-radius:18px;border:1.5px solid #D8F3DC;overflow:hidden;">
          <div style="background:linear-gradient(90deg,#1B4332,#2D6A4F);padding:16px 24px;">
            <span style="font-size:14px;font-weight:700;color:#ffffff;text-transform:uppercase;letter-spacing:1px;">
              📋 Détails de la réservation
            </span>
          </div>
          <table width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
            <tr style="border-bottom:1px solid #E5F0EA;">
              <td style="padding:16px 24px;width:40%;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">Type de lieu</div>
                <div style="font-size:15px;font-weight:700;color:#1B4332;">$typeLieu</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #E5F0EA;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">Nom du lieu</div>
                <div style="font-size:15px;font-weight:700;color:#1B4332;">$nomLieu</div>
              </td>
            </tr>
            <tr style="border-bottom:1px solid #E5F0EA;">
              <td style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">📅 Date</div>
                <div style="font-size:15px;font-weight:700;color:#1B4332;">$date</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #E5F0EA;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">⏰ Horaires</div>
                <div style="font-size:15px;font-weight:700;color:#1B4332;">$debut → $fin</div>
              </td>
            </tr>
            <tr style="border-bottom:1px solid #E5F0EA;">
              <td style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">💶 Prix total</div>
                <div style="font-size:20px;font-weight:800;color:#2D6A4F;">$prix</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #E5F0EA;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">📌 Statut</div>
                <div style="font-size:14px;font-weight:700;color:{$statusColor};">$status</div>
              </td>
            </tr>
            <tr>
              <td colspan="2" style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:6px;">📝 Motif</div>
                <div style="font-size:14px;color:#374151;line-height:1.7;background:#ffffff;border-radius:10px;padding:12px 16px;border:1px solid #E5F0EA;">$motif</div>
              </td>
            </tr>
          </table>
        </div>
      </td>
    </tr>

    <tr>
      <td style="padding:24px 48px 0;">
        <div style="background:#FEF9EC;border-radius:14px;border-left:4px solid #D97706;padding:16px 20px;">
          <p style="margin:0;font-size:13.5px;color:#92400E;line-height:1.7;">
            ⏳ <strong>En attente de confirmation</strong> — Notre équipe examinera votre demande et vous contactera dans les plus brefs délais pour confirmer votre réservation.
          </p>
        </div>
      </td>
    </tr>

    <tr>
      <td style="padding:36px 48px 44px;margin-top:8px;">
        <div style="border-top:1px solid #E5F0EA;padding-top:28px;">
          <table width="100%" cellpadding="0" cellspacing="0">
            <tr>
              <td>
                <div style="font-family:Georgia,serif;font-size:18px;font-weight:700;color:#1B4332;">🌿 MindAura</div>
                <div style="font-size:12px;color:#9CA3AF;margin-top:4px;">Plateforme de bien-être &amp; santé mentale</div>
              </td>
              <td align="right" style="vertical-align:top;">
                <div style="font-size:11px;color:#9CA3AF;line-height:1.8;">
                  Cet email est automatique, merci de ne pas répondre.<br/>
                  &copy; 2026 MindAura — Tous droits réservés.
                </div>
              </td>
            </tr>
          </table>
        </div>
      </td>
    </tr>

  </table>

</td></tr>
</table>

</body>
</html>
HTML;
    }

    // ── HTML annulation ──────────────────────────────────────────────────────

    private function buildAnnulationHtml(
        string $nom, string $prenom,
        string $nomLieu, string $typeLieu,
        string $date, string $debut, string $fin,
        string $prix, string $motif, ?int $id
    ): string {
        return <<<HTML
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Annulation de réservation — MindAura</title>
</head>
<body style="margin:0;padding:0;background:#F0F4F8;font-family:'Segoe UI',Arial,sans-serif;">

<table width="100%" cellpadding="0" cellspacing="0" style="background:#F0F4F8;padding:40px 0;">
<tr><td align="center">

  <table width="620" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 20px 60px rgba(127,29,29,.12);">

    <!-- ══ HEADER ROUGE ══ -->
    <tr>
      <td style="background:linear-gradient(135deg,#7F1D1D 0%,#B91C1C 60%,#EF4444 100%);padding:44px 48px 36px;">
        <table width="100%" cellpadding="0" cellspacing="0">
          <tr>
            <td>
              <div style="font-family:Georgia,serif;font-size:28px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;line-height:1.1;">
                🌿 MindAura
              </div>
              <div style="font-size:11px;color:rgba(255,255,255,.65);letter-spacing:2px;text-transform:uppercase;margin-top:4px;">
                Bien-être &amp; Sérénité
              </div>
            </td>
            <td align="right">
              <div style="background:rgba(255,255,255,.15);border:1px solid rgba(255,255,255,.2);border-radius:99px;padding:6px 16px;display:inline-block;">
                <span style="font-size:12px;color:rgba(255,255,255,.9);font-weight:600;">Réservation #$id</span>
              </div>
            </td>
          </tr>
        </table>

        <div style="margin-top:32px;">
          <div style="font-size:13px;color:rgba(255,255,255,.7);margin-bottom:8px;text-transform:uppercase;letter-spacing:1px;">Notification envoyée à</div>
          <div style="font-family:Georgia,serif;font-size:34px;font-weight:700;color:#ffffff;line-height:1.15;">
            $prenom $nom
          </div>
        </div>

        <!-- Badge Annulée -->
        <div style="margin-top:24px;display:inline-block;background:rgba(255,255,255,.12);border:1.5px solid rgba(255,255,255,.25);border-radius:12px;padding:10px 20px;">
          <span style="font-size:13px;font-weight:700;color:#ffffff;">Statut :</span>
          <span style="font-size:13px;font-weight:700;color:#ffffff;background:#DC2626;border-radius:99px;padding:3px 14px;margin-left:8px;">
            ❌ Annulée
          </span>
        </div>
      </td>
    </tr>

    <!-- ══ MESSAGE D'INTRO ══ -->
    <tr>
      <td style="padding:36px 48px 0;">
        <p style="font-size:15.5px;color:#374151;line-height:1.8;margin:0;">
          Bonjour <strong>$prenom $nom</strong>,<br/>
          Nous vous informons que votre réservation <strong>#$id</strong> a été <strong style="color:#DC2626;">annulée</strong> par notre équipe.
          Vous trouverez ci-dessous le récapitulatif de la réservation concernée.
        </p>
      </td>
    </tr>

    <!-- ══ RÉCAPITULATIF ══ -->
    <tr>
      <td style="padding:28px 48px 0;">
        <div style="background:#FFF5F5;border-radius:18px;border:1.5px solid #FECACA;overflow:hidden;">

          <div style="background:linear-gradient(90deg,#7F1D1D,#B91C1C);padding:16px 24px;">
            <span style="font-size:14px;font-weight:700;color:#ffffff;text-transform:uppercase;letter-spacing:1px;">
              📋 Détails de la réservation annulée
            </span>
          </div>

          <table width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
            <tr style="border-bottom:1px solid #FEE2E2;">
              <td style="padding:16px 24px;width:40%;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">Type de lieu</div>
                <div style="font-size:15px;font-weight:700;color:#7F1D1D;">$typeLieu</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #FEE2E2;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">Nom du lieu</div>
                <div style="font-size:15px;font-weight:700;color:#7F1D1D;">$nomLieu</div>
              </td>
            </tr>
            <tr style="border-bottom:1px solid #FEE2E2;">
              <td style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">📅 Date</div>
                <div style="font-size:15px;font-weight:700;color:#7F1D1D;">$date</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #FEE2E2;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">⏰ Horaires</div>
                <div style="font-size:15px;font-weight:700;color:#7F1D1D;">$debut → $fin</div>
              </td>
            </tr>
            <tr style="border-bottom:1px solid #FEE2E2;">
              <td style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">💶 Prix total</div>
                <div style="font-size:20px;font-weight:800;color:#B91C1C;">$prix</div>
              </td>
              <td style="padding:16px 24px;border-left:1px solid #FEE2E2;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:3px;">📌 Statut final</div>
                <div style="font-size:14px;font-weight:700;color:#DC2626;">❌ Annulée</div>
              </td>
            </tr>
            <tr>
              <td colspan="2" style="padding:16px 24px;">
                <div style="font-size:11px;color:#6B7280;text-transform:uppercase;letter-spacing:0.8px;font-weight:600;margin-bottom:6px;">📝 Motif initial</div>
                <div style="font-size:14px;color:#374151;line-height:1.7;background:#ffffff;border-radius:10px;padding:12px 16px;border:1px solid #FEE2E2;">$motif</div>
              </td>
            </tr>
          </table>
        </div>
      </td>
    </tr>

    <!-- ══ NOTE INFO ══ -->
    <tr>
      <td style="padding:24px 48px 0;">
        <div style="background:#FEF2F2;border-radius:14px;border-left:4px solid #DC2626;padding:16px 20px;">
          <p style="margin:0;font-size:13.5px;color:#7F1D1D;line-height:1.7;">
            ℹ️ <strong>Réservation annulée</strong> — Si vous pensez qu'il s'agit d'une erreur ou si vous souhaitez effectuer une nouvelle réservation, n'hésitez pas à nous contacter ou à vous reconnecter sur la plateforme.
          </p>
        </div>
      </td>
    </tr>

    <!-- ══ FOOTER ══ -->
    <tr>
      <td style="padding:36px 48px 44px;margin-top:8px;">
        <div style="border-top:1px solid #FEE2E2;padding-top:28px;">
          <table width="100%" cellpadding="0" cellspacing="0">
            <tr>
              <td>
                <div style="font-family:Georgia,serif;font-size:18px;font-weight:700;color:#1B4332;">🌿 MindAura</div>
                <div style="font-size:12px;color:#9CA3AF;margin-top:4px;">Plateforme de bien-être &amp; santé mentale</div>
              </td>
              <td align="right" style="vertical-align:top;">
                <div style="font-size:11px;color:#9CA3AF;line-height:1.8;">
                  Cet email est automatique, merci de ne pas répondre.<br/>
                  &copy; 2026 MindAura — Tous droits réservés.
                </div>
              </td>
            </tr>
          </table>
        </div>
      </td>
    </tr>

  </table>

</td></tr>
</table>

</body>
</html>
HTML;
    }
}