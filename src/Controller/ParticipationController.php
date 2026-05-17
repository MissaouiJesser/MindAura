<?php

namespace App\Controller;

use App\Entity\Email;
use App\Entity\Participation;
use App\Entity\Utilisateurs;
use App\Form\UserParticipationType;
use App\Repository\EvenementRepository;
use App\Repository\ParticipationRepository;
use App\Service\QrCodeService;
use App\Service\ParticipationMailer;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\BinaryFileResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\ResponseHeaderBag;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/evenements')]
class ParticipationController extends AbstractController
{
    #[Route('/', name: 'user_evenements_list', methods: ['GET'])]
    public function list(EvenementRepository $repo): Response
    {
        return $this->render('evenement/liste.html.twig', [
            'evenements' => $repo->findAllWithType(),
        ]);
    }

    #[Route('/{id}/participer', name: 'user_participation_new', methods: ['GET', 'POST'])]
    public function participer(
        int $id,
        Request $request,
        EntityManagerInterface $em,
        EvenementRepository $evenementRepo,
        ParticipationRepository $participationRepo,
        QrCodeService $qrService,
        ParticipationMailer $mailer
    ): Response {
        // ── Vérifier que l'utilisateur est connecté ──
        if (!$this->getUser()) {
            $this->addFlash('warning', 'Vous devez être connecté(e) pour participer à un événement.');
            return $this->redirectToRoute('app_login');
        }

        $evenement = $evenementRepo->find($id);
        if (!$evenement) {
            throw $this->createNotFoundException('Événement introuvable.');
        }

        $statut = $evenement->getStatutEvenemnt();
        if (in_array($statut, ['annule', 'termine'])) {
            $this->addFlash('danger', 'Cet événement est ' . ($statut ?? 'inconnu') . '.');
            return $this->redirectToRoute('user_evenements_list');
        }

        $estComplet     = !$evenement->hasPlacesDisponibles();
        $listeAttenteOk = $evenement->hasPlaceEnAttente();

        if ($estComplet && !$listeAttenteOk) {
            $this->addFlash('warning', "Désolé, cet événement est complet et la liste d'attente est fermée.");
            return $this->redirectToRoute('user_evenements_list');
        }

        // ── Récupérer nom, prénom, email depuis le compte connecté ──
        /** @var Utilisateurs $user */
        $user      = $this->getUser();
        $userNom    = $user->getNomUtilisateur()    ?? '';
        $userPrenom = $user->getPrenomUtilisateur() ?? '';
        $userEmail  = $user->getEmailUtilisateur()  ?? $user->getUserIdentifier();
        $userTel    = $user->getTelephoneUtilisateur() ?? '';

        $participation = new Participation();
        $participation->setEvenement($evenement);
        // Pré-remplir depuis le profil
        $participation->setNom($userNom);
        $participation->setPrenom($userPrenom);
        $participation->setTelephone($userTel);

        $form = $this->createForm(UserParticipationType::class, $participation, [
            'user_email'  => $userEmail,
            'user_nom'    => $userNom,
            'user_prenom' => $userPrenom,
            'user_tel'    => $userTel,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && !$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                if ($error instanceof FormError) {
                    $this->addFlash('danger', $error->getMessage());
                }
            }
            return $this->render('evenement/participer.html.twig', [
                'form'           => $form->createView(),
                'evenement'      => $evenement,
                'estComplet'     => $estComplet,
                'listeAttenteOk' => $listeAttenteOk,
                'userEmail'      => $userEmail,
            ]);
        }

        if ($form->isSubmitted() && $form->isValid()) {

            // ── Email : toujours pris depuis le compte connecté (champ readonly) ──
            try {
                $emailObject = new Email($userEmail);
            } catch (\InvalidArgumentException $e) {
                $this->addFlash('danger', 'Adresse email du compte invalide : ' . $e->getMessage());
                return $this->redirectToRoute('user_evenements_list');
            }

            $participation->setEmail($emailObject);
            $email = $emailObject->getValue();

            // ── Vérifier si déjà inscrit ──
            if ($participationRepo->isAlreadyRegistered($evenement->getId() ?? 0, $email)) {
                $this->addFlash('danger', 'Vous êtes déjà inscrit(e) à cet événement avec cet email.');
                return $this->render('evenement/participer.html.twig', [
                    'form'           => $form->createView(),
                    'evenement'      => $evenement,
                    'estComplet'     => $estComplet,
                    'listeAttenteOk' => $listeAttenteOk,
                    'userEmail'      => $userEmail,
                ]);
            }

            $estCompletNow     = !$evenement->hasPlacesDisponibles();
            $listeAttenteOkNow = $evenement->hasPlaceEnAttente();

            if (!$estCompletNow) {
                // ── Inscription confirmée ──
                $participation->setStatut('confirmee');
                $participation->setPositionAttente(null);

                $qrData = sprintf(
                    'PARTICIPATION|ID:%s|Nom:%s %s|Email:%s|Evenement:%s|Date:%s',
                    uniqid(),
                    $participation->getNom(),
                    $participation->getPrenom(),
                    $email,
                    $evenement->getTitreEvenement(),
                    (new \DateTime())->format('Y-m-d H:i')
                );

                $qrFilename = 'qr_' . uniqid();
                $qrFile     = $qrService->generate($qrData, $qrFilename);

                if ($qrFile === '') {
                    $this->addFlash('danger', 'Erreur lors de la génération du QR code. Veuillez réessayer.');
                    return $this->redirectToRoute('user_evenements_list');
                }

                $participation->setCodeQr($qrFile);
                $em->persist($participation);
                $em->flush();

                $emailSent = false;
                try {
                    $qrFullPath = $qrService->getAbsolutePath($qrFile);
                    $mailer->sendConfirmation($participation, $qrFullPath);
                    $emailSent = true;
                } catch (\Exception $e) {
                    $this->addFlash('warning', "Inscription réussie, mais l'email de confirmation n'a pas pu être envoyé.");
                }

                return $this->render('evenement/participation_success.html.twig', [
                    'participation' => $participation,
                    'evenement'     => $evenement,
                    'emailSent'     => $emailSent,
                    'modeAttente'   => false,
                ]);

            } elseif ($listeAttenteOkNow) {
                // ── Liste d'attente ──
                $participation->setStatut('en_attente');
                $participation->setPositionAttente($evenement->getNextPositionAttente());
                $participation->setCodeQr(null);

                $em->persist($participation);
                $em->flush();

                try {
                    $mailer->sendListeAttenteConfirmation($participation);
                } catch (\Exception $e) {
                    $this->addFlash('warning', "Inscription en liste d'attente réussie, mais l'email n'a pas pu être envoyé.");
                }

                return $this->render('evenement/participation_success.html.twig', [
                    'participation' => $participation,
                    'evenement'     => $evenement,
                    'emailSent'     => true,
                    'modeAttente'   => true,
                ]);

            } else {
                $this->addFlash('warning', "L'événement est complet et la liste d'attente est également fermée.");
                return $this->redirectToRoute('user_evenements_list');
            }
        }

        return $this->render('evenement/participer.html.twig', [
            'form'           => $form->createView(),
            'evenement'      => $evenement,
            'estComplet'     => $estComplet,
            'listeAttenteOk' => $listeAttenteOk,
            'userEmail'      => $userEmail,
        ]);
    }

    #[Route('/qr/{id}/download', name: 'user_participation_download_qr', methods: ['GET'])]
    public function downloadQr(int $id, ParticipationRepository $repo, QrCodeService $qrService): Response
    {
        $participation = $repo->find($id);
        if (!$participation) {
            throw $this->createNotFoundException('Participation introuvable.');
        }

        $qrCode = $participation->getCodeQr();
        if (empty($qrCode)) {
            throw $this->createNotFoundException('QR code introuvable.');
        }

        $filePath = $qrService->getAbsolutePath($qrCode);
        if (!file_exists($filePath)) {
            throw $this->createNotFoundException('Fichier QR code introuvable.');
        }

        $response = new BinaryFileResponse($filePath);
        $response->setContentDisposition(
            ResponseHeaderBag::DISPOSITION_ATTACHMENT,
            'mon_billet_qr.png'
        );
        return $response;
    }
}