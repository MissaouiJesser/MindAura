<?php

namespace App\Controller;

use App\Repository\ReclamationRepository;
use App\Service\IdentitySafeInsertService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use App\Service\ReclamationPriorityService;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\Routing\Annotation\Route;
use App\Entity\Categorie;
use App\Entity\Reclamation;
use App\Entity\Reponse;
use App\Entity\Utilisateurs;
use App\Service\GroqServiceR;
use App\Service\ReclamationNotificationMailer;

#[Route('/reclamation', name: 'reclamation_')]
class ReclamationController extends AbstractController
{
    #[Route('/{id}/priority', name: 'priority', methods: ['GET'])]
    public function priority(
        Reclamation $reclamation,
        ReclamationPriorityService $priorityService
    ): JsonResponse {
        $texte = $reclamation->getSujet_reclamation() . ' ' . $reclamation->getDescription_reclamation();
        $prio = $priorityService->classifierPriorite((int) $reclamation->getId_reclamation(), $texte);

        return $this->json(['success' => true, 'priority' => $prio]);
    }

    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(Request $request, ReclamationRepository $repo, EntityManagerInterface $em, ReclamationPriorityService $priorityService): Response
    {
        $rawStatus = $request->query->all('statut');
        if ($rawStatus === []) {
            $scalarStatus = $request->query->get('statut', '');
            $rawStatus = $scalarStatus !== '' ? [$scalarStatus] : [];
        }
        $statusCodes = array_values(array_filter(array_map('strval', (array) $rawStatus)));

        $normalizedStatusFilters = [];
        foreach ($statusCodes as $code) {
            $mapped = $this->statusFromCodeForFilter($code);
            if ($mapped !== null) {
                $normalizedStatusFilters[] = $mapped;
            }
        }

        $params = [
            'search' => $request->query->get('search', ''),
            'statut' => $normalizedStatusFilters,
            'categorie' => $request->query->get('categorie', ''),
            'dateDebut' => $request->query->get('date_from', ''),
            'dateFin' => $request->query->get('date_to', ''),
            'sort' => $request->query->get('sort', ReclamationRepository::DEFAULT_SORT),
            'direction' => $request->query->get('direction', ReclamationRepository::DEFAULT_DIRECTION),
            'page' => $request->query->getInt('page', 1),
            'perPage' => $request->query->getInt('perPage', ReclamationRepository::PAGE_SIZE),
        ];

        $pagination = $repo->findFiltered($params);
        $categories = $em->getRepository(Categorie::class)->findAll();

        $priorites = [];
        foreach ($pagination['items'] as $reclamation) {
            $texte = $reclamation->getSujet_reclamation() . ' ' . $reclamation->getDescription_reclamation();
            $priorites[$reclamation->getId_reclamation()] = $priorityService->classifierPriorite((int) $reclamation->getId_reclamation(), $texte);
        }

        return $this->render('reclamation/index.html.twig', [
            'reclamations' => $pagination['items'],
            'categories' => $categories,
            'total' => $pagination['total'],
            'pages' => $pagination['pages'],
            'page' => $pagination['page'],
            'perPage' => $pagination['perPage'],
            'sortableColumns' => ReclamationRepository::SORTABLE_COLUMNS,
            'search' => $params['search'],
            'statut' => $statusCodes,
            'statusChoices' => [
                'EN_ATTENTE' => 'En attente',
                'EN_COURS' => 'En cours de traitement',
                'TRAITEE' => 'Résolu',
                'REJETEE' => 'Rejeté',
            ],
            'categorie_filter' => $params['categorie'],
            'date_from' => $params['dateDebut'],
            'date_to' => $params['dateFin'],
            'sort' => $params['sort'],
            'direction' => $params['direction'],
            'priorites' => $priorites,
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em, IdentitySafeInsertService $identityInsert): Response
    {
        $categories = $em->getRepository(Categorie::class)->findAll();

        if ($request->isMethod('POST')) {
            $categorieId = $request->request->getInt('categorie_id');
            $categorie = $em->getRepository(Categorie::class)->find($categorieId);
            $sujet = trim((string) $request->request->get('sujet_reclamation', ''));
            $description = trim((string) $request->request->get('description_reclamation', ''));
            $utilisateur = $request->request->getInt('id_utilisateur');
            $rate = $request->request->get('rate_Reclamation');
            $rateSum = $request->request->getInt('rate_sum');
            $rateCount = $request->request->getInt('rate_count');

            if ($categorie && $sujet !== '') {
                $reclamation = new Reclamation();
                $reclamation->setCategorie($categorie);
                $reclamation->setSujet_reclamation($sujet);
                $reclamation->setDescription_reclamation($description);
                // Date set automatically in __construct(), no manual call needed
                $reclamation->setStatut_reclamation('En attente');
                $reclamation->setId_utilisateur($utilisateur);
                $reclamation->setRate_Reclamation((float) $rate);
                $reclamation->setRate_sum($rateSum);
                $reclamation->setRate_count($rateCount);

                try {
                    $identityInsert->insertReclamation($em, $reclamation);
                    $this->addFlash('success', 'Réclamation ajoutée avec succès.');
                } catch (\Throwable $e) {
                    $this->addFlash('error', 'Enregistrement impossible : ' . $e->getMessage());
                    return $this->render('reclamation/new.html.twig', [
                        'categories' => $categories,
                    ]);
                }

                return $this->redirectToRoute('reclamation_index');
            }

            $this->addFlash('error', 'La catégorie et le sujet sont requis.');
        }

        return $this->render('reclamation/new.html.twig', [
            'categories' => $categories,
        ]);
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET', 'POST'])]
    public function show(Reclamation $reclamation, EntityManagerInterface $em, Request $request): Response
    {
        if ($request->isMethod('POST') && $request->request->get('action') === 'add_reponse') {
            $user = $this->getUser();
            if ($user instanceof Utilisateurs) {
                $reponse = new Reponse();
                $reponse->setReclamation($reclamation);
                $reponse->setContenu_reponse((string) $request->request->get('contenu_reponse', ''));
                // Date set automatically in __construct(), no manual call needed
                $reponse->setId_utilisateur($user->getIdUtilisateur());
                $reponse->setNom_utilisateur(trim($user->getPrenomUtilisateur().' '.$user->getNomUtilisateur()));
                $em->persist($reponse);
                $em->flush();
            }
            if ($request->isXmlHttpRequest()) {
                return $this->json(['success' => true]);
            }
            return $this->redirectToRoute('reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        if ($request->isMethod('POST') && $request->request->get('action') === 'edit_reponse') {
            $reponseId = $request->request->getInt('reponse_id');
            $reponse = $em->getRepository(Reponse::class)->find($reponseId);
            $user = $this->getUser();
            if ($reponse && $user instanceof Utilisateurs && $user->getIdUtilisateur() === $reponse->getId_utilisateur()) {
                $reponse->setContenu_reponse((string) $request->request->get('contenu_reponse', ''));
                $em->flush();
            }
            if ($request->isXmlHttpRequest()) {
                return $this->json(['success' => true]);
            }
            return $this->redirectToRoute('reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        $sortedReponses = $reclamation->getReponses()->toArray();
        usort($sortedReponses, static function (Reponse $a, Reponse $b) {
            return $a->getDate_reponse() <=> $b->getDate_reponse();
        });

        $responseUserIds = array_values(array_unique(array_filter(array_map(
            static fn (Reponse $r): ?int => $r->getId_utilisateur(),
            $sortedReponses
        ))));
        $adminUserIds = [];
        if ($responseUserIds !== []) {
            $adminRows = $em->getRepository(Utilisateurs::class)
                ->createQueryBuilder('u')
                ->select('u.idUtilisateur AS id')
                ->where('u.idUtilisateur IN (:ids)')
                ->andWhere('u.roleUtilisateur = :adminRole')
                ->setParameter('ids', $responseUserIds)
                ->setParameter('adminRole', 'ROLE_ADMIN')
                ->getQuery()
                ->getArrayResult();

            $adminUserIds = array_map(
                static fn (array $row): int => (int) $row['id'],
                $adminRows
            );
        }

        return $this->render('reclamation/show.html.twig', [
            'reclamation' => $reclamation,
            'sortedReponses' => $sortedReponses,
            'adminUserIds' => $adminUserIds,
        ]);
    }

    #[Route('/{id}/suggest-reponse', name: 'suggest_reponse', methods: ['POST'])]
    public function suggestReponse(Reclamation $reclamation, GroqServiceR $groqServiceR): Response
    {
        $suggestion = $groqServiceR->suggestReponse(
            $reclamation->getSujet_reclamation(),
            $reclamation->getDescription_reclamation()
        );

        return $this->json(['suggestion' => $suggestion]);
    }

    #[Route('/{id}/update-status', name: 'update_status', methods: ['POST'])]
    public function updateStatus(
        Request $request,
        Reclamation $reclamation,
        EntityManagerInterface $em,
        ReclamationNotificationMailer $reclamationMailer
    ): Response {
        $beforeCode = $reclamation->getStatutCode();
        $statut = trim((string) $request->request->get('statut_reclamation', ''));
        if ($statut === '') {
            $this->addFlash('error', 'Statut invalide.');
            return $this->redirectToRoute('reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        $reclamation->setStatut_reclamation($this->normalizeStatusCode($statut));
        $em->flush();
        $afterCode = $reclamation->getStatutCode();

        if ($afterCode === 'TRAITEE' && $beforeCode !== 'TRAITEE') {
            try {
                $submitter = $em->getRepository(Utilisateurs::class)->find($reclamation->getId_utilisateur());
                if ($submitter instanceof Utilisateurs) {
                    $reclamationMailer->notifyUserReclamationTreated($reclamation, $submitter);
                }
            } catch (\Throwable) {
            }
        }
        $this->addFlash('success', 'Statut mis à jour avec succès.');

        return $this->redirectToRoute('reclamation_show', ['id' => $reclamation->getId_reclamation()]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Reclamation $reclamation, EntityManagerInterface $em): Response
    {
        $categories = $em->getRepository(Categorie::class)->findAll();

        if ($request->isMethod('POST')) {
            $categorieId = $request->request->getInt('categorie_id');
            $categorie = $em->getRepository(Categorie::class)->find($categorieId);
            $sujet = trim((string) $request->request->get('sujet_reclamation', ''));
            $description = trim((string) $request->request->get('description_reclamation', ''));
            $statut = trim((string) $request->request->get('statut_reclamation', ''));
            $utilisateur = $request->request->getInt('id_utilisateur');
            $rate = $request->request->get('rate_Reclamation');
            $rateSum = $request->request->getInt('rate_sum');
            $rateCount = $request->request->getInt('rate_count');

            if ($categorie && $sujet !== '') {
                $reclamation->setCategorie($categorie);
                $reclamation->setSujet_reclamation($sujet);
                $reclamation->setDescription_reclamation($description);
                $reclamation->setStatut_reclamation(
                    $statut !== ''
                        ? $this->normalizeStatusCode($statut)
                        : $reclamation->getStatut_reclamation()
                );
                $reclamation->setId_utilisateur($utilisateur);
                $reclamation->setRate_Reclamation((float) $rate);
                $reclamation->setRate_sum($rateSum);
                $reclamation->setRate_count($rateCount);

                $em->flush();

                return $this->redirectToRoute('reclamation_index');
            }

            $this->addFlash('error', 'La catégorie et le sujet sont requis.');
        }

        return $this->render('reclamation/edit.html.twig', [
            'reclamation' => $reclamation,
            'categories' => $categories,
        ]);
    }

    #[Route('/export/pdf', name: 'export_pdf', methods: ['GET'])]
    public function exportPdf(Request $request, ReclamationRepository $repo): Response
    {
        $rawStatus = $request->query->all('statut');
        if ($rawStatus === []) {
            $scalarStatus = $request->query->get('statut', '');
            $rawStatus = $scalarStatus !== '' ? [$scalarStatus] : [];
        }
        $normalizedStatusFilters = [];
        foreach ((array) $rawStatus as $code) {
            $mapped = $this->statusFromCodeForFilter((string) $code);
            if ($mapped !== null) {
                $normalizedStatusFilters[] = $mapped;
            }
        }

        $params = [
            'search' => (string) $request->query->get('search', ''),
            'statut' => $normalizedStatusFilters,
            'categorie' => $request->query->get('categorie', ''),
            'dateDebut' => $request->query->get('date_from', ''),
            'dateFin' => $request->query->get('date_to', ''),
        ];

        $reclamations = $repo->findAllFiltered($params);
        $rows = $this->buildReclamationPdfRows($reclamations);

        $filters = [
            'search' => $this->sanitizeStringForPdf($params['search']),
            'statut' => implode(', ', $normalizedStatusFilters),
            'categorie' => $params['categorie'],
            'dateFrom' => $params['dateDebut'],
            'dateTo' => $params['dateFin'],
        ];

        $html = $this->renderView('reclamation/export_pdf.html.twig', [
            'rows' => $rows,
            'filters' => $filters,
            'exportedAt' => new \DateTime(),
        ]);

        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'reclamations_' . date('Ymd_His') . '.pdf';

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type' => 'application/pdf',
                'Content-Disposition' => "attachment; filename=\"{$filename}\"",
            ]
        );
    }

    #[Route('/export/excel', name: 'export_excel', methods: ['GET'])]
    public function exportExcel(Request $request, ReclamationRepository $repo): Response
    {
        $rawStatus = $request->query->all('statut');
        if ($rawStatus === []) {
            $scalarStatus = $request->query->get('statut', '');
            $rawStatus = $scalarStatus !== '' ? [$scalarStatus] : [];
        }
        $normalizedStatusFilters = [];
        foreach ((array) $rawStatus as $code) {
            $mapped = $this->statusFromCodeForFilter((string) $code);
            if ($mapped !== null) {
                $normalizedStatusFilters[] = $mapped;
            }
        }

        $params = [
            'search' => $request->query->get('search', ''),
            'statut' => $normalizedStatusFilters,
            'categorie' => $request->query->get('categorie', ''),
            'dateDebut' => $request->query->get('date_from', ''),
            'dateFin' => $request->query->get('date_to', ''),
        ];

        $reclamations = $repo->findAllFiltered($params);
        $filename = 'reclamations_' . date('Ymd_His') . '.csv';

        $response = new StreamedResponse(function () use ($reclamations) {
            $handle = fopen('php://output', 'w');
            if ($handle === false) {
                return;
            }
            fprintf($handle, chr(0xEF).chr(0xBB).chr(0xBF));
            fputcsv($handle, ['ID', 'Sujet', 'Catégorie', 'Statut', 'Date de création', 'Note'], ';');
            foreach ($reclamations as $r) {
                fputcsv($handle, [
                    $r->getId_reclamation(),
                    $r->getSujet_reclamation(),
                    $r->getCategorie() ? $r->getCategorie()->getNom_categorie() : '',
                    $r->getStatut_reclamation(),
                    $r->getDateCreation_reclamation()->format('d/m/Y H:i'),
                    $r->getRate_Reclamation(),
                ], ';');
            }
            fclose($handle);
        });

        $response->headers->set('Content-Type', 'text/csv; charset=utf-8');
        $response->headers->set('Content-Disposition', "attachment; filename=\"{$filename}\"");

        return $response;
    }

    /**
     * @param array<int, Reclamation> $reclamations
     * @return array<int, array<string, mixed>>
     */
    private function buildReclamationPdfRows(array $reclamations): array
    {
        return array_map(function (Reclamation $reclamation) {
            $statut = $reclamation->getStatut_reclamation();
            return [
                'id' => $reclamation->getId_reclamation(),
                'sujet' => $this->sanitizeStringForPdf($reclamation->getSujet_reclamation()),
                'categorie' => $reclamation->getCategorie()
                    ? $this->sanitizeStringForPdf($reclamation->getCategorie()->getNom_categorie())
                    : 'N/A',
                'statut' => $statut,
                'statut_class' => $this->reclamationStatutPdfClass($statut),
                'date' => $reclamation->getDateCreation_reclamation()->format('d/m/Y H:i'),
                'note' => number_format($reclamation->getRate_Reclamation(), 1, ',', ' '),
            ];
        }, $reclamations);
    }

    private function reclamationStatutPdfClass(string $statut): string
    {
        return match (trim($statut)) {
            'En attente' => 'status-attente',
            'En cours de traitement' => 'status-cours',
            'Résolu' => 'status-resolu',
            'Rejeté' => 'status-rejete',
            default => 'status-defaut',
        };
    }

    private function sanitizeStringForPdf(?string $string): string
    {
        if ($string === null) {
            return '';
        }
        if (!mb_check_encoding($string, 'UTF-8')) {
            $string = (string) mb_convert_encoding($string, 'UTF-8', 'auto');
        }
        $clean = preg_replace('/\p{C}/u', '', $string) ?? '';
        $clean = preg_replace('/\s+/', ' ', $clean) ?? '';
        return trim($clean);
    }

    private function normalizeStatusCode(string $status): string
    {
        return match (trim($status)) {
            'EN_ATTENTE', 'En attente' => 'EN_ATTENTE',
            'EN_COURS', 'En cours', 'En cours de traitement' => 'EN_COURS',
            'TRAITEE', 'Traitée', 'Résolu' => 'TRAITEE',
            'REJETEE', 'Rejetée', 'Rejeté' => 'REJETEE',
            default => 'EN_ATTENTE',
        };
    }

    private function statusFromCodeForFilter(string $status): ?string
    {
        return match (trim($status)) {
            'EN_ATTENTE', 'En attente' => 'EN_ATTENTE',
            'EN_COURS', 'En cours', 'En cours de traitement' => 'EN_COURS',
            'TRAITEE', 'Traitée', 'Résolu' => 'TRAITEE',
            'REJETEE', 'Rejetée', 'Rejeté' => 'REJETEE',
            default => null,
        };
    }
}