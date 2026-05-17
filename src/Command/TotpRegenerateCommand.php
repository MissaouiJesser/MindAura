<?php

namespace App\Command;

use App\Controller\EmailVerificationController;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use OTPHP\TOTP;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Helper\QuestionHelper;
use Symfony\Component\Console\Question\ConfirmationQuestion;
use Symfony\Component\Console\Style\SymfonyStyle;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

/**
 * Régénère proprement le secret TOTP d'un utilisateur existant
 * et génère un fichier HTML local avec le QR code à scanner.
 *
 * Usage :
 *   php bin/console app:totp:regenerate user@email.com
 *   php bin/console app:totp:regenerate user@email.com --force
 */
#[AsCommand(
    name: 'app:totp:regenerate',
    description: 'Régénère le secret TOTP d\'un utilisateur (nouveau QR code à scanner)',
)]
class TotpRegenerateCommand extends Command
{
    public function __construct(
        private readonly UtilisateursRepository $repo,
        private readonly EntityManagerInterface $em,
        #[Autowire('%kernel.project_dir%')]
        private readonly string                 $projectDir,
    ) {
        parent::__construct();
    }

    protected function configure(): void
    {
        $this
            ->addArgument('email', InputArgument::REQUIRED, 'Adresse e-mail du compte')
            ->addOption('force',  'f', InputOption::VALUE_NONE, 'Ne pas demander de confirmation');
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io    = new SymfonyStyle($input, $output);
        $email = $input->getArgument('email');
        $force = $input->getOption('force');

        $io->title('🔄 Régénération du secret TOTP');

        // ── Trouver l'utilisateur ────────────────────────────────────
        $user = $this->repo->findOneBy(['emailUtilisateur' => $email]);
        if (!$user) {
            $io->error(sprintf('Aucun utilisateur trouvé avec "%s".', $email));
            return Command::FAILURE;
        }

        $io->definitionList(
            ['Utilisateur' => $user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur()],
            ['Email'       => $user->getEmailUtilisateur()],
            ['Rôle'        => $user->getRoleUtilisateur() ?? '-'],
            ['Ancien secret' => $user->getTotpSecret()
                ? substr($user->getTotpSecret(), 0, 6) . '...' . substr($user->getTotpSecret(), -4) . ' (' . strlen($user->getTotpSecret()) . ' chars)'
                : '(aucun)'
            ],
        );

        // ── Confirmation ─────────────────────────────────────────────
        if (!$force) {
            $io->warning([
                'Cette opération va :',
                '  1. Générer un NOUVEAU secret TOTP (l\'ancien sera écrasé)',
                '  2. Invalider l\'entrée MindAura dans l\'app Google Authenticator',
                '  3. Tu devras scanner le NOUVEAU QR code affiché ci-dessous',
            ]);

            /** @var QuestionHelper $helper */
            $helper   = $this->getHelper('question');
            $question = new ConfirmationQuestion('Confirmer la régénération ? (y/N) ', false);
            if (!$helper->ask($input, $output, $question)) {
                $io->info('Opération annulée.');
                return Command::SUCCESS;
            }
        }

        // ── Générer un secret propre (32 chars Base32, RFC 6238) ─────
        $newSecret = EmailVerificationController::generateStandardTotpSecret();
        if (empty($newSecret)) {
            $io->error('Impossible de générer un secret TOTP valide.');
            return Command::FAILURE;
        }

        $totp = TOTP::createFromSecret($newSecret);
        $totp->setLabel($user->getEmailUtilisateur() !== null && $user->getEmailUtilisateur() !== '' ? $user->getEmailUtilisateur() : 'user@mindaura.app');
        $totp->setIssuer('MindAura');

        // ── Sauvegarder en base ──────────────────────────────────────
        $user->setTotpSecret($newSecret);
        $this->em->flush();

        $io->success(sprintf('✨ Nouveau secret sauvegardé en base (%d caractères).', strlen($newSecret)));

        // ──────────────────────────────────────────────────────────────
        //  GÉNÉRER UN FICHIER HTML LOCAL AVEC LE QR CODE
        // ──────────────────────────────────────────────────────────────
        $qrCodeUrl    = $totp->getQrCodeUri(
            'https://api.qrserver.com/v1/create-qr-code/?data=[DATA]&size=280x280&margin=20',
            '[DATA]'
        );

        $publicDir = $this->projectDir . DIRECTORY_SEPARATOR . 'public';
        if (!is_dir($publicDir)) {
            @mkdir($publicDir, 0775, true);
        }
        $htmlFilename = 'totp_setup_' . $user->getIdUtilisateur() . '.html';
        $htmlPath     = $publicDir . DIRECTORY_SEPARATOR . $htmlFilename;

        $html = $this->buildQrHtml($user->getPrenomUtilisateur() ?? '', $user->getEmailUtilisateur() ?? '', $qrCodeUrl, $newSecret);
        @file_put_contents($htmlPath, $html);

        // URL locale (adapte le port selon ton serveur)
        $serverUrl = 'http://127.0.0.1:8000/' . $htmlFilename;

        // ── MÉTHODE 1 : Fichier HTML local ──────────────────────────
        $io->section('📱 Méthode 1 — Scanner le QR code (recommandé)');
        $io->writeln('<fg=green>Un fichier HTML a été créé :</> ' . $htmlPath);
        $io->writeln('');
        $io->writeln('<fg=yellow;options=bold>👉 Ouvre cette URL dans ton navigateur :</>');
        $io->writeln('   <fg=cyan;options=bold>' . $serverUrl . '</>');
        $io->writeln('');
        $io->writeln('<fg=gray>(Si ton serveur tourne sur un autre port, adapte l\'URL. Ex : :8080, :80, etc.)</>');
        $io->writeln('');
        $io->writeln('<fg=gray>Ou ouvre directement le fichier :</> file://' . str_replace('\\', '/', $htmlPath));

        // ── MÉTHODE 2 : Saisie manuelle ─────────────────────────────
        $io->section('⌨️  Méthode 2 — Saisir la clé manuellement');
        $io->writeln('Dans Google Authenticator → <fg=green>+</> → <fg=green>« Saisir une clé de configuration »</>');
        $io->writeln('');
        $io->writeln('<fg=yellow>Nom du compte :</> <options=bold>MindAura (' . $user->getEmailUtilisateur() . ')</>');
        $io->writeln('<fg=yellow>Votre clé     :</>');
        $io->writeln('');
        // Afficher le secret formaté en groupes de 4 pour faciliter la saisie
        $formattedSecret = trim(chunk_split($newSecret, 4, ' '));
        $io->writeln('   <bg=yellow;fg=black;options=bold>  ' . $formattedSecret . '  </>');
        $io->writeln('');
        $io->writeln('<fg=yellow>Type de clé   :</> <options=bold>Basé sur le temps</>');

        // ── MÉTHODE 3 : URL directe du QR code ──────────────────────
        $io->section('🔗 Méthode 3 — URL directe du QR code');
        $io->writeln('<fg=gray>(Si les méthodes 1 et 2 ne fonctionnent pas)</>');
        $io->writeln($qrCodeUrl);

        // ── INSTRUCTIONS FINALES ────────────────────────────────────
        $io->section('✅ Étapes à suivre');
        $io->listing([
            'Ouvre Google Authenticator sur ton téléphone',
            'SUPPRIME l\'ancienne entrée « MindAura » (appui long → corbeille)',
            'Ajoute une nouvelle entrée avec la méthode 1 ou 2 ci-dessus',
            'Note le code à 6 chiffres qui s\'affiche',
            'Teste avec : php bin/console app:totp:debug ' . $email . ' --code=XXXXXX',
        ]);

        // ── Code attendu ────────────────────────────────────────────
        $io->section('🎯 Code attendu MAINTENANT');
        $io->writeln(sprintf(
            '   <bg=green;fg=black;options=bold>  %s  </>   <fg=gray>(à %s, valide ~30s)</>',
            $totp->now(),
            date('H:i:s')
        ));
        $io->writeln('');
        $io->writeln('<fg=gray>Après avoir scanné le QR, ton téléphone doit afficher ce même code.</>');

        // ── Rappel sécurité ─────────────────────────────────────────
        $io->newLine();
        $io->warning([
            '⚠️  SUPPRIME le fichier HTML quand tu as fini :',
            '    ' . $htmlPath,
            '',
            'Ce fichier contient ton secret 2FA en clair.',
        ]);

        return Command::SUCCESS;
    }

    /**
     * Génère le HTML du fichier local avec le QR code et les infos utiles.
     */
    private function buildQrHtml(string $firstName, string $email, string $qrUrl, string $secret): string
    {
        $formatted = trim(chunk_split($secret, 4, ' '));
        $escEmail  = htmlspecialchars($email, ENT_QUOTES, 'UTF-8');
        $escName   = htmlspecialchars($firstName, ENT_QUOTES, 'UTF-8');
        $escQr     = htmlspecialchars($qrUrl, ENT_QUOTES, 'UTF-8');
        $escSecret = htmlspecialchars($secret, ENT_QUOTES, 'UTF-8');
        $escFmt    = htmlspecialchars($formatted, ENT_QUOTES, 'UTF-8');

        return <<<HTML
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>MindAura — Configuration 2FA pour {$escEmail}</title>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700&family=Playfair+Display:wght@700&family=JetBrains+Mono:wght@700&display=swap" rel="stylesheet">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Outfit', sans-serif;
      background: linear-gradient(135deg, #1B4332 0%, #7B5EA7 100%);
      min-height: 100vh;
      display: flex; align-items: center; justify-content: center;
      padding: 20px; color: #1A1A2E;
    }
    .card {
      background: #fff;
      border-radius: 22px;
      max-width: 600px; width: 100%;
      padding: 40px;
      box-shadow: 0 32px 80px rgba(0,0,0,.3);
    }
    h1 {
      font-family: 'Playfair Display', serif;
      font-size: 1.8rem; color: #1B4332;
      margin-bottom: 6px;
    }
    .subtitle { color: #5A6475; font-size: .95rem; margin-bottom: 28px; }
    .user-badge {
      display: inline-flex; align-items: center; gap: 8px;
      background: #EBF7F0; color: #1B4332;
      padding: 6px 14px; border-radius: 999px;
      font-size: .8rem; font-weight: 600;
      margin-bottom: 24px;
    }
    .qr-wrap {
      text-align: center;
      padding: 24px;
      background: #F0F4F8;
      border-radius: 16px;
      border: 2px dashed #52B788;
      margin-bottom: 24px;
    }
    .qr-wrap img {
      display: block; margin: 0 auto 12px;
      border-radius: 12px;
      background: #fff; padding: 12px;
      box-shadow: 0 4px 14px rgba(0,0,0,.08);
    }
    .qr-wrap .hint { font-size: .8rem; color: #5A6475; }
    .divider {
      display: flex; align-items: center; gap: 12px;
      margin: 24px 0;
    }
    .divider::before, .divider::after {
      content: ''; flex: 1; height: 1px; background: #D5DCE6;
    }
    .divider span {
      color: #5A6475; font-size: .8rem; text-transform: uppercase; letter-spacing: 1px;
    }
    .secret-block {
      background: #EDE9F6; border-left: 4px solid #7B5EA7;
      padding: 18px 20px; border-radius: 10px;
      margin-bottom: 20px;
    }
    .secret-block .label {
      font-size: .75rem; font-weight: 600; color: #7B5EA7;
      text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;
    }
    .secret-value {
      font-family: 'JetBrains Mono', monospace;
      font-size: 1.2rem; font-weight: 700;
      color: #1A1A2E;
      word-break: break-all;
      user-select: all;
      cursor: pointer;
    }
    .secret-value:hover { background: rgba(123,94,167,.1); }
    .steps {
      background: #FFF3E0; border-radius: 12px;
      padding: 18px 22px; margin-top: 18px;
    }
    .steps h3 { color: #E65100; margin-bottom: 10px; font-size: .95rem; }
    .steps ol { padding-left: 20px; font-size: .88rem; line-height: 1.7; color: #1A1A2E; }
    .steps li { margin-bottom: 3px; }
    .warning {
      margin-top: 22px;
      background: #FEF2F2; border: 1px solid #FECACA;
      color: #B71C1C;
      padding: 12px 16px; border-radius: 10px;
      font-size: .82rem; line-height: 1.5;
    }
    .copy-feedback {
      position: fixed; bottom: 20px; left: 50%;
      transform: translateX(-50%) translateY(100px);
      background: #1B4332; color: #fff;
      padding: 10px 20px; border-radius: 999px;
      font-size: .85rem; font-weight: 600;
      transition: transform .3s;
    }
    .copy-feedback.show { transform: translateX(-50%) translateY(0); }
  </style>
</head>
<body>
  <div class="card">
    <h1>🔐 Configuration 2FA MindAura</h1>
    <p class="subtitle">Scanne ce QR code avec Google Authenticator pour réinitialiser ton second facteur.</p>
    <div class="user-badge">👤 {$escName} · {$escEmail}</div>

    <div class="qr-wrap">
      <img src="{$escQr}" alt="QR code 2FA MindAura" width="280" height="280"/>
      <div class="hint">📱 Ouvre Google Authenticator → + → Scanner un QR code</div>
    </div>

    <div class="divider"><span>ou saisie manuelle</span></div>

    <div class="secret-block">
      <div class="label">Clé secrète (clique pour copier)</div>
      <div class="secret-value" id="secret" title="Cliquer pour copier">{$escFmt}</div>
    </div>

    <div class="steps">
      <h3>📋 Étapes</h3>
      <ol>
        <li><strong>Supprime</strong> l'ancienne entrée MindAura dans Google Authenticator</li>
        <li><strong>Scanne</strong> le QR code ci-dessus (ou saisis la clé manuellement)</li>
        <li><strong>Vérifie</strong> que les codes générés sont maintenant valides</li>
        <li><strong>Retourne</strong> au terminal pour tester :<br/>
          <code style="background:#1A1A2E;color:#52B788;padding:2px 8px;border-radius:4px;font-family:monospace;font-size:.82rem;">php bin/console app:totp:debug {$escEmail} --code=XXXXXX</code>
        </li>
      </ol>
    </div>

    <div class="warning">
      ⚠️ <strong>Supprime ce fichier quand tu as terminé !</strong>
      Il contient ton secret 2FA en clair et ne doit pas être partagé ni committé.
    </div>
  </div>

  <div class="copy-feedback" id="feedback">✅ Clé copiée !</div>

  <script>
    document.getElementById('secret').addEventListener('click', function() {
      const raw = '{$escSecret}';
      navigator.clipboard.writeText(raw).then(() => {
        const fb = document.getElementById('feedback');
        fb.classList.add('show');
        setTimeout(() => fb.classList.remove('show'), 1800);
      });
    });
  </script>
</body>
</html>
HTML;
    }
}