<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use Doctrine\ORM\EntityManagerInterface;
use OTPHP\TOTP;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Mime\Email;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\String\Slugger\SluggerInterface;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Psr\Log\LoggerInterface;

class RegistrationController extends AbstractController
{
    public function __construct(
        #[Autowire('%env(MAILER_FROM_ADDRESS)%')]
        private string $mailerFromAddress,

        #[Autowire('%env(MAILER_FROM_NAME)%')]
        private string $mailerFromName,
    ) {}

    // ══════════════════════════════════════════════════════════════════
    //  INSCRIPTION — collecte des données + envoi email vérification
    // ══════════════════════════════════════════════════════════════════

    #[Route('/register', name: 'app_register')]
    public function register(
        Request                     $request,
        EntityManagerInterface      $em,
        UserPasswordHasherInterface $passwordHasher,
        SluggerInterface            $slugger,
        ValidatorInterface          $validator,
        MailerInterface             $mailer,
        LoggerInterface             $logger
    ): Response {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_dashboard');
        }

        $fieldErrors = [];
        $formData    = [];

        if ($request->isMethod('POST')) {

            // ── Récupération des champs ──
            $prenom     = trim((string) $request->request->get('prenom_utilisateur', ''));
            $nom        = trim((string) $request->request->get('nom_utilisateur', ''));
            $email      = trim((string) $request->request->get('email_utilisateur', ''));
            $telephone  = trim((string) $request->request->get('telephone_utilisateur', ''));
            $dateNaiss  = (string) $request->request->get('date_naissance_utilisateur', '');
            $roleRaw    = (string) $request->request->get('role_utilisateur', 'user');
            $role       = 'ROLE_' . strtoupper($roleRaw);
            $mdp        = (string) $request->request->get('mdp_utilisateur', '');
            $mdpConfirm = (string) $request->request->get('mdp_confirm', '');
            $terms      = $request->request->get('terms');

            $formData = [
                'prenom'        => $prenom,
                'nom'           => $nom,
                'email'         => $email,
                'telephone'     => $telephone,
                'dateNaissance' => $dateNaiss,
                'role'          => $roleRaw,
                'mode'          => 'register',
            ];

            // ── Construction de l'entité ──
            $user = new Utilisateurs();
            $user->setPrenomUtilisateur($prenom);
            $user->setNomUtilisateur($nom);
            $user->setEmailUtilisateur($email);
            $user->setTelephoneUtilisateur($telephone);
            $user->setRoleUtilisateur($role);
            $user->setPlainPassword($mdp);

            if (!empty($dateNaiss)) {
                try {
                    $user->setDateNaissanceUtilisateur(new \DateTime($dateNaiss));
                } catch (\Exception) {
                    $fieldErrors['date_naissance_utilisateur'] = 'La date de naissance est invalide.';
                }
            }

            // ── Validation via les contraintes de l'entité ──
            $violations = $validator->validate($user);
            foreach ($violations as $violation) {
                $prop = $violation->getPropertyPath();
                $fieldName = match($prop) {
                    'prenomUtilisateur'        => 'prenom_utilisateur',
                    'nomUtilisateur'           => 'nom_utilisateur',
                    'emailUtilisateur'         => 'email_utilisateur',
                    'telephoneUtilisateur'     => 'telephone_utilisateur',
                    'dateNaissanceUtilisateur' => 'date_naissance_utilisateur',
                    'roleUtilisateur'          => 'role_utilisateur',
                    'plainPassword'            => 'mdp_utilisateur',
                    default                    => $prop,
                };
                if (!isset($fieldErrors[$fieldName])) {
                    $fieldErrors[$fieldName] = $violation->getMessage();
                }
            }

            if (empty($fieldErrors['mdp_utilisateur']) && $mdp !== $mdpConfirm) {
                $fieldErrors['mdp_confirm'] = 'Les mots de passe ne correspondent pas.';
            }
            if (empty($dateNaiss) && !isset($fieldErrors['date_naissance_utilisateur'])) {
                $fieldErrors['date_naissance_utilisateur'] = 'La date de naissance est obligatoire.';
            }
            if (!$terms) {
                $fieldErrors['terms'] = "Vous devez accepter les conditions d'utilisation.";
            }

            // ── Si aucune erreur → persistance ──
            if (empty($fieldErrors)) {

                // Gestion de la photo de profil
                $photoFilename = 'default.png';
                $projectDir    = $this->getParameter('kernel.project_dir');
                $avatarsDir    = (is_string($projectDir) ? $projectDir : '') . '/public/avatars';
                if (!is_dir($avatarsDir)) {
                    mkdir($avatarsDir, 0775, true);
                }

                /** @var UploadedFile|null $uploadedFile */
                $uploadedFile = $request->files->get('photo_profil');
                if ($uploadedFile && $uploadedFile->isValid()) {
                    $originalName  = pathinfo($uploadedFile->getClientOriginalName(), PATHINFO_FILENAME);
                    $safeBase      = $slugger->slug($originalName);
                    $extension     = $uploadedFile->guessExtension() ?? 'jpg';
                    $photoFilename = $safeBase . '_' . uniqid() . '.' . $extension;
                    $uploadedFile->move($avatarsDir, $photoFilename);
                } else {
                    $cameraData = (string) $request->request->get('photo_profil_camera', '');
                    if (!empty($cameraData) && str_starts_with($cameraData, 'data:image')) {
                        [, $base64String] = explode(',', $cameraData, 2);
                        $imageData = base64_decode($base64String, true);
                        if ($imageData !== false) {
                            $photoFilename = 'cam_' . uniqid() . '.jpg';
                            file_put_contents($avatarsDir . '/' . $photoFilename, $imageData);
                        }
                    }
                }

                // Finalisation de l'entité
                $user->setDateInscriptionUtilisateur(new \DateTime());
                $user->setEstActifUtilisateur(true);
                $user->setBioUtilisateur('');
                $user->setPhotoProfilUtilisateur($photoFilename);

                // Hachage du mot de passe
                $hashedPwd = $passwordHasher->hashPassword($user, $mdp);
                $user->setMdpUtilisateur($hashedPwd);

                // ── Générer le token de vérification d'email ──
                $verificationToken  = bin2hex(random_bytes(32));
                $verificationExpiry = new \DateTime('+24 hours');
                $user->initEmailVerificationToken($verificationToken, $verificationExpiry);
                $user->setIsEmailVerified(false);   // compte non vérifié

                $em->persist($user);
                $em->flush();

                // ── Construire l'URL de vérification ──
                $verifyUrl = $this->generateUrl(
                    'app_verify_email',
                    ['token' => $verificationToken],
                    UrlGeneratorInterface::ABSOLUTE_URL
                );

                // ── Envoyer l'email de vérification ──
                $html = $this->renderView('security/emails/verify_email.html.twig', [
                    'user'      => $user,
                    'verifyUrl' => $verifyUrl,
                ]);

                try {
                    $emailMsg = (new Email())
                        ->from(new Address($this->mailerFromAddress, $this->mailerFromName))
                        ->to(new Address(
                            $user->getEmailUtilisateur() ?? '',
                            ($user->getPrenomUtilisateur() ?? '') . ' ' . ($user->getNomUtilisateur() ?? '')
                        ))
                        ->subject('MindAura — Confirmez votre adresse e-mail')
                        ->html($html)
                        ->text(sprintf(
                            "Bonjour %s,\n\nCliquez sur ce lien pour confirmer votre adresse e-mail (valable 24h) :\n%s\n\n— L'équipe MindAura",
                            $user->getPrenomUtilisateur(),
                            $verifyUrl
                        ));

                    $mailer->send($emailMsg);

                    $logger->info('Email verification sent', [
                        'to' => $user->getEmailUtilisateur(),
                    ]);

                } catch (\Throwable $e) {
                    $logger->error('Failed to send verification email', [
                        'to'    => $user->getEmailUtilisateur(),
                        'error' => $e->getMessage(),
                    ]);
                    // On continue même en cas d'échec d'envoi : l'utilisateur peut redemander
                }

                // Stocker l'ID en session pour la page "check your email"
                $request->getSession()->set('verify_pending_user_id', $user->getIdUtilisateur());

                return $this->redirectToRoute('app_verify_email_pending');
            }
        }

        return $this->render('security/auth.html.twig', array_merge([
            'fieldErrors' => $fieldErrors,
            'mode'        => 'register',
        ], $formData));
    }

    // ══════════════════════════════════════════════════════════════════
    //  PAGE "Vérifiez votre boîte mail"
    // ══════════════════════════════════════════════════════════════════

    #[Route('/register/verify-pending', name: 'app_verify_email_pending')]
    public function verifyEmailPending(
        Request                $request,
    ): Response {
        $userId = $request->getSession()->get('verify_pending_user_id');
        if (!$userId) {
            return $this->redirectToRoute('app_register');
        }

        return $this->render('security/email_verification.html.twig');
    }

    // ══════════════════════════════════════════════════════════════════
    //  RENVOI de l'email de vérification
    // ══════════════════════════════════════════════════════════════════

    #[Route('/register/resend-verification', name: 'app_resend_verification', methods: ['POST'])]
    public function resendVerification(
        Request                $request,
        EntityManagerInterface $em,
        MailerInterface        $mailer,
        LoggerInterface        $logger
    ): Response {
        $userId = $request->getSession()->get('verify_pending_user_id');
        if (!$userId) {
            return $this->redirectToRoute('app_register');
        }

        $user = $em->find(Utilisateurs::class, $userId);
        if (!$user || $user->isEmailVerified()) {
            return $this->redirectToRoute('app_login');
        }

        // Nouveau token
        $verificationToken  = bin2hex(random_bytes(32));
        $user->initEmailVerificationToken($verificationToken, new \DateTime('+24 hours'));
        $em->flush();

        $verifyUrl = $this->generateUrl(
            'app_verify_email',
            ['token' => $verificationToken],
            UrlGeneratorInterface::ABSOLUTE_URL
        );

        $html = $this->renderView('security/emails/verify_email.html.twig', [
            'user'      => $user,
            'verifyUrl' => $verifyUrl,
        ]);

        try {
            $emailMsg = (new Email())
                ->from(new Address($this->mailerFromAddress, $this->mailerFromName))
                ->to(new Address($user->getEmailUtilisateur() ?? '', $user->getPrenomUtilisateur() ?? ''))
                ->subject('MindAura — Confirmez votre adresse e-mail')
                ->html($html);

            $mailer->send($emailMsg);
            $this->addFlash('success', 'Un nouvel e-mail de vérification a été envoyé.');
        } catch (\Throwable $e) {
            $logger->error('Resend verification failed', ['error' => $e->getMessage()]);
            $this->addFlash('error', "L'envoi a échoué. Réessayez dans quelques instants.");
        }

        return $this->redirectToRoute('app_verify_email_pending');
    }
}