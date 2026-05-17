<?php

namespace App\Tests\Service;

use App\Entity\Evenement;
use App\Repository\EvenementRepository;
use App\Service\EvenementStatutUpdater;
use Doctrine\ORM\EntityManagerInterface;
use PHPUnit\Framework\TestCase;


class EvenementStatutUpdaterTest extends TestCase
{
    private EvenementStatutUpdater $updater;

    protected function setUp(): void
    {
        // Les dépendances BD ne sont pas nécessaires pour computeStatut / updateOne
        $repo = $this->createMock(EvenementRepository::class);
        $em   = $this->createMock(EntityManagerInterface::class);

        $this->updater = new EvenementStatutUpdater($repo, $em);
    }



    /**
     * @param int $debutOffset Décalage en jours depuis aujourd'hui pour la date de début
     * @param int $finOffset   Décalage en jours depuis aujourd'hui pour la date de fin
     */
    private function makeEvenement(
        ?int $debutOffset = null,
        ?int $finOffset   = null,
        ?string $statut   = null
    ): Evenement {
        $e = new Evenement();
        $e->setIdentifiantEvenemnt('EVT-TEST');
        $e->setTitreEvenement('Événement de test');
        $e->setLieuEvenement('Tunis');
        $e->setCapaciteEvenement(50);

        if ($debutOffset !== null) {
            
            $debut = new \DateTimeImmutable('today');
            $debut = $debut->modify(sprintf('%+d days', $debutOffset));
            $e->setDatedebutEvenemnt($debut);
        }

        if ($finOffset !== null) {
            
            $fin = new \DateTimeImmutable('today');
            $fin = $fin->modify(sprintf('%+d days', $finOffset));
            $e->setDatefinEvenemnt($fin);
        }

        if ($statut !== null) {
            $e->setStatutEvenemnt($statut);
        }

        return $e;
    }



    public function testComputeStatutAVenir(): void
    {
        $e = $this->makeEvenement(debutOffset: 5, finOffset: 10);
        $this->assertSame('a_venir', $this->updater->computeStatut($e));
    }



    public function testComputeStatutEnCours(): void
    {
        // Débuté il y a 2 jours, se termine dans 3 jours
        $e = $this->makeEvenement(debutOffset: -2, finOffset: 3);
        $this->assertSame('en_cours', $this->updater->computeStatut($e));
    }

    
    public function testComputeStatutTermine(): void
    {
        // Débuté il y a 10 jours, terminé il y a 2 jours
        $e = $this->makeEvenement(debutOffset: -10, finOffset: -2);
        $this->assertSame('termine', $this->updater->computeStatut($e));
    }


    public function testComputeStatutDatesNulles(): void
    {
        $e = $this->makeEvenement(); // aucun offset → dates null
        $this->assertSame('a_venir', $this->updater->computeStatut($e));
    }


    public function testUpdateOneChangeSiDifferent(): void
    {
        $e = $this->makeEvenement(debutOffset: -5, finOffset: -1, statut: 'a_venir');
        $this->updater->updateOne($e);
        $this->assertSame('termine', $e->getStatutEvenemnt());
    }

  

    public function testUpdateOneNeModifieJamaisAnnule(): void
    {
        $e = $this->makeEvenement(debutOffset: -5, finOffset: -1, statut: 'annule');
        $this->updater->updateOne($e);
        $this->assertSame('annule', $e->getStatutEvenemnt());
    }



    public function testUpdateOneNeChangePasStatutDejaCorrect(): void
    {
        $e = $this->makeEvenement(debutOffset: 2, finOffset: 5, statut: 'a_venir');
        $this->updater->updateOne($e);
        $this->assertSame('a_venir', $e->getStatutEvenemnt());
    }

   

    public function testGetLabelAVenir(): void
    {
        $this->assertSame('À venir', EvenementStatutUpdater::getLabel('a_venir'));
    }

    public function testGetLabelEnCours(): void
    {
        $this->assertSame('En cours', EvenementStatutUpdater::getLabel('en_cours'));
    }

    public function testGetLabelTermine(): void
    {
        $this->assertSame('Terminé', EvenementStatutUpdater::getLabel('termine'));
    }

    public function testGetLabelAnnule(): void
    {
        $this->assertSame('Annulé', EvenementStatutUpdater::getLabel('annule'));
    }



    public function testGetBadgeClassAVenir(): void
    {
        $this->assertSame('bg-primary', EvenementStatutUpdater::getBadgeClass('a_venir'));
    }

    public function testGetBadgeClassEnCours(): void
    {
        $this->assertSame('bg-success', EvenementStatutUpdater::getBadgeClass('en_cours'));
    }

    public function testGetBadgeClassTermine(): void
    {
        $this->assertSame('bg-secondary', EvenementStatutUpdater::getBadgeClass('termine'));
    }

    public function testGetBadgeClassAnnule(): void
    {
        $this->assertSame('bg-danger', EvenementStatutUpdater::getBadgeClass('annule'));
    }

   

    public function testGetLabelInconnu(): void
    {
        $this->assertSame('Inconnu', EvenementStatutUpdater::getLabel('xyz'));
    }

    public function testGetBadgeClassInconnu(): void
    {
        $this->assertSame('bg-dark', EvenementStatutUpdater::getBadgeClass('xyz'));
    }


    public function testComputeStatutFinAujourdhui(): void
    {
        $e = $this->makeEvenement(debutOffset: -3, finOffset: 0);
        $this->assertSame('en_cours', $this->updater->computeStatut($e));
    }
}