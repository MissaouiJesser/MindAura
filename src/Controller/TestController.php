<?php

namespace App\Controller;

use App\Entity\Objectif;
use App\Entity\ReponseClient;
use App\Entity\TestPsychologique;
use App\Form\Front\TestResponseType;
use App\Repository\ObjectifRepository;
use App\Repository\ReponseClientRepository;
use App\Repository\TestPsychologiqueRepository;
use App\Entity\Utilisateurs;
use App\Service\ExportExcelService;
use App\Service\ExportPdfService;
use App\Service\GroqService;
use App\Service\MailService;
use App\Service\QuotableService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_USER')]
class TestController extends AbstractController
{
    // ────────────────────────────────────────────────────────────────────────
    // Liste des tests disponibles
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests', name: 'front_tests_index')]
    public function index(
        Request $request,
        TestPsychologiqueRepository $testRepo,
        QuotableService $quotableService
    ): Response {
        $result = $testRepo->searchForFront([
            'q'       => $request->query->get('q', ''),
            'type'    => $request->query->get('type', ''),
            'page'    => $request->query->getInt('page', 1),
            'perPage' => 6,
        ]);

        $quote = $quotableService->getRandomQuote(
            (string) $request->query->get('type', '')
        );

        return $this->render('site/tests/index.html.twig', [
            'tests' => $result['items'],
            'total' => $result['total'],
            'page'  => $result['page'],
            'pages' => $result['pages'],
            'q'     => (string) $request->query->get('q', ''),
            'type'  => (string) $request->query->get('type', ''),
            'quote' => $quote,
        ]);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Passer un test
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/{id}/take', name: 'front_tests_take')]
    public function take(
        TestPsychologique $test,
        Request $request,
        EntityManagerInterface $em,
        ObjectifRepository $objectifRepo,
        GroqService $groqService,
        MailService $mailService
    ): Response {
        /** @var Utilisateurs $user */
        $user      = $this->getUser();
        $questions = $test->getQuestionReponses();
        $answeredCount = 0;

        $form = $this->createForm(TestResponseType::class, null, [
            'questions' => $questions,
        ]);

        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $data       = $form->getData();
            $totalScore = 0;

            // Un seul passage "actif" par test/utilisateur : on remplace les anciennes réponses.
            foreach ($em->getRepository(ReponseClient::class)->findBy([
                'testPsychologique' => $test,
                'id_utilisateur'    => $user->getIdUtilisateur(),
            ]) as $old) {
                $em->remove($old);
            }

            foreach ($questions as $question) {
                $key = 'question_' . $question->getIdQuestionReponse();

                if (!\array_key_exists($key, $data)) {
                    continue;
                }

                $value    = $data[$key];
                $freeText = $question->getTypeQuestion() === 'texte_libre' || $question->getOptions() === [];

                if ($freeText) {
                    $text = trim((string) $value);
                    if ($text === '') {
                        continue;
                    }
                    $reponseClient = new ReponseClient();
                    $reponseClient->setTestPsychologique($test);
                    $reponseClient->setQuestionReponse($question);
                    $reponseClient->setIdUtilisateur($user->getIdUtilisateur());
                    $reponseClient->setOptionChoisie(null);
                    $reponseClient->setReponseTexteLibre($text);
                    $reponseClient->setScoreObtenu(0);
                    // date_reponse est initialisée automatiquement via @PrePersist
                    $em->persist($reponseClient);
                    continue;
                }

                if ($value === null || $value === '') {
                    continue;
                }

                $selectedIndex = (int) $value;
                $options       = $question->getOptions();
                $scores        = $question->getScores();

                if (!isset($options[$selectedIndex], $scores[$selectedIndex])) {
                    continue;
                }

                $reponseClient = new ReponseClient();
                $reponseClient->setTestPsychologique($test);
                $reponseClient->setQuestionReponse($question);
                $reponseClient->setIdUtilisateur($user->getIdUtilisateur());
                $reponseClient->setOptionChoisie($options[$selectedIndex]);
                $reponseClient->setScoreObtenu($scores[$selectedIndex]);
                // date_reponse est initialisée automatiquement via @PrePersist

                $em->persist($reponseClient);
                $totalScore += $scores[$selectedIndex];
                ++$answeredCount;
            }

            $em->flush();

            $suggested = $objectifRepo->findSuggestedForScore((string) $test->getTypeTest(), $totalScore);

            // ── Analyse Groq ─────────────────────────────────────────────────
            $groqAnalysis = $groqService->analyzeTestResult(
                (string) $test->getTitreTest(),
                (string) $test->getTypeTest(),
                $totalScore,
                $answeredCount,
                $questions->count()
            );

            $request->getSession()->set('groq_analysis', $groqAnalysis);
            // ────────────────────────────────────────────────────────────────

            // ── Envoi email ──────────────────────────────────────────────────
            $mailResult = $mailService->sendTestResult(
                $user,
                $test,
                $totalScore,
                $answeredCount,
                $suggested,
                $groqAnalysis,
            );
            $this->addFlash($mailResult['flash_type'], $mailResult['flash_message']);
            // ────────────────────────────────────────────────────────────────

            return $this->redirectToRoute('front_tests_results', [
                'id'    => $test->getIdTest(),
                'score' => $totalScore,
            ]);
        }

        return $this->render('site/tests/take.html.twig', [
            'test' => $test,
            'form' => $form->createView(),
        ]);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Résultats après soumission
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/{id}/results/{score}', name: 'front_tests_results')]
    public function results(
        TestPsychologique $test,
        int $score,
        Request $request,
        ObjectifRepository $objectifRepo,
        GroqService $groqService
    ): Response {
        /** @var Utilisateurs $user */
        $user = $this->getUser();

        $suggested = $objectifRepo->findSuggestedForScore((string) $test->getTypeTest(), $score);

        $adoptedTitles = array_map(
            static fn (Objectif $objectif): string => (string) $objectif->getTitre(),
            $objectifRepo->findBy([
                'source'         => 'patient',
                'id_utilisateur' => $user->getIdUtilisateur(),
            ])
        );

        $groqAnalysis = $request->getSession()->get('groq_analysis', '');
        $request->getSession()->remove('groq_analysis');

        if ($groqAnalysis === '') {
            $groqAnalysis = $groqService->analyzeTestResult(
                (string) $test->getTitreTest(),
                (string) $test->getTypeTest(),
                $score,
                $score,
                $test->getQuestionReponses()->count()
            );
        }

        return $this->render('site/tests/results.html.twig', [
            'test'                => $test,
            'score'               => $score,
            'suggested_objectifs' => $suggested,
            'adopted_titles'      => $adoptedTitles,
            'groq_analysis'       => $groqAnalysis,
        ]);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Réponses d'un test spécifique
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/{id}/responses', name: 'front_tests_responses')]
    public function responses(TestPsychologique $test, ReponseClientRepository $reponseRepo): Response
    {
        /** @var Utilisateurs $user */
        $user = $this->getUser();

        $reponses = $reponseRepo->findBy(
            [
                'testPsychologique' => $test,
                'id_utilisateur'    => $user->getIdUtilisateur(),
            ],
            ['date_reponse' => 'DESC']
        );

        return $this->render('site/tests/responses.html.twig', [
            'test'     => $test,
            'reponses' => $reponses,
        ]);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Toutes mes réponses (avec pagination & filtres)
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/mes-reponses', name: 'front_tests_my_responses')]
    public function myResponses(
        Request $request,
        ReponseClientRepository $reponseRepo,
        TestPsychologiqueRepository $testRepo
    ): Response {
        /** @var Utilisateurs $user */
        $user   = $this->getUser();
        $userId = $user->getIdUtilisateur() ?? throw new \LogicException('User has no ID.');

        $q      = (string) $request->query->get('q', '');
        $testId = $request->query->getInt('testId', 0);

        $result = $reponseRepo->searchUserResponses($userId, [
            'q'       => $q,
            'testId'  => $testId,
            'page'    => $request->query->getInt('page', 1),
            'perPage' => 10,
        ]);

        $exportQuery = [];
        if ($q !== '') {
            $exportQuery['q'] = $q;
        }
        if ($testId > 0) {
            $exportQuery['testId'] = $testId;
        }

        return $this->render('site/tests/my_responses.html.twig', [
            'reponses'    => $result['items'],
            'total'       => $result['total'],
            'page'        => $result['page'],
            'pages'       => $result['pages'],
            'q'           => $q,
            'testId'      => $testId,
            'tests'       => $testRepo->findBy(['est_actif' => true], ['titre_test' => 'ASC']),
            'exportQuery' => $exportQuery,
        ]);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Export PDF
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/mes-reponses/export/pdf', name: 'front_tests_my_responses_export_pdf', methods: ['GET'])]
    public function myResponsesExportPdf(
        Request $request,
        ReponseClientRepository $repo,
        ExportPdfService $exportPdfService
    ): Response {
        /** @var Utilisateurs $user */
        $user    = $this->getUser();
        $userId  = $user->getIdUtilisateur() ?? throw new \LogicException('User has no ID.');
        $filters = [
            'q'      => $request->query->get('q', ''),
            'testId' => $request->query->getInt('testId', 0),
        ];

        $rows = $repo->findAllForUserExport($userId, $filters);

        return $exportPdfService->generateResponse($rows, $filters);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Export Excel
    // ────────────────────────────────────────────────────────────────────────

    #[Route('/tests/mes-reponses/export/excel', name: 'front_tests_my_responses_export_excel', methods: ['GET'])]
    public function myResponsesExportExcel(
        Request $request,
        ReponseClientRepository $repo,
        ExportExcelService $exportExcelService
    ): StreamedResponse {
        /** @var Utilisateurs $user */
        $user    = $this->getUser();
        $userId  = $user->getIdUtilisateur() ?? throw new \LogicException('User has no ID.');
        $filters = [
            'q'      => $request->query->get('q', ''),
            'testId' => $request->query->getInt('testId', 0),
        ];

        $rows = $repo->findAllForUserExport($userId, $filters);

        return $exportExcelService->generateResponse($rows);
    }
}