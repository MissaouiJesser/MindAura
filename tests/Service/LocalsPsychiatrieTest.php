<?php

namespace App\Tests\Service;

use App\Entity\LocalsPsychiatrie;
use App\Service\LocalsPsychiatrieManager;
use PHPUnit\Framework\Attributes\CoversNothing;
use PHPUnit\Framework\TestCase;

#[CoversNothing]
class LocalsPsychiatrieTest extends TestCase
{
    private function makeValidLocal(): LocalsPsychiatrie
    {
        $local = new LocalsPsychiatrie();
        $local->setNomLocal('Cabinet Dr. Ben Ali');
        $local->setAdresseLocal('12 Rue de la République');
        $local->setVilleLocal('Tunis');
        $local->setDescriptionLocal('Un cabinet moderne pour consultations psychiatriques.');
        $local->setCapaciteLocal('20');
        $local->setTypeLocal('Cabinet');
        $local->setTelephoneLocal(20123456);
        $local->setEmailLocal('contact@cabinet.tn');
        $local->setDisponibiliteLocal('Disponible');
        return $local;
    }

    private function makeManager(): LocalsPsychiatrieManager
    {
        return new LocalsPsychiatrieManager();
    }

    public function testLocalValidePasseLaValidation(): void
    {
        $this->assertTrue($this->makeManager()->validate($this->makeValidLocal()));
    }

    public function testNomVideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le nom du local est obligatoire.');

        $local = $this->makeValidLocal();
        $local->setNomLocal('   ');
        $this->makeManager()->validate($local);
    }

    public function testNomTropCourtLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le nom doit contenir au moins 3 caractères.');

        $local = $this->makeValidLocal();
        $local->setNomLocal('AB');
        $this->makeManager()->validate($local);
    }

    public function testNomValidePasseLaValidation(): void
    {
        $local = $this->makeValidLocal();
        $local->setNomLocal('Clinique El Amal');

        $this->assertTrue($this->makeManager()->validate($local));
    }

    public function testAdresseVideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage("L'adresse est obligatoire.");

        $local = $this->makeValidLocal();
        $local->setAdresseLocal('');
        $this->makeManager()->validate($local);
    }

    public function testAdresseTropCourteLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage("L'adresse doit contenir au moins 5 caractères.");

        $local = $this->makeValidLocal();
        $local->setAdresseLocal('Rue');
        $this->makeManager()->validate($local);
    }

    public function testAdresseValidePasseLaValidation(): void
    {
        $local = $this->makeValidLocal();
        $local->setAdresseLocal('Avenue Habib Bourguiba, Tunis');

        $this->assertTrue($this->makeManager()->validate($local));
    }

    public function testDisponibiliteInvalideLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La disponibilité doit être "Disponible" ou "Indisponible".');

        $local = $this->makeValidLocal();
        $local->setDisponibiliteLocal('Peut-être');
        $this->makeManager()->validate($local);
    }

    public function testDisponibiliteDisponibleEstValide(): void
    {
        $local = $this->makeValidLocal();
        $local->setDisponibiliteLocal('Disponible');

        $this->assertTrue($this->makeManager()->validate($local));
    }

    public function testDisponibiliteIndisponibleEstValide(): void
    {
        $local = $this->makeValidLocal();
        $local->setDisponibiliteLocal('Indisponible');

        $this->assertTrue($this->makeManager()->validate($local));
    }

    public function testTelephoneNullLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le numéro de téléphone est obligatoire.');

        $local = $this->makeValidLocal();
        $local->setTelephoneLocal(null);
        $this->makeManager()->validate($local);
    }

    public function testTelephoneTropCourtLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le numéro de téléphone doit contenir 8 chiffres');

        $local = $this->makeValidLocal();
        $local->setTelephoneLocal(1234567); // 7 chiffres
        $this->makeManager()->validate($local);
    }

    public function testTelephoneTropLongLeveException(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le numéro de téléphone doit contenir 8 chiffres');

        $local = $this->makeValidLocal();
        $local->setTelephoneLocal(123456789); // 9 chiffres
        $this->makeManager()->validate($local);
    }

    public function testTelephoneValidePasseLaValidation(): void
    {
        $local = $this->makeValidLocal();
        $local->setTelephoneLocal(55667788);

        $this->assertTrue($this->makeManager()->validate($local));
    }
}