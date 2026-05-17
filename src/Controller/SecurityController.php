<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use App\Service\ActivityLoggerService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;
use Symfony\Component\Security\Http\Event\LogoutEvent;

class SecurityController extends AbstractController
{
    public function __construct(
        /** @phpstan-ignore property.onlyWritten */
        private readonly ActivityLoggerService $activityLogger,
    ) {}

    /**
     * Route racine : redirige automatiquement vers /login.
     */
    #[Route('/', name: 'app_root')]
    public function root(): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }
        return $this->redirectToRoute('app_login');
    }

    #[Route('/login', name: 'app_login')]
    public function login(AuthenticationUtils $authenticationUtils): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        $error        = $authenticationUtils->getLastAuthenticationError();
        $lastUsername = $authenticationUtils->getLastUsername();

        return $this->render('security/auth.html.twig', [
            'last_username' => $lastUsername,
            'error'         => $error,
        ]);
    }

    /**
     * Le logout est intercepté par Symfony avant d'atteindre ce contrôleur.
     * Pour logger le logout, utilisez un EventSubscriber sur LogoutEvent.
     * (voir App\EventSubscriber\ActivityLogSubscriber)
     */
    #[Route('/logout', name: 'app_logout')]
    public function logout(): void
    {
        throw new \LogicException('This method should not be reached.');
    }
}