<?php

// src/Controller/Admin/ParticipationController.php

namespace App\Controller\Admin;

use App\Entity\Email;
use App\Entity\Evenement;
use App\Entity\Participation;
use App\Form\ParticipationType;
use App\Form\ParticipationSearchType;
use App\Repository\ParticipationRepository;
use App\Service\QrCodeService;
use App\Service\ParticipationMailer;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\BinaryFileResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\ResponseHeaderBag;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/participation')]
class ParticipationController extends AbstractController
{
    #[Route('/', name: 'admin_participation_index', methods: ['GET'])]
    public function index(Request $request, ParticipationRepository $repo): Response
    {
        $searchForm = $this->createForm(ParticipationSearchType::class);
        $searchForm->handleRequest($request);

        $criteria = [];
        if ($searchForm->isSubmitted() && $searchForm->isValid()) {
            $data = $searchForm->getData();
            if (!empty($data['nom']))        $criteria['nom']       = $data['nom'];
            if (!empty($data['statut']))     $criteria['statut']    = $data['statut'];
            if (!empty($data['evenement']))  $criteria['evenement'] = $data['evenement']->getId();
        }

        $participations = $repo->search($criteria);

        return $this->render('admin/participation/index.html.twig', [
            'participations' => $participations,
            'searchForm'     => $searchForm->createView(),
        ]);
    }

    #[Route('/new', name: 'admin_participation_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        QrCodeService $qrService,
        ParticipationMailer $mailer
    ): Response {
        $participation = new Participation();
        $form = $this->createForm(ParticipationType::class, $participation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $evenement = $participation->getEvenement();

            if ($evenement && !$evenement->hasPlacesDisponibles()) {
                $this->addFlash('danger', 'Impossible d\'ajouter ce participant : l\'événement "' . $evenement->getTitreEvenement() . '" est complet (capacité maximale atteinte).');
                return $this->render('admin/participation/form.html.twig', [
                    'form'  => $form->createView(),
                    'title' => 'Nouvelle participation',
                ]);
            }
            $participation->setStatut('confirmee');

            $qrData = sprintf(
                'PARTICIPATION|ID:%s|Nom:%s %s|Email:%s|Evenement:%s|Date:%s',
                uniqid(),
                $participation->getNom(),
                $participation->getPrenom(),
                $participation->getEmail()->getValue(),
                $evenement?->getTitreEvenement(),
                (new \DateTime())->format('Y-m-d H:i')
            );
            $qrFilename = 'qr_' . uniqid();
            $qrFile     = $qrService->generate($qrData, $qrFilename);
            $participation->setCodeQr($qrFile);

            $em->persist($participation);
            $em->flush();

            $emailSent = true;
            try {
                $projectDir = $this->getParameter('kernel.project_dir');
                if (!is_string($projectDir)) {
                    throw new \RuntimeException('Le paramètre kernel.project_dir n\'est pas une chaîne valide.');
                }
                $qrFullPath = $projectDir . '/public/qrcodes/' . (string) $qrFile;
                $mailer->sendConfirmation($participation, $qrFullPath);
            } catch (\Exception $e) {
                $emailSent = false;
                $this->addFlash('warning', 'Participation créée, mais l\'email n\'a pas pu être envoyé : ' . $e->getMessage());
            }

            if ($emailSent) {
                $this->addFlash('success', '✅ Participation créée ! Email de confirmation envoyé avec le QR code.');
            } else {
                $this->addFlash('info', '✅ Participation créée. Le QR code est disponible dans la liste.');
            }

            return $this->redirectToRoute('admin_participation_index');
        }

        return $this->render('admin/participation/form.html.twig', [
            'form'  => $form->createView(),
            'title' => 'Nouvelle participation',
        ]);
    }

    #[Route('/edit/{id}', name: 'admin_participation_edit', methods: ['GET', 'POST'])]
    public function edit(
        int $id,
        Request $request,
        EntityManagerInterface $em,
        ParticipationRepository $repo
    ): Response {
        $participation = $repo->find($id);
        if (!$participation) {
            throw $this->createNotFoundException('Participation introuvable.');
        }

        $form = $this->createForm(ParticipationType::class, $participation);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();
            $this->addFlash('success', 'Participation modifiée avec succès !');
            return $this->redirectToRoute('admin_participation_index');
        }

        return $this->render('admin/participation/form.html.twig', [
            'form'          => $form->createView(),
            'title'         => 'Modifier la participation',
            'participation' => $participation,
        ]);
    }

    #[Route('/delete/{id}', name: 'admin_participation_delete', methods: ['POST'])]
    public function delete(
        int $id,
        Request $request,
        EntityManagerInterface $em,
        ParticipationRepository $repo,
        ParticipationMailer $mailer,
        QrCodeService $qrService
    ): Response {
        $participation = $repo->find($id);
        if (!$participation) {
            throw $this->createNotFoundException('Participation introuvable.');
        }

        $token = $request->request->get('_token');
        if ($this->isCsrfTokenValid('delete_participation_' . $id, (string) $token)) {
            $evenement    = $participation->getEvenement();
            $etaitConfirm = $participation->getStatut() === 'confirmee';

            $projectDir = $this->getParameter('kernel.project_dir');
            if (is_string($projectDir) && $participation->getCodeQr()) {
                $qrFile = $projectDir . '/public/qrcodes/' . (string) $participation->getCodeQr();
                if (file_exists($qrFile)) {
                    unlink($qrFile);
                }
            }

            $em->remove($participation);
            $em->flush();

            $this->addFlash('danger', 'Participation supprimée. Une place a été libérée sur l\'événement.');

            if ($etaitConfirm && $evenement) {
                $evenementId = $evenement->getId();
                if ($evenementId !== null) {
                    $premierAttente = $repo->findFirstEnAttente($evenementId);

                    if ($premierAttente) {
                        $qrData = sprintf(
                            'PARTICIPATION|ID:%s|Nom:%s %s|Email:%s|Evenement:%s|Date:%s',
                            uniqid(),
                            $premierAttente->getNom(),
                            $premierAttente->getPrenom(),
                            $premierAttente->getEmail()->getValue(),
                            $evenement->getTitreEvenement(),
                            (new \DateTime())->format('Y-m-d H:i')
                        );
                        $qrFilename = 'qr_' . uniqid();
                        $qrFileNew  = $qrService->generate($qrData, $qrFilename);

                        $premierAttente->setStatut('confirmee');
                        $premierAttente->setPositionAttente(null);
                        $premierAttente->setCodeQr($qrFileNew);
                        $em->flush();

                        $repo->reorderListeAttente($evenementId, $em);

                        $projectDir = $this->getParameter('kernel.project_dir');
                        if (!is_string($projectDir)) {
                            throw new \RuntimeException('Le paramètre kernel.project_dir n\'est pas une chaîne valide.');
                        }
                        $qrPath = $projectDir . '/public/qrcodes/' . (string) $qrFileNew;
                        try {
                            $mailer->sendListeAttenteNotification($premierAttente, $qrPath);
                            $this->addFlash('success', sprintf(
                                '📧 %s %s (liste d\'attente) a été notifié(e) qu\'une place est disponible et son QR code a été généré.',
                                $premierAttente->getPrenom(),
                                $premierAttente->getNom()
                            ));
                        } catch (\Exception $e) {
                            $this->addFlash('warning', 'La notification email au premier en liste d\'attente a échoué : ' . $e->getMessage());
                        }
                    }
                }
            }
        } else {
            $this->addFlash('warning', 'Token CSRF invalide, suppression annulée.');
        }

        return $this->redirectToRoute('admin_participation_index');
    }

    #[Route('/show/{id}', name: 'admin_participation_show', methods: ['GET'])]
    public function show(int $id, ParticipationRepository $repo): Response
    {
        $participation = $repo->find($id);
        if (!$participation) {
            throw $this->createNotFoundException('Participation introuvable.');
        }

        return $this->render('admin/participation/show.html.twig', [
            'participation' => $participation,
        ]);
    }

    #[Route('/download-qr/{id}', name: 'admin_participation_download_qr', methods: ['GET'])]
    public function downloadQr(int $id, ParticipationRepository $repo): Response
    {
        $participation = $repo->find($id);
        if (!$participation || !$participation->getCodeQr()) {
            throw $this->createNotFoundException('QR code introuvable.');
        }

        $projectDir = $this->getParameter('kernel.project_dir');
        if (!is_string($projectDir)) {
            throw new \RuntimeException('Le paramètre kernel.project_dir n\'est pas une chaîne valide.');
        }
        $filePath = $projectDir . '/public/qrcodes/' . (string) $participation->getCodeQr();
        if (!file_exists($filePath)) {
            throw $this->createNotFoundException('Fichier QR code introuvable.');
        }

        $response = new BinaryFileResponse($filePath);
        $response->setContentDisposition(
            ResponseHeaderBag::DISPOSITION_ATTACHMENT,
            'qr_participation_' . $participation->getId() . '.png'
        );
        return $response;
    }
}