<?php

namespace App\Command;

use App\Repository\RessourcesRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Style\SymfonyStyle;

#[AsCommand(name: 'app:fix-urls', description: 'Corrige les chemins Windows en chemins web')]
class FixUrlsCommand extends Command
{
    public function __construct(
        private RessourcesRepository $repo,
        private EntityManagerInterface $em
    ) {
        parent::__construct();
    }

    protected function execute(InputInterface $input, OutputInterface $output): int
    {
        $io = new SymfonyStyle($input, $output);
        $ressources = $this->repo->findAll();
        $fixed = 0;

        foreach ($ressources as $r) {
            // ── Corriger espaces dans image_url ──
            $imageUrl = $r->getImageUrl();
            if ($imageUrl && str_contains($imageUrl, ' ')) {
                $parts = explode('/', $imageUrl);
                $filename = array_pop($parts);
                $newPath = implode('/', $parts) . '/' . rawurlencode($filename);
                $r->setImageUrl($newPath);
                $io->text("Espace encodé : $imageUrl → $newPath");
                $fixed++;
            }

            // ── Corriger espaces dans url ──
            $url = $r->getUrl();
            if ($url && str_contains($url, ' ') && str_starts_with($url, '/uploads')) {
                $parts = explode('/', $url);
                $filename = array_pop($parts);
                $newPath = implode('/', $parts) . '/' . rawurlencode($filename);
                $r->setUrl($newPath);
                $fixed++;
            }
        }

        $this->em->flush();
        $io->success("$fixed chemin(s) corrigé(s) avec succès.");

        return Command::SUCCESS;
    }
}