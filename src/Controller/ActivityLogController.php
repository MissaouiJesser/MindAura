<?php

namespace App\Controller;

use App\Enum\ActivityActionEnum;
use App\Repository\ActivityLogRepository;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

/**
 * Page dédiée au journal d'activité complet (back-office admin).
 *
 * Routes :
 *   GET /admin/activity-logs         → liste paginée + filtres
 *   POST /admin/activity-logs/purge  → purge des logs > 30 jours (CSRF)
 */
#[Route('/admin/activity-logs')]
#[IsGranted('ROLE_ADMIN')]
class ActivityLogController extends AbstractController
{
    #[Route('', name: 'app_admin_activity_logs', methods: ['GET'])]
    public function index(
        Request               $request,
        ActivityLogRepository $repo
    ): Response {
        $params = [
            'search'  => $request->query->get('search',  ''),
            'action'  => $request->query->get('action',  ''),
            'context' => $request->query->get('context', ''),
            'page'    => $request->query->getInt('page',    1),
            'perPage' => $request->query->getInt('perPage', 25),
        ];

        $pagination = $repo->paginateAll($params);

        // FIX: build available actions and icons from ActivityActionEnum
        // instead of the removed ActivityLog::ICONS constant.
        $availableActions = array_column(ActivityActionEnum::cases(), 'value');

        // Build an icons map keyed by enum value for use in Twig
        $icons = [];
        foreach (ActivityActionEnum::cases() as $case) {
            $icons[$case->value] = $case->iconMeta();
        }

        return $this->render('admin/activity_logs/index.html.twig', [
            'adminUser'        => $this->getUser(),
            'logs'             => $pagination['items'],
            'total'            => $pagination['total'],
            'pages'            => $pagination['pages'],
            'page'             => $pagination['page'],
            'perPage'          => $params['perPage'],
            'search'           => $params['search'],
            'action'           => $params['action'],
            'context'          => $params['context'],
            'availableActions' => $availableActions,
            'icons'            => $icons,
        ]);
    }

    /**
     * Supprime les logs plus vieux que 30 jours.
     */
    #[Route('/purge', name: 'app_admin_activity_logs_purge', methods: ['POST'])]
    public function purge(
        Request               $request,
        ActivityLogRepository $repo
    ): Response {
        if (!$this->isCsrfTokenValid('purge_logs', (string) $request->request->get('_token'))) {
            $this->addFlash('error', 'Token CSRF invalide.');
            return $this->redirectToRoute('app_admin_activity_logs');
        }

        $threshold = new \DateTime('-30 days');
        $count     = $repo->deleteOlderThan($threshold);

        $this->addFlash('success', sprintf('%d log(s) supprimé(s) (plus de 30 jours).', $count));
        return $this->redirectToRoute('app_admin_activity_logs');
    }
}