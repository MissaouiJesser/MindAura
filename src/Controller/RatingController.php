<?php

namespace App\Controller;

use App\Entity\Reclamation;
use App\Entity\Reponse;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/rating', name: 'rating_')]
class RatingController extends AbstractController
{
    /**
     * Note une réclamation (1-5 étoiles).
     * 
     * @param int $id ID de la réclamation
     * @param Request $request Contient 'rating' (int 1-5)
     * @return JsonResponse
     */
    #[Route('/reclamation/{id}', name: 'reclamation', methods: ['POST'])]
    #[IsGranted('ROLE_USER')]
    public function rateReclamation(
        int $id,
        Request $request,
        EntityManagerInterface $em
    ): JsonResponse {
        try {
            // Récupère le rating du corps JSON
            $data = json_decode($request->getContent(), true);
            $rating = (int)($data['rating'] ?? 0);

            // Validation
            if ($rating < 1 || $rating > 5) {
                return $this->json([
                    'success' => false,
                    'message' => 'La note doit être entre 1 et 5.'
                ], Response::HTTP_BAD_REQUEST);
            }

            // Récupère la réclamation
            $reclamation = $em->getRepository(Reclamation::class)->find($id);
            if (!$reclamation) {
                return $this->json([
                    'success' => false,
                    'message' => 'Réclamation introuvable.'
                ], Response::HTTP_NOT_FOUND);
            }

            // Calcule la nouvelle moyenne
            $newSum = $reclamation->getRate_sum() + $rating;
            $newCount = $reclamation->getRate_count() + 1;
            $newAverage = $newSum / $newCount;

            // Met à jour l'entité
            $reclamation->setRate_sum($newSum);
            $reclamation->setRate_count($newCount);
            $reclamation->setRate_Reclamation($newAverage);

            $em->persist($reclamation);
            $em->flush();

            return $this->json([
                'success' => true,
                'moyenne' => round($newAverage, 1),
                'count' => $newCount
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'message' => 'Erreur serveur : ' . $e->getMessage()
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Note une réponse (1-5 étoiles).
     * 
     * @param int $id ID de la réponse
     * @param Request $request Contient 'rating' (int 1-5)
     * @return JsonResponse
     */
    #[Route('/reponse/{id}', name: 'reponse', methods: ['POST'])]
    #[IsGranted('ROLE_USER')]
    public function rateReponse(
        int $id,
        Request $request,
        EntityManagerInterface $em
    ): JsonResponse {
        try {
            // Récupère le rating du corps JSON
            $data = json_decode($request->getContent(), true);
            $rating = (int)($data['rating'] ?? 0);

            // Validation
            if ($rating < 1 || $rating > 5) {
                return $this->json([
                    'success' => false,
                    'message' => 'La note doit être entre 1 et 5.'
                ], Response::HTTP_BAD_REQUEST);
            }

            // Récupère la réponse
            $reponse = $em->getRepository(Reponse::class)->find($id);
            if (!$reponse) {
                return $this->json([
                    'success' => false,
                    'message' => 'Réponse introuvable.'
                ], Response::HTTP_NOT_FOUND);
            }

            // Calcule la nouvelle moyenne
            $newSum = $reponse->getRate_sum() + $rating;
            $newCount = $reponse->getRate_count() + 1;
            $newAverage = $newSum / $newCount;

            // Met à jour l'entité
            $reponse->setRate_sum($newSum);
            $reponse->setRate_count($newCount);
            $reponse->setRate_reponse($newAverage);

            $em->persist($reponse);
            $em->flush();

            return $this->json([
                'success' => true,
                'moyenne' => round($newAverage, 1),
                'count' => $newCount
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'message' => 'Erreur serveur : ' . $e->getMessage()
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
}