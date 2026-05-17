<?php

namespace App\Controller;

use App\Repository\ReponseClientRepository;
use App\Repository\TestPsychologiqueRepository;
use Dompdf\Dompdf;
use Dompdf\Options;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
#[Route('/admin/reponses', name: 'admin_reponses_')]
class ReponseClientAdminController extends AbstractController
{
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(
        Request $request,
        ReponseClientRepository $repo,
        TestPsychologiqueRepository $testRepo,
    ): Response
    {
        $q = (string) $request->query->get('q', '');
        $testId = $request->query->getInt('testId', 0);

        $result = $repo->searchForAdmin([
            'q'       => $q,
            'testId'  => $testId,
            'page'    => $request->query->getInt('page', 1),
            'perPage' => 5,
        ]);

        $exportQuery = [];
        if ($q !== '') {
            $exportQuery['q'] = $q;
        }
        if ($testId > 0) {
            $exportQuery['testId'] = $testId;
        }

        return $this->render('admin/reponses/index.html.twig', [
            'reponses' => $result['items'],
            'total'    => $result['total'],
            'page'     => $result['page'],
            'pages'    => $result['pages'],
            'q'        => $q,
            'testId'   => $testId,
            'tests'    => $testRepo->findBy([], ['titre_test' => 'ASC']),
            'exportQuery' => $exportQuery,
        ]);
    }

    #[Route('/export/pdf', name: 'export_pdf', methods: ['GET'])]
    public function exportPdf(Request $request, ReponseClientRepository $repo): Response
    {
        $filters = [
            'q'      => $request->query->get('q', ''),
            'testId' => $request->query->getInt('testId', 0),
        ];
        $rows = $repo->findAllForAdminExport($filters);

        $html = $this->renderView('admin/reponses/export_pdf.html.twig', [
            'reponses'   => $rows,
            'filters'    => $filters,
            'exportedAt' => new \DateTimeImmutable(),
        ]);

        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'reponses_utilisateurs_' . date('Ymd_His') . '.pdf';

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => 'attachment; filename="' . $filename . '"',
            ]
        );
    }

   
}
