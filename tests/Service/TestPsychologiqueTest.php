<?php

namespace App\Tests\Service;

use App\Entity\TestPsychologique;
use App\Service\TestPsychologiqueManager;
use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\TestCase;

class TestPsychologiqueTest extends TestCase
{
    private TestPsychologiqueManager $manager;


    protected function setUp(): void
    {
        $this->manager = new TestPsychologiqueManager();
    }

    private function validTest(): TestPsychologique
    {
        $test = new TestPsychologique();
        $test->setTitre_test('Test de stress professionnel');
        $test->setDescription_test('Évalue le niveau de stress ressenti dans un contexte de travail.');
        $test->setType_test('stress');
        $test->setDuree_estimee(30);
        $test->setInstructions_test('Répondez spontanément sans trop réfléchir.');
        $test->setEst_actif(true);

        return $test;
    }

   
    public function testValidTestPsychologique(): void
    {
        $test = $this->validTest();
        $this->assertTrue($this->manager->validate($test));
    }

  
    public function testTitreObligatoire(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le titre du test est obligatoire.');

        $test = $this->validTest();
        $test->setTitre_test('');
        $this->manager->validate($test);
    }

   
    public function testTitreTropCourt(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('au moins 3 caractères');

        $test = $this->validTest();
        $test->setTitre_test('AB');
        $this->manager->validate($test);
    }

    
    public function testTitreTropLong(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('dépasser 255 caractères');

        $test = $this->validTest();
        $test->setTitre_test(str_repeat('A', 256));
        $this->manager->validate($test);
    }

  
    public function testTitreExactementMaxAutorise(): void
    {
        $test = $this->validTest();
        $test->setTitre_test(str_repeat('X', 255));
        $this->assertTrue($this->manager->validate($test));
    }

    
    public function testTypeObligatoire(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le type de test est obligatoire.');

        $test = $this->validTest();
        $test->setType_test('');
        $this->manager->validate($test);
    }

   
    public function testTypeInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('invalide');

        $test = $this->validTest();
        $test->setType_test('inconnu');
        $this->manager->validate($test);
    }

   
    #[DataProvider('typesAutorises')]
    public function testTypesAutorises(string $type): void
    {
        $test = $this->validTest();
        $test->setType_test($type);
        $this->assertTrue($this->manager->validate($test));
    }

    public static function typesAutorises(): array
    {
        return [
            'personnalité' => ['personnalité'],
            'stress'       => ['stress'],
            'logique'      => ['logique'],
            'mémoire'      => ['mémoire'],
            'autre'        => ['autre'],
        ];
    }

 
    
    public function testDureeZeroInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('entre 1 et 1440');

        $test = $this->validTest();
        $test->setDuree_estimee(0);
        $this->manager->validate($test);
    }

 
    public function testDureeNegativeInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('entre 1 et 1440');

        $test = $this->validTest();
        $test->setDuree_estimee(-5);
        $this->manager->validate($test);
    }

    
    public function testDureeTropLongue(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('entre 1 et 1440');

        $test = $this->validTest();
        $test->setDuree_estimee(1441);
        $this->manager->validate($test);
    }

    #[DataProvider('dureesLimitesValides')]
    public function testDureesLimitesValides(int $duree): void
    {
        $test = $this->validTest();
        $test->setDuree_estimee($duree);
        $this->assertTrue($this->manager->validate($test));
    }

    public static function dureesLimitesValides(): array
    {
        return [
            '1 minute'     => [1],
            '60 minutes'   => [60],
            '1440 minutes' => [1440],
        ];
    }

    
    public function testDescriptionObligatoire(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La description du test est obligatoire.');

        $test = $this->validTest();
        $test->setDescription_test('');
        $this->manager->validate($test);
    }

    public function testDescriptionTropLongue(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('dépasser 1000 caractères');

        $test = $this->validTest();
        $test->setDescription_test(str_repeat('D', 1001));
        $this->manager->validate($test);
    }

    public function testInstructionsObligatoires(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Les instructions du test sont obligatoires.');

        $test = $this->validTest();
        $test->setInstructions_test('');
        $this->manager->validate($test);
    }

    public function testInstructionsTropLongues(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('dépasser 1000 caractères');

        $test = $this->validTest();
        $test->setInstructions_test(str_repeat('I', 1001));
        $this->manager->validate($test);
    }
}