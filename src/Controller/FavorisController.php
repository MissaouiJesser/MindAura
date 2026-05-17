<?php

namespace App\Controller;

use App\Entity\Favoris;
use App\Entity\Utilisateurs;
use App\Repository\FavorisRepository;
use App\Repository\RessourcesRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use App\Service\NotificationServiceRessource;

#[Route('/ressources/favoris', name: 'ressource_favori_')]
class FavorisController extends AbstractController
{
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(FavorisRepository $repo): Response
    {
        $user = $this->getUser();
        if (!$user) {
            $this->addFlash('error', 'Connectez-vous pour accéder à vos favoris.');
            return $this->redirectToRoute('ressources_index');
        }

        /** @var Utilisateurs $user */
        $userId = $user->getIdUtilisateur();
        if ($userId === null) {
            throw new \LogicException('L\'identifiant utilisateur ne peut pas être nul.');
        }

        $favoris = $repo->findByUser($userId);
        $total   = $repo->countByUser($userId);

        return $this->render('favoris/index.html.twig', [
            'favoris' => $favoris,
            'total'   => $total,
        ]);
    }

    #[Route('/toggle/{id}', name: 'toggle', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function toggle(
        int $id,
        Request $request,
        FavorisRepository $favoriRepo,
        RessourcesRepository $ressourceRepo,
        EntityManagerInterface $em,
        NotificationServiceRessource $notificationService
    ): JsonResponse {
        $user = $this->getUser();
        if (!$user) {
            return $this->json(['error' => 'Non connecté'], 401);
        }

        /** @var Utilisateurs $user */
        $token = (string) $request->request->get('_token', '');
        if (!$this->isCsrfTokenValid('ressource_favori_' . $id, $token)) {
            return $this->json(['error' => 'Token invalide'], 403);
        }

        $ressource = $ressourceRepo->find($id);
        if (!$ressource) {
            return $this->json(['error' => 'Ressource introuvable'], 404);
        }

        $userId = $user->getIdUtilisateur();
        if ($userId === null) {
            return $this->json(['error' => 'Identifiant utilisateur invalide'], 400);
        }

        $favori = $favoriRepo->findOneByUserAndRessource($userId, $id);

        if ($favori) {
            $em->remove($favori);
            $em->flush();
            return $this->json([
                'status' => 'removed',
                'total'  => $favoriRepo->countByUser($userId),
            ]);
        }

        $newFavori = new Favoris($user, $ressource);
        $em->persist($newFavori);
        $em->flush();

        $fanName = $user->getPrenomUtilisateur() ?? $user->getUserIdentifier();
        try {
            $notificationService->notifierFavoriAjoute($ressource, $fanName);
        } catch (\Exception) {
            // non bloquant
        }

        return $this->json([
            'status' => 'added',
            'total'  => $favoriRepo->countByUser($userId),
        ]);
    }

    #[Route('/remove/{id}', name: 'remove', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function remove(
        int $id,
        Request $request,
        FavorisRepository $favoriRepo,
        EntityManagerInterface $em
    ): Response {
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('ressources_index');
        }

        /** @var Utilisateurs $user */
        $userId = $user->getIdUtilisateur();
        if ($userId === null) {
            $this->addFlash('error', 'Utilisateur invalide.');
            return $this->redirectToRoute('ressources_index');
        }

        $token = (string) $request->request->get('_token', '');
        if ($this->isCsrfTokenValid('ressource_favori_remove_' . $id, $token)) {
            $favori = $favoriRepo->findOneByUserAndRessource($userId, $id);
            if ($favori) {
                $em->remove($favori);
                $em->flush();
                $this->addFlash('success', 'Retiré de vos favoris.');
            }
        } else {
            $this->addFlash('error', 'Token CSRF invalide.');
        }

        return $this->redirectToRoute('ressource_favori_index');
    }

    #[Route('/check/{id}', name: 'check', methods: ['GET'], requirements: ['id' => '\d+'])]
    public function check(int $id, FavorisRepository $repo): JsonResponse
    {
        $user = $this->getUser();
        if (!$user) {
            return $this->json(['isFavori' => false]);
        }

        /** @var Utilisateurs $user */
        $userId = $user->getIdUtilisateur();
        if ($userId === null) {
            return $this->json(['isFavori' => false]);
        }

        return $this->json([
            'isFavori' => $repo->isFavori($userId, $id),
        ]);
    }
}