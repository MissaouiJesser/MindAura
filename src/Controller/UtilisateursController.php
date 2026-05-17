<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use App\Form\UtilisateursType;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use Dompdf\Dompdf;
use Dompdf\Options;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Component\String\Slugger\SluggerInterface;

/**
 * Gestion CRUD des utilisateurs (back-office admin).
 *
 * Toutes les routes sont protégées par ROLE_ADMIN.
 *
 * Routes exposées :
 *   GET  /admin/utilisateurs                  → liste paginée (+ tri, filtres, recherche)
 *   GET  /admin/utilisateurs/{id}             → détails du profil
 *   GET  /admin/utilisateurs/new              → formulaire de création
 *   POST /admin/utilisateurs/new              → traitement de la création
 *   GET  /admin/utilisateurs/{id}/edit        → formulaire d'édition
 *   POST /admin/utilisateurs/{id}/edit        → traitement de l'édition
 *   POST /admin/utilisateurs/{id}/toggle      → activer / désactiver le compte (token CSRF)
 *   POST /admin/utilisateurs/{id}/delete      → suppression (token CSRF)
 *   GET  /admin/utilisateurs/export/csv       → export CSV (filtres conservés)
 *   GET  /admin/utilisateurs/export/pdf       → export PDF (filtres conservés)
 */
#[Route('/admin/utilisateurs')]
#[IsGranted('ROLE_ADMIN')]
class UtilisateursController extends AbstractController
{
    // ══════════════════════════════════════════════════════════════════
    //  LISTE  (pagination + tri + filtres multi-attributs)
    // ══════════════════════════════════════════════════════════════════

    #[Route('', name: 'app_admin_utilisateurs', methods: ['GET'])]
    public function index(Request $request, UtilisateursRepository $repo): Response
    {
        $params = [
            'search'    => $request->query->get('search',    ''),
            'role'      => $request->query->get('role',      ''),
            'statut'    => $request->query->get('statut',    ''),
            'dateDebut' => $request->query->get('dateDebut', ''),
            'dateFin'   => $request->query->get('dateFin',   ''),
            'sort'      => $request->query->get('sort',      UtilisateursRepository::DEFAULT_SORT),
            'direction' => $request->query->get('direction', UtilisateursRepository::DEFAULT_DIRECTION),
            'page'      => $request->query->getInt('page',    1),
            'perPage'   => $request->query->getInt('perPage', UtilisateursRepository::PAGE_SIZE),
        ];

        $pagination = $repo->findFiltered($params);

        return $this->render('admin/utilisateurs/index.html.twig', [
            'utilisateurs'    => $pagination['items'],
            'total'           => $pagination['total'],
            'pages'           => $pagination['pages'],
            'page'            => $pagination['page'],
            'perPage'         => $pagination['perPage'],
            'sortableColumns' => UtilisateursRepository::SORTABLE_COLUMNS,
            'search'    => $params['search'],
            'role'      => $params['role'],
            'statut'    => $params['statut'],
            'dateDebut' => $params['dateDebut'],
            'dateFin'   => $params['dateFin'],
            'sort'      => $params['sort'],
            'direction' => $params['direction'],
            'adminUser' => $this->getUser(),
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  DÉTAIL
    // ══════════════════════════════════════════════════════════════════

    #[Route('/{id}', name: 'app_admin_utilisateurs_show', requirements: ['id' => '\d+'], methods: ['GET'])]
    public function show(int $id, UtilisateursRepository $repo): Response
    {
        $utilisateur = $repo->find($id);
        if (!$utilisateur) {
            throw $this->createNotFoundException('Utilisateur introuvable (id=' . $id . ').');
        }

        return $this->render('admin/utilisateurs/show.html.twig', [
            'utilisateur' => $utilisateur,
            'adminUser'   => $this->getUser(),
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  EXPORT CSV
    // ══════════════════════════════════════════════════════════════════

    #[Route('/export/csv', name: 'app_admin_utilisateurs_export_csv', methods: ['GET'])]
    public function exportCsv(Request $request, UtilisateursRepository $repo): StreamedResponse
    {
        $utilisateurs = $this->getFilteredAll($request, $repo);

        $filename = 'utilisateurs_' . date('Ymd_His') . '.csv';

        $response = new StreamedResponse(function () use ($utilisateurs) {
            // FIX lines 103/106/127/140 : fopen peut retourner false → vérification avant usage
            $handle = fopen('php://output', 'w');
            if ($handle === false) {
                return;
            }

            // BOM UTF-8 pour une ouverture correcte dans Excel
            fwrite($handle, "\xEF\xBB\xBF");

            // En-tête
            fputcsv($handle, [
                'ID',
                'Prénom',
                'Nom',
                'Email',
                'Téléphone',
                'Rôle',
                'Date de naissance',
                'Date d\'inscription',
                'Statut',
            ], ';');

            $roleLabels = [
                'ROLE_PATIENT'     => 'Patient',
                'ROLE_PSYCHOLOGUE' => 'Psychologue',
                'ROLE_COACH'       => 'Coach',
                'ROLE_ADMIN'       => 'Administrateur',
                'ROLE_USER'        => 'Utilisateur',
            ];

            foreach ($utilisateurs as $u) {
                fputcsv($handle, [
                    $u->getIdUtilisateur(),
                    $u->getPrenomUtilisateur(),
                    $u->getNomUtilisateur(),
                    $u->getEmailUtilisateur(),
                    $u->getTelephoneUtilisateur(),
                    $roleLabels[$u->getRoleUtilisateur()] ?? $u->getRoleUtilisateur(),
                    $u->getDateNaissanceUtilisateur()?->format('d/m/Y') ?? '—',
                    $u->getDateInscriptionUtilisateur()->format('d/m/Y'),
                    $u->isEstActifUtilisateur() ? 'Actif' : 'Inactif',
                ], ';');
            }

            fclose($handle);
        });

        $response->headers->set('Content-Type', 'text/csv; charset=UTF-8');
        $response->headers->set('Content-Disposition', "attachment; filename=\"{$filename}\"");

        return $response;
    }

    // ══════════════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════════════════════════════

    #[Route('/export/pdf', name: 'app_admin_utilisateurs_export_pdf', methods: ['GET'])]
    public function exportPdf(Request $request, UtilisateursRepository $repo): Response
    {
        $utilisateurs = $this->getFilteredAll($request, $repo);

        $filters = [
            'search'    => $request->query->get('search',    ''),
            'role'      => $request->query->get('role',      ''),
            'statut'    => $request->query->get('statut',    ''),
            'dateDebut' => $request->query->get('dateDebut', ''),
            'dateFin'   => $request->query->get('dateFin',   ''),
        ];

        $html = $this->renderView('admin/utilisateurs/export_pdf.html.twig', [
            'utilisateurs' => $utilisateurs,
            'filters'      => $filters,
            'adminUser'    => $this->getUser(),
            'exportedAt'   => new \DateTime(),
        ]);

        $options = new Options();
        $options->set('isHtml5ParserEnabled', true);
        $options->set('isRemoteEnabled', false);
        $options->set('defaultFont', 'DejaVu Sans');

        $dompdf = new Dompdf($options);
        $dompdf->loadHtml($html, 'UTF-8');
        $dompdf->setPaper('A4', 'landscape');
        $dompdf->render();

        $filename = 'utilisateurs_' . date('Ymd_His') . '.pdf';

        return new Response(
            $dompdf->output(),
            200,
            [
                'Content-Type'        => 'application/pdf',
                'Content-Disposition' => "attachment; filename=\"{$filename}\"",
            ]
        );
    }

    // ══════════════════════════════════════════════════════════════════
    //  CRÉATION
    // ══════════════════════════════════════════════════════════════════

    #[Route('/new', name: 'app_admin_utilisateurs_new', methods: ['GET', 'POST'])]
    public function new(
        Request                     $request,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher,
        SluggerInterface            $slugger
    ): Response {
        $utilisateur = new Utilisateurs();
        $form = $this->createForm(UtilisateursType::class, $utilisateur);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            $plain   = $form->get('plainPassword')->getData();
            $confirm = $form->get('plainPasswordConfirm')->getData();

            if ($plain !== $confirm) {
                $this->addFlash('error', 'Les mots de passe ne correspondent pas.');
                return $this->render('admin/utilisateurs/form.html.twig', [
                    'form'      => $form,
                    'editMode'  => false,
                    'adminUser' => $this->getUser(),
                ]);
            }

            // FIX line 427 : getParameter() retourne mixed → cast explicite en string
            $projectDir = $this->getParameter('kernel.project_dir');
            $avatarsDir = (is_string($projectDir) ? $projectDir : '') . '/public/avatars';

            $utilisateur->setMdpUtilisateur($passwordHasher->hashPassword($utilisateur, is_string($plain) ? $plain : ''));
            $utilisateur->setPhotoProfilUtilisateur($this->uploadPhoto($form->get('photoProfil')->getData(), $slugger, $avatarsDir));
            $utilisateur->setDateInscriptionUtilisateur(new \DateTime());
            $utilisateur->setEstActifUtilisateur(true);
            $utilisateur->setBioUtilisateur('');

            $em->persist($utilisateur);
            $em->flush();

            $this->addFlash('success', 'Utilisateur créé avec succès.');
            return $this->redirectToRoute('app_admin_utilisateurs');
        }

        return $this->render('admin/utilisateurs/form.html.twig', [
            'form'      => $form,
            'editMode'  => false,
            'adminUser' => $this->getUser(),
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ÉDITION
    // ══════════════════════════════════════════════════════════════════

    #[Route('/{id}/edit', name: 'app_admin_utilisateurs_edit', requirements: ['id' => '\d+'], methods: ['GET', 'POST'])]
    public function edit(
        int                         $id,
        Request                     $request,
        UtilisateursRepository      $repo,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher,
        SluggerInterface            $slugger
    ): Response {
        $utilisateur = $repo->find($id);
        if (!$utilisateur) {
            throw $this->createNotFoundException('Utilisateur introuvable (id=' . $id . ').');
        }

        $form = $this->createForm(UtilisateursType::class, $utilisateur, ['is_edit' => true]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            $plain   = $form->get('plainPassword')->getData();
            $confirm = $form->get('plainPasswordConfirm')->getData();

            if (!empty($plain)) {
                if ($plain !== $confirm) {
                    $this->addFlash('error', 'Les mots de passe ne correspondent pas.');
                    return $this->render('admin/utilisateurs/form.html.twig', [
                        'form'        => $form,
                        'editMode'    => true,
                        'utilisateur' => $utilisateur,
                        'adminUser'   => $this->getUser(),
                    ]);
                }
                $utilisateur->setMdpUtilisateur($passwordHasher->hashPassword($utilisateur, is_string($plain) ? $plain : ''));
            }

            // FIX line 427 : getParameter() retourne mixed → cast explicite en string
            $projectDir2 = $this->getParameter('kernel.project_dir');
            $avatarsDir  = (is_string($projectDir2) ? $projectDir2 : '') . '/public/avatars';

            $photo = $form->get('photoProfil')->getData();
            if ($photo) {
                $utilisateur->setPhotoProfilUtilisateur($this->uploadPhoto($photo, $slugger, $avatarsDir));
            }

            $em->flush();

            $this->addFlash('success', 'Utilisateur mis à jour avec succès.');
            return $this->redirectToRoute('app_admin_utilisateurs');
        }

        return $this->render('admin/utilisateurs/form.html.twig', [
            'form'        => $form,
            'editMode'    => true,
            'utilisateur' => $utilisateur,
            'adminUser'   => $this->getUser(),
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  TOGGLE STATUT
    // ══════════════════════════════════════════════════════════════════

    #[Route('/{id}/toggle', name: 'app_admin_utilisateurs_toggle', requirements: ['id' => '\d+'], methods: ['POST'])]
    public function toggle(
        int                    $id,
        Request                $request,
        UtilisateursRepository $repo,
        EntityManagerInterface $em
    ): Response {
        $utilisateur = $repo->find($id);
        if (!$utilisateur) {
            throw $this->createNotFoundException('Utilisateur introuvable (id=' . $id . ').');
        }

        // FIX lines 345/383 : _token est mixed → cast en string|null pour isCsrfTokenValid()
        $token = $request->request->get('_token');
        $token = is_string($token) ? $token : null;

        if ($this->isCsrfTokenValid('toggle_user_' . $id, $token)) {
            $newStatus = !$utilisateur->isEstActifUtilisateur();
            $utilisateur->setEstActifUtilisateur($newStatus);
            $em->flush();

            $label = $newStatus ? 'activé' : 'désactivé';
            $this->addFlash('success', sprintf(
                'Le compte de %s %s a été %s avec succès.',
                $utilisateur->getPrenomUtilisateur(),
                $utilisateur->getNomUtilisateur(),
                $label
            ));
        } else {
            $this->addFlash('error', 'Token CSRF invalide — action annulée.');
        }

        // FIX lines 362/392 : redirect() attend string → cast explicite
        $referer = $request->request->get('_referer');
        $url = is_string($referer) && $referer !== ''
            ? $referer
            : $this->generateUrl('app_admin_utilisateurs');

        return $this->redirect($url);
    }

    // ══════════════════════════════════════════════════════════════════
    //  SUPPRESSION
    // ══════════════════════════════════════════════════════════════════

    #[Route('/{id}/delete', name: 'app_admin_utilisateurs_delete', requirements: ['id' => '\d+'], methods: ['POST'])]
    public function delete(
        int                    $id,
        Request                $request,
        UtilisateursRepository $repo,
        EntityManagerInterface $em
    ): Response {
        $utilisateur = $repo->find($id);
        if (!$utilisateur) {
            throw $this->createNotFoundException('Utilisateur introuvable (id=' . $id . ').');
        }

        // FIX lines 383/392 : même correction token + redirect
        $token = $request->request->get('_token');
        $token = is_string($token) ? $token : null;

        if ($this->isCsrfTokenValid('delete_user_' . $id, $token)) {
            $em->remove($utilisateur);
            $em->flush();
            $this->addFlash('success', 'Utilisateur supprimé avec succès.');
        } else {
            $this->addFlash('error', 'Token CSRF invalide — suppression annulée.');
        }

        $referer = $request->request->get('_referer');
        $url = is_string($referer) && $referer !== ''
            ? $referer
            : $this->generateUrl('app_admin_utilisateurs');

        return $this->redirect($url);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════════

    /**
     * @return Utilisateurs[]
     */
    private function getFilteredAll(Request $request, UtilisateursRepository $repo): array
    {
        return $repo->findFiltered([
            'search'    => $request->query->get('search',    ''),
            'role'      => $request->query->get('role',      ''),
            'statut'    => $request->query->get('statut',    ''),
            'dateDebut' => $request->query->get('dateDebut', ''),
            'dateFin'   => $request->query->get('dateFin',   ''),
            'sort'      => $request->query->get('sort',      UtilisateursRepository::DEFAULT_SORT),
            'direction' => $request->query->get('direction', UtilisateursRepository::DEFAULT_DIRECTION),
            'page'      => 1,
            'perPage'   => 10000,
        ])['items'];
    }

    private function uploadPhoto(?UploadedFile $file, SluggerInterface $slugger, string $avatarsDir): string
    {
        if (!$file || !$file->isValid()) {
            return 'default.png';
        }

        if (!is_dir($avatarsDir)) {
            mkdir($avatarsDir, 0775, true);
        }

        $baseName  = $slugger->slug(pathinfo($file->getClientOriginalName(), PATHINFO_FILENAME));
        $extension = $file->guessExtension() ?? 'jpg';
        $filename  = $baseName . '_' . uniqid() . '.' . $extension;
        $file->move($avatarsDir, $filename);

        return $filename;
    }
}