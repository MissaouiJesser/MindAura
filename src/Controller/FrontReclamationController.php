<?php

namespace App\Controller;

use App\Entity\Reclamation;
use App\Entity\Reponse;
use App\Entity\Utilisateurs;
use App\Form\ReclamationType;
use App\Form\ReponseType;
use App\Repository\ReclamationRepository;
use App\Service\IdentitySafeInsertService;
use App\Service\GroqServiceR;
use App\Service\ProfanityCheckerService;
use App\Service\ReclamationNotificationMailer;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/espace')]
class FrontReclamationController extends AbstractController
{
    public function __construct(
        private readonly ProfanityCheckerService $profanityChecker,
    ) {}

    #[Route('/reclamations', name: 'front_reclamation_index', methods: ['GET'])]
    public function index(Request $request, ReclamationRepository $repo, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        $currentUserId = $user instanceof Utilisateurs ? $user->getIdUtilisateur() : null;

        if ($request->query->has('mine')) {
            $mine = $request->query->get('mine') === '1';
        } else {
            $mine = true;
        }

        $params = [
            'search'    => $request->query->get('search', ''),
            'statut'    => $request->query->get('statut', ''),
            'categorie' => $request->query->get('categorie', ''),
            'dateDebut' => $request->query->get('date_from', ''),
            'dateFin'   => $request->query->get('date_to', ''),
            'sort'      => $request->query->get('sort', ReclamationRepository::DEFAULT_SORT),
            'direction' => $request->query->get('direction', ReclamationRepository::DEFAULT_DIRECTION),
            'page'      => $request->query->getInt('page', 1),
            'perPage'   => $request->query->getInt('perPage', ReclamationRepository::PAGE_SIZE),
        ];

        if ($mine && $currentUserId) {
            $params['userId'] = $currentUserId;
        }
        if ($mine && !$currentUserId) {
            $mine = false;
        }

        $pagination = $repo->findFiltered($params);
        $categories = $em->getRepository(\App\Entity\Categorie::class)->findAll();
        $authorMap  = $this->buildUserDisplayMap($em, array_map(
            static fn(Reclamation $r) => $r->getId_utilisateur(),
            $pagination['items']
        ));

        return $this->render('reclamation/front/index.html.twig', [
            'reclamations'     => $pagination['items'],
            'categories'       => $categories,
            'total'            => $pagination['total'],
            'pages'            => $pagination['pages'],
            'page'             => $pagination['page'],
            'perPage'          => $pagination['perPage'],
            'search'           => $params['search'],
            'statut'           => $params['statut'],
            'categorie_filter' => $params['categorie'],
            'date_from'        => $params['dateDebut'],
            'date_to'          => $params['dateFin'],
            'sort'             => $params['sort'],
            'direction'        => $params['direction'],
            'mine'             => $mine,
            'authorMap'        => $authorMap,
            'currentUserId'    => $currentUserId,
        ]);
    }

    #[Route('/reclamations/nouvelle', name: 'front_reclamation_new', methods: ['GET', 'POST'])]
    public function new(
        Request $request,
        EntityManagerInterface $em,
        IdentitySafeInsertService $identityInsert,
        ReclamationNotificationMailer $reclamationMailer,
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }

        // date_creation initialisée dans le constructeur de Reclamation
        $reclamation = new Reclamation();
        $reclamation->setId_utilisateur($user->getIdUtilisateur() ?? throw new \LogicException('User has no ID.'));
        $reclamation->setStatut_reclamation('En attente');
        $reclamation->setRate_Reclamation(0.0);
        $reclamation->setRate_sum(0);
        $reclamation->setRate_count(0);

        $form = $this->createForm(ReclamationType::class, $reclamation, [
            'context'       => 'front_new',
            'csrf_token_id' => 'front_reclamation_new',
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $sujetCheck = $this->profanityChecker->check($reclamation->getSujet_reclamation());
            $descCheck  = $this->profanityChecker->check($reclamation->getDescription_reclamation());

            if ($sujetCheck['isProfanity']) {
                $this->addFlash('error', '🚫 Le sujet contient des termes offensants. Merci de reformuler.');
                return $this->render('reclamation/front/new.html.twig', ['form' => $form]);
            }

            if ($descCheck['isProfanity']) {
                $this->addFlash('error', '🚫 La description contient des termes offensants. Merci de reformuler.');
                return $this->render('reclamation/front/new.html.twig', ['form' => $form]);
            }

            try {
                $newId = $identityInsert->insertReclamation($em, $reclamation);
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Enregistrement impossible : ' . $e->getMessage());
                return $this->render('reclamation/front/new.html.twig', ['form' => $form]);
            }

            try {
                $created = $em->getRepository(Reclamation::class)->find($newId);
                if ($created instanceof Reclamation) {
                    $reclamationMailer->notifyAdminNewReclamation($created, $user);
                }
            } catch (\Throwable) {
            }

            $this->addFlash('success', 'Votre réclamation a été enregistrée.');
            return $this->redirectToRoute('front_reclamation_show', ['id' => $newId]);
        }

        return $this->render('reclamation/front/new.html.twig', ['form' => $form]);
    }

    #[Route('/reclamations/{id}', name: 'front_reclamation_show', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function show(Reclamation $reclamation, EntityManagerInterface $em): Response
    {
        $user          = $this->getUser();
        $currentUserId = $user instanceof Utilisateurs ? $user->getIdUtilisateur() : null;
        $authorMap     = $this->buildUserDisplayMap($em, [$reclamation->getId_utilisateur()]);

        $sortedReponses = $reclamation->getReponses()->toArray();
        usort($sortedReponses, static fn(Reponse $a, Reponse $b) => $a->getDate_reponse() <=> $b->getDate_reponse());

        // date_reponse initialisée dans le constructeur de Reponse
        $replyDraft = new Reponse();
        $replyDraft->setReclamation($reclamation);
        if ($user instanceof Utilisateurs) {
            $replyDraft->setId_utilisateur($user->getIdUtilisateur());
            $replyDraft->setNom_utilisateur(trim($user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur()));
        }

        $replyForm = $this->createForm(ReponseType::class, $replyDraft, [
            'context'       => 'front_reply',
            'action'        => $this->generateUrl('front_reclamation_reply', ['id' => $reclamation->getId_reclamation()]),
            'method'        => 'POST',
            'csrf_token_id' => 'reply_reclamation_' . $reclamation->getId_reclamation(),
        ]);

        return $this->render('reclamation/front/show.html.twig', [
            'reclamation'    => $reclamation,
            'sortedReponses' => $sortedReponses,
            'authorName'     => $authorMap[$reclamation->getId_utilisateur()] ?? 'Membre',
            'currentUserId'  => $currentUserId,
            'replyForm'      => $replyForm,
        ]);
    }

    #[Route('/reclamations/{id}/modifier', name: 'front_reclamation_edit', methods: ['GET', 'POST'], requirements: ['id' => '\d+'])]
    public function edit(Request $request, Reclamation $reclamation, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }
        if ($reclamation->getId_utilisateur() !== $user->getIdUtilisateur()) {
            throw $this->createAccessDeniedException();
        }

        $form = $this->createForm(ReclamationType::class, $reclamation, [
            'context'       => 'front_edit',
            'csrf_token_id' => 'front_reclamation_edit_' . $reclamation->getId_reclamation(),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            foreach ([
                'sujet'       => $reclamation->getSujet_reclamation(),
                'description' => $reclamation->getDescription_reclamation(),
            ] as $field => $value) {
                if ($this->profanityChecker->isProfane($value)) {
                    $this->addFlash('error', sprintf('🚫 Le %s contient des termes offensants.', $field));
                    return $this->render('reclamation/front/edit.html.twig', [
                        'form'        => $form,
                        'reclamation' => $reclamation,
                    ]);
                }
            }

            $em->flush();
            $this->addFlash('success', 'Réclamation mise à jour.');
            return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        return $this->render('reclamation/front/edit.html.twig', [
            'form'        => $form,
            'reclamation' => $reclamation,
        ]);
    }

    #[Route('/reclamations/{id}/supprimer', name: 'front_reclamation_delete', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function delete(Request $request, Reclamation $reclamation, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }
        if ($reclamation->getId_utilisateur() !== $user->getIdUtilisateur()) {
            throw $this->createAccessDeniedException();
        }

        if (!$this->isCsrfTokenValid('delete_reclamation_' . $reclamation->getId_reclamation(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Jeton de sécurité invalide.');
            return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        $em->remove($reclamation);
        $em->flush();
        $this->addFlash('success', 'Réclamation supprimée.');
        return $this->redirectToRoute('front_reclamation_index');
    }

    #[Route('/reclamations/{id}/repondre', name: 'front_reclamation_reply', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function reply(
        Request $request,
        Reclamation $reclamation,
        EntityManagerInterface $em,
        IdentitySafeInsertService $identityInsert,
    ): Response {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }

        // date_reponse initialisée dans le constructeur de Reponse
        $reponse = new Reponse();
        $reponse->setReclamation($reclamation);
        $reponse->setId_utilisateur($user->getIdUtilisateur());
        $reponse->setNom_utilisateur(trim($user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur()));
        $reponse->setRate_reponse(0.0);
        $reponse->setRate_sum(0);
        $reponse->setRate_count(0);

        $form = $this->createForm(ReponseType::class, $reponse, [
            'context'       => 'front_reply',
            'csrf_token_id' => 'reply_reclamation_' . $reclamation->getId_reclamation(),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($this->profanityChecker->isProfane($reponse->getContenu_reponse())) {
                $this->addFlash('error', '🚫 Votre réponse contient des termes offensants. Merci de rester respectueux.');
                return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
            }

            try {
                $identityInsert->insertReponse($em, $reponse);
            } catch (\Throwable $e) {
                $this->addFlash('error', 'Publication impossible : ' . $e->getMessage());
                return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
            }

            $this->addFlash('success', 'Votre réponse a été publiée.');
            return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
        }

        if ($form->isSubmitted()) {
            $this->addFlash('error', 'Le message ne peut pas être vide ou est invalide.');
        }

        return $this->redirectToRoute('front_reclamation_show', ['id' => $reclamation->getId_reclamation()]);
    }

    #[Route('/reponses/{id}/modifier', name: 'front_reponse_edit', methods: ['GET', 'POST'], requirements: ['id' => '\d+'])]
    public function editReponse(Request $request, Reponse $reponse, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }
        if ($reponse->getId_utilisateur() !== $user->getIdUtilisateur()) {
            throw $this->createAccessDeniedException();
        }

        $rid  = $reponse->getReclamation()?->getId_reclamation();
        $form = $this->createForm(ReponseType::class, $reponse, [
            'context'       => 'front_edit',
            'csrf_token_id' => 'front_reponse_edit_' . $reponse->getId_reponse(),
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if ($this->profanityChecker->isProfane($reponse->getContenu_reponse())) {
                $this->addFlash('error', '🚫 Votre réponse modifiée contient des termes offensants.');
                return $this->render('reclamation/front/reponse_edit.html.twig', [
                    'form'        => $form,
                    'reponse'     => $reponse,
                    'reclamation' => $reponse->getReclamation(),
                ]);
            }

            $reponse->setNom_utilisateur(trim($user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur()));
            $em->flush();
            $this->addFlash('success', 'Réponse mise à jour.');
            return $this->redirectToRoute('front_reclamation_show', ['id' => $rid]);
        }

        return $this->render('reclamation/front/reponse_edit.html.twig', [
            'form'        => $form,
            'reponse'     => $reponse,
            'reclamation' => $reponse->getReclamation(),
        ]);
    }

    #[Route('/reclamations/{id}/suggest-reponse', name: 'front_reclamation_suggest_reponse', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function suggestReponse(Reclamation $reclamation, GroqServiceR $groqServiceR): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->json(['error' => 'Non autorisé'], 403);
        }

        if ($reclamation->getId_utilisateur() !== $user->getIdUtilisateur()) {
            return $this->json(['error' => 'Accès refusé'], 403);
        }

        $suggestion = $groqServiceR->suggestReponse(
            $reclamation->getSujet_reclamation(),
            $reclamation->getDescription_reclamation()
        );

        return $this->json(['suggestion' => $suggestion]);
    }

    #[Route('/reponses/{id}/supprimer', name: 'front_reponse_delete', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function deleteReponse(Request $request, Reponse $reponse, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user instanceof Utilisateurs) {
            return $this->redirectToRoute('app_login');
        }
        if ($reponse->getId_utilisateur() !== $user->getIdUtilisateur()) {
            throw $this->createAccessDeniedException();
        }

        $rid = $reponse->getReclamation()?->getId_reclamation();

        if (!$this->isCsrfTokenValid('delete_reponse_' . (string) $reponse->getId_reponse(), (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Jeton de sécurité invalide.');
        } else {
            $em->remove($reponse);
            $em->flush();
            $this->addFlash('success', 'Réponse supprimée.');
        }

        return $this->redirectToRoute('front_reclamation_show', ['id' => $rid]);
    }

    /**
     * @param int[] $userIds
     * @return array<int, string>
     */
    private function buildUserDisplayMap(EntityManagerInterface $em, array $userIds): array
    {
        $userIds = array_values(array_unique(array_filter($userIds)));
        if ($userIds === []) {
            return [];
        }

        $map = [];
        foreach ($userIds as $id) {
            $map[$id] = 'Membre';
        }

        $users = $em->getRepository(Utilisateurs::class)->createQueryBuilder('u')
            ->where('u.idUtilisateur IN (:ids)')
            ->setParameter('ids', $userIds)
            ->getQuery()
            ->getResult();

        foreach ($users as $u) {
            $map[$u->getIdUtilisateur()] = trim($u->getPrenomUtilisateur() . ' ' . $u->getNomUtilisateur());
        }

        return $map;
    }
}