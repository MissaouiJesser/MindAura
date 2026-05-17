<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use OTPHP\TOTP;
use ParagonIE\ConstantTime\Base32;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

/**
 * Gère la confirmation de l'adresse e-mail après inscription.
 *
 * Flux :
 *   1. L'utilisateur s'inscrit → RegistrationController envoie l'email.
 *   2. Il clique sur le lien reçu → app_verify_email (ce contrôleur).
 *   3. Token valide → isEmailVerified = true, génération TOTP → app_2fa_setup.
 *   4. Token invalide / expiré → page d'erreur.
 */
class EmailVerificationController extends AbstractController
{
    #[Route('/verify-email/{token}', name: 'app_verify_email')]
    public function verify(
        string                 $token,
        Request                $request,
        UtilisateursRepository $repo,
        EntityManagerInterface $em
    ): Response {
        $user = $repo->findOneBy(['emailVerificationToken' => $token]);

        if (
            !$user
            || $user->getEmailVerificationTokenExpiry() === null
            || $user->getEmailVerificationTokenExpiry() < new \DateTime()
        ) {
            return $this->render('security/email_verification_invalid.html.twig');
        }

        if ($user->isEmailVerified()) {
            $this->addFlash('success', 'Votre e-mail est déjà confirmé. Connectez-vous !');
            return $this->redirectToRoute('app_login');
        }

        // ✅ Marquer l'email comme vérifié et invalider le token
        $user->setIsEmailVerified(true);
        $user->clearEmailVerificationToken();
        $em->flush();

        $request->getSession()->remove('verify_pending_user_id');

        // ── Générer le secret TOTP (20 bytes = 32 chars Base32 standard RFC 6238)
        // ⚠️  Ne PAS utiliser TOTP::generate() qui crée un secret de 104 chars,
        //     tronqué par la colonne VARCHAR(64) → bug de synchronisation silencieux.
        $secret = self::generateStandardTotpSecret();

        $request->getSession()->set('2fa_pending_user_id', $user->getIdUtilisateur());
        $request->getSession()->set('2fa_totp_secret', $secret);

        $this->addFlash('success', '✅ Votre e-mail est confirmé ! Configurez maintenant votre authentification à deux facteurs.');

        return $this->redirectToRoute('app_2fa_setup');
    }

    /**
     * Génère un secret TOTP de 20 bytes (160 bits) encodé en Base32.
     * Produit exactement 32 caractères, compatible RFC 6238 et toutes les apps
     * (Google Authenticator, Authy, Microsoft Authenticator, 1Password...).
     */
    public static function generateStandardTotpSecret(): string
    {
        return rtrim(Base32::encodeUpper(random_bytes(20)), '=');
    }
}