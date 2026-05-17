<?php

namespace App\Controller;

use App\Controller\EmailVerificationController;
use App\Entity\Utilisateurs;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpFoundation\Session\SessionInterface;
use Symfony\Component\Routing\Annotation\Route;
use OTPHP\TOTP;

/**
 * Gestion de l'authentification à deux facteurs (TOTP / Google Authenticator).
 *
 * Flux d'inscription :
 *   1. RegistrationController crée le compte et stocke le secret TOTP en session.
 *   2. L'utilisateur est redirigé vers /2fa/setup → affichage du QR code.
 *   3. L'utilisateur scanne le QR code et saisit le code → /2fa/setup (POST).
 *   4. Si le code est valide, le secret est persisté en BDD et l'utilisateur
 *      est redirigé vers la page de connexion.
 */
class TwoFactorController extends AbstractController
{
    // ±90 s de tolérance (window=3 → accepte les 3 codes avant et après).
    // Résout les problèmes courants de désynchronisation d'horloge du téléphone
    // ou du serveur (XAMPP/WAMP en local ont souvent un décalage).
    private const TOTP_WINDOW = 3;

    // ══════════════════════════════════════════════════════════════════
    //  SETUP — Affiché immédiatement après l'inscription
    // ══════════════════════════════════════════════════════════════════

    /**
     * GET  /2fa/setup → affiche le QR code à scanner.
     * POST /2fa/setup → vérifie le code saisi et finalise l'inscription.
     */
    #[Route('/2fa/setup', name: 'app_2fa_setup', methods: ['GET', 'POST'])]
    public function setup(
        Request                $request,
        EntityManagerInterface $em,
        UtilisateursRepository $repo
    ): Response {
        // Récupérer le pending_user_id mis en session par RegistrationController
        $userId = $request->getSession()->get('2fa_pending_user_id');

        if (!$userId) {
            return $this->redirectToRoute('app_register');
        }

        /** @var Utilisateurs|null $user */
        $user = $repo->find($userId);
        if (!$user) {
            $request->getSession()->remove('2fa_pending_user_id');
            return $this->redirectToRoute('app_register');
        }

        // ── Récupérer ou générer le secret TOTP ──────────────────────
        $secret = $request->getSession()->get('2fa_totp_secret');
        if (!$secret) {
            // ⚠️  Secret de 20 bytes (32 chars Base32) — RFC 6238 standard.
            //     Ne PAS utiliser TOTP::generate() qui crée 104 chars tronqués
            //     par la colonne VARCHAR(64) → bug de synchronisation silencieux.
            $secret = EmailVerificationController::generateStandardTotpSecret();
            $request->getSession()->set('2fa_totp_secret', $secret);
        }
        $totp = TOTP::createFromSecret($secret);

        $totp->setLabel($user->getEmailUtilisateur() !== null && $user->getEmailUtilisateur() !== '' ? $user->getEmailUtilisateur() : 'user@mindaura.app');
        $totp->setIssuer('MindAura');

        $qrCodeUri   = $totp->getQrCodeUri(
            'https://api.qrserver.com/v1/create-qr-code/?data=[DATA]&size=200x200',
            '[DATA]'
        );
        $provisionUri = $totp->getProvisioningUri();

        $error = null;

        // ── Traitement POST ───────────────────────────────────────────
        if ($request->isMethod('POST')) {
            $codeInput = trim((string) str_replace(' ', '', (string) $request->request->get('totp_code', '')));

            if (empty($codeInput) || strlen($codeInput) !== 6 || !ctype_digit($codeInput)) {
                $error = 'Veuillez saisir un code à 6 chiffres.';
            } elseif (!$totp->verify($codeInput, null, self::TOTP_WINDOW)) {
                // ⚠ Message d'erreur enrichi avec conseils de synchronisation
                $error = 'Code incorrect. Si le problème persiste, synchronisez l\'heure '
                       . 'de votre application : dans Google Authenticator, allez dans '
                       . 'Paramètres → « Correction du temps pour les codes » → '
                       . '« Synchroniser maintenant ».';
            } else {
                // ✅ Code valide → persister le secret dans l'entité
                $user->setTotpSecret($secret);
                $em->flush();

                $request->getSession()->remove('2fa_pending_user_id');
                $request->getSession()->remove('2fa_totp_secret');

                $this->addFlash('success', '🎉 Authentification à deux facteurs activée ! Vous pouvez maintenant vous connecter.');
                return $this->redirectToRoute('app_login');
            }
        }

        return $this->render('security/2fa_setup.html.twig', [
            'user'         => $user,
            'qrCodeUri'    => $qrCodeUri,
            'provisionUri' => $provisionUri,
            'secret'       => $secret,
            'error'        => $error,
        ]);
    }
}