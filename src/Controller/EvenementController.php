<?php
namespace App\Controller;

use App\Entity\Evenement;
use App\Form\EvenementSearchType;
use App\Form\EvenementType;
use App\Repository\EvenementRepository;
use App\Repository\ProduitRepository;
use App\Service\EvenementMailer;
use App\Service\EvenementPdfExporter;
use App\Service\EvenementStatutUpdater;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/evenement')]
class EvenementController extends AbstractController
{
    public function __construct(
        private EvenementStatutUpdater $statutUpdater,
        private EvenementMailer $evenementMailer
    ) {}

    #[Route('/', name: 'evenement_index', methods: ['GET'])]
    public function index(Request $request, EvenementRepository $repo): Response
    {
        $session = $request->getSession();
        $today = date('Y-m-d');

        if ($session->get('statut_last_update') !== $today) {
            $this->statutUpdater->updateAll();
            $session->set('statut_last_update', $today);
        }

        $searchForm = $this->createForm(EvenementSearchType::class);
        $searchForm->handleRequest($request);

        $criteria = [];
        if ($searchForm->isSubmitted() && $searchForm->isValid()) {
            $data = $searchForm->getData();
            if (!empty($data['query'])) $criteria['query'] = (string) $data['query'];
            if (!empty($data['statut'])) $criteria['statut'] = (string) $data['statut'];
            if (!empty($data['typeEvenement'])) $criteria['type'] = (int) $data['typeEvenement'];
            if (!empty($data['dateDebut'])) $criteria['dateFrom'] = $data['dateDebut'];
            if (!empty($data['dateFin'])) $criteria['dateTo'] = $data['dateFin'];
            if (isset($data['gratuit']) && $data['gratuit'] !== '') {
                $criteria['gratuit'] = (bool) $data['gratuit'];
            }
        }

        $evenements = $repo->search($criteria);

        return $this->render('admin/evenement/index.html.twig', [
            'evenements' => $evenements,
            'searchForm' => $searchForm->createView(),
            'totalCount' => count($evenements),
        ]);
    }

    #[Route('/export-pdf', name: 'evenement_export_pdf', methods: ['GET'])]
    public function exportPdf(Request $request, EvenementRepository $repo, EvenementPdfExporter $exporter): Response
    {
        $criteria = [];
        if ($v = $request->query->get('query')) $criteria['query'] = (string) $v;
        if ($v = $request->query->get('statut')) $criteria['statut'] = (string) $v;
        if ($v = $request->query->get('type')) $criteria['type'] = (int) $v;
        if ($v = $request->query->get('dateDebut')) $criteria['dateFrom'] = new \DateTime((string) $v);
        if ($v = $request->query->get('dateFin')) $criteria['dateTo'] = new \DateTime((string) $v);
        if ($v = $request->query->get('gratuit')) $criteria['gratuit'] = (bool) $v;

        $evenements = $repo->search($criteria);
        return $exporter->export($evenements);
    }

    #[Route('/new', name: 'evenement_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $evenement = new Evenement();
        $form = $this->createForm(EvenementType::class, $evenement);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->statutUpdater->updateOne($evenement);

            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                try {
                    $newFilename = uniqid() . '.' . $imageFile->guessExtension();
                    $imageFile->move($this->getParameter('images_directory'), $newFilename);
                    $evenement->setImage($newFilename);
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l’upload de l’image.');
                }
            }

            $em->persist($evenement);
            $em->flush();

            $this->addFlash('success', 'Événement créé avec succès !');
            return $this->redirectToRoute('evenement_index');
        }

        return $this->render('admin/evenement/form.html.twig', [
            'form' => $form->createView(),
            'title' => 'Nouvel événement',
            'evenement' => null,
        ]);
    }

    #[Route('/edit/{id}', name: 'evenement_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, EntityManagerInterface $em, EvenementRepository $repo, int $id): Response
    {
        $evenement = $repo->find($id);
        if (!$evenement) {
            throw $this->createNotFoundException('Événement introuvable.');
        }

        $form = $this->createForm(EvenementType::class, $evenement);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $this->statutUpdater->updateOne($evenement);

            $imageFile = $form->get('imageFile')->getData();
            if ($imageFile) {
                try {
                    $newFilename = uniqid() . '.' . $imageFile->guessExtension();
                    $imageFile->move($this->getParameter('images_directory'), $newFilename);
                    $evenement->setImage($newFilename);
                } catch (FileException $e) {
                    $this->addFlash('error', 'Erreur lors de l’upload de l’image.');
                }
            }

            $em->flush();

            $this->evenementMailer->sendEvenementModifie($evenement);

            $this->addFlash('success', 'Événement modifié avec succès !');
            return $this->redirectToRoute('evenement_index');
        }

        return $this->render('admin/evenement/form.html.twig', [
            'form' => $form->createView(),
            'title' => "Modifier l'événement",
            'evenement' => $evenement,
        ]);
    }

    #[Route('/delete/{id}', name: 'evenement_delete', methods: ['POST'])]
    public function delete(Request $request, EntityManagerInterface $em, EvenementRepository $repo, int $id): Response
    {
        $evenement = $repo->find($id);
        if (!$evenement) {
            throw $this->createNotFoundException('Événement introuvable.');
        }

        $token = $request->request->get('_token');
        if ($this->isCsrfTokenValid('delete' . $evenement->getId(), (string) $token)) {
            $this->evenementMailer->sendEvenementSupprime($evenement);
            $em->remove($evenement);
            $em->flush();
            $this->addFlash('error', 'Événement supprimé.');
        } else {
            $this->addFlash('warning', 'Token CSRF invalide, suppression annulée.');
        }

        return $this->redirectToRoute('evenement_index');
    }

    #[Route('/show/{id}', name: 'evenement_show', methods: ['GET'])]
    public function show(EvenementRepository $repo, int $id): Response
    {
        $evenement = $repo->findWithDetails($id);
        if (!$evenement) {
            throw $this->createNotFoundException('Événement introuvable.');
        }
        return $this->render('admin/evenement/show.html.twig', [
            'evenement' => $evenement,
        ]);
    }

    #[Route('/public', name: 'evenement_public_index', methods: ['GET'])]
    public function publicIndex(Request $request, EvenementRepository $repo): Response
    {
        $this->statutUpdater->updateAll();

        $page    = max(1, $request->query->getInt('page', 1));
        $limit   = 20;
        $offset  = ($page - 1) * $limit;
        $total   = $repo->countAll();

        $evenements = $repo->findAllWithType($limit, $offset);

        return $this->render('evenement/index.html.twig', [
            'evenements' => $evenements,
            'total'      => $total,
            'page'       => $page,
            'pages'      => (int) ceil($total / $limit),
        ]);
    }

    #[Route('/public/{id}', name: 'evenement_public_show', methods: ['GET'])]
    public function publicShow(EvenementRepository $repo, int $id): Response
    {
        $evenement = $repo->findWithDetails($id);
        if (!$evenement) {
            throw $this->createNotFoundException('Événement introuvable.');
        }
        return $this->render('evenement/show.html.twig', [
            'evenement' => $evenement,
        ]);
    }

    #[Route('/evenements/liste', name: 'evenement_admin_liste')]
    public function liste(EvenementRepository $evRepo, ProduitRepository $prodRepo): Response
    {
        $evenements = $evRepo->findBy([], ['id' => 'DESC'], 50);
        $produits   = $prodRepo->findBy([], ['id' => 'DESC'], 50);
        return $this->render('evenement/liste.html.twig', [
            'evenements' => $evenements,
            'produits'   => $produits,
        ]);
    }
}