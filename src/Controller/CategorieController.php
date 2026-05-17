<?php

namespace App\Controller;

use App\Entity\Categorie;
use App\Form\CategorieType;
use App\Repository\CategorieRepository;
use App\Service\IdentitySafeInsertService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/categorie', name: 'categorie_')]
class CategorieController extends AbstractController
{
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(Request $request, CategorieRepository $repo): Response
    {
        $params = [
            'search'    => $request->query->get('search', ''),
            'sort'      => $request->query->get('sort', CategorieRepository::DEFAULT_SORT),
            'direction' => $request->query->get('direction', CategorieRepository::DEFAULT_DIRECTION),
            'page'      => $request->query->getInt('page', 1),
            'perPage'   => $request->query->getInt('perPage', CategorieRepository::PAGE_SIZE),
        ];

        $pagination = $repo->findFiltered($params);

        return $this->render('categorie/index.html.twig', [
            'categories'      => $pagination['items'],
            'total'           => $pagination['total'],
            'pages'           => $pagination['pages'],
            'page'            => $pagination['page'],
            'perPage'         => $pagination['perPage'],
            'sortableColumns' => CategorieRepository::SORTABLE_COLUMNS,
            'search'          => $params['search'],
            'sort'            => $params['sort'],
            'direction'       => $params['direction'],
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, IdentitySafeInsertService $identityInsert): Response
    {
        // La date_creation est initialisée dans le constructeur de Categorie.
        $categorie = new Categorie();

        $form = $this->createForm(CategorieType::class, $categorie);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            try {
                $identityInsert->insertCategorie($em, $categorie);
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Enregistrement impossible : ' . $e->getMessage());

                return $this->render('categorie/new.html.twig', [
                    'form' => $form,
                ]);
            }

            return $this->redirectToRoute('categorie_index');
        }

        return $this->render('categorie/new.html.twig', [
            'form' => $form,
        ]);
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET'])]
    public function show(Categorie $categorie): Response
    {
        return $this->render('categorie/show.html.twig', [
            'categorie' => $categorie,
        ]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Categorie $categorie, EntityManagerInterface $em): Response
    {
        if ($request->isMethod('POST')) {
            $nom         = trim((string) $request->request->get('nom_categorie', ''));
            $description = trim((string) $request->request->get('description', ''));

            if ($nom !== '') {
                $categorie->setNom_categorie($nom);
                $categorie->setDescription($description);
                $em->flush();

                return $this->redirectToRoute('categorie_index');
            }

            $this->addFlash('error', 'Le nom de la catégorie est requis.');
        }

        return $this->render('categorie/edit.html.twig', [
            'categorie' => $categorie,
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(Request $request, Categorie $categorie, EntityManagerInterface $em): Response
    {
        $em->remove($categorie);
        $em->flush();

        return $this->redirectToRoute('categorie_index');
    }

    #[Route('/export/pdf', name: 'export_pdf', methods: ['GET'])]
    public function exportPdf(Request $request, CategorieRepository $repo): Response
    {
        $params = [
            'search' => (string) $request->query->get('search', ''),
        ];

        $categories = $repo->findAllFiltered($params);
        $rows       = $this->buildCategoriePdfRows($categories);

        $filters = [
            'search' => $this->sanitizeStringForPdf($params['search']),
        ];

        $html = $this->renderView('categorie/export_pdf.html.twig', [
            'rows'       => $rows,
            'filters'    => $filters,
            'exportedAt' => new \DateTime(),
        ]);

        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        $filename = 'categories_' . date('Ymd_His') . '.pdf';

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => "attachment; filename=\"{$filename}\"",
            ]
        );
    }

    #[Route('/export/excel', name: 'export_excel', methods: ['GET'])]
    public function exportExcel(Request $request, CategorieRepository $repo): Response
    {
        $params = [
            'search' => $request->query->get('search', ''),
        ];

        $categories = $repo->findAllFiltered($params);
        $filename   = 'categories_' . date('Ymd_His') . '.csv';

        $response = new StreamedResponse(function () use ($categories) {
            $handle = fopen('php://output', 'w');
            if ($handle === false) {
                return;
            }

            // UTF-8 BOM for Excel
            fprintf($handle, chr(0xEF) . chr(0xBB) . chr(0xBF));

            fputcsv($handle, ['ID', 'Nom de la catégorie', 'Description', 'Date de création'], ';');

            foreach ($categories as $categorie) {
                fputcsv($handle, [
                    $categorie->getId_categorie(),
                    $categorie->getNom_categorie(),
                    $categorie->getDescription(),
                    $categorie->getDate_creation()->format('d/m/Y H:i'),
                ], ';');
            }

            fclose($handle);
        });

        $response->headers->set('Content-Type', 'text/csv; charset=utf-8');
        $response->headers->set('Content-Disposition', "attachment; filename=\"{$filename}\"");

        return $response;
    }

    /**
     * @param Categorie[] $categories
     * @return array<int, array{id: int|null, nom: string, description: string, date: string}>
     */
    private function buildCategoriePdfRows(array $categories): array
    {
        return array_map(function (Categorie $categorie) {
            return [
                'id'          => $categorie->getId_categorie(),
                'nom'         => $this->sanitizeStringForPdf($categorie->getNom_categorie()),
                'description' => $this->sanitizeStringForPdf($categorie->getDescription()),
                'date'        => $categorie->getDate_creation()->format('d/m/Y H:i'),
            ];
        }, $categories);
    }

    private function sanitizeStringForPdf(?string $string): string
    {
        if ($string === null) {
            return '';
        }

        if (!mb_check_encoding($string, 'UTF-8')) {
            $string = (string) mb_convert_encoding($string, 'UTF-8', 'auto');
        }

        $string = preg_replace('/\p{C}/u', '', $string) ?? '';
        $string = preg_replace('/\s+/', ' ', $string) ?? '';

        return trim($string);
    }
}