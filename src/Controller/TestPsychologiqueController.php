<?php

namespace App\Controller;

use App\Entity\QuestionReponse;
use App\Entity\TestPsychologique;
use App\Form\QuestionReponseType;
use App\Form\TestPsychologiqueType;
use App\Repository\QuestionReponseRepository;
use App\Repository\ReponseClientRepository;
use App\Repository\TestPsychologiqueRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormError;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
#[Route('/admin/tests', name: 'app_admin_tests_')]
class TestPsychologiqueController extends AbstractController
{
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(Request $request, TestPsychologiqueRepository $repo): Response
    {
        $perPage = $request->query->getInt('perPage', 5);

        $result = $repo->searchForAdmin([
            'q'       => $request->query->get('q', ''),
            'type'    => $request->query->get('type', ''),
            'statut'  => $request->query->get('statut', ''),
            'page'    => $request->query->getInt('page', 1),
            'perPage' => $perPage,
        ]);

        return $this->render('admin/Tests_psychologique/tests.html.twig', [
            'tests'   => $result['items'],
            'total'   => $result['total'],
            'page'    => $result['page'],
            'pages'   => $result['pages'],
            'perPage' => $result['perPage'],
            'q'       => (string) $request->query->get('q', ''),
            'type'    => (string) $request->query->get('type', ''),
            'statut'  => (string) $request->query->get('statut', ''),
        ]);
    }

    #[Route('/new', name: 'new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $em): Response
    {
        $test = new TestPsychologique();
        $form = $this->createForm(TestPsychologiqueType::class, $test);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($test);
            $em->flush();

            $this->addFlash('success', 'Test créé avec succès.');
            return $this->redirectToRoute('app_admin_tests_index');
        }

        return $this->render('admin/Tests_psychologique/new.html.twig', [
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/edit', name: 'edit', methods: ['GET', 'POST'])]
    public function edit(TestPsychologique $test, Request $request, EntityManagerInterface $em): Response
    {
        $form = $this->createForm(TestPsychologiqueType::class, $test);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->flush();

            $this->addFlash('success', 'Test mis à jour.');
            return $this->redirectToRoute('app_admin_tests_index');
        }

        return $this->render('admin/Tests_psychologique/edit.html.twig', [
            'test' => $test,
            'form' => $form->createView(),
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'])]
    public function delete(
        TestPsychologique $test,
        Request $request,
        EntityManagerInterface $em,
        ReponseClientRepository $reponseClientRepo,
        QuestionReponseRepository $questionRepo,
    ): Response {
        if (!$this->isCsrfTokenValid('delete_test_' . $test->getIdTest(), (string) $request->request->get('_token'))) {
            $this->addFlash('danger', 'Jeton CSRF invalide.');
            return $this->redirectToRoute('app_admin_tests_index');
        }

        try {
            foreach ($reponseClientRepo->findBy(['testPsychologique' => $test]) as $rc) {
                $em->remove($rc);
            }
            foreach ($questionRepo->findBy(['testPsychologique' => $test]) as $qr) {
                $em->remove($qr);
            }
            $em->remove($test);
            $em->flush();
            $this->addFlash('success', 'Test supprimé.');
        } catch (\Throwable) {
            $this->addFlash('danger', 'Impossible de supprimer ce test (erreur base de données).');
        }

        return $this->redirectToRoute('app_admin_tests_index');
    }

    #[Route('/{id}/show', name: 'show', methods: ['GET'])]
    public function show(TestPsychologique $test): Response
    {
        return $this->render('admin/Tests_psychologique/show.html.twig', [
            'test' => $test,
        ]);
    }

    #[Route('/{id}/questions', name: 'questions', methods: ['GET', 'POST'])]
    public function questions(
        TestPsychologique $test,
        Request $request,
        EntityManagerInterface $em,
        QuestionReponseRepository $questionRepo
    ): Response {
        $editId = $request->query->getInt('edit', 0);
        $question = null;

        if ($editId > 0) {
            $question = $questionRepo->findOneBy([
                'id_question_reponse' => $editId,
                'testPsychologique'   => $test,
            ]);
            if ($question === null) {
                $this->addFlash('danger', 'Question introuvable pour ce test.');
                return $this->redirectToRoute('app_admin_tests_questions', ['id' => $test->getIdTest()]);
            }
        } else {
            $question = new QuestionReponse();
            $question->setTestPsychologique($test);
        }

        $form = $this->createForm(QuestionReponseType::class, $question, [
            'test_locked' => true,
        ]);
        $form->handleRequest($request);

        if ($form->isSubmitted()) {
            $duplicate = $questionRepo->findOneBy([
                'testPsychologique' => $test,
                'ordre_question'    => $question->getOrdreQuestion(),
            ]);

            if ($duplicate !== null && $duplicate->getIdQuestionReponse() !== $editId) {
                $form->get('ordre_question')->addError(
                    new FormError('Cet ordre est déjà utilisé pour une autre question de ce test.')
                );
            }

            if ($form->isValid()) {
                if ($editId === 0) {  // nouvelle question : pas encore persistée
                    $em->persist($question);
                }
                $em->flush();
                $this->addFlash('success', $editId > 0 ? 'Question mise à jour.' : 'Question ajoutée au test.');
                return $this->redirectToRoute('app_admin_tests_questions', ['id' => $test->getIdTest()]);
            }
        }

        $questions = $questionRepo->findBy(
            ['testPsychologique' => $test],
            ['ordre_question'    => 'ASC']
        );

        return $this->render('admin/Tests_psychologique/questions.html.twig', [
            'test'            => $test,
            'questions'       => $questions,
            'form'            => $form->createView(),
            'editingQuestion' => $editId > 0 ? $question : null,
        ]);
    }

    #[Route('/{id}/questions/{questionId}/delete', name: 'question_delete', methods: ['POST'])]
    public function deleteQuestion(
        TestPsychologique $test,
        int $questionId,
        Request $request,
        EntityManagerInterface $em,
        QuestionReponseRepository $questionRepo,
        ReponseClientRepository $reponseClientRepo
    ): Response {
        $question = $questionRepo->findOneBy([
            'id_question_reponse' => $questionId,
            'testPsychologique'   => $test,
        ]);

        if ($question === null) {
            $this->addFlash('danger', 'Question introuvable.');
            return $this->redirectToRoute('app_admin_tests_questions', ['id' => $test->getIdTest()]);
        }

        if (!$this->isCsrfTokenValid('delete_question_' . $question->getIdQuestionReponse(), (string) $request->request->get('_token'))) {
            $this->addFlash('danger', 'Jeton CSRF invalide.');
            return $this->redirectToRoute('app_admin_tests_questions', ['id' => $test->getIdTest()]);
        }

        foreach ($reponseClientRepo->findBy(['questionReponse' => $question]) as $reponse) {
            $em->remove($reponse);
        }
        $em->remove($question);
        $em->flush();

        $this->addFlash('success', 'Question supprimée.');
        return $this->redirectToRoute('app_admin_tests_questions', ['id' => $test->getIdTest()]);
    }
}