<?php
namespace App\Controller;

use App\Entity\TypeEvenement;
use App\Form\TypeEvenementType;
use App\Repository\TypeEvenementRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/type-evenement')]
class TypeEvenementController extends AbstractController
{
    #[Route('/', name: 'type_evenement_index', methods: ['GET'])]
    public function index(TypeEvenementRepository $repo): Response
    {
        return $this->render('admin/type_evenement/index.html.twig', [
            'types' => $repo->findAll()
        ]);
    }

    #[Route('/new', name: 'type_evenement_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $type = new TypeEvenement();
        $form = $this->createForm(TypeEvenementType::class, $type);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($type);
            $em->flush();
            $this->addFlash('success', 'Type créé avec succès !');
            return $this->redirectToRoute('type_evenement_index');
        }

        return $this->render('admin/type_evenement/form.html.twig', [
            'form' => $form->createView(),
            'title' => 'Nouveau type'
        ]);
    }

    #[Route('/edit/{id}', name: 'type_evenement_edit', methods: ['GET', 'POST'])]
    public function edit(TypeEvenement $type, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(TypeEvenementType::class, $type);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();
            $this->addFlash('success', 'Type modifié avec succès !');
            return $this->redirectToRoute('type_evenement_index');
        }

        return $this->render('admin/type_evenement/form.html.twig', [
            'form' => $form->createView(),
            'title' => 'Modifier le type',
            'type' => $type
        ]);
    }

    #[Route('/delete/{id}', name: 'type_evenement_delete', methods: ['POST'])]
    public function delete(Request $request, TypeEvenement $type, EntityManagerInterface $em): Response
    {
        $token = $request->request->get('_token');
        $id = $type->getId();
        if ($id !== null && $this->isCsrfTokenValid('delete' . $id, (string) $token)) {
            $em->remove($type);
            $em->flush();
            $this->addFlash('success', 'Type supprimé avec succès !');
        } else {
            $this->addFlash('error', 'Token CSRF invalide.');
        }
        return $this->redirectToRoute('type_evenement_index');
    }
}