<?php

namespace App\Service;

use App\Entity\ReponseClient;
use Symfony\Component\HttpFoundation\StreamedResponse;

/**
 * Génère un export CSV des réponses d'un utilisateur.
 * Aucune dépendance externe — s'ouvre directement dans Excel.
 */
class ExportExcelService
{
    private const HEADERS = ['ID', 'Test', 'Question', 'Option / texte', 'Score', 'Date réponse'];

    /**
     * @param ReponseClient[] $reponses
     */
    public function generateResponse(array $reponses): StreamedResponse
    {
        $filename = 'mes_reponses_' . date('Ymd_His') . '.csv';

        return new StreamedResponse(
            function () use ($reponses): void {
                $handle = fopen('php://output', 'w');

                // ✅ Vérification du retour de fopen()
                if ($handle === false) {
                    throw new \RuntimeException('Impossible d\'ouvrir le flux de sortie.');
                }

                // BOM UTF-8 pour qu'Excel détecte bien l'encodage
                fwrite($handle, "\xEF\xBB\xBF");

                // En-têtes
                fputcsv($handle, self::HEADERS, ';');

                // Données
                foreach ($reponses as $rep) {
                    $test   = $rep->getTestPsychologique();
                    $qst    = $rep->getQuestionReponse();
                    $valeur = $rep->getReponseTexteLibre()
                        ? (string) $rep->getReponseTexteLibre()
                        : (string) ($rep->getOptionChoisie() ?? '');

                    fputcsv($handle, [
                        $rep->getIdReponseClient(),
                        $test ? $test->getTitre_test() : '',
                        $qst  ? $qst->getTexte_question() : '',
                        $valeur,
                        $rep->getScoreObtenu() ?? '',
                        $rep->getDateReponse()
                            ? $rep->getDateReponse()->format('Y-m-d H:i:s')
                            : '',
                    ], ';');
                }

                fclose($handle);
            },
            StreamedResponse::HTTP_OK,
            [
                'Content-Type'        => 'text/csv; charset=UTF-8',
                'Content-Disposition' => 'attachment; filename="' . $filename . '"',
                'Cache-Control'       => 'max-age=0',
            ]
        );
    }
}