<?php

namespace App\Service;

use App\Entity\ReponseClient;
use App\Entity\Utilisateurs;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Component\HttpFoundation\Response;
use Twig\Environment;

/**
 * Génère un export PDF des réponses d'un utilisateur.
 */
class ExportPdfService
{
    public function __construct(
        private readonly Environment $twig
    ) {}

    /**
     * @param ReponseClient[]         $reponses
     * @param array<string, mixed>    $filters
     */
    public function generateResponse(
        array $reponses,
        array $filters = [],
        string $template = 'site/tests/my_responses_export_pdf.html.twig'
    ): Response {
        $html = $this->twig->render($template, [
            'reponses'   => $reponses,
            'filters'    => $filters,
            'exportedAt' => new \DateTimeImmutable(),
        ]);

        $dompdf = $this->buildDompdf();
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'mes_reponses_' . date('Ymd_His') . '.pdf';

        return new Response(
            $dompdf->output(),
            Response::HTTP_OK,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="' . $filename . '"',
            ]
        );
    }

    // ─────────────────────────────────────────────────────────────────────────

    private function buildDompdf(): Dompdf
    {
        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        return new Dompdf($options);
    }
}