<?php

namespace App\Controller;

use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use OTPHP\TOTP;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Psr\Log\LoggerInterface;

/**
 * Contrôleur de réinitialisation de mot de passe avec DEUX méthodes au choix :
 *   1. Par e-mail (lien sécurisé, valable 30 min)
 *   2. Par Google Authenticator (code TOTP 6 chiffres, token valable 10 min)
 *
 * Flux :
 *   /forgot-password                       → page de choix
 *   /forgot-password/email                 → saisie e-mail → envoi du lien
 *   /forgot-password/authenticator         → saisie e-mail
 *   /forgot-password/authenticator/verify  → saisie code 6 chiffres
 *   /reset-password/{token}                → nouveau mot de passe (commun aux 2 flux)
 */
class ResetPasswordController extends AbstractController
{
    private const TOKEN_TTL_MINUTES      = 30;   // Lien email
    private const TOTP_TOKEN_TTL_MINUTES = 10;   // TOTP (plus court = plus sécurisé)
    private const TOTP_MAX_ATTEMPTS      = 5;    // Nombre max de tentatives de code
    private const TOTP_WINDOW            = 3;    // ±90 s de tolérance de synchro

    public function __construct(
        #[Autowire('%env(MAILER_FROM_ADDRESS)%')]
        private string $mailerFromAddress,

        #[Autowire('%env(MAILER_FROM_NAME)%')]
        private string $mailerFromName,
    ) {}

    // ══════════════════════════════════════════════════════════════════
    //  PAGE DE CHOIX — E-mail ou Google Authenticator
    // ══════════════════════════════════════════════════════════════════

    #[Route('/forgot-password', name: 'app_forgot_password', methods: ['GET'])]
    public function chooseMethod(): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }
        return $this->render('security/forgot_password_choice.html.twig');
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUX 1 — RÉINITIALISATION PAR E-MAIL
    // ══════════════════════════════════════════════════════════════════

    #[Route('/forgot-password/email', name: 'app_forgot_password_email', methods: ['GET', 'POST'])]
    public function forgotPasswordEmail(
        Request                $request,
        UtilisateursRepository $repo,
        EntityManagerInterface $em,
        MailerInterface        $mailer,
        LoggerInterface        $logger
    ): Response {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        $sent  = false;
        $error = null;

        if ($request->isMethod('POST')) {
            $emailInput = trim((string) $request->request->get('email', ''));

            if (empty($emailInput) || !filter_var($emailInput, FILTER_VALIDATE_EMAIL)) {
                $error = 'Veuillez saisir une adresse e-mail valide.';
            } else {
                $user = $repo->findOneBy(['emailUtilisateur' => $emailInput]);

                if ($user) {
                    // Générer token + expiration
                    $token  = bin2hex(random_bytes(32));
                    $expiry = new \DateTime('+' . self::TOKEN_TTL_MINUTES . ' minutes');

                    $user->initResetPasswordToken($token, $expiry);
                    $em->flush();

                    $resetUrl = $this->generateUrl(
                        'app_reset_password',
                        ['token' => $token],
                        UrlGeneratorInterface::ABSOLUTE_URL
                    );

                    $html = $this->renderView('security/emails/reset_password.html.twig', [
                        'user'     => $user,
                        'resetUrl' => $resetUrl,
                        'ttl'      => self::TOKEN_TTL_MINUTES,
                    ]);

                    try {
                        $email = (new Email())
                            ->from(new Address($this->mailerFromAddress, $this->mailerFromName))
                            ->to(new Address(
                                $user->getEmailUtilisateur() ?? '',
                                ($user->getPrenomUtilisateur() ?? '') . ' ' . ($user->getNomUtilisateur() ?? '')
                            ))
                            ->subject('Réinitialisation de votre mot de passe MindAura')
                            ->html($html)
                            ->text(sprintf(
                                "Bonjour %s,\n\nCliquez sur ce lien pour réinitialiser votre mot de passe (valable %d minutes) :\n%s\n\nSi vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.\n\n— L'équipe MindAura",
                                $user->getPrenomUtilisateur(),
                                self::TOKEN_TTL_MINUTES,
                                $resetUrl
                            ));

                        $mailer->send($email);
                        $logger->info('Reset password email sent', ['to' => $user->getEmailUtilisateur()]);

                    } catch (\Throwable $e) {
                        $logger->error('Failed to send reset password email', [
                            'to'    => $user->getEmailUtilisateur(),
                            'error' => $e->getMessage(),
                        ]);

                        if ($this->getParameter('kernel.environment') === 'dev') {
                            $error = 'Erreur d\'envoi Gmail : ' . $e->getMessage();
                        } else {
                            $error = 'Une erreur est survenue lors de l\'envoi de l\'e-mail. Veuillez réessayer dans quelques instants.';
                        }

                        return $this->render('security/forgot_password.html.twig', [
                            'sent'  => false,
                            'error' => $error,
                        ]);
                    }
                }

                // On affiche "envoyé" même si l'e-mail n'existe pas (anti-énumération)
                $sent = true;
            }
        }

        return $this->render('security/forgot_password.html.twig', [
            'sent'  => $sent,
            'error' => $error,
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  FLUX 2 — RÉINITIALISATION PAR GOOGLE AUTHENTICATOR
    // ══════════════════════════════════════════════════════════════════

    /**
     * ÉTAPE 1 — Saisir l'e-mail pour identifier le compte.
     */
    #[Route('/forgot-password/authenticator', name: 'app_forgot_password_totp', methods: ['GET', 'POST'])]
    public function forgotPasswordTotp(
        Request                $request,
        UtilisateursRepository $repo
    ): Response {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        $error = null;

        if ($request->isMethod('POST')) {
            $emailInput = trim((string) $request->request->get('email', ''));

            if (empty($emailInput) || !filter_var($emailInput, FILTER_VALIDATE_EMAIL)) {
                $error = 'Veuillez saisir une adresse e-mail valide.';
            } else {
                $user = $repo->findOneBy(['emailUtilisateur' => $emailInput]);

                // Anti-énumération : on stocke toujours un flag en session et
                // on redirige TOUJOURS vers la page de code, même si l'user
                // n'existe pas ou n'a pas de 2FA. La vraie vérification
                // se fait à l'étape 2 avec un délai artificiel.
                $session = $request->getSession();
                $session->set('totp_reset_email',    $emailInput);
                $session->set('totp_reset_valid',    $user !== null && !empty($user->getTotpSecret()));
                $session->set('totp_reset_attempts', 0);
                $session->set('totp_reset_expires',  (new \DateTime('+15 minutes'))->getTimestamp());

                // Délai artificiel anti-timing-attack
                usleep(300_000);

                return $this->redirectToRoute('app_forgot_password_totp_verify');
            }
        }

        return $this->render('security/forgot_password_totp.html.twig', [
            'error' => $error,
        ]);
    }

    /**
     * ÉTAPE 2 — Saisir le code 6 chiffres de Google Authenticator.
     */
    #[Route('/forgot-password/authenticator/verify', name: 'app_forgot_password_totp_verify', methods: ['GET', 'POST'])]
    public function forgotPasswordTotpVerify(
        Request                $request,
        UtilisateursRepository $repo,
        EntityManagerInterface $em
    ): Response {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        $session = $request->getSession();
        $email   = $session->get('totp_reset_email');
        $expires = (int) $session->get('totp_reset_expires', 0);

        // Pas d'email en session OU session expirée → retour au choix
        if (!$email || $expires < time()) {
            $this->clearTotpResetSession($session);
            return $this->redirectToRoute('app_forgot_password');
        }

        $error    = null;
        $attempts = (int) $session->get('totp_reset_attempts', 0);

        if ($request->isMethod('POST')) {
            $codeInput = trim((string) str_replace(' ', '', (string) $request->request->get('totp_code', '')));

            // Validation format
            if (empty($codeInput) || strlen($codeInput) !== 6 || !ctype_digit($codeInput)) {
                $error = 'Veuillez saisir un code à 6 chiffres.';
            } else {
                $isValid = $session->get('totp_reset_valid', false);
                $user    = $isValid ? $repo->findOneBy(['emailUtilisateur' => $email]) : null;

                // Vérifier TOTP avec window=2 (±60 s de tolérance)
                $codeOk = false;
                if ($user && $user->getTotpSecret()) {
                    try {
                        $totp   = TOTP::createFromSecret($user->getTotpSecret());
                        $codeOk = $totp->verify($codeInput, null, self::TOTP_WINDOW);
                    } catch (\Throwable) {
                        $codeOk = false;
                    }
                }

                if ($codeOk && $user) {
                    // ✅ Code valide → générer un token de reset court
                    $token  = bin2hex(random_bytes(32));
                    $expiry = new \DateTime('+' . self::TOTP_TOKEN_TTL_MINUTES . ' minutes');

                    $user->initResetPasswordToken($token, $expiry);
                    $em->flush();

                    $this->clearTotpResetSession($session);

                    return $this->redirectToRoute('app_reset_password', ['token' => $token]);
                }

                // Échec : incrémenter les tentatives
                $attempts++;
                $session->set('totp_reset_attempts', $attempts);

                // Délai anti-brute-force (plus long à chaque tentative)
                usleep(300_000 + ($attempts * 200_000));

                if ($attempts >= self::TOTP_MAX_ATTEMPTS) {
                    $this->clearTotpResetSession($session);
                    $this->addFlash('error', 'Trop de tentatives de code échouées. Par sécurité, veuillez recommencer ou utiliser la méthode par e-mail.');
                    return $this->redirectToRoute('app_forgot_password');
                }

                $remaining = self::TOTP_MAX_ATTEMPTS - $attempts;
                $error = sprintf(
                    'Code incorrect. Vérifiez que votre application est bien synchronisée puis réessayez. (%d tentative%s restante%s)',
                    $remaining,
                    $remaining > 1 ? 's' : '',
                    $remaining > 1 ? 's' : ''
                );
            }
        }

        return $this->render('security/forgot_password_totp_verify.html.twig', [
            'error'       => $error,
            'maskedEmail' => $this->maskEmail($email),
            'attempts'    => $attempts,
            'maxAttempts' => self::TOTP_MAX_ATTEMPTS,
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  ÉTAPE FINALE — Nouveau mot de passe (commune aux 2 flux)
    // ══════════════════════════════════════════════════════════════════

    #[Route('/reset-password/{token}', name: 'app_reset_password', methods: ['GET', 'POST'])]
    public function resetPassword(
        string                      $token,
        Request                     $request,
        UtilisateursRepository      $repo,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher
    ): Response {
        $user = $repo->findOneBy(['resetPasswordToken' => $token]);

        if (
            !$user
            || $user->getResetPasswordTokenExpiry() === null
            || $user->getResetPasswordTokenExpiry() < new \DateTime()
        ) {
            return $this->render('security/reset_password_invalid.html.twig');
        }

        $fieldErrors = [];
        $success     = false;

        if ($request->isMethod('POST')) {
            $newPassword     = (string) $request->request->get('password', '');
            $confirmPassword = (string) $request->request->get('password_confirm', '');

            if (strlen($newPassword) < 8) {
                $fieldErrors['password'] = 'Le mot de passe doit contenir au moins 8 caractères.';
            } elseif (!preg_match('/[A-Z]/', $newPassword)) {
                $fieldErrors['password'] = 'Le mot de passe doit contenir au moins une majuscule.';
            } elseif (!preg_match('/[0-9]/', $newPassword)) {
                $fieldErrors['password'] = 'Le mot de passe doit contenir au moins un chiffre.';
            }

            if (empty($fieldErrors['password']) && $newPassword !== $confirmPassword) {
                $fieldErrors['password_confirm'] = 'Les mots de passe ne correspondent pas.';
            }

            if (empty($fieldErrors)) {
                $user->setMdpUtilisateur($passwordHasher->hashPassword($user, $newPassword));
                $user->clearResetPasswordToken();
                $em->flush();

                $success = true;
            }
        }

        return $this->render('security/reset_password.html.twig', [
            'token'       => $token,
            'fieldErrors' => $fieldErrors,
            'success'     => $success,
        ]);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Masque un e-mail pour l'affichage : jean.dupont@gmail.com → je********@gmail.com
     */
    private function maskEmail(string $email): string
    {
        $parts = explode('@', $email);
        if (count($parts) !== 2) {
            return $email;
        }
        [$name, $domain] = $parts;

        if (strlen($name) <= 2) {
            $masked = $name[0] . str_repeat('*', max(1, strlen($name) - 1));
        } else {
            $masked = substr($name, 0, 2) . str_repeat('*', min(8, strlen($name) - 2));
        }

        return $masked . '@' . $domain;
    }

    /**
     * Nettoie toutes les clés de session liées au flux TOTP reset.
     */
    private function clearTotpResetSession(\Symfony\Component\HttpFoundation\Session\SessionInterface $session): void
    {
        $session->remove('totp_reset_email');
        $session->remove('totp_reset_valid');
        $session->remove('totp_reset_attempts');
        $session->remove('totp_reset_expires');
    }
}