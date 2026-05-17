<?php

namespace App\Service;

use App\Entity\Objectif;
use App\Entity\TestPsychologique;
use App\Entity\Utilisateurs;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Mime\Address;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;

class MailService
{
    public function __construct(
        private readonly MailerInterface        $mailer,
        private readonly UrlGeneratorInterface  $urlGenerator,
        private readonly string                 $mailerFrom,
        private readonly string                 $mailerFromName,
    ) {}

    /**
     * Envoie l'email de résultats d'un test psychologique à l'utilisateur connecté.
     *
     * @param Objectif[] $suggestedObjectifs
     *
     * @return array{success: bool, flash_type: string, flash_message: string}
     */
    public function sendTestResult(
        Utilisateurs       $user,
        TestPsychologique  $test,
        int                $totalScore,
        int                $answeredCount,
        array              $suggestedObjectifs,
        string             $groqAnalysis,
    ): array {
        $fullName      = trim($user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur());
        $recipientName = $fullName !== '' ? $fullName : $user->getUserIdentifier();

        // On utilise directement l'email de l'utilisateur connecté (pas de MAILER_FORCE_TO)
        $recipientEmail = (string) $user->getEmailUtilisateur();

        $resultsUrl = $this->urlGenerator->generate(
            'front_tests_results',
            ['id' => $test->getIdTest(), 'score' => $totalScore],
            UrlGeneratorInterface::ABSOLUTE_URL
        );

        $email = (new TemplatedEmail())
            ->from(new Address($this->mailerFrom, $this->mailerFromName))
            ->to(new Address($recipientEmail, $recipientName))
            ->subject('Vos résultats - ' . $test->getTitreTest())
            ->htmlTemplate('emails/test_result.html.twig')
            ->context([
                'user'                => $user,
                'test'                => $test,
                'score'               => $totalScore,
                'answered_count'      => $answeredCount,
                'total_questions'     => $test->getQuestionReponses()->count(),
                'suggested_objectifs' => $suggestedObjectifs,
                'groq_analysis'       => $groqAnalysis,
                'results_url'         => $resultsUrl,
                'sent_at'             => new \DateTimeImmutable(),
            ]);

        try {
            $this->mailer->send($email);

            return [
                'success'       => true,
                'flash_type'    => 'success',
                'flash_message' => 'Vos résultats ont aussi été envoyés par email.',
            ];
        } catch (\Throwable) {
            return [
                'success'       => false,
                'flash_type'    => 'warning',
                'flash_message' => 'Test enregistré, mais envoi email indisponible pour le moment.',
            ];
        }
    }
}