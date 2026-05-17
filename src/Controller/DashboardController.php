<?php

namespace App\Controller;

use App\Repository\LocalsPsychiatrieRepository;
use App\Repository\SalleRepository;
use App\Repository\ActivityLogRepository;
use App\Repository\UtilisateursRepository;
use App\Repository\RessourcesRepository;
use App\Repository\CommentairesRepository;
use App\Repository\EvenementRepository;
use App\Repository\TypeEvenementRepository;
use App\Repository\ProduitRepository;
use App\Service\EvenementStatutUpdater;
use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

class DashboardController extends AbstractController
{
    #[Route('/home', name: 'app_home')]
    public function home(): Response
    {
        if ($this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_dashboard');
        }
        return $this->render('site/home.html.twig');
    }

    #[Route('/admin/dashboard', name: 'app_dashboard')]
    #[IsGranted('ROLE_ADMIN')]
    public function index(
        Request $request,
        UtilisateursRepository $repo,
        ActivityLogRepository  $logRepo,
        LocalsPsychiatrieRepository $locauxRepo,
        SalleRepository $salleRepo,
        Connection $db,
        RessourcesRepository $ressourceRepo,
        CommentairesRepository $commentaireRepo,
        EvenementRepository $evenementRepo,
        TypeEvenementRepository $typeRepo,
        EvenementStatutUpdater $statutUpdater,
        ProduitRepository $produitRepo
    ): Response {
        // ── Mise à jour des statuts des événements ─────────────────────────────
        $session = $request->getSession();
        $today   = (new \DateTime())->format('Y-m-d');

        if ($session->get('statut_last_update') !== $today) {
            $statutUpdater->updateAll();
            $session->set('statut_last_update', $today);
        }

        // ── Statistiques utilisateurs ──────────────────────────────────────────
        $countByRole = $repo->countByRole();
        $roleMap = [];
        foreach ($countByRole as $row) {
            $roleMap[$row['role']] = (int) $row['total'];
        }

        $totalUsers      = array_sum($roleMap);
        $countPatients   = $roleMap['ROLE_PATIENT']     ?? 0;
        $countPsychos    = $roleMap['ROLE_PSYCHOLOGUE'] ?? 0;
        $countCoachs     = $roleMap['ROLE_COACH']       ?? 0;
        $countAdmins     = $roleMap['ROLE_ADMIN']       ?? 0;
        $countActifs     = count($repo->findActifs());
        $countInactifs   = $totalUsers - $countActifs;

        $debutMois = new \DateTime('first day of this month midnight');
        $newThisMonth = $repo->findFiltered([
            'dateDebut' => $debutMois->format('Y-m-d'),
            'perPage'   => 1,
        ])['total'];

        $debutSemaine = new \DateTime('-7 days midnight');
        $newThisWeek  = $repo->findFiltered([
            'dateDebut' => $debutSemaine->format('Y-m-d'),
            'perPage'   => 1,
        ])['total'];

        $registrationsByDayRaw = $repo->countByDayLastWeek();
        $registrationsByDay = array_map(function($r) {
            return [
                'day'   => (new \DateTime($r['day']))->format('d/m'),
                'total' => (int) $r['total'],
            ];
        }, $registrationsByDayRaw);

        $recentUsers = $repo->findFiltered([
            'sort'      => 'inscription',
            'direction' => 'DESC',
            'perPage'   => 5,
        ])['items'];

        $pctPatients  = $totalUsers > 0 ? round($countPatients  / $totalUsers * 100) : 0;
        $pctPsychos   = $totalUsers > 0 ? round($countPsychos   / $totalUsers * 100) : 0;
        $pctCoachs    = $totalUsers > 0 ? round($countCoachs    / $totalUsers * 100) : 0;
        $pctAdmins    = $totalUsers > 0 ? round($countAdmins    / $totalUsers * 100) : 0;

        // ── Statistiques Locaux ─────────────────────────────────────────────
        $allLocaux              = $locauxRepo->findAll();
        $totalLocaux            = count($allLocaux);
        $countLocauxDisponibles   = 0;
        $countLocauxIndisponibles = 0;
        $locauxParType            = [];

        foreach ($allLocaux as $local) {
            if ($local->getDisponibiliteLocal() === 'Disponible') {
                $countLocauxDisponibles++;
            } else {
                $countLocauxIndisponibles++;
            }
            $type = $local->getTypeLocal() ?? 'Autre';
            $locauxParType[$type] = ($locauxParType[$type] ?? 0) + 1;
        }

        $pctLocauxDispo = $totalLocaux > 0 ? round($countLocauxDisponibles / $totalLocaux * 100) : 0;
        arsort($locauxParType);
        $locauxParType = array_slice($locauxParType, 0, 5, true);

        // ── Statistiques Salles ─────────────────────────────────────────────
        $allSalles             = $salleRepo->findAll();
        $totalSalles           = count($allSalles);
        $countSallesDisponibles   = 0;
        $countSallesIndisponibles = 0;
        $countSallesMaintenance   = 0;
        $sallesParType            = [];

        foreach ($allSalles as $salle) {
            match ($salle->getDisponibiliteSalle()) {
                'Disponible'     => $countSallesDisponibles++,
                'En maintenance' => $countSallesMaintenance++,
                default          => $countSallesIndisponibles++,
            };
            $type = $salle->getTypeSalle() ?? 'Autre';
            $sallesParType[$type] = ($sallesParType[$type] ?? 0) + 1;
        }

        $pctSallesDispo = $totalSalles > 0 ? round($countSallesDisponibles / $totalSalles * 100) : 0;
        arsort($sallesParType);
        $sallesParType = array_slice($sallesParType, 0, 5, true);

        // ── Réclamations ────────────────────────────────────────────────────
        $statusOrder = ['En attente', 'En cours de traitement', 'Résolu', 'Rejeté'];
        $reclamationByStatus = array_fill_keys($statusOrder, 0);
        try {
            $rows = $db->fetchAllAssociative(
                'SELECT statut_reclamation AS s, COUNT(*) AS c FROM reclamation GROUP BY statut_reclamation'
            );
            foreach ($rows as $row) {
                $s = match (trim((string) $row['s'])) {
                    'EN_ATTENTE', 'En attente', '' => 'En attente',
                    'EN_COURS', 'En cours', 'En cours de traitement' => 'En cours de traitement',
                    'TRAITEE', 'Traitée', 'Résolu' => 'Résolu',
                    'REJETEE', 'Rejetée', 'Rejeté' => 'Rejeté',
                    default => (string) $row['s'],
                };
                if (array_key_exists($s, $reclamationByStatus)) {
                    $reclamationByStatus[$s] = (int) $row['c'];
                }
            }
        } catch (\Throwable) {
            $reclamationByStatus = array_fill_keys($statusOrder, 0);
        }
        $reclamationTotal = array_sum($reclamationByStatus);

        // :170 — 'Résolu' est garanti présent par array_fill_keys($statusOrder, 0) : pas de ??
        $reclamationResolues = $reclamationByStatus['Résolu'];
        $reclamationTauxResolution = $reclamationTotal > 0 ? round($reclamationResolues / $reclamationTotal * 100) : 0;

        $reclamationMonthlyRaw = [];
        try {
            $fromRecl = (new \DateTimeImmutable('first day of this month midnight'))->modify('-5 months');
            $rows = $db->fetchAllAssociative(
                'SELECT DATE_FORMAT(dateCreation_reclamation, \'%Y-%m\') AS ym, COUNT(*) AS c
                 FROM reclamation
                 WHERE dateCreation_reclamation >= :from
                 GROUP BY ym ORDER BY ym',
                ['from' => $fromRecl->format('Y-m-d H:i:s')]
            );
            foreach ($rows as $row) {
                $reclamationMonthlyRaw[(string) $row['ym']] = (int) $row['c'];
            }
        } catch (\Throwable) {
            $reclamationMonthlyRaw = [];
        }
        $reclamationMonthly = $this->buildLastMonthsSeries(6, $reclamationMonthlyRaw);

        // ── Tests psychologiques ───────────────────────────────────────────
        $testCatalogue = $testActifs = $reponsesTotal = 0;
        $reponsesByType = ['personnalité' => 0, 'stress' => 0, 'logique' => 0, 'mémoire' => 0, 'autre' => 0];
        $reponsesMonthlyRaw = [];
        try {
            $testCatalogue = (int) $db->fetchOne('SELECT COUNT(*) FROM test_psychologique');
            $testActifs = (int) $db->fetchOne('SELECT COUNT(*) FROM test_psychologique WHERE est_actif = 1');
            $reponsesTotal = (int) $db->fetchOne('SELECT COUNT(*) FROM reponse_client');
            $rows = $db->fetchAllAssociative(
                'SELECT t.type_test AS tp, COUNT(rc.id_reponse_client) AS c
                 FROM reponse_client rc
                 INNER JOIN test_psychologique t ON t.id_test = rc.id_test
                 GROUP BY t.type_test'
            );
            foreach ($rows as $row) {
                $tp = (string) $row['tp'];
                if (isset($reponsesByType[$tp])) {
                    $reponsesByType[$tp] = (int) $row['c'];
                } else {
                    $reponsesByType['autre'] += (int) $row['c'];
                }
            }
            $fromRep = (new \DateTimeImmutable('first day of this month midnight'))->modify('-5 months');
            $rows = $db->fetchAllAssociative(
                'SELECT DATE_FORMAT(rc.date_reponse, \'%Y-%m\') AS ym, COUNT(*) AS c
                 FROM reponse_client rc
                 WHERE rc.date_reponse >= :from
                 GROUP BY ym ORDER BY ym',
                ['from' => $fromRep->format('Y-m-d H:i:s')]
            );
            foreach ($rows as $row) {
                $reponsesMonthlyRaw[(string) $row['ym']] = (int) $row['c'];
            }
        } catch (\Throwable) {
        }
        $reponsesMonthly = $this->buildLastMonthsSeries(6, $reponsesMonthlyRaw);

        // ── Objectifs ──────────────────────────────────────────────────────
        $objectifTotal = 0;
        $objectifBySource = ['admin' => 0, 'patient' => 0];
        $objectifByStatut = ['actif' => 0, 'inactif' => 0];
        try {
            $objectifTotal = (int) $db->fetchOne('SELECT COUNT(*) FROM objectif');
            $rows = $db->fetchAllAssociative('SELECT source, COUNT(*) AS c FROM objectif GROUP BY source');
            foreach ($rows as $row) {
                $src = (string) $row['source'];
                if (isset($objectifBySource[$src])) {
                    $objectifBySource[$src] = (int) $row['c'];
                }
            }
            $rows = $db->fetchAllAssociative('SELECT statut, COUNT(*) AS c FROM objectif GROUP BY statut');
            foreach ($rows as $row) {
                $st = (string) $row['statut'];
                if (isset($objectifByStatut[$st])) {
                    $objectifByStatut[$st] = (int) $row['c'];
                }
            }
        } catch (\Throwable) {
        }

        // :250 — 'patient' est garanti présent par l'initialisation : pas de ??
        $objectifAdoptesRatio = $objectifTotal > 0 ? round($objectifBySource['patient'] / $objectifTotal * 100) : 0;

        $reponsesTypeOrder = ['personnalité', 'stress', 'logique', 'mémoire', 'autre'];
        $reponsesTypeLabelsFr = [
            'personnalité' => 'Personnalité',
            'stress'       => 'Stress',
            'logique'      => 'Logique',
            'mémoire'      => 'Mémoire',
            'autre'        => 'Autre',
        ];
        $reponsesTypeLabels = [];
        $reponsesTypeCounts = [];
        foreach ($reponsesTypeOrder as $k) {
            $reponsesTypeLabels[] = $reponsesTypeLabelsFr[$k];

            // :264 — toutes les clés de $reponsesTypeOrder existent dans $reponsesByType : pas de ??
            $reponsesTypeCounts[] = $reponsesByType[$k];
        }

        // ── Statistiques Ressources ─────────────────────────────────────────
        $totalRessources = $ressourceRepo->count([]);
        $byType      = $ressourceRepo->countByType();
        $byCategorie = $ressourceRepo->countByCategorie();
        $byNiveau    = $ressourceRepo->countByNiveau();
        $topVues     = $ressourceRepo->findTopVues(5);
        $topLikes    = $ressourceRepo->findTopLikes(5);

        // ── Commentaires ────────────────────────────────────────────────────
        $signales   = $commentaireRepo->findSignales();
        $sentiments = $commentaireRepo->getSentimentsGlobaux();
        $topThemes  = $commentaireRepo->getTopThemes(5);

        // ── Statistiques Événements ─────────────────────────────────────────
        $totalEvenements    = $evenementRepo->count([]);
        $totalTypes         = $typeRepo->count([]);
        $totalGratuits      = $evenementRepo->countByGratuit(true);
        $totalPayants       = $evenementRepo->countByGratuit(false);

        $evenementsAVenir   = $evenementRepo->countByStatut('a_venir');
        $evenementsEnCours  = $evenementRepo->countByStatut('en_cours');
        $evenementsTermines = $evenementRepo->countByStatut('termine');
        $evenementsAnnules  = $evenementRepo->countByStatut('annule');

        $parCategorie       = $evenementRepo->countByCategorie();
        $parCategorieLabels = array_keys($parCategorie);
        $parCategorieCounts = array_values($parCategorie);

        $parModalite        = $evenementRepo->countByModalite();
        $parModaliteLabels  = array_keys($parModalite);
        $parModaliteCounts  = array_values($parModalite);

        $parMois            = $evenementRepo->countByMonth();
        $parMoisLabels      = array_keys($parMois);
        $parMoisValues      = array_values($parMois);

        $capaciteTotale  = $evenementRepo->sumCapacite();
        $capaciteMoyenne = $totalEvenements > 0
            ? (int) round($capaciteTotale / $totalEvenements)
            : 0;

        $prochains           = $evenementRepo->findUpcoming(5);
        $prochainsEvenements = $prochains;
        $enCours             = $evenementRepo->findCurrent();

        // ── Statistiques Produits ───────────────────────────────────────────
        $totalProduits      = $produitRepo->count([]);
        $produitsActifs     = $produitRepo->count(['estActif' => true]);
        $produitsInactifs   = $totalProduits - $produitsActifs;
        $ruptureStock       = $produitRepo->countRuptureStock();
        $produitsNumeriques = $produitRepo->countNumeriques();
        $prixMoyen          = $produitRepo->getPrixMoyen();
        $valeurStock        = $produitRepo->getValeurTotaleStock();
        $top5Produits       = $produitRepo->findTop5PlusChers();
        $produitParMois     = $produitRepo->countByMonth();
        $produitStatut      = $produitRepo->countByStatut();

        // ── 20 derniers logs d'activité ───────────────────────────────────────
        $activityLogs = $logRepo->findRecent(20);

        return $this->render('dashboard/index.html.twig', [
            'adminUser' => $this->getUser(),
            'stats' => [
                // Utilisateurs
                'totalUsers'         => $totalUsers,
                'countPatients'      => $countPatients,
                'countPsychos'       => $countPsychos,
                'countCoachs'        => $countCoachs,
                'countAdmins'        => $countAdmins,
                'countActifs'        => $countActifs,
                'countInactifs'      => $countInactifs,
                'newThisMonth'       => $newThisMonth,
                'newThisWeek'        => $newThisWeek,
                'pctPatients'        => $pctPatients,
                'pctPsychos'         => $pctPsychos,
                'pctCoachs'          => $pctCoachs,
                'pctAdmins'          => $pctAdmins,
                'registrationsByDay' => $registrationsByDay,
                // Locaux
                'totalLocaux'              => $totalLocaux,
                'countLocauxDisponibles'   => $countLocauxDisponibles,
                'countLocauxIndisponibles' => $countLocauxIndisponibles,
                'pctLocauxDispo'           => $pctLocauxDispo,
                'locauxParType'            => $locauxParType,
                // Salles
                'totalSalles'              => $totalSalles,
                'countSallesDisponibles'   => $countSallesDisponibles,
                'countSallesIndisponibles' => $countSallesIndisponibles,
                'countSallesMaintenance'   => $countSallesMaintenance,
                'pctSallesDispo'           => $pctSallesDispo,
                'sallesParType'            => $sallesParType,
                // Réclamations
                'reclamationTotal'          => $reclamationTotal,
                'reclamationByStatus'       => $reclamationByStatus,
                'reclamationTauxResolution' => $reclamationTauxResolution,
                'reclamationMonthlyLabels'  => $reclamationMonthly['labels'],
                'reclamationMonthlyValues'  => $reclamationMonthly['values'],
                'reclamationStatusLabels'   => array_keys($reclamationByStatus),
                'reclamationStatusCounts'   => array_values($reclamationByStatus),
                // Tests & réponses
                'testCatalogue'         => $testCatalogue,
                'testActifs'            => $testActifs,
                'reponsesTotal'         => $reponsesTotal,
                'reponsesByType'        => $reponsesByType,
                'reponsesTypeLabels'    => $reponsesTypeLabels,
                'reponsesTypeCounts'    => $reponsesTypeCounts,
                'reponsesMonthlyLabels' => $reponsesMonthly['labels'],
                'reponsesMonthlyValues' => $reponsesMonthly['values'],
                // Objectifs
                'objectifTotal'        => $objectifTotal,
                'objectifBySource'     => $objectifBySource,
                'objectifByStatut'     => $objectifByStatut,
                'objectifAdoptesRatio' => $objectifAdoptesRatio,
                // Événements
                'totalEvenements'    => $totalEvenements,
                'totalTypes'         => $totalTypes,
                'totalGratuits'      => $totalGratuits,
                'totalPayants'       => $totalPayants,
                'evenementsAVenir'   => $evenementsAVenir,
                'evenementsEnCours'  => $evenementsEnCours,
                'evenementsTermines' => $evenementsTermines,
                'evenementsAnnules'  => $evenementsAnnules,
                'parCategorie'       => $parCategorie,
                'parCategorieLabels' => $parCategorieLabels,
                'parCategorieCounts' => $parCategorieCounts,
                'parModalite'        => $parModalite,
                'parModaliteLabels'  => $parModaliteLabels,
                'parModaliteCounts'  => $parModaliteCounts,
                'capaciteTotale'     => $capaciteTotale,
                'capaciteMoyenne'    => $capaciteMoyenne,
                'parMois'            => $parMois,
                'parMoisLabels'      => $parMoisLabels,
                'parMoisValues'      => $parMoisValues,
            ],
            'recentUsers'         => $recentUsers,
            'activityLogs'        => $activityLogs,
            'prochains'           => $prochains,
            'prochainsEvenements' => $prochainsEvenements,
            'enCours'             => $enCours,
            'ressourcesStats' => [
                'total'       => $totalRessources,
                'byType'      => $byType,
                'byCategorie' => $byCategorie,
                'byNiveau'    => $byNiveau,
                'topVues'     => $topVues,
                'topLikes'    => $topLikes,
                'signales'    => $signales,
                'sentiments'  => $sentiments,
                'topThemes'   => $topThemes,
            ],
            'totalProduits'      => $totalProduits,
            'produitsActifs'     => $produitsActifs,
            'produitsInactifs'   => $produitsInactifs,
            'ruptureStock'       => $ruptureStock,
            'produitsNumeriques' => $produitsNumeriques,
            'prixMoyen'          => $prixMoyen,
            'valeurStock'        => $valeurStock,
            'top5Produits'       => $top5Produits,
            'produitParMois'     => $produitParMois,
            'produitStatut'      => $produitStatut,
        ]);
    }

    /**
     * Builds a series of labels and values for the last N months.
     *
     * @param array<string, int> $countsByYm Counts keyed by 'Y-m' (e.g. '2024-03').
     * @return array{labels: list<string>, values: list<int>}
     */
    private function buildLastMonthsSeries(int $months, array $countsByYm): array
    {
        $labels = [];
        $values = [];
        $start = (new \DateTimeImmutable('first day of this month midnight'))
            ->modify('-' . ($months - 1) . ' months');
        for ($i = 0; $i < $months; ++$i) {
            $k        = $start->format('Y-m');
            $labels[] = $start->format('m/Y');
            $values[] = (int) ($countsByYm[$k] ?? 0);
            $start    = $start->modify('+1 month');
        }
        return ['labels' => $labels, 'values' => $values];
    }
}