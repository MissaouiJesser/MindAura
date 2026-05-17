<?php

namespace App\Controller;

use App\Entity\QuestionReponse;
use App\Form\QuestionReponseType;
use App\Repository\QuestionReponseRepository;
use App\Repository\TestPsychologiqueRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
#[Route('/admin/questions', name: 'admin_question_')]
class QuestionReponseController extends AbstractController
{
    #[Route('/', name: 'index', methods: ['GET'])]
    public function index(
        Request $request,
        QuestionReponseRepository $repo,
        TestPsychologiqueRepository $testRepo,
    ): Response {
        $result = $repo->searchForAdmin([
            'q'       => $request->query->get('q', ''),
            'testId'  => $request->query->getInt('testId', 0),
            'type'    => $request->query->get('type', ''),
            'oblig'   => $request->query->get('oblig', ''),
            'page'    => $request->query->getInt('page', 1),
            'perPage' => 5,
        ]);

        $tests = $testRepo->findBy([], ['titre_test' => 'ASC']);

        return $this->render('admin/questions/index.html.twig', [
            'questions' => $result['items'],
            'total'     => $result['total'],
            'page'      => $result['page'],
            'pages'     => $result['pages'],
            'q'         => (string) $request->query->get('q', ''),
            'testId'    => $request->query->getInt('testId', 0),
            'type'      => (string) $request->query->get('type', ''),
            'oblig'     => (string) $request->query->get('oblig', ''),
            'tests'     => $tests,
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $question = new QuestionReponse();
        $form = $this->createForm(QuestionReponseType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($question);
            $em->flush();
            $this->addFlash('success', 'Question créée.');
            return $this->redirectToRoute('admin_question_index');
        }

        return $this->render('admin/questions/new.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(QuestionReponse $question, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(QuestionReponseType::class, $question);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();
            $this->addFlash('success', 'Question mise à jour.');
            return $this->redirectToRoute('admin_question_index');
        }

        return $this->render('admin/questions/edit.html.twig', [
            'question' => $question,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET'])]
    public function show(QuestionReponse $question): Response
    {
        return $this->render('admin/questions/show.html.twig', [
            'question' => $question,
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(QuestionReponse $question, Request $request, EntityManagerInterface $em): Response
    {
        $token = $request->request->get('_token');
        if ($this->isCsrfTokenValid('delete_question_' . $question->getId_question_reponse(), is_string($token) ? $token : null)) {
            $em->remove($question);
            $em->flush();
            $this->addFlash('success', 'Question supprimée.');
        } else {
            $this->addFlash('danger', 'Jeton CSRF invalide.');
        }

        return $this->redirectToRoute('admin_question_index');
    }
}