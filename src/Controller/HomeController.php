<?php
namespace App\Controller;

use App\Repository\EvenementRepository;
use App\Repository\ProduitRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class HomeController extends AbstractController
{
    #[Route('/', name: 'home')]
    public function index(EvenementRepository $repo, ProduitRepository $produitRepo): Response
    {
        return $this->render('home/index.html.twig', [
            'evenements' => $repo->findAllWithType(),
            'produits'   => $produitRepo->findActifs(),
        ]);
    }

    #[Route('/evenements', name: 'app_evenements')]
    public function evenements(EvenementRepository $repo, ProduitRepository $produitRepo): Response
    {
        return $this->render('home/index.html.twig', [
            'evenements' => $repo->findAllWithType(),
            'produits'   => $produitRepo->findActifs(),
        ]);
    }

    #[Route('/evenements/liste', name: 'evenement_liste')]
    public function liste(EvenementRepository $repo, ProduitRepository $produitRepo): Response
    {
        return $this->render('evenement/liste.html.twig', [
            'evenements' => $repo->findAllWithType(),
            'produits'   => $produitRepo->findActifs(),
        ]);
    }

    #[Route('/home/index', name: 'home_index_redirect')]
    public function homeIndexRedirect(): Response
    {
        return $this->redirectToRoute('evenement_liste');
    }
}