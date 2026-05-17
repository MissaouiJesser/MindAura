<?php
// src/Controller/PdfReservationController.php

namespace App\Controller;

use App\Entity\ReservationLocal;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Psr\Log\LoggerInterface;

#[Route('/reservations', name: 'reservation_local_')]
class PdfReservationController extends AbstractController
{
    private const MODEL           = 'openrouter/free';
    private const MODEL_FALLBACK1 = 'meta-llama/llama-3.3-70b-instruct:free';
    private const MODEL_FALLBACK2 = 'google/gemma-3-27b-it:free';

    public function __construct(
        private HttpClientInterface $httpClient,
        private string $openRouterApiKey,
        private LoggerInterface $logger
    ) {}

    // ── EXPORT PDF IA ──────────────────────────────────────────────────────
    #[Route('/{id}/pdf', name: 'pdf', methods: ['GET'])]
    public function exportPdf(ReservationLocal $reservation): Response
    {
        $aiSummary = $this->generateAiSummary($reservation);
        $html      = $this->buildPdfHtml($reservation, $aiSummary);

        $options = new Options();
        $options->set('defaultFont', 'DejaVu Sans');
        $options->set('isRemoteEnabled', false);
        $options->set('isHtml5ParserEnabled', true);

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $filename = 'reservation-' . $reservation->getIdReservation()
            . '-' . date('Ymd') . '.pdf';

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="' . $filename . '"',
            ]
        );
    }

    // ── APERÇU HTML ────────────────────────────────────────────────────────
    #[Route('/{id}/pdf/preview', name: 'pdf_preview', methods: ['GET'])]
    public function previewPdf(ReservationLocal $reservation): Response
    {
        $aiSummary = $this->generateAiSummary($reservation);
        $html      = $this->buildPdfHtml($reservation, $aiSummary);

        return new Response($html, 200, ['Content-Type' => 'text/html; charset=UTF-8']);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MÉTHODES PRIVÉES
    // ─────────────────────────────────────────────────────────────────────

    private function generateAiSummary(ReservationLocal $reservation): string
    {
        $nomClient  = trim($reservation->getPrenomCl() . ' ' . $reservation->getNomCl());
        $nomLocal   = $reservation->getLocal()?->getNomLocal() ?? 'Non spécifié';
        $nomSalle   = $reservation->getSalle()?->getNomSalle() ?? 'Non spécifié';
        $date       = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';
        $heureDebut = $reservation->getHeureDebutReservation()?->format('H:i') ?? '—';
        $heureFin   = $reservation->getHeureFinReservation()?->format('H:i') ?? '—';
        $statut     = $reservation->getStatusReservation() ?? '—';
        $motif      = $reservation->getMotifReservation() ?? 'Non précisé';
        $prix       = $reservation->getPrixReservation() ?? '—';

        $prompt = <<<PROMPT
Tu es un assistant administratif pour la plateforme MindAura, spécialisée en santé mentale.
Rédige un résumé professionnel et bienveillant de la réservation suivante, en français.
Le résumé doit être structuré en 2-3 courts paragraphes sans titres, en language formel mais chaleureux.
Il doit mentionner le client, le local, la salle, la date, les horaires, le motif et le statut.
Termine par une phrase d'accompagnement professionnel liée à la santé mentale.

Données de la réservation :
- Client : $nomClient
- Local : $nomLocal
- Salle : $nomSalle
- Date : $date
- Horaires : $heureDebut — $heureFin
- Statut : $statut
- Motif : $motif
- Prix : $prix TND

Réponds UNIQUEMENT avec le texte du résumé, sans titre, sans markdown, sans balises HTML.
PROMPT;

        $modelsToTry = [self::MODEL, self::MODEL_FALLBACK1, self::MODEL_FALLBACK2];

        foreach ($modelsToTry as $model) {
            try {
                $response = $this->httpClient->request('POST', 'https://openrouter.ai/api/v1/chat/completions', [
                    'headers' => [
                        'Content-Type'  => 'application/json',
                        'Authorization' => 'Bearer ' . $this->openRouterApiKey,
                        'HTTP-Referer'  => 'https://mindaura.local',
                        'X-Title'       => 'MindAura PDF Export',
                    ],
                    'json' => [
                        'model'       => $model,
                        'messages'    => [['role' => 'user', 'content' => $prompt]],
                        'max_tokens'  => 400,
                        'temperature' => 0.6,
                    ],
                    'timeout' => 20,
                ]);

                $statusCode = $response->getStatusCode();
                $body       = json_decode($response->getContent(false), true);

                if ($statusCode === 200) {
                    $text = $body['choices'][0]['message']['content'] ?? null;
                    if (!empty(trim($text))) {
                        $this->logger->info('[PDF IA] Résumé généré', ['model' => $model]);
                        return nl2br(htmlspecialchars(trim($text), ENT_QUOTES, 'UTF-8'));
                    }
                }

                if (in_array($statusCode, [404, 503])) { continue; }

                $this->logger->error('[PDF IA] Erreur OpenRouter', ['model' => $model, 'status' => $statusCode]);
                break;

            } catch (\Exception $e) {
                $this->logger->error('[PDF IA] Exception', ['message' => $e->getMessage()]);
                continue;
            }
        }

        $this->logger->warning('[PDF IA] Fallback résumé statique');
        return $this->buildStaticSummary($reservation);
    }

    private function buildStaticSummary(ReservationLocal $reservation): string
    {
        $nomClient = htmlspecialchars(trim($reservation->getPrenomCl() . ' ' . $reservation->getNomCl()), ENT_QUOTES, 'UTF-8');
        $nomLocal  = htmlspecialchars($reservation->getLocal()?->getNomLocal() ?? 'Non spécifié', ENT_QUOTES, 'UTF-8');
        $nomSalle  = htmlspecialchars($reservation->getSalle()?->getNomSalle() ?? 'Non spécifié', ENT_QUOTES, 'UTF-8');
        $date      = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';
        $debut     = $reservation->getHeureDebutReservation()?->format('H:i') ?? '—';
        $fin       = $reservation->getHeureFinReservation()?->format('H:i') ?? '—';
        $statut    = htmlspecialchars($reservation->getStatusReservation() ?? '—', ENT_QUOTES, 'UTF-8');
        $motif     = htmlspecialchars($reservation->getMotifReservation() ?? 'Non précisé', ENT_QUOTES, 'UTF-8');

        return "La présente réservation concerne $nomClient pour le local $nomLocal, salle $nomSalle, "
             . "le $date de $debut à $fin.<br><br>"
             . "Motif de la consultation : $motif. Statut actuel : $statut.<br><br>"
             . "MindAura vous accompagne dans votre démarche de bien-être mental avec professionnalisme et bienveillance.";
    }

    private function buildPdfHtml(ReservationLocal $reservation, string $aiSummary): string
    {
        $id      = $reservation->getIdReservation();
        $client  = htmlspecialchars(trim($reservation->getPrenomCl() . ' ' . $reservation->getNomCl()), ENT_QUOTES, 'UTF-8');
        $local   = htmlspecialchars($reservation->getLocal()?->getNomLocal() ?? '—', ENT_QUOTES, 'UTF-8');
        $adresse = htmlspecialchars($reservation->getLocal()?->getAdresseLocal() ?? '—', ENT_QUOTES, 'UTF-8');
        $ville   = htmlspecialchars($reservation->getLocal()?->getVilleLocal() ?? '—', ENT_QUOTES, 'UTF-8');
        $salle   = htmlspecialchars($reservation->getSalle()?->getNomSalle() ?? '—', ENT_QUOTES, 'UTF-8');
        $date    = $reservation->getDateReservation()?->format('d/m/Y') ?? '—';
        $debut   = $reservation->getHeureDebutReservation()?->format('H:i') ?? '—';
        $fin     = $reservation->getHeureFinReservation()?->format('H:i') ?? '—';
        $statut  = htmlspecialchars($reservation->getStatusReservation() ?? '—', ENT_QUOTES, 'UTF-8');
        $motif   = htmlspecialchars($reservation->getMotifReservation() ?? 'Non précisé', ENT_QUOTES, 'UTF-8');
        $prix    = $reservation->getPrixReservation() ? number_format((float)$reservation->getPrixReservation(), 2, ',', ' ') . ' TND' : '—';
        $today   = date('d/m/Y à H:i');

        // Couleurs dynamiques selon statut
        [$statutBg, $statutColor, $statutBorder, $statutDot] = match($statut) {
            'Confirmée'  => ['#DCFCE7', '#166534', '#86EFAC', '#22C55E'],
            'En attente' => ['#FEF9C3', '#854D0E', '#FDE047', '#EAB308'],
            'Annulée'    => ['#FEE2E2', '#991B1B', '#FCA5A5', '#EF4444'],
            default      => ['#F3F4F6', '#374151', '#D1D5DB', '#9CA3AF'],
        };

        $logoB64 = '/9j/4AAQSkZJRgABAQAAAQABAAD/4gHYSUNDX1BST0ZJTEUAAQEAAAHIAAAAAAQwAABtbnRyUkdCIFhZWiAH4AABAAEAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAACRyWFlaAAABFAAAABRnWFlaAAABKAAAABRiWFlaAAABPAAAABR3dHB0AAABUAAAABRyVFJDAAABZAAAAChnVFJDAAABZAAAAChiVFJDAAABZAAAAChjcHJ0AAABjAAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAAgAAAAcAHMAUgBHAEJYWVogAAAAAAAAb6IAADj1AAADkFhZWiAAAAAAAABimQAAt4UAABjaWFlaIAAAAAAAACSgAAAPhAAAts9YWVogAAAAAAAA9tYAAQAAAADTLXBhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABtbHVjAAAAAAAAAAEAAAAMZW5VUwAAACAAAAAcAEcAbwBvAGcAbABlACAASQBuAGMALgAgADIAMAAxADb/2wBDAAUDBAQEAwUEBAQFBQUGBwwIBwcHBw8LCwkMEQ8SEhEPERETFhwXExQaFRERGCEYGh0dHx8fExciJCIeJBweHx7/2wBDAQUFBQcGBw4ICA4eFBEUHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh4eHh7/wAARCAH0AfQDASIAAhEBAxEB/8QAHQABAAEFAQEBAAAAAAAAAAAAAAMEBQYHCAIBCf/EAFAQAAEDAwEEBwQHBQUGBQMEAwEAAgMEBREGEiExQQcTIlFhcYEUMpGhCCNCUrHB0RUzYnKCQ1OS4fAWJDRzorJUY5PC8RclRCY1g9Kzw9P/xAAcAQEAAgMBAQEAAAAAAAAAAAAABAUCAwYBBwj/xABCEQABAwIDBAgFAgMGBgMBAAABAAIDBBEFITESQVFhBhMicYGRofAUMrHB0SPhQlKSFTNicqLxB1OCstLiFiQ0wv/aAAwDAQACEQMRAD8A4yRERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERTMpp3DaEZDfvO3D4lfephZ+8qAT3Rja+e4LMRu1svLhQKpgoamaLrY48t5b8ZXnradn7uDaPfI7PyGFdqKqcIupMbetbEZNlu4cdw88EKTTwRvdZ7vJYPcQMlYjuOCiqXMZUOL4TsyE5Mbjx8j+Sp3AtJDgQRxBUZzC3uWYN18REWC9REREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREREVTTUVTUN2o4+z94nAWTGOebNF14SBqqZFU1NFU07dqSPs/eByFTI9jmGzhZAQdERfQCTgAk9wUoppQMybMQ/jOPlxQNc7QJeyhRT7NMz3pXyHuYMD4n9E69jf3UEbfF3aPz3fJe7AGpS6jjikkOI43O8hlSez7P72aOPwztH4BeJJpZBh8jiO7O74KNe3YNBf373pmp80rODZJT4nZH5p7U9v7prIv5W7/id6gROscNMksvT3vecvc5x7ycryiLWTdeqalY0vL3jMcY2nePcPUqSkleap8me25rnZ8QM/kvM/wBVE2Ae8e1J58h6D8V8of8Ai4x947Px3KQ07L2tHFYnMEr5VtDZyWDDHdpvkV9bO2QBlQ0vHAPHvD9Ud9ZRg/aidg/ynh88/FQLBzi11xoUAuFLLA5jesYRJH95v59yiXuKR8TtpjsHn3HzUuIZ/dxDJ3E9l3keS82Q75deC901VOi9SMfG8se0tcORXlayLZFeoiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiKWkjEtTHGeDnAHyWUtAa0NaMADAAWJxPdHI2RvFpBCyimmbPCyRoI2hwKuMLc3tDetEwOS9uAc0t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PeQf1XnrojxpYvQuH5rRPWmQbLTYLQ2Oxuc1CqqljdVRmnGNtp2mE93MfmvHWU54059JCp6CqpqefbEUoyNk9sH8gosLWB4DnZb9fwsnE2yC911DNGyMbUYjY3GS7G/mqTqYx71TEPAAn8lX3W4RT04hhycnJJCtSzqTE2TsZheM2iM1Ns0w4yyO/lYPzKbVKOEUrvN4H5KFFH2+AHvvWdlN10Q92mj/AKi4/mrtbq+mFKGSObE5udwG70VjRboap8TtoLFzA4WVRcZm1FY+Vg7J3DxwqdEWh7i9xcd6yAsLIiIsV6iIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIvrcbQycDO9XGtqG07Y4aObLQzBcDv454q2otrJSwEDfvXhbdTe1TH3nB/87Q78U65h9+niPiMj8CoUWPWO3lLBT7VK7jHKzyeD+SudklhBfBHI92e0A5uMd/NWVS0spgqGSt+yfiFup6jq5A4hYubcWXS/0UdRilv1fpid+I65ntFOCf7Vg7QHiW7/AOhdN2uc09ax2cNcdl3kVwLpm8VNkvlBe6F311LK2Zm/c7HI+BG4+BXcdhudLerLR3ahft01XC2aM88EZwfEcD4hWFbHZ20NCuYxKGz9vcVqn6aGkrpVG26xpQ6ahpIPZKpgG+HLyWv8iXbJPIhveuVLzSGeISsGZGDh3hfpgIaW8WR9JXQR1FPURGGeKQZa9pGHAjxC4Y6bdA1PR9rSa24fJbajM1vnd9uIn3SfvNO4+h5hZUzmzRmF63YVV5dUdRotLoq+80vUT9YwfVyb/I81QKoljMTyx25dG03F0REWteqdu+hf/DIPmD+igU8O+lnHcGu+ePzX2JrYWCaUAk/u2Hn4nw/FbS3at3fdY3sjAKdokcMykZY0/Z8T+SgJJJJJJPElHuc95c4kuJySV8WLnXyGi9ARERYL1ERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERX2xVHWU5gce1Hw8l0v9FXVwkparR1ZL24s1NDk8Wk/WMHkTtY8Xdy5Ro53U9Q2VvI7x3hZxpa9VVivlDfLa8CelkbLGeThzafAgkHwJV3SvFRB1Z1HsKvrafrGFvu6/QGx1YglMMjsRv4E8isX+kToca26PKmOmh27rbgaqhIGXOIHajH8zd2O8N7l60pfKLUmnaK9292aeqjDwM72Hg5p8QQQfJNZ9Ilv0JYnV92mEgOW09MD9bM77rfDvJ3D4KG0Pa8bOq5VjZGygs+YFcHV8AqKV8fPGW+axggg4PFZ5qG4Mut9r7pHRw0TKuoknFPETsRBzidkZ5DKwm4OjdWyui3sLt35qRibBZr967KBxIsVAiIqhSFNSysiL9tm21zcYz4g/ko5XukeXvOSV5RZF5I2V5beiIixXqIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiK8WKr3eyvPiz9FZ19Y5zHh7ThwOQVup5jC8OCxc3aFlvXod6UKjQsFwoqmmkrqGdhkgha7GxPwByeDSNx48AR44frLU921TeZrxeqrrJXbmtG5kTOTWjkB/mclY3TXCnkpw+SRjHAdppPPwVruVe6pdsMy2IcvveaupamGIdYMyVCZTN6wuAzUl0uJmzDCSI+Z5u/yVtRFRyyuldtOU5rQ0WCIiLWvURERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERF/9k=';

        return <<<HTML
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8"/>
  <style>

    /* ══════════════════════════════════════════════
       RESET & BASE  —  compatible Dompdf
    ══════════════════════════════════════════════ */
    /* ══ PALETTE
       Vert primaire  : #1B4332
       Vert accent    : #2D6A4F
       Fond général   : #F0F4F8
       Violet badge   : #7B5EA7
       Texte principal: #1A1A2E
       Texte secondaire: #5A6475
    ══ */

    * { margin: 0; padding: 0; box-sizing: border-box; }

    body {
      font-family: DejaVu Sans, sans-serif;
      font-size: 11px;
      color: #1A1A2E;
      background: #F0F4F8;
      line-height: 1.5;
    }

    /* ══════════════════════════════════════════════
       BANDE DÉCORATIVE SUPÉRIEURE
    ══════════════════════════════════════════════ */
    .top-stripe {
      background: #1B4332;
      height: 6px;
      width: 100%;
    }

    /* ══════════════════════════════════════════════
       EN-TÊTE PRINCIPAL
    ══════════════════════════════════════════════ */
    .header-wrap {
      background: #1B4332;
      padding: 28px 36px 22px 36px;
    }

    .header-table {
      width: 100%;
      border-collapse: collapse;
    }

    .brand-logo {
      font-size: 28px;
      font-weight: 700;
      color: #ffffff;
      letter-spacing: -0.5px;
    }

    .brand-dot {
      color: #B7E4C7;
    }

    .brand-tagline {
      font-size: 9px;
      color: #95D5B2;
      letter-spacing: 2px;
      text-transform: uppercase;
      margin-top: 4px;
    }

    .doc-type-cell {
      text-align: right;
      vertical-align: top;
    }

    .doc-type-label {
      font-size: 8px;
      color: #95D5B2;
      text-transform: uppercase;
      letter-spacing: 2px;
      margin-bottom: 4px;
    }

    .doc-type-title {
      font-size: 16px;
      font-weight: 700;
      color: #ffffff;
    }

    .doc-id-pill {
      display: inline-block;
      background: #2D6A4F;
      color: #D8F3DC;
      font-size: 10px;
      font-weight: 700;
      padding: 3px 12px;
      border-radius: 20px;
      margin-top: 6px;
      letter-spacing: 1px;
    }

    /* ── Barre de méta-infos sous le header ── */
    .meta-bar {
      background: #2D6A4F;
      padding: 0 36px;
    }

    .meta-bar-table {
      width: 100%;
      border-collapse: collapse;
    }

    .meta-bar-table td {
      padding: 12px 0;
      border-right: 1px solid #40916C;
      padding-right: 20px;
      padding-left: 20px;
      vertical-align: middle;
    }

    .meta-bar-table td:first-child {
      padding-left: 0;
    }

    .meta-bar-table td:last-child {
      border-right: none;
      padding-right: 0;
    }

    .meta-label {
      font-size: 8px;
      color: #B7E4C7;
      text-transform: uppercase;
      letter-spacing: 1.5px;
      margin-bottom: 3px;
    }

    .meta-value {
      font-size: 11px;
      font-weight: 700;
      color: #ffffff;
    }

    .statut-pill {
      display: inline-block;
      background: $statutBg;
      color: $statutColor;
      border: 1px solid $statutBorder;
      font-size: 9px;
      font-weight: 700;
      padding: 3px 10px;
      border-radius: 20px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
    }

    .statut-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      background: $statutDot;
      border-radius: 50%;
      margin-right: 4px;
      vertical-align: middle;
    }

    .prix-value {
      font-size: 13px;
      font-weight: 700;
      color: #ffffff;
    }

    /* ══════════════════════════════════════════════
       CORPS PRINCIPAL
    ══════════════════════════════════════════════ */
    .body-wrap {
      padding: 28px 36px;
      background: #F0F4F8;
    }

    /* ── Titres de section ── */
    .section-header {
      margin-bottom: 14px;
      margin-top: 22px;
    }

    .section-header:first-child {
      margin-top: 0;
    }

    .section-line-table {
      width: 100%;
      border-collapse: collapse;
    }

    .section-line-table td {
      vertical-align: middle;
    }

    .section-icon-cell {
      width: 28px;
    }

    .section-icon {
      display: inline-block;
      width: 22px;
      height: 22px;
      background: #1B4332;
      border-radius: 6px;
      text-align: center;
      line-height: 22px;
      font-size: 11px;
      color: #ffffff;
      font-weight: 700;
    }

    .section-title-text {
      font-size: 11px;
      font-weight: 700;
      color: #1B4332;
      text-transform: uppercase;
      letter-spacing: 1.5px;
      padding-left: 8px;
    }

    .section-rule-cell {
      text-align: right;
    }

    .section-rule {
      display: inline-block;
      height: 1px;
      width: 100%;
      background: #B7E4C7;
    }

    /* ── Cartes d'information (2 colonnes via table) ── */
    .info-card-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 6px;
    }

    .info-card-table td {
      width: 50%;
      vertical-align: top;
      padding: 0;
    }

    .info-card-table td:first-child {
      padding-right: 8px;
    }

    .info-card-table td:last-child {
      padding-left: 8px;
    }

    .info-card {
      background: #ffffff;
      border: 1px solid #D8E8DF;
      border-radius: 10px;
      padding: 14px 16px;
      border-top: 3px solid #2D6A4F;
    }

    .info-card-icon {
      font-size: 16px;
      margin-bottom: 6px;
      color: #2D6A4F;
    }

    .info-card-label {
      font-size: 8px;
      font-weight: 700;
      color: #5A6475;
      text-transform: uppercase;
      letter-spacing: 1.2px;
      margin-bottom: 5px;
    }

    .info-card-value {
      font-size: 13px;
      font-weight: 700;
      color: #1A1A2E;
      margin-bottom: 3px;
    }

    .info-card-sub {
      font-size: 9.5px;
      color: #5A6475;
      line-height: 1.5;
    }

    /* ── Bloc horaires ── */
    .horaires-wrap {
      background: #ffffff;
      border: 1px solid #D8E8DF;
      border-radius: 10px;
      padding: 16px 20px;
      border-top: 3px solid #1B4332;
      margin-bottom: 6px;
    }

    .horaires-table {
      width: 100%;
      border-collapse: collapse;
    }

    .horaires-table td {
      vertical-align: middle;
      text-align: center;
    }

    .horaire-block-label {
      font-size: 8px;
      font-weight: 700;
      color: #5A6475;
      text-transform: uppercase;
      letter-spacing: 1.2px;
      margin-bottom: 5px;
    }

    .horaire-block-time {
      font-size: 26px;
      font-weight: 700;
      color: #1B4332;
      letter-spacing: -0.5px;
    }

    .horaire-block-sub {
      font-size: 9px;
      color: #5A6475;
      margin-top: 3px;
    }

    .horaire-arrow-cell {
      width: 60px;
    }

    .horaire-arrow {
      font-size: 20px;
      color: #2D6A4F;
      font-weight: 300;
    }

    .horaire-duration-badge {
      display: inline-block;
      background: #D8F3DC;
      color: #1B4332;
      border: 1px solid #B7E4C7;
      font-size: 9px;
      font-weight: 700;
      padding: 2px 10px;
      border-radius: 20px;
      margin-top: 6px;
    }

    /* ── Bloc motif ── */
    .motif-wrap {
      background: #ffffff;
      border: 1px solid #D8E8DF;
      border-radius: 10px;
      padding: 14px 16px;
      border-left: 4px solid #7B5EA7;
      margin-bottom: 6px;
    }

    .motif-label {
      font-size: 8px;
      font-weight: 700;
      color: #5A6475;
      text-transform: uppercase;
      letter-spacing: 1.2px;
      margin-bottom: 5px;
    }

    .motif-text {
      font-size: 12px;
      color: #1A1A2E;
      font-style: italic;
      line-height: 1.6;
    }

    /* ── Bloc résumé IA ── */
    .ai-wrap {
      background: #F5F0FC;
      border: 1px solid #D9CCF0;
      border-radius: 10px;
      padding: 18px 20px;
      border-top: 3px solid #7B5EA7;
      margin-bottom: 6px;
    }

    .ai-header-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 12px;
    }

    .ai-header-table td {
      vertical-align: middle;
    }

    .ai-badge {
      display: inline-block;
      background: #7B5EA7;
      color: #ffffff;
      font-size: 8px;
      font-weight: 700;
      padding: 3px 10px;
      border-radius: 20px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .ai-title {
      font-size: 11px;
      font-weight: 700;
      color: #5A4080;
      padding-left: 10px;
    }

    .ai-model-tag {
      font-size: 8px;
      color: #7B5EA7;
      text-align: right;
    }

    .ai-separator {
      height: 1px;
      background: #D9CCF0;
      margin-bottom: 12px;
    }

    .ai-content {
      font-size: 10.5px;
      line-height: 1.8;
      color: #1A1A2E;
      text-align: justify;
    }

    /* ── Bloc QR / numéro de référence ── */
    .ref-wrap {
      background: #ffffff;
      border: 1px solid #D8E8DF;
      border-radius: 10px;
      padding: 12px 16px;
      margin-top: 14px;
    }

    .ref-table {
      width: 100%;
      border-collapse: collapse;
    }

    .ref-table td {
      vertical-align: middle;
    }

    .ref-label {
      font-size: 8px;
      color: #5A6475;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 3px;
    }

    .ref-number {
      font-size: 13px;
      font-weight: 700;
      color: #1B4332;
      letter-spacing: 2px;
    }

    .ref-date {
      font-size: 9px;
      color: #5A6475;
      margin-top: 2px;
    }

    .ref-valid-badge {
      display: inline-block;
      background: #D8F3DC;
      color: #1B4332;
      border: 1px solid #B7E4C7;
      font-size: 8px;
      font-weight: 700;
      padding: 2px 10px;
      border-radius: 20px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
    }

    .ref-right-cell {
      text-align: right;
    }

    /* ══════════════════════════════════════════════
       PIED DE PAGE
    ══════════════════════════════════════════════ */
    .footer-stripe {
      background: #2D6A4F;
      height: 3px;
      width: 100%;
    }

    .footer-wrap {
      background: #1B4332;
      padding: 14px 36px;
    }

    .footer-table {
      width: 100%;
      border-collapse: collapse;
    }

    .footer-table td {
      vertical-align: middle;
    }

    .footer-brand {
      font-size: 12px;
      font-weight: 700;
      color: #ffffff;
    }

    .footer-tagline {
      font-size: 8px;
      color: #95D5B2;
      margin-top: 2px;
    }

    .footer-center {
      text-align: center;
    }

    .footer-confidential {
      font-size: 7px;
      color: #B7E4C7;
      text-transform: uppercase;
      letter-spacing: 1.5px;
      border: 1px solid #40916C;
      padding: 2px 10px;
      border-radius: 3px;
      display: inline-block;
    }

    .footer-right-cell {
      text-align: right;
    }

    .footer-meta {
      font-size: 8px;
      color: #95D5B2;
      line-height: 1.6;
    }

  </style>
</head>
<body>

<!-- ══ BANDE DÉCORATIVE ══ -->
<div class="top-stripe"></div>

<!-- ══ EN-TÊTE ══ -->
<div class="header-wrap">
  <table class="header-table">
    <tr>
      <td style="vertical-align:middle;">
        <table style="border-collapse:collapse;">
          <tr>
            <td style="vertical-align:middle; padding-right:14px;">
              <img src="data:image/jpeg;base64,$logoB64" width="60" height="60" style="border-radius:8px; display:block;"/>
            </td>
            <td style="vertical-align:middle;">
              <div class="brand-logo">Mind<span class="brand-dot">Aura</span></div>
              <div class="brand-tagline">Plateforme de Sante Mentale &amp; Bien-etre</div>
            </td>
          </tr>
        </table>
      </td>
      <td class="doc-type-cell">
        <div class="doc-type-label">Document officiel</div>
        <div class="doc-type-title">Confirmation de Reservation</div>
        <div><span class="doc-id-pill">REF #$id</span></div>
      </td>
    </tr>
  </table>
</div>

<!-- ── Barre de méta-infos ── -->
<div class="meta-bar">
  <table class="meta-bar-table">
    <tr>
      <td>
        <div class="meta-label">Client</div>
        <div class="meta-value">$client</div>
      </td>
      <td>
        <div class="meta-label">Date de reservation</div>
        <div class="meta-value">$date</div>
      </td>
      <td>
        <div class="meta-label">Statut</div>
        <div style="margin-top:2px;">
          <span class="statut-pill"><span class="statut-dot"></span>$statut</span>
        </div>
      </td>
      <td style="text-align:right;">
        <div class="meta-label">Montant total</div>
        <div class="prix-value">$prix</div>
      </td>
    </tr>
  </table>
</div>

<!-- ══ CORPS ══ -->
<div class="body-wrap">

  <!-- ── Section : Informations du local ── -->
  <div class="section-header">
    <table class="section-line-table">
      <tr>
        <td class="section-icon-cell"><span class="section-icon">L</span></td>
        <td><span class="section-title-text">Informations du local</span></td>
        <td class="section-rule-cell" style="padding-left:10px;">
          <table style="width:100%;"><tr><td><div style="height:1px;background:#B7E4C7;"></div></td></tr></table>
        </td>
      </tr>
    </table>
  </div>

  <table class="info-card-table">
    <tr>
      <td>
        <div class="info-card">
          <div class="info-card-label">Local psychiatrique</div>
          <div class="info-card-value">$local</div>
          <div class="info-card-sub">$adresse<br>$ville</div>
        </div>
      </td>
      <td>
        <div class="info-card">
          <div class="info-card-label">Salle de consultation</div>
          <div class="info-card-value">$salle</div>
          <div class="info-card-sub">Espace dedie aux consultations<br>individuelles et therapeutiques</div>
        </div>
      </td>
    </tr>
  </table>

  <!-- ── Section : Motif ── -->
  <div class="section-header" style="margin-top:16px;">
    <table class="section-line-table">
      <tr>
        <td class="section-icon-cell"><span class="section-icon" style="background:#7B5EA7;">M</span></td>
        <td><span class="section-title-text" style="color:#7B5EA7;">Motif de la consultation</span></td>
        <td class="section-rule-cell" style="padding-left:10px;">
          <table style="width:100%;"><tr><td><div style="height:1px;background:#D9CCF0;"></div></td></tr></table>
        </td>
      </tr>
    </table>
  </div>

  <div class="motif-wrap">
    <div class="motif-label">Raison de la visite</div>
    <div class="motif-text">&laquo; $motif &raquo;</div>
  </div>

  <!-- ── Section : Horaires ── -->
  <div class="section-header" style="margin-top:16px;">
    <table class="section-line-table">
      <tr>
        <td class="section-icon-cell"><span class="section-icon" style="background:#2D6A4F;">H</span></td>
        <td><span class="section-title-text" style="color:#2D6A4F;">Horaires de la seance</span></td>
        <td class="section-rule-cell" style="padding-left:10px;">
          <table style="width:100%;"><tr><td><div style="height:1px;background:#B7E4C7;"></div></td></tr></table>
        </td>
      </tr>
    </table>
  </div>

  <div class="horaires-wrap">
    <table class="horaires-table">
      <tr>
        <td>
          <div class="horaire-block-label">Heure de debut</div>
          <div class="horaire-block-time">$debut</div>
          <div class="horaire-block-sub">Debut de seance</div>
        </td>
        <td class="horaire-arrow-cell">
          <div class="horaire-arrow">&#8594;</div>
        </td>
        <td>
          <div class="horaire-block-label">Heure de fin</div>
          <div class="horaire-block-time">$fin</div>
          <div class="horaire-block-sub">Fin de seance</div>
        </td>
        <td style="width:140px; text-align:center; padding-left:20px; border-left:1px dashed #B7E4C7;">
          <div class="horaire-block-label">Date</div>
          <div style="font-size:14px; font-weight:700; color:#1B4332;">$date</div>
          <div><span class="horaire-duration-badge">Confirme</span></div>
        </td>
      </tr>
    </table>
  </div>

  <!-- ── Section : Résumé IA ── -->
  <div class="section-header" style="margin-top:16px;">
    <table class="section-line-table">
      <tr>
        <td class="section-icon-cell"><span class="section-icon" style="background:#7B5EA7;">IA</span></td>
        <td><span class="section-title-text" style="color:#7B5EA7;">Analyse intelligente</span></td>
        <td class="section-rule-cell" style="padding-left:10px;">
          <table style="width:100%;"><tr><td><div style="height:1px;background:#D9CCF0;"></div></td></tr></table>
        </td>
      </tr>
    </table>
  </div>

  <div class="ai-wrap">
    <table class="ai-header-table">
      <tr>
        <td>
          <span class="ai-badge">IA MindAura</span>
          <span class="ai-title">Resume genere par intelligence artificielle</span>
        </td>
        <td style="text-align:right;">
          <span class="ai-model-tag">Genere automatiquement</span>
        </td>
      </tr>
    </table>
    <div class="ai-separator"></div>
    <div class="ai-content">$aiSummary</div>
  </div>

  <!-- ── Bloc de référence ── -->
  <div class="ref-wrap">
    <table class="ref-table">
      <tr>
        <td>
          <div class="ref-label">Reference de reservation</div>
          <div class="ref-number">MIND-$id</div>
          <div class="ref-date">Emis le $today</div>
        </td>
        <td class="ref-right-cell">
          <span class="ref-valid-badge">&#10003; Document valide</span>
          <div style="font-size:8px; color:#95D5B2; margin-top:6px;">Ce document fait foi de reservation<br>aupres des equipes MindAura</div>
        </td>
      </tr>
    </table>
  </div>

</div>
<!-- fin body-wrap -->

<!-- ══ PIED DE PAGE ══ -->
<div class="footer-stripe"></div>
<div class="footer-wrap">
  <table class="footer-table">
    <tr>
      <td>
        <div class="footer-brand">MindAura</div>
        <div class="footer-tagline">Sante mentale &amp; Bien-etre — mindaura.tn</div>
      </td>
      <td class="footer-center">
        <span class="footer-confidential">Document Confidentiel</span>
      </td>
      <td class="footer-right-cell">
        <div class="footer-meta">
          Reservation #$id<br>
          Genere le $today
        </div>
      </td>
    </tr>
  </table>
</div>

</body>
</html>
HTML;
    }
}