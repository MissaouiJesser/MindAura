<?php

namespace App\Tests\Service;

use App\Entity\Ressources;
use App\Service\RessourcesManager;
use PHPUnit\Framework\TestCase;

class RessourcesTest extends TestCase
{
    private RessourcesManager $manager;

    protected function setUp(): void
    {
        $this->manager = new RessourcesManager();
    }

    private function makeRessource(
        string  $titre      = 'Gérer son stress au quotidien',
        string  $resume     = 'Un résumé complet sur la gestion du stress.',
        string  $contenu    = 'Article',
        string  $categorie  = 'gestion_stress',
        string  $niveau     = 'DEBUTANT',
        int     $duree      = 5,
        ?string $url        = 'https://example.com/article',
        ?string $email      = null
    ): Ressources {
        $r = new Ressources();
        $r->setTitre($titre);
        $r->setResume($resume);
        $r->setContenu($contenu);
        $r->setCategorie($categorie);
        $r->setNiveau($niveau);
        $r->setDureeLecture($duree);
        $r->setUrl($url);
        if ($email !== null) {
            $r->setEmailAuteur($email);
        }
        return $r;
    }


    public function testRessourceArticleValide(): void
    {
        $r = $this->makeRessource();
        $this->assertTrue($this->manager->validate($r));
    }

    // ─────────────────────────────────────────────────
    // 2. Ressource Video valide (sans URL)
    // ─────────────────────────────────────────────────

    public function testRessourceVideoValide(): void
    {
        $r = $this->makeRessource(
            contenu: 'Video',
            url: null
        );
        $this->assertTrue($this->manager->validate($r));
    }

    public function testRessourcePdfValide(): void
    {
        $r = $this->makeRessource(
            contenu: 'PDF',
            url: null
        );
        $this->assertTrue($this->manager->validate($r));
    }

    public function testTitreVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le titre est obligatoire.');

        $r = $this->makeRessource(titre: '');
        $this->manager->validate($r);
    }

    public function testTitreEspacesSeuls(): void
    {
        $this->expectException(\InvalidArgumentException::class);

        $r = $this->makeRessource(titre: '   ');
        $this->manager->validate($r);
    }

    public function testTitreTropLong(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le titre ne doit pas dépasser 255 caractères.');

        $r = $this->makeRessource(titre: str_repeat('a', 256));
        $this->manager->validate($r);
    }

    public function testTitreExactement255Caracteres(): void
    {
        $r = $this->makeRessource(titre: str_repeat('a', 255));
        $this->assertTrue($this->manager->validate($r));
    }

    public function testResumeVide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le résumé est obligatoire.');

        $r = $this->makeRessource(resume: '');
        $this->manager->validate($r);
    }

    public function testContenuInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Type de contenu invalide.');

        $r = $this->makeRessource(contenu: 'Blog', url: null);
        $this->manager->validate($r);
    }

    public function testTousLesContenusValides(): void
    {
        foreach (['Video', 'PDF'] as $type) {
            $r = $this->makeRessource(contenu: $type, url: null);
            $this->assertTrue($this->manager->validate($r), "Le type '$type' devrait être valide.");
        }
    }

    public function testCategorieInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Catégorie invalide.');

        $r = $this->makeRessource(categorie: 'sport');
        $this->manager->validate($r);
    }

    public function testToutesLesCategoriesValides(): void
    {
        $categories = [
            'gestion_stress', 'confiance_en_soi', 'motivation',
            'communication', 'bien_etre', 'protectivite', 'intelligence_emotionnelle',
        ];

        foreach ($categories as $cat) {
            $r = $this->makeRessource(categorie: $cat);
            $this->assertTrue($this->manager->validate($r), "La catégorie '$cat' devrait être valide.");
        }
    }

    public function testNiveauInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Niveau invalide.');

        $r = $this->makeRessource(niveau: 'EXPERT');
        $this->manager->validate($r);
    }

    public function testTousLesNiveauxValides(): void
    {
        foreach (['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'] as $niveau) {
            $r = $this->makeRessource(niveau: $niveau);
            $this->assertTrue($this->manager->validate($r), "Le niveau '$niveau' devrait être valide.");
        }
    }

    public function testDureeLectureNegative(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('La durée de lecture doit être positive ou nulle.');

        $r = $this->makeRessource(duree: -1);
        $this->manager->validate($r);
    }

    public function testDureeLectureZeroAcceptee(): void
    {
        $r = $this->makeRessource(duree: 0);
        $this->assertTrue($this->manager->validate($r));
    }

    public function testUrlManquantePourArticle(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le lien est obligatoire pour un article ou podcast.');

        $r = $this->makeRessource(contenu: 'Article', url: null);
        $this->manager->validate($r);
    }

    public function testUrlManquantePourPodcast(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Le lien est obligatoire pour un article ou podcast.');

        $r = $this->makeRessource(contenu: 'Podcast', url: null);
        $this->manager->validate($r);
    }

    public function testUrlInvalidePourArticle(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage("Le lien n'est pas une URL valide.");

        $r = $this->makeRessource(contenu: 'Article', url: 'pas-une-url');
        $this->manager->validate($r);
    }

    public function testEmailAuteurInvalide(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage("L'email auteur n'est pas valide.");

        $r = $this->makeRessource(email: 'email_invalide');
        $this->manager->validate($r);
    }

    public function testEmailAuteurValide(): void
    {
        $r = $this->makeRessource(email: 'auteur@example.com');
        $this->assertTrue($this->manager->validate($r));
    }

    public function testEmailAuteurNullAccepte(): void
    {
        $r = $this->makeRessource(email: null);
        $this->assertTrue($this->manager->validate($r));
    }

    public function testIncrementerVues(): void
    {
        $r = $this->makeRessource();
        $this->assertEquals(0, $r->getNbrVues());

        $total = $this->manager->incrementerVues($r);
        $this->assertEquals(1, $total);

        $this->manager->incrementerVues($r);
        $this->assertEquals(2, $r->getNbrVues());
    }

    public function testIncrementerLikes(): void
    {
        $r = $this->makeRessource();
        $this->assertEquals(0, $r->getLikes());

        $total = $this->manager->incrementerLikes($r);
        $this->assertEquals(1, $total);
    }

    public function testVideoSansUrlEstValide(): void
    {
        $r = $this->makeRessource(contenu: 'Video', url: null);
        $this->assertTrue($this->manager->validate($r));
    }

    public function testPdfSansUrlEstValide(): void
    {
        $r = $this->makeRessource(contenu: 'PDF', url: null);
        $this->assertTrue($this->manager->validate($r));
    }
}
