<?php
namespace App\Service;

use App\Entity\Evenement;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Component\HttpFoundation\Response;
use Twig\Environment;

class EvenementPdfExporter
{
    public function __construct(
        private Environment $twig
    ) {}

    /**
     * @param Evenement[] $evenements
     */
    public function export(array $evenements): Response
    {
        // --- Desactiver temporairement la notice iconv ---
        // Dompdf appelle mb_convert_encoding() en interne, lequel passe par le
        // polyfill symfony/polyfill-mbstring qui appelle iconv() et genere une
        // Notice sur les caracteres multi-octets. On la masque le temps du rendu.
        $previousLevel = error_reporting(error_reporting() & ~E_NOTICE);

        try {
            // 1. Rendu HTML via Twig
            $html = $this->twig->render('pdf/evenements_list.html.twig', [
                'evenements'  => $evenements,
                'generatedAt' => new \DateTime(),
            ]);

            // 2. Convertir tous les caracteres non-ASCII en entites HTML numeriques.
            //    Apres cette etape le HTML est 100% ASCII => iconv ne verra jamais
            //    de caractere multi-octet, meme si Dompdf l'appelle en interne.
            $html = $this->encodeNonAscii($html);

            // 3. Configuration DomPDF
            $options = new Options();
            $options->set('defaultFont', 'DejaVu Sans');
            $options->set('isRemoteEnabled', false);
            $options->set('isHtml5ParserEnabled', true);
            $options->set('isFontSubsettingEnabled', true);

            $dompdf = new Dompdf($options);
            $dompdf->setPaper('A4', 'landscape');
            $dompdf->loadHtml($html, 'UTF-8');
            $dompdf->render();

            $output = $dompdf->output();

        } finally {
            // Restaurer le niveau d'erreur dans tous les cas
            error_reporting($previousLevel);
        }

        $filename = 'evenements_' . (new \DateTime())->format('Y-m-d_His') . '.pdf';

        return new Response(
            $output,
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="' . $filename . '"',
            ]
        );
    }

    /**
     * Convertit tous les caracteres dont le codepoint > 127 en entites &#N;
     * en decodant manuellement les sequences UTF-8 octet par octet.
     * N'utilise ni iconv ni mb_*.
     *
     * Les balises HTML (<...>) sont preservees intactes.
     */
    private function encodeNonAscii(string $html): string
    {
        $out   = '';
        $len   = strlen($html);
        $inTag = false;

        for ($i = 0; $i < $len; $i++) {
            $byte = ord($html[$i]);

            // Detection ouverture/fermeture de balise
            if ($html[$i] === '<') {
                $inTag = true;
                $out  .= '<';
                continue;
            }
            if ($html[$i] === '>') {
                $inTag = false;
                $out  .= '>';
                continue;
            }

            // Dans une balise => copier tel quel (attributs, classes, etc.)
            if ($inTag) {
                $out .= $html[$i];
                continue;
            }

            // Caractere ASCII simple
            if ($byte < 0x80) {
                $out .= $html[$i];
                continue;
            }

            // --- Sequence UTF-8 multi-octets ---
            // Determiner le nombre d'octets de la sequence
            if (($byte & 0xE0) === 0xC0) {
                // 2 octets : 110xxxxx 10xxxxxx
                $seqLen = 2;
                $cp     = $byte & 0x1F;
            } elseif (($byte & 0xF0) === 0xE0) {
                // 3 octets : 1110xxxx 10xxxxxx 10xxxxxx
                $seqLen = 3;
                $cp     = $byte & 0x0F;
            } elseif (($byte & 0xF8) === 0xF0) {
                // 4 octets : 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                $seqLen = 4;
                $cp     = $byte & 0x07;
            } else {
                // Octet de continuation isole ou invalide => ignorer
                continue;
            }

            // Lire les octets de continuation
            $valid = true;
            for ($j = 1; $j < $seqLen; $j++) {
                if ($i + $j >= $len) {
                    $valid = false;
                    break;
                }
                $next = ord($html[$i + $j]);
                if (($next & 0xC0) !== 0x80) {
                    $valid = false;
                    break;
                }
                $cp = ($cp << 6) | ($next & 0x3F);
            }

            if ($valid) {
                $out .= '&#' . $cp . ';';
                $i   += $seqLen - 1;
            }
            // Si invalide, on saute l'octet silencieusement
        }

        return $out;
    }
}