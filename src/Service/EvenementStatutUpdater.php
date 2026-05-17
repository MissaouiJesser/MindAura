<?php
namespace App\Service;

use App\Entity\Evenement;
use App\Repository\EvenementRepository;
use Doctrine\ORM\EntityManagerInterface;

class EvenementStatutUpdater
{
    private const BATCH_SIZE = 50;

    public function __construct(
        private EvenementRepository $evenementRepository,
        private EntityManagerInterface $em
    ) {}

    public function computeStatut(Evenement $evenement): string
    {
        $today = new \DateTime('today');
        $debut = $evenement->getDatedebutEvenemnt();
        $fin   = $evenement->getDatefinEvenemnt();

        if ($debut === null || $fin === null) {
            return 'a_venir';
        }

        $debut = \DateTime::createFromFormat('Y-m-d', $debut->format('Y-m-d'));
        $fin   = \DateTime::createFromFormat('Y-m-d', $fin->format('Y-m-d'));

        if ($debut > $today) {
            return 'a_venir';
        }

        if ($fin < $today) {
            return 'termine';
        }

        return 'en_cours';
    }

    public function updateOne(Evenement $evenement): void
    {
        if ($evenement->getStatutEvenemnt() === 'annule') {
            return;
        }

        $newStatut = $this->computeStatut($evenement);

        if ($evenement->getStatutEvenemnt() !== $newStatut) {
            $evenement->setStatutEvenemnt($newStatut);
        }
    }

    public function updateAll(): void
    {
        $lockFile = sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'statut_update_' . date('Y-m-d') . '.lock';

        $oldLocks = glob(sys_get_temp_dir() . DIRECTORY_SEPARATOR . 'statut_update_*.lock');
        if ($oldLocks !== false) {
            foreach ($oldLocks as $old) {
                if ($old !== $lockFile) {
                    @unlink($old);
                }
            }
        }

        if (file_exists($lockFile)) {
            return;
        }

        file_put_contents($lockFile, date('Y-m-d H:i:s'));

        try {
            $evenements = $this->evenementRepository->findForStatusUpdate();

            $i = 0;
            foreach ($evenements as $evenement) {
                $this->updateOne($evenement);
                $i++;

                if ($i % self::BATCH_SIZE === 0) {
                    $this->em->flush();
                    $this->em->clear();
                }
            }

            $this->em->flush();
            $this->em->clear();

        } catch (\Throwable $e) {
            @unlink($lockFile);
            throw $e;
        }

        @unlink($lockFile);
    }

    public static function getLabel(string $statut): string
    {
        return match ($statut) {
            'a_venir'  => 'À venir',
            'en_cours' => 'En cours',
            'termine'  => 'Terminé',
            'annule'   => 'Annulé',
            default    => 'Inconnu',
        };
    }

    public static function getBadgeClass(string $statut): string
    {
        return match ($statut) {
            'a_venir'  => 'bg-primary',
            'en_cours' => 'bg-success',
            'termine'  => 'bg-secondary',
            'annule'   => 'bg-danger',
            default    => 'bg-dark',
        };
    }
}