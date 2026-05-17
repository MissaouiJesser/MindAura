<?php

namespace App\Command;

use App\Repository\UtilisateursRepository;
use OTPHP\TOTP;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

/**
 * Commande de diagnostic pour le TOTP / Google Authenticator.
 *
 * Usage :
 *   php bin/console app:totp:debug user@email.com
 *   php bin/console app:totp:debug user@email.com --code=123456
 */
#[AsCommand(
    name: 'app:totp:debug',
    description: 'Diagnostique les problèmes de synchronisation Google Authenticator',
)]
class TotpDebugCommand extends Command
{
    public function __construct(
        private readonly UtilisateursRepository $repo,
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this
            ->addArgument('email', InputArgument::REQUIRED, 'Adresse e-mail du compte à diagnostiquer')
            ->addOption('code', 'c', InputOption::VALUE_REQUIRED, 'Code à 6 chiffres à vérifier (optionnel)');
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io    = new SymfonyStyle($input, $output);
        $email = $input->getArgument('email');
        $code  = $input->getOption('code');

        $io->title('🔍 Diagnostic TOTP / Google Authenticator');

        // ── 1. Heure du serveur ──────────────────────────────────────
        $io->section('⏰ Heure du serveur PHP');
        $now    = time();
        $nowUtc = gmdate('Y-m-d H:i:s', $now);
        $nowLoc = date('Y-m-d H:i:s T', $now);

        $io->table(
            ['Info', 'Valeur'],
            [
                ['Timestamp Unix',           (string) $now],
                ['Heure UTC',                $nowUtc],
                ['Heure locale serveur',     $nowLoc],
                ['Timezone PHP configurée',  date_default_timezone_get()],
            ]
        );

        $io->note([
            'ℹ️  Compare l\'heure UTC ci-dessus avec https://time.is',
            'Si l\'écart est > 30 secondes, c\'est la cause du problème.',
            'Sur Windows : clic droit horloge → Ajuster la date/heure → Synchroniser maintenant.',
        ]);

        // ── 2. Utilisateur ───────────────────────────────────────────
        $io->section('👤 Utilisateur');
        $user = $this->repo->findOneBy(['emailUtilisateur' => $email]);

        if (!$user) {
            $io->error(sprintf('Aucun utilisateur trouvé avec l\'e-mail "%s".', $email));
            return Command::FAILURE;
        }

        $io->table(
            ['Champ', 'Valeur'],
            [
                ['ID',                   (string) $user->getIdUtilisateur()],
                ['Nom complet',          $user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur()],
                ['Email',                $user->getEmailUtilisateur()],
                ['Rôle',                 $user->getRoleUtilisateur() ?? '-'],
                ['Actif',                $user->isEstActifUtilisateur() ? '✅ Oui' : '❌ Non'],
                ['E-mail vérifié',       $user->isEmailVerified()     ? '✅ Oui' : '❌ Non'],
                ['Google OAuth',         $user->getGoogleId() ? '✅ ' . $user->getGoogleId() : '—'],
                ['GitHub OAuth',         $user->getGithubId() ? '✅ ' . $user->getGithubId() : '—'],
            ]
        );

        // ── 3. Secret TOTP ───────────────────────────────────────────
        $io->section('🔐 Secret TOTP en base');
        $secret = $user->getTotpSecret();

        if (empty($secret)) {
            $io->error([
                'Cet utilisateur n\'a AUCUN secret TOTP en base !',
                '',
                'Causes possibles :',
                '  • Le compte a été créé via OAuth (Google/GitHub) → pas de 2FA configurée',
                '  • Le setup 2FA a été interrompu avant validation',
                '',
                'Solution : utilise la méthode de reset par e-mail, ou reconfigure la 2FA.',
            ]);
            return Command::FAILURE;
        }

        $io->success('Secret TOTP présent : ' . substr($secret, 0, 6) . '...' . substr($secret, -4) . ' (' . strlen($secret) . ' caractères)');

        // ── 4. Codes attendus (fenêtre ±2 minutes) ───────────────────
        $io->section('🎯 Codes TOTP attendus (fenêtre ±2 minutes)');

        $totp = TOTP::createFromSecret($secret);
        $totp->setLabel($user->getEmailUtilisateur() !== null && $user->getEmailUtilisateur() !== '' ? $user->getEmailUtilisateur() : 'user@mindaura.app');
        $totp->setIssuer('MindAura');

        $rows = [];
        for ($offset = -4; $offset <= 4; $offset++) {
            $t        = $now + ($offset * 30);
            $expected = $totp->at(max(0, $t));
            $marker   = $offset === 0 ? '  ← MAINTENANT' : '';
            $label    = $offset === 0 ? 'Maintenant'
                      : ($offset < 0 ? sprintf('Il y a %ds', abs($offset) * 30)
                                     : sprintf('Dans %ds',  $offset * 30));
            $rows[] = [
                $label,
                date('H:i:s', $t),
                $expected . $marker,
            ];
        }

        $io->table(['Moment', 'Heure', 'Code attendu'], $rows);

        $io->note([
            'Ouvre Google Authenticator, regarde le code affiché pour MindAura,',
            'et vérifie qu\'il correspond à la ligne « Maintenant » ci-dessus.',
        ]);

        // ── 5. Vérification d'un code fourni ────────────────────────
        if ($code !== null) {
            $io->section('✅ Vérification du code saisi');
            $code = trim(str_replace(' ', '', $code));

            if (strlen($code) !== 6 || !ctype_digit($code)) {
                $io->error('Le code doit être composé de 6 chiffres uniquement.');
                return Command::FAILURE;
            }

            // Teste plusieurs windows
            $results = [];
            foreach ([0, 1, 2, 3, 4] as $w) {
                $ok = $totp->verify($code, null, $w);
                $results[] = [
                    sprintf('window=%d (±%ds)', $w, $w * 30),
                    $ok ? '✅ VALIDE' : '❌ invalide',
                ];
            }
            $io->table(['Tolérance', 'Résultat'], $results);

            $anyOk = array_filter($results, fn($r) => str_contains($r[1], 'VALIDE'));
            if (empty($anyOk)) {
                $io->error([
                    'Le code saisi est REJETÉ même avec une tolérance de ±2 minutes.',
                    '',
                    'Diagnostic probable :',
                    '  • Horloge du serveur décalée de plus de 2 minutes → synchronise Windows',
                    '  • Tu saisis le code d\'un autre compte dans Authenticator',
                    '  • Le secret en base ne correspond pas au QR code que tu as scanné',
                ]);
            } else {
                $minW = array_key_first($anyOk);
                $io->success(sprintf(
                    'Le code est valide à partir de window=%d. Configure cette valeur minimale dans ton contrôleur.',
                    $minW
                ));
            }
        } else {
            $io->note('Pour tester un code saisi dans Google Authenticator : php bin/console app:totp:debug ' . $email . ' --code=123456');
        }

        return Command::SUCCESS;
    }
}