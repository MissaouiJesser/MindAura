<?php

namespace App\Controller;

use App\Entity\Objectif;
use App\Entity\Utilisateurs;
use App\Repository\ObjectifRepository;
use App\Service\VoiceRssService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_USER')]
#[Route('/objectifs', name: 'front_objectifs_')]
class PatientObjectifController extends AbstractController
{
    #[Route('/', name: 'index')]
    public function index(Request $request, ObjectifRepository $repo): Response
    {
        /** @var Utilisateurs $user */
        $user   = $this->getUser();
        $userId = $user->getIdUtilisateur() ?? throw new \LogicException('User has no ID.');

        $result = $repo->searchAdoptedByUser($userId, [
            'q'       => $request->query->get('q', ''),
            'type'    => $request->query->get('type', ''),
            'statut'  => $request->query->get('statut', ''),
            'page'    => $request->query->getInt('page', 1),
            'perPage' => 8,
        ]);

        return $this->render('site/objectifs/index.html.twig', [
            'objectifs' => $result['items'],
            'total'     => $result['total'],
            'page'      => $result['page'],
            'pages'     => $result['pages'],
            'q'         => (string) $request->query->get('q', ''),
            'type'      => (string) $request->query->get('type', ''),
            'statut'    => (string) $request->query->get('statut', ''),
        ]);
    }

    #[Route('/adopt/{id}', name: 'adopt', methods: ['POST'])]
    public function adopt(Objectif $objectif, EntityManagerInterface $em, Request $request): Response
    {
        /** @var Utilisateurs $user */
        $user = $this->getUser();

        if (!$this->isCsrfTokenValid('adopt_' . $objectif->getIdObjectif(), (string) $request->request->get('_token'))) {
            $this->addFlash('danger', 'Jeton CSRF invalide.');
            return $this->redirectToRoute('front_tests_index');
        }

        $exists = $em->getRepository(Objectif::class)->findOneBy([
            'source'         => 'patient',
            'id_utilisateur' => $user->getIdUtilisateur(),
            'titre'          => $objectif->getTitre(),
            'type_test'      => $objectif->getTypeTest(),
        ]);

        if ($exists !== null) {
            $this->addFlash('warning', 'Cet objectif est deja dans votre espace.');
            return $this->redirectToRoute('front_objectifs_index');
        }

        $adopted = new Objectif();
        $adopted
            ->setSource('patient')
            ->setIdUtilisateur($user->getIdUtilisateur())
            ->setTitre($objectif->getTitre())
            ->setDescription($objectif->getDescription())
            ->setTypeObjectif('patient')
            ->setTypeTest($objectif->getTypeTest())
            ->setNiveauRecommande($objectif->getNiveauRecommande())
            ->setScoreMin($objectif->getScoreMin())
            ->setScoreMax($objectif->getScoreMax())
            ->setCategorie($objectif->getCategorie())
            ->setDureeEstimee($objectif->getDureeEstimee())
            ->setDifficule($objectif->getDifficule())
            // date_creation est initialisée automatiquement via @PrePersist
            ->setDateEcheance(
                // getDateEcheance() is non-nullable per its return type — use it directly
                \DateTimeImmutable::createFromInterface($objectif->getDateEcheance())
            )
            ->setStatut('actif')
            ->setEstPublic(false);

        $em->persist($adopted);
        $em->flush();

        $this->addFlash('success', 'Objectif "' . $objectif->getTitre() . '" adopté avec succès !');

        return $this->redirectToRoute('front_objectifs_index');
    }

    #[Route('/{id}/tts', name: 'tts', methods: ['GET'])]
    public function tts(Objectif $objectif, VoiceRssService $tts): Response
    {
        // getTitre() and getDescription() are non-nullable strings — no ?? needed
        $titre = $objectif->getTitre();
        $desc  = $objectif->getDescription();
        $text  = trim($titre . '. ' . $desc);

        if ($text === '' || $text === '.') {
            return new Response('Texte vide.', 400, ['Content-Type' => 'text/plain']);
        }

        try {
            $audio = $tts->textToSpeech($text, 'fr-fr', 'mp3');
        } catch (\Throwable $e) {
            return new Response(
                get_class($e) . ': ' . $e->getMessage() . "\n\nFile: " . $e->getFile() . ':' . $e->getLine(),
                500,
                ['Content-Type' => 'text/plain']
            );
        }

        return new Response($audio, 200, [
            'Content-Type'  => 'audio/mpeg',
            'Cache-Control' => 'private, max-age=3600',
        ]);
    }
}