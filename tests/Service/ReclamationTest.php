<?php

namespace App\Tests\Service;

use App\Entity\Reclamation;
use App\Service\ReclamationManager;
use PHPUnit\Framework\TestCase;

class ReclamationTest extends TestCase
{
    public function testReclamationValide(): void
    {
        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('Problème de paiement');
        $reclamation->setDescriptionReclamation('Je n\'arrive pas à finaliser mon paiement en ligne.');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setRateReclamation(4.5);

        $manager = new ReclamationManager();

        $this->assertTrue($manager->validate($reclamation));
    }

    public function testSujetVideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le sujet de la réclamation est obligatoire.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('');
        $reclamation->setDescriptionReclamation('Description suffisamment longue.');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testSujetTropCourtLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le sujet doit contenir au moins 3 caractères.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('AB');
        $reclamation->setDescriptionReclamation('Description suffisamment longue.');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testDescriptionVideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La description de la réclamation est obligatoire.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('Sujet valide');
        $reclamation->setDescriptionReclamation('');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testDescriptionTropCourteLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La description doit contenir au moins 10 caractères.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('Sujet valide');
        $reclamation->setDescriptionReclamation('C');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);

        (new ReclamationManager())->validate($reclamation);
    }


    public function testStatutInvalideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('n\'est pas valide');

        $reclamation = $this->getMockBuilder(Reclamation::class)
            ->onlyMethods(['getStatutCode'])
            ->getMock();

        $reclamation->method('getStatutCode')->willReturn('STATUT_INCONNU');
        $reclamation->setSujetReclamation('Sujet valide');
        $reclamation->setDescriptionReclamation('Description suffisamment longue.');
        $reclamation->setIdUtilisateur(1);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testTousLesStatutsValidesPassent(): void
    {
        $manager = new ReclamationManager();
        $statuts = ['EN_ATTENTE', 'EN_COURS', 'TRAITEE', 'REJETEE'];

        foreach ($statuts as $statut) {
            $reclamation = new Reclamation();
            $reclamation->setSujetReclamation('Sujet valide');
            $reclamation->setDescriptionReclamation('Description suffisamment longue.');
            $reclamation->setStatutReclamation($statut);
            $reclamation->setIdUtilisateur(1);

            $this->assertTrue(
                $manager->validate($reclamation),
                sprintf('Le statut "%s" devrait être accepté.', $statut)
            );
        }
    }

    public function testRateNegativeLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La note doit être comprise entre 0 et 5.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('Sujet valide');
        $reclamation->setDescriptionReclamation('Description suffisamment longue.');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);
        $reclamation->setRateReclamation(-1.0);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testRateSuperieure5LeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La note doit être comprise entre 0 et 5.');

        $reclamation = new Reclamation();
        $reclamation->setSujetReclamation('Sujet valide');
        $reclamation->setDescriptionReclamation('Description suffisamment longue.');
        $reclamation->setStatutReclamation('EN_ATTENTE');
        $reclamation->setIdUtilisateur(1);
        $reclamation->setRateReclamation(6.0);

        (new ReclamationManager())->validate($reclamation);
    }

    public function testRateLimitesAcceptees(): void
    {
        $manager = new ReclamationManager();

        foreach ([0.0, 2.5, 5.0] as $rate) {
            $reclamation = new Reclamation();
            $reclamation->setSujetReclamation('Sujet valide');
            $reclamation->setDescriptionReclamation('Description suffisamment longue.');
            $reclamation->setStatutReclamation('EN_ATTENTE');
            $reclamation->setIdUtilisateur(1);
            $reclamation->setRateReclamation($rate);

            $this->assertTrue(
                $manager->validate($reclamation),
                sprintf('La note %.1f devrait être acceptée.', $rate)
            );
        }
    }
}
