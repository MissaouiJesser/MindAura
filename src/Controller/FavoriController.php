<?php

namespace App\Controller;

use App\Entity\Evenement;
use App\Entity\Favori;
use App\Repository\EvenementRepository;
use App\Repository\FavoriRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/favoris')]
class FavoriController extends AbstractController
{
    #[Route('/', name: 'favori_liste', methods: ['GET', 'POST'])]
    public function liste(Request $request, FavoriRepository $favoriRepo): Response
    {
        $email   = null;
        $favoris = [];

        if ($request->isMethod('POST')) {
            $email = trim((string) $request->request->get('email', ''));
            if ($email && filter_var($email, FILTER_VALIDATE_EMAIL)) {
                $favoris = $favoriRepo->findByEmail($email);
            }
        }

        return $this->render('favori/liste.html.twig', [
            'email'   => $email,
            'favoris' => $favoris,
        ]);
    }

    #[Route('/{id}/toggle', name: 'favori_toggle', methods: ['POST'])]
    public function toggle(
        Evenement $evenement,
        Request $request,
        EntityManagerInterface $em,
        FavoriRepository $favoriRepo
    ): JsonResponse {
        $data  = json_decode($request->getContent(), true);
        $email = trim((string) ($data['email'] ?? ''));

        if (empty($email)) {
            return $this->json([
                'success' => false,
                'error'   => 'Adresse email requise pour utiliser les favoris.',
            ], 400);
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return $this->json([
                'success' => false,
                'error'   => 'Adresse email invalide.',
            ], 400);
        }

        $favori = $em->getRepository(Favori::class)->findOneBy([
            'emailUtilisateur' => $email,
            'evenement'        => $evenement,
        ]);

        if ($favori) {
            $em->remove($favori);
            $em->flush();

            return $this->json([
                'success'  => true,
                'isFavori' => false,
                'message'  => '💔 "' . $evenement->getTitreEvenement() . '" retiré de vos favoris.',
            ]);
        } else {
            $favori = new Favori();
            $favori->setEmailUtilisateur($email);
            $favori->setEvenement($evenement);

            $em->persist($favori);
            $em->flush();

            return $this->json([
                'success'  => true,
                'isFavori' => true,
                'message'  => '❤️ "' . $evenement->getTitreEvenement() . '" ajouté à vos favoris !',
            ]);
        }
    }

    #[Route('/ids', name: 'favori_ids', methods: ['GET'])]
    public function ids(Request $request, FavoriRepository $favoriRepo): JsonResponse
    {
        $email = trim((string) $request->query->get('email', ''));

        if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return $this->json(['ids' => []]);
        }

        return $this->json(['ids' => $favoriRepo->getFavoriIds($email)]);
    }

    #[Route('/{id}/supprimer', name: 'favori_supprimer', methods: ['POST'])]
    public function supprimer(Favori $favori, Request $request, EntityManagerInterface $em): Response
    {
        $token = $request->request->get('_token');
        if ($this->isCsrfTokenValid('delete_favori' . $favori->getId(), (string) $token)) {
            $em->remove($favori);
            $em->flush();
            $this->addFlash('success', 'Favori supprimé.');
        }

        $email = $request->request->get('email', '');
        return $this->redirectToRoute('favori_liste', ['email' => $email]);
    }
}